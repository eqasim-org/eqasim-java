package org.eqasim.core.components.network_calibration.cost_calibration;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.components.network_calibration.LinkCategorizer;
import org.eqasim.core.components.network_calibration.NetworkCalibrationConfigGroup;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Maintains mapping from real penalty keys to calibration keys.
 */
public class PenaltyKeyManager {
    private static final Logger logger = LogManager.getLogger(PenaltyKeyManager.class);
    public static final int MIN_OBSERVATIONS_URBAN_RURAL = 10;
    public static final int MIN_OBSERVATIONS_SPECIAL = 5;

    private final Map<PenaltyGroupKey, PenaltyGroupKey> keyMapping = new HashMap<>();
    private final LinkCategorizer categorizer;

    public PenaltyKeyManager(NetworkCalibrationConfigGroup config, Network network, LinkCategorizer categorizer) {
        this.categorizer = categorizer;

        boolean isPenaltyCalibrationEnabled = config.isActivated()
                && config.isCalibrationEnabled()
                && config.isOneOfObjectives("penalty");

        if (!isPenaltyCalibrationEnabled || !config.hasCountsFile()) {
            logger.info("Penalty key mapping is disabled (calibration={}, hasCountsFile={}). Using identity mapping.",
                    isPenaltyCalibrationEnabled, config.hasCountsFile());
            return;
        }

        Set<PenaltyGroupKey> allRealKeys = new HashSet<>();
        Map<Id<Link>, PenaltyGroupKey> realKeyByLinkId = new HashMap<>();

        for (Link link : network.getLinks().values()) {
            PenaltyGroupKey key = categorizer.getPenaltyGroupKey(link);
            if (key != null) {
                allRealKeys.add(key);
                realKeyByLinkId.put(link.getId(), key);
            }
        }

        Map<PenaltyGroupKey, Integer> observationsByKey = readObservationsByRealKey(config.getCountsFile(), network, realKeyByLinkId);

        // Identity mapping by default.
        for (PenaltyGroupKey key : allRealKeys) {
            if (key != null) {
                keyMapping.put(key, key);
            }
        }

        mergeSparseSpecialGroups(observationsByKey);
        mergeSparseUrbanRuralGroups(observationsByKey);
        flattenMappings();

        logger.info("Initialized penalty key mapping for {} real groups ({} calibration groups).",
            keyMapping.size(), getCalibrationKeys(allRealKeys).size());
    }

    public PenaltyGroupKey toCalibrationKey(Link link) {
        if (link == null) {
            return null;
        }
        PenaltyGroupKey realKey = categorizer.getPenaltyGroupKey(link);
        return toCalibrationKey(realKey);
    }

    public PenaltyGroupKey toCalibrationKey(PenaltyGroupKey realKey) {
        if (realKey == null) {
            return null;
        }

        if (keyMapping.isEmpty()) {
            return realKey;
        }

        return keyMapping.getOrDefault(realKey, realKey);
    }

    public Set<PenaltyGroupKey> getCalibrationKeys(Set<PenaltyGroupKey> realKeys) {
        Set<PenaltyGroupKey> keys = new HashSet<>();
        for (PenaltyGroupKey realKey : realKeys) {
            PenaltyGroupKey mapped = toCalibrationKey(realKey);
            if (mapped != null) {
                keys.add(mapped);
            }
        }
        return keys;
    }

    private void mergeSparseSpecialGroups(Map<PenaltyGroupKey, Integer> observationsByKey) {
        for (PenaltyGroupKey key : new HashSet<>(keyMapping.keySet())) {
            if (key.getSpecialRegion() == 0) {
                continue;
            }

            int observations = observationsByKey.getOrDefault(key, 0);
            if (observations >= MIN_OBSERVATIONS_SPECIAL) {
                continue;
            }

            PenaltyGroupKey fallback = new PenaltyGroupKey(key.getLinkCategory(), key.isUrban(), 0);
            keyMapping.put(key, fallback);
            logger.info("Merging sparse special region group {} ({} observations) into fallback group {}.",
                    key, observations, fallback);
        }
    }

    private void mergeSparseUrbanRuralGroups(Map<PenaltyGroupKey, Integer> observationsByKey) {
        for (int linkCategory = 1; linkCategory <= 5; linkCategory++) {
            PenaltyGroupKey rural = new PenaltyGroupKey(linkCategory, false, 0);
            PenaltyGroupKey urban = new PenaltyGroupKey(linkCategory, true, 0);

            int ruralCount = getMappedObservationCount(rural, observationsByKey);
            int urbanCount = getMappedObservationCount(urban, observationsByKey);

            if (ruralCount < MIN_OBSERVATIONS_URBAN_RURAL || urbanCount < MIN_OBSERVATIONS_URBAN_RURAL) {
                keyMapping.put(urban, rural);
                logger.info("Merging sparse urban/rural groups for link category {}: urban ({} observations) merged into rural ({} observations).",
                        linkCategory, urbanCount, ruralCount);
            }
        }
    }

    private int getMappedObservationCount(PenaltyGroupKey calibrationKey, Map<PenaltyGroupKey, Integer> observationsByKey) {
        int count = observationsByKey.getOrDefault(calibrationKey, 0);
        for (Map.Entry<PenaltyGroupKey, PenaltyGroupKey> entry : keyMapping.entrySet()) {
            PenaltyGroupKey real = entry.getKey();
            PenaltyGroupKey mapped = resolveCalibrationKey(real);
            if (!real.equals(calibrationKey) && calibrationKey.equals(mapped)) {
                count += observationsByKey.getOrDefault(real, 0);
            }
        }
        return count;
    }

    private PenaltyGroupKey resolveCalibrationKey(PenaltyGroupKey realKey) {
        PenaltyGroupKey mapped = keyMapping.getOrDefault(realKey, realKey);
        while (!mapped.equals(keyMapping.getOrDefault(mapped, mapped))) {
            mapped = keyMapping.getOrDefault(mapped, mapped);
        }
        return mapped;
    }

    private void flattenMappings() {
        for (PenaltyGroupKey real : new HashSet<>(keyMapping.keySet())) {
            keyMapping.put(real, resolveCalibrationKey(real));
        }
    }

    public Map<PenaltyGroupKey, PenaltyGroupKey> getKeyMapping() {
        return new HashMap<>(keyMapping);
    }

    private Map<PenaltyGroupKey, Integer> readObservationsByRealKey(String countsFile,
                                                                     Network network,
                                                                     Map<Id<Link>, PenaltyGroupKey> realKeyByLinkId) {
        File inputFile = new File(countsFile);
        if (!inputFile.exists()) {
            throw new IllegalArgumentException("Counts file " + countsFile + " does not exist.");
        }

        try {
            CsvMapper mapper = new CsvMapper();
            CsvSchema schema = mapper.typedSchemaFor(CountPoint.class)
                    .withHeader()
                    .withColumnSeparator(',')
                    .withComments()
                    .withColumnReordering(true);

            MappingIterator<CountPoint> iterator = mapper.readerWithTypedSchemaFor(CountPoint.class)
                    .with(schema)
                    .readValues(inputFile);

            List<CountPoint> points = iterator.readAll();
            Map<PenaltyGroupKey, Integer> observationsByKey = new HashMap<>();

            for (CountPoint point : points) {
                if (point.count <= 0.0) {
                    continue;
                }

                Id<Link> linkId = Id.create(point.linkId, Link.class);
                PenaltyGroupKey realKey = realKeyByLinkId.get(linkId);

                if (realKey == null) {
                    if (!network.getLinks().containsKey(linkId)) {
                        throw new IllegalArgumentException("Link with ID '" + point.linkId + "' not found in the network.");
                    }
                    // we ignore it, if it doesn't have a key, probably, it is a pt link
                    continue;
                }

                observationsByKey.put(realKey, observationsByKey.getOrDefault(realKey, 0) + 1);
            }

            return observationsByKey;
        } catch (Exception e) {
            throw new RuntimeException("Error reading penalty observations from counts file: " + countsFile, e);
        }
    }

    private static class CountPoint {
        @JsonProperty("linkId")
        @JsonAlias({"link", "link_id", "linkId"})
        public String linkId;

        @JsonProperty("count")
        @JsonAlias({"count", "counts", "Count"})
        public double count;
    }
}
