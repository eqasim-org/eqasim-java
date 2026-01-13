package org.eqasim.core.components.network_calibration.Processors;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.components.network_calibration.NetworkCalibrationConfigGroup;
import org.eqasim.core.components.network_calibration.NetworkCalibrationUtils;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.controler.OutputDirectoryHierarchy;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CountsProcessor {
    private static final Logger logger = LogManager.getLogger(CountsProcessor.class);

    private final String countsFile;
    private final String averageCountsPerCategoryFile;
    private final Network network;
    private final NetworkCalibrationConfigGroup config;
    private final Map<Integer, Double> AverageCountsPerCategory =  new HashMap<>();
    private final Set<Id<Link>> allLinks =  new java.util.HashSet<>();
    private final IdMap<Link, Integer> roadCategories = new IdMap<>(Link.class);
    private final boolean hasCountsFile;
    private final boolean hasAverageCountsPerCategoryFile;
    private final OutputDirectoryHierarchy outputHierarchy;

    public CountsProcessor(Network network, NetworkCalibrationConfigGroup config,
                           OutputDirectoryHierarchy outputHierarchy) {
        this.hasCountsFile = config.hasCountsFile();
        this.hasAverageCountsPerCategoryFile = config.hasAverageCountsPerCategoryFile();
        // check that at least one of the files is provided
        if (!hasCountsFile && !hasAverageCountsPerCategoryFile) {
            throw new IllegalArgumentException("Either counts file or average counts per category file must be provided.");
        }
        // csv files that contain counts and average counts per category
        this.countsFile = config.getCountsFile();
        this.averageCountsPerCategoryFile = config.getAverageCountsPerCategoryFile();
        // network and output
        this.network = network;
        this.outputHierarchy = outputHierarchy;
        this.config = config;

        // set whether we separate urban and rural links
        NetworkCalibrationUtils.setSeparateUrban(config.getSeparateUrbanRoads());
        try {
            build();
            saveAverageCounts();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void build() throws IOException {
        // init road categories
        for (Link link : network.getLinks().values()) {
            int category = NetworkCalibrationUtils.getCategory(link);
            roadCategories.put(link.getId(), category);
        }
        // read counts file and process
        if (hasCountsFile) {
            readCounts();
        } else {
            readAverageCountsPerCategory();
        }
    }

    public Integer getLinkCategory(Id<Link> linkId) {
        return roadCategories.getOrDefault(linkId, null);
    }

    private void readCounts() throws IOException {
        // Read counts file
        File inputFile = new File(countsFile);
        if (!inputFile.exists()) {
            throw new IOException("Counts file " + countsFile + " does not exist.");
        }

        CsvMapper mapper = new CsvMapper();
        CsvSchema taskSchema = mapper.typedSchemaFor(CountPoint.class).withHeader().withColumnSeparator(',').withComments()
                .withColumnReordering(true);

        MappingIterator<CountPoint> taskIterator = mapper.readerWithTypedSchemaFor(CountPoint.class).with(taskSchema)
                .readValues(inputFile);

        List<CountPoint> counts = taskIterator.readAll();

        // assign average counts to each category
        Map<Integer, Integer> categoryCounts = new HashMap<>();

        for (CountPoint point : counts) {
            Id<Link> linkId = Id.create(point.linkId, Link.class);
            Integer category = getLinkCategory(linkId);
            if (category == null) {
                throw new IOException("Link with ID " + point.linkId + " not found in the network.");
            }
            if (category != NetworkCalibrationUtils.UNKNOWN_CATEGORY) {
                AverageCountsPerCategory.put(category, AverageCountsPerCategory.getOrDefault(category, 0.0) + point.count);
                categoryCounts.put(category, categoryCounts.getOrDefault(category, 0) + 1);
                //  register the links
                allLinks.add(linkId);
            }
        }

        // Compute averages
        for (Map.Entry<Integer, Double> entry : AverageCountsPerCategory.entrySet()) {
            int category = entry.getKey();
            AverageCountsPerCategory.put(category, entry.getValue() / categoryCounts.get(category));
            logger.info("Average traffic count for category {}: {}", category, AverageCountsPerCategory.get(category));
        }
    }

    private void readAverageCountsPerCategory() throws IOException {
        // Read average counts per category file
        File inputFile = new File(averageCountsPerCategoryFile);
        if (!inputFile.exists()) {
            throw new IOException("Average counts per category file " + averageCountsPerCategoryFile + " does not exist.");
        }

        CsvMapper mapper = new CsvMapper();
        CsvSchema taskSchema = mapper.typedSchemaFor(OutputPoint.class).withHeader().withColumnSeparator(',').withComments()
                .withColumnReordering(true);
        MappingIterator<OutputPoint> taskIterator = mapper.readerWithTypedSchemaFor(OutputPoint.class).with(taskSchema)
                .readValues(inputFile);
        List<OutputPoint> averageCounts = taskIterator.readAll();
        for (OutputPoint point : averageCounts) {
            int category = Integer.parseInt(point.category);
            AverageCountsPerCategory.put(category, point.averageCount);
            logger.info("Average traffic count for category {}: {}", category, AverageCountsPerCategory.get(category));
        }
    }

    public boolean contains(Id<Link> linkId) {
        return hasCountsFile ? allLinks.contains(linkId): true; // if counts file is not provided, assume all links are contained
    }

    public Double getAverageCountForCategory(int category) {
        return AverageCountsPerCategory.getOrDefault(category, 0.0);
    }

    private void saveAverageCounts() throws IOException, JsonGenerationException, JsonMappingException {
        // Save average counts per category to output directory
        String fileName =  "target_counts_per_category.csv";
        File outputFile = new File(fileName);

        CsvMapper mapper = new CsvMapper();
        CsvSchema schema = mapper.schemaFor(OutputPoint.class).withHeader().withColumnSeparator(';');

        try (SequenceWriter writer = mapper.writer(schema).writeValues(outputFile)) {
            for (Map.Entry<Integer, Double> entry : AverageCountsPerCategory.entrySet()) {
                OutputPoint point = new OutputPoint();
                point.category = String.valueOf(entry.getKey());
                point.averageCount = entry.getValue();
                writer.write(point);
            }
        }

        logger.info("Average counts per category saved to {}", outputFile.getAbsolutePath());
    }

    static public class CountPoint {
        @JsonProperty("linkId")
        public String linkId;

        @JsonProperty("count")
        public double count;

    }

    static public class OutputPoint {
        @JsonProperty("category")
        public String category;

        @JsonProperty("averageCount")
        public double averageCount;

    }

}
