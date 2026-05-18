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

    private final int MIN_OBSERVATION_TO_CONSIDER = PenaltyKeyManager.MIN_OBSERVATIONS_SPECIAL;

    private final String countsFile;
    private final Map<PenaltyGroupKey, Double> averageCountsPerGroup =  new HashMap<>();
    private final IdMap<Link, Float> allLinks =  new IdMap<>(Link.class);
    private final IdMap<Link, PenaltyGroupKey> roadGroups = new IdMap<>(Link.class);
    private final LinkCategorizer categorizer;
    private final PenaltyKeyManager penaltyKeyManager;
    private final Map<PenaltyGroupKey, Integer> numCountsByGroup = new HashMap<>();

    public CountsProcessor(Network network, NetworkCalibrationConfigGroup config,
                           OutputDirectoryHierarchy outputHierarchy, LinkCategorizer categorizer,
                           PenaltyKeyManager penaltyKeyManager) {
        this.countsFile = config.getCountsFile();
        this.categorizer = categorizer;
        this.penaltyKeyManager = penaltyKeyManager;

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
        File inputFile = new File(countsFile);
        if (!inputFile.exists()) {
            throw new IOException("Counts file " + countsFile + " does not exist.");
        }

        averageCountsPerGroup.clear();
        numCountsByGroup.clear();
        allLinks.clear();

        CsvMapper mapper = new CsvMapper();
        CsvSchema taskSchema = mapper.typedSchemaFor(CountPoint.class).withHeader().withColumnSeparator(',').withComments()
                .withColumnReordering(true);

        MappingIterator<CountPoint> taskIterator = mapper.readerWithTypedSchemaFor(CountPoint.class).with(taskSchema)
                .readValues(inputFile);

        List<CountPoint> counts = taskIterator.readAll();

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

    public Double getAverageCountForGroup(PenaltyGroupKey key) {
        if (numCountsByGroup.containsKey(key) && numCountsByGroup.get(key) < MIN_OBSERVATION_TO_CONSIDER) {
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

    static public class CountPoint {
        @JsonProperty("linkId")
        @JsonAlias({"link", "link_id", "linkId"})
        public String linkId;

        @JsonProperty("count")
        @JsonAlias({"count", "counts", "Count"})
        public double count;

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
}
