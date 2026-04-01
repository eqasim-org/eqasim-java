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

    private final int MIN_OBSERVATIONS_RURAL_VS_URBAN = 15;
    private final int MIN_OBSERVATIONS_SPECIAL_REST = 7;
    private final int MIN_OBSERVATION_TO_CONSIDER = 5;

    private final String countsFile;
    private final String averageCountsPerCategoryFile;
    private final Network network;
    private final Map<Integer, Double> AverageCountsPerCategory =  new HashMap<>();
    private final IdMap<Link, Float> allLinks =  new IdMap<>(Link.class);
    private final IdMap<Link, Integer> roadCategories = new IdMap<>(Link.class);
    private final boolean hasCountsFile;
    private final LinkCategorizer categorizer;
    private final Map<Integer, Integer> numCountsByCategory = new HashMap<>();

    public CountsProcessor(Network network, NetworkCalibrationConfigGroup config,
                           OutputDirectoryHierarchy outputHierarchy, LinkCategorizer categorizer) {
        this.hasCountsFile = config.hasCountsFile();
        this.averageCountsPerCategoryFile = config.getAverageCountsPerCategoryFile();
        this.countsFile = config.getCountsFile();
        this.network = network;
        this.categorizer = categorizer;

        if (!hasCountsFile && !config.hasAverageCountsPerCategoryFile()) {
            throw new IllegalArgumentException("Either counts file or average counts per category file must be provided.");
        }

        // Initialize categorizer with separate urban flag
        try {
            build();
            saveAverageCounts();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void build() throws IOException {
        // init road categories
        initRoadCategories();

        // read counts file and process
        if (hasCountsFile) {
            readCounts();
        } else {
            readAverageCountsPerCategory();
        }

        // when distinction is made between urban and rural, and counts are provided, categories with less than 20 counts are merged (merge urban into rural)
        // special regions have different limits
        boolean merged = false;
        if (hasCountsFile && (categorizer.isSeparateUrban() || categorizer.itHasSpecialRegions())) {
            for (Map.Entry<Integer, Integer> entry : numCountsByCategory.entrySet()) {
                int category = entry.getKey();
                int count = entry.getValue();
                boolean isInSpecialRegion = categorizer.isInSpecialRegion(category);

                if (count < MIN_OBSERVATIONS_RURAL_VS_URBAN && !isInSpecialRegion) {
                    // This first conditions works also for rural, if there isn't enough in rural, they will be merged with urban anyway
                    logger.warn("Merging category {} into rural due to insufficient number of roads: {}", category, count);
                    int cat = category % 10 + 10;
                    categorizer.drop("urban", cat);
                    merged = true;
                }

                if (count < MIN_OBSERVATIONS_SPECIAL_REST && isInSpecialRegion) {
                    logger.warn("Merging category {} into other non-special regions due to insufficient number of roads: {}", category, count);
                    int cat = category % 10 + 20;
                    categorizer.drop("special", cat);
                    merged = true;
                }
            }

            // re-compute average counts per category after merging
            if (merged){
                initRoadCategories();
                AverageCountsPerCategory.clear();
                numCountsByCategory.clear();
                allLinks.clear();
                readCounts();
            }
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
        for (CountPoint point : counts) {
            Id<Link> linkId = Id.create(point.linkId, Link.class);
            Integer category = getLinkCategory(linkId);
            if (category == null) {
                // check whether the link is missing in the network
                if (!network.getLinks().containsKey(linkId)) {
                    throw new IOException("Link with ID '" + point.linkId + "' not found in the network.");
                } else {
                    throw new IOException("Link with ID '" + point.linkId + "' is found in the network, but has somehow null category.");
                }
            }
            if (category != LinkCategorizer.UNKNOWN_CATEGORY && point.count>0.0) {
                AverageCountsPerCategory.put(category, AverageCountsPerCategory.getOrDefault(category, 0.0) + point.count);
                numCountsByCategory.put(category, numCountsByCategory.getOrDefault(category, 0) + 1);
                //  register the links
                allLinks.put(linkId,(float) point.count);
            }
        }

        // Compute averages
        for (Map.Entry<Integer, Double> entry : AverageCountsPerCategory.entrySet()) {
            int category = entry.getKey();
            AverageCountsPerCategory.put(category, entry.getValue() / numCountsByCategory.get(category));
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
        CsvSchema taskSchema = mapper.typedSchemaFor(OutputPoint.class).withHeader().withColumnSeparator(';').withComments()
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
        return !hasCountsFile || allLinks.containsKey(linkId); // if counts file is not provided, assume all links are contained
    }

    public Double getAverageCountForCategory(int category) {
        if (numCountsByCategory.containsKey(category) && numCountsByCategory.get(category) < MIN_OBSERVATION_TO_CONSIDER) {
            return Double.NaN;
        }
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
        @JsonAlias({"link", "link_id", "linkId"})
        public String linkId;

        @JsonProperty("count")
        @JsonAlias({"count", "counts", "Count"})
        public double count;

    }

    static public class OutputPoint {
        @JsonProperty("category")
        public String category;

        @JsonProperty("averageCount")
        public double averageCount;

    }

    private void initRoadCategories(){
        // init road categories
        for (Link link : network.getLinks().values()) {
            int category = categorizer.getCategory(link);
            roadCategories.put(link.getId(), category);
        }
    }

    public int size(){
        return hasCountsFile ? allLinks.size():0 ;
    }

    public float getLinkCounts(Id<Link> linkId) {
        return allLinks.getOrDefault(linkId,-1.0F);
    }
}
