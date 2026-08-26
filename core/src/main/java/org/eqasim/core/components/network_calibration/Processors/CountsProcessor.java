package org.eqasim.core.components.network_calibration.Processors;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.components.network_calibration.LinkCategorizer;
import org.eqasim.core.components.network_calibration.NetworkCalibrationConfigGroup;
import org.eqasim.core.components.network_calibration.cost_calibration.PenaltyGroupKey;
import org.eqasim.core.components.network_calibration.cost_calibration.PenaltyKeyManager;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.controler.OutputDirectoryHierarchy;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class CountsProcessor {
    private static final Logger logger = LogManager.getLogger(CountsProcessor.class);

    private final int minObservationsToConsider;

    private final String countsFile;
    private final Map<PenaltyGroupKey, Double> averageCountsPerGroup =  new HashMap<>();
    private final IdMap<Link, Float> allLinks =  new IdMap<>(Link.class);
    private final IdMap<Link, Float> weights =  new IdMap<>(Link.class);
    private final IdMap<Link, PenaltyGroupKey> roadGroups = new IdMap<>(Link.class);
    private final LinkCategorizer categorizer;
    private final PenaltyKeyManager penaltyKeyManager;
    private final Map<PenaltyGroupKey, Integer> numCountsByGroup = new HashMap<>();

    public CountsProcessor(Network network, NetworkCalibrationConfigGroup config,
                           OutputDirectoryHierarchy outputHierarchy, LinkCategorizer categorizer,
                           PenaltyKeyManager penaltyKeyManager, int minObservationsToConsider) {
        this.countsFile = config.getCountsFile();
        this.categorizer = categorizer;
        this.penaltyKeyManager = penaltyKeyManager;
        this.minObservationsToConsider = minObservationsToConsider;

        if (!config.hasCountsFile()) {
            throw new IllegalArgumentException("countsFile must be provided for penalty calibration.");
        }

        try {
            initRoadGroups(network);
            readCounts(network);
            saveAverageCounts();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public PenaltyGroupKey getRealLinkGroup(Id<Link> linkId) {
        return roadGroups.getOrDefault(linkId, null);
    }

    public PenaltyGroupKey getLinkGroup(Id<Link> linkId) {
        return mapToCalibrationGroup(getRealLinkGroup(linkId));
    }

    public PenaltyGroupKey mapToCalibrationGroup(PenaltyGroupKey realKey) {
        return penaltyKeyManager.toCalibrationKey(realKey);
    }

    private void readCounts(Network network) throws IOException {
        // Read counts file
        List<CountPoint> counts = readCounts(countsFile);

        averageCountsPerGroup.clear();
        numCountsByGroup.clear();
        allLinks.clear();
        weights.clear();

        // Assign counts directly to mapped calibration groups.
        for (CountPoint point : counts) {
            Id<Link> linkId = Id.create(point.linkId, Link.class);
            PenaltyGroupKey realGroupKey = getRealLinkGroup(linkId);
            if (realGroupKey == null) {
                // check whether the link is missing in the network
                if (!network.getLinks().containsKey(linkId)) {
                    throw new IOException("Link with ID '" + point.linkId + "' not found in the network.");
                } else {
                    // we ignore it, since it might be a pt link
                    continue;
                }
            }

            PenaltyGroupKey groupKey = mapToCalibrationGroup(realGroupKey);
            if (groupKey == null) {
                continue;
            }

            if (point.count > 0.0) {
                averageCountsPerGroup.put(groupKey, averageCountsPerGroup.getOrDefault(groupKey, 0.0) + point.count);
                numCountsByGroup.put(groupKey, numCountsByGroup.getOrDefault(groupKey, 0) + 1);
                //  register the links
                allLinks.put(linkId,(float) point.count);
                weights.put(linkId, (float) point.weight);
            }
        }

        // Compute averages
        for (Map.Entry<PenaltyGroupKey, Double> entry : averageCountsPerGroup.entrySet()) {
            PenaltyGroupKey key = entry.getKey();
            averageCountsPerGroup.put(key, entry.getValue() / numCountsByGroup.get(key));
            logger.info("Average traffic count for mapped group {}: {}", key, averageCountsPerGroup.get(key));
        }
    }

    public boolean contains(Id<Link> linkId) {
        return allLinks.containsKey(linkId);
    }

    /** All links with a valid positive count, exposed as a read-only view. */
    public Set<Id<Link>> linkIds() {
        return Collections.unmodifiableSet(allLinks.keySet());
    }

    public Double getAverageCountForGroup(PenaltyGroupKey key) {
        if (numCountsByGroup.containsKey(key) && numCountsByGroup.get(key) < minObservationsToConsider) {
            return Double.NaN;
        }
        return averageCountsPerGroup.getOrDefault(key, 0.0);
    }

    public Set<PenaltyGroupKey> getGroups() {
        return penaltyKeyManager.getCalibrationKeys(averageCountsPerGroup.keySet());
    }

    private void saveAverageCounts() throws IOException, JsonGenerationException, JsonMappingException {
        // Save average counts per penalty group.
        String fileName =  "target_counts_per_penalty_group.csv";
        File outputFile = new File(fileName);

        CsvMapper mapper = new CsvMapper();
        CsvSchema schema = mapper.schemaFor(OutputPoint.class).withHeader().withColumnSeparator(';');

        try (SequenceWriter writer = mapper.writer(schema).writeValues(outputFile)) {
            for (Map.Entry<PenaltyGroupKey, Double> entry : averageCountsPerGroup.entrySet()) {
                PenaltyGroupKey key = entry.getKey();
                int n = numCountsByGroup.getOrDefault(key, 0);
                if (n <= 0) {
                    continue;
                }

                OutputPoint point = new OutputPoint();
                point.linkCategory = key.getLinkCategory();
                point.urban = key.isUrban();
                point.specialRegion = key.getSpecialRegion();
                point.averageCount = entry.getValue();
                writer.write(point);
            }
        }

        logger.info("Average counts per penalty group saved to {}", outputFile.getAbsolutePath());
    }

    public static List<CountPoint> readCounts(String filename) throws IOException {
        // Read counts file
        File inputFile = new File(filename);
        if (!inputFile.exists()) {
            throw new IOException("Counts file " + filename + " does not exist.");
        }

        CsvMapper mapper = new CsvMapper();
        CsvSchema bootstrapSchema = CsvSchema.emptySchema()
                .withHeader()
                .withColumnSeparator(',')
                .withComments();

        MappingIterator<CountPoint> taskIterator = mapper.readerFor(CountPoint.class)
                .with(bootstrapSchema)
                .readValues(inputFile);

        List<CountPoint> counts = taskIterator.readAll();
        for (CountPoint point : counts) {
            if (point.weight < 1e-3) {
                point.weight = 1.0;
            }
        }
        return counts;
    }

    public static class CountPoint {
        @JsonProperty("linkId")
        @JsonAlias({"link", "link_id", "linkId"})
        public String linkId;

        @JsonProperty("count")
        @JsonAlias({"count", "counts", "Count"})
        public double count;

        @JsonProperty("weight")
        @JsonAlias({"weight", "weights", "Weight"})
        public double weight;

    }

    static public class OutputPoint {
        @JsonProperty("linkCategory")
        public int linkCategory;

        @JsonProperty("isUrban")
        public boolean urban;

        @JsonProperty("specialRegion")
        public int specialRegion;

        @JsonProperty("averageCount")
        public double averageCount;

    }

    private void initRoadGroups(Network network){
        for (Link link : network.getLinks().values()) {
            PenaltyGroupKey key = categorizer.getPenaltyGroupKey(link);
            roadGroups.put(link.getId(), key);
        }
    }

    public int size(){
        return allLinks.size();
    }

    public float getLinkCounts(Id<Link> linkId) {
        return allLinks.getOrDefault(linkId,-1.0F);
    }

    public float getWeight(Id<Link> linkId) {
        return weights.getOrDefault(linkId, 1.0F);
    }

    public float getPercentile(double percentile) {
        if (percentile < 0 || percentile > 100) {
            throw new IllegalArgumentException("Percentile must be between 0 and 100.");
        }
        if (allLinks.isEmpty()) {
            return 0.0F;
        }

        List<Float> values = new ArrayList<>(allLinks.values());
        Collections.sort(values);

        int n = values.size();
        if (n == 1) {
            return values.getFirst();
        }

        // Rank position (0-based) corresponding to the requested percentile
        double rank = (percentile / 100.0) * (n - 1);
        int lowIndex = (int) Math.floor(rank);
        int highIndex = (int) Math.ceil(rank);

        if (lowIndex == highIndex) {
            return values.get(lowIndex);
        }

        // Linear interpolation between the two surrounding values
        double fraction = rank - lowIndex;
        float low = values.get(lowIndex);
        float high = values.get(highIndex);
        return (float) (low + fraction * (high - low));
    }
}
