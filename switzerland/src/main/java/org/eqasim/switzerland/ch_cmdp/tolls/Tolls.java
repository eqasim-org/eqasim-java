package org.eqasim.switzerland.ch_cmdp.tolls;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.vehicles.Vehicle;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Computes open- and closed-system road tolls from a CSV definition.
 * Thread-safe for concurrent calls to {@link #getToll(Link, Vehicle)}, as expected
 * during multi-threaded MATSim routing/simulation.
 */
public class Tolls {

    // filenames tried, in order, when no explicit filename is given to the constructor
    private static final List<String> DEFAULT_TOLLS_FILENAMES =
            List.of("tolls.csv", "road_tolls.csv", "roadTolls.csv","link_prices.csv");
    private static final String NETWORK_TOLL_ATTRIBUTE = "toll";
    private static final Logger logger = LogManager.getLogger(Tolls.class);

    public final boolean hasTolls;

    // price paid when passing through this link (open system)
    private final IdMap<Link, Float> openTolls = new IdMap<>(Link.class);
    // price paid on exit link, keyed by entry link (closed system)
    private final IdMap<Link, IdMap<Link, Float>> closedTolls = new IdMap<>(Link.class);
    // fallback average price per exit link, in case the entry link was not recorded
    private final IdMap<Link, Float> closedTollsAverage = new IdMap<>(Link.class);

    // links that can act as an entry point into a closed system
    private final Set<Id<Link>> entryLinks = new HashSet<>();
    // union of every link involved in tolling, used as a fast reject before deeper lookups
    private final Set<Id<Link>> allLinks = new HashSet<>();

    // per-vehicle entry link for closed systems; mutated concurrently during routing/simulation
    private final Map<Id<Vehicle>, Id<Link>> vehicleEntryLinks = new ConcurrentHashMap<>();

    public Tolls(Network network) {
        this(network, null);
    }

    public Tolls(Network network, String filename) {
        boolean tollsExist;
        TollInfo tollInfoFromFile;
        try {
            tollInfoFromFile = readTolls(resolveTollsFile(filename));
            tollsExist = tollInfoFromFile.tollsExist();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // We also check the network for tolls
        TollInfo tollInfoNetwork = readTollsFromNetwork(network);
        tollsExist = tollsExist || tollInfoNetwork.tollsExist();

        // if tolls exist, we process them (save them into dictionary of open and closed systems
        if (tollsExist){
            // merge the two tollInfo
            TollInfo tollInfo = mergeTollInfo(tollInfoFromFile, tollInfoNetwork);
            processTolls(tollInfo, network);
        }

        // log whether this is activated or not
        this.hasTolls = tollsExist;
        if (hasTolls) {
            logger.info("Tolls are found in the scenario, open tolls: {}, closed tolls: {}", openTolls.size(), closedTolls.size());
        } else {
            logger.info("No tolls found in the scenario.");
        }

    }

    /**
     * Returns the toll to pay for traversing {@code link} with {@code vehicle}.
     * For a closed system, entering the system returns 0.0 and records the entry link;
     * the actual price is charged on exit, based on the recorded entry link.
     */
    public double getToll(Link link, Vehicle vehicle) {
        if (!hasTolls) {
            return 0.0;
        }

        Id<Link> linkId = link.getId();
        if (!allLinks.contains(linkId)) {
            return 0.0;
        }

        Float openPrice = openTolls.get(linkId);
        if (openPrice != null) {
            return openPrice;
        }

        Id<Vehicle> vehicleId = vehicle.getId();
        Id<Link> entryLinkId = vehicleEntryLinks.get(vehicleId);

        // exit takes priority: a link can be an exit for one gate pair and an entry
        // for another, so we must resolve against the vehicle's current entry first
        IdMap<Link, Float> exitPrices = closedTolls.get(linkId);
        if (entryLinkId != null && exitPrices != null) {
            vehicleEntryLinks.remove(vehicleId);
            Float price = exitPrices.get(entryLinkId);
            return price != null ? price : closedTollsAverage.getOrDefault(linkId, 0.0F);
        }

        if (entryLinks.contains(linkId)) {
            vehicleEntryLinks.put(vehicleId, linkId);
            return 0.0;
        }

        return 0.0;
    }

    // ------------------- HELPER METHODS -------------------

    private void processTolls(TollInfo tollInfo, Network network) {
        List<Toll> tollsList = tollInfo.tollsList();
        if (tollsList == null || tollsList.isEmpty()) {
            return;
        }

        for (Toll toll : tollsList) {
            Id<Link> linkId = Id.createLinkId(toll.linkId);
            Link link = network.getLinks().get(linkId);
            if (link == null) {
                logger.warn("Link not found in network: {}", toll.linkId);
                continue;
            }

            if (toll.entryLink == null || toll.entryLink.isEmpty()) {
                openTolls.put(linkId, toll.price);
                allLinks.add(linkId);
                continue;
            }

            Id<Link> entryLinkId = Id.createLinkId(toll.entryLink);
            if (network.getLinks().get(entryLinkId) == null) {
                logger.warn("Entry link not found in network: {}", toll.entryLink);
                continue;
            }

            closedTolls.computeIfAbsent(linkId, id -> new IdMap<>(Link.class))
                       .put(entryLinkId, toll.price);
            entryLinks.add(entryLinkId);
            allLinks.add(linkId);
            allLinks.add(entryLinkId);
        }

        for (Map.Entry<Id<Link>, IdMap<Link, Float>> entry : closedTolls.entrySet()) {
            IdMap<Link, Float> entryPrices = entry.getValue();
            float sum = 0.0f;
            for (Float price : entryPrices.values()) {
                sum += price;
            }
            closedTollsAverage.put(entry.getKey(), sum / entryPrices.size());
        }
    }

    /**
     * If {@code filename} is given, uses it as-is (whether it exists or not, so a bad
     * explicit path fails loudly via "file not found" rather than silently falling back).
     * Otherwise tries each of {@link #DEFAULT_TOLLS_FILENAMES} in order and returns the
     * first one that exists, or the first candidate if none do (so the "no tolls" path
     * still gets a sensible name to report).
     */
    private static String resolveTollsFile(String filename) {
        if (filename != null) {
            return filename;
        }
        for (String candidate : DEFAULT_TOLLS_FILENAMES) {
            if (new File(candidate).exists()) {
                return candidate;
            }
        }
        return DEFAULT_TOLLS_FILENAMES.getFirst();
    }

    public static TollInfo readTolls(String file) throws IOException {
        File tollsFile = new File(file);
        if (!tollsFile.exists()) {
            logger.warn("Tolls file does not exist: {}", file);
            return new TollInfo(false, null);
        }

        CsvMapper csvMapper = new CsvMapper();
        CsvSchema tollsSchema = csvMapper.typedSchemaFor(Toll.class)
                .withHeader()
                .withColumnSeparator(detectSeparator(tollsFile))
                .withComments()
                .withColumnReordering(true);

        MappingIterator<Toll> tollsIterator = csvMapper.readerFor(Toll.class)
                .with(tollsSchema)
                .readValues(tollsFile);
        List<Toll> tollsList = tollsIterator.readAll();

        if (tollsList.isEmpty()) {
            logger.warn("Tolls file is empty: {}", file);
            return new TollInfo(false, null);
        }
        logger.info("Read tolls: {}", tollsList.size());
        return new TollInfo(true, tollsList);
    }

    private TollInfo readTollsFromNetwork(Network network){
        List<Toll> tollsList = new ArrayList<>();
        for (Link link: network.getLinks().values()) {
            Object passagePrice = link.getAttributes().getAttribute(NETWORK_TOLL_ATTRIBUTE);
            if (passagePrice instanceof Double price) {
                tollsList.add(new Toll(link.getId().toString(), null, price.floatValue()));
            }
        }
        if (tollsList.isEmpty()) {
            return new TollInfo(false, null);
        }
        return new TollInfo(true, tollsList);
    }

    private TollInfo mergeTollInfo(TollInfo tollInfo1, TollInfo tollInfo2) {
        List<Toll> mergedTollsList = new ArrayList<>();
        if (tollInfo1.tollsList() != null) {
            mergedTollsList.addAll(tollInfo1.tollsList());
        }
        if (tollInfo2.tollsList() != null) {
            mergedTollsList.addAll(tollInfo2.tollsList());
        }
        boolean tollsExist = !mergedTollsList.isEmpty();
        return new TollInfo(tollsExist, mergedTollsList);
    }
    /**
     * Picks ',' or ';' by checking which one appears in the header line. Defaults to ','
     * if neither is found (e.g. a single-column file) or the file can't be peeked at.
     */
    private static char detectSeparator(File file) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String header = reader.readLine();
            if (header != null && header.chars().filter(c -> c == ';').count()
                    > header.chars().filter(c -> c == ',').count()) {
                return ';';
            }
        } catch (IOException e) {
            logger.warn("Could not peek at tolls file header to detect separator, defaulting to ','", e);
        }
        return ',';
    }

    public record TollInfo(boolean tollsExist, List<Toll> tollsList) {}

    public static class Toll {
        @JsonProperty("linkId")
        @JsonAlias({"link_id", "link"})
        public String linkId;

        @JsonProperty("entryLink")
        @JsonAlias({"entry_link", "entry", "entyLinkId", "entry_link_id"})
        public String entryLink;

        @JsonProperty("price")
        @JsonAlias({"value", "toll", "cost"})
        public float price;

        public Toll(String linkId, String entryLInk, float price) {
            this.linkId = linkId;
            this.entryLink = entryLInk;
            this.price = price;
        }
    }
}