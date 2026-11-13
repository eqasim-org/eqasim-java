package org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt.inputs;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt.LemanPass;
import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt.SwissPtPricingConfigGroup;
import org.matsim.core.config.ConfigGroup;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

/**
 * Reads the Léman Pass single tickets written by eqasim-switzerland (write_java_tables in utils/gtfs_router/lemanpass.py):
 * lemanpass_products.csv, one row per product, and lemanpass_stations.csv, one row per rail stop of a station. Both are
 * UTF-8 CSV files with a header. Files with only their header mean no Léman Pass.
 */
public class LemanPassReader {
    private static final Logger LOG = LogManager.getLogger(LemanPassReader.class);

    private static final List<String> PRODUCT_COLUMNS = List.of("product_id", "label", "pieces", "end_a", "end_b", "cover");
    private static final List<String> STATION_COLUMNS = List.of("stop_id", "station", "zone", "country");

    /**
     * The Léman Pass of the ptPricing module (lemanPassProductsPath and lemanPassStationsPath, relative to the config
     * file), or null: no paths, or no products in the file.
     */
    public static LemanPass read(SwissPtPricingConfigGroup group, URL context) throws IOException {
        boolean products = SwissPtPricingConfigGroup.isSet(group.getLemanPassProductsPath());
        boolean stations = SwissPtPricingConfigGroup.isSet(group.getLemanPassStationsPath());
        if (!products && !stations) {
            return null;
        }
        if (products != stations) {
            throw new IllegalArgumentException("ptPricing: lemanPassProductsPath and lemanPassStationsPath must be given together");
        }
        return read(ConfigGroup.getInputFileURL(context, group.getLemanPassProductsPath()),
                ConfigGroup.getInputFileURL(context, group.getLemanPassStationsPath()));
    }

    /** The Léman Pass of the two files, or null if the products file has no products. */
    public static LemanPass read(URL productsUrl, URL stationsUrl) throws IOException {
        Map<String, LemanPass.Station> stations = readStations(stationsUrl);
        List<LemanPass.Product> products = readProducts(productsUrl);
        if (products.isEmpty()) {
            LOG.info("No Léman Pass: no products in {}", productsUrl);
            return null;
        }

        Set<String> names = new HashSet<>();
        stations.values().forEach(s -> names.add(s.name()));
        Set<String> missing = new TreeSet<>();
        for (LemanPass.Product product : products) {
            for (String place : product.pieces()) {
                if (place.startsWith("station:") && !names.contains(place.substring("station:".length()))) {
                    missing.add(place.substring("station:".length()));
                }
            }
        }
        if (!missing.isEmpty()) {
            LOG.warn("Léman Pass stations without a stop in {}: {}", stationsUrl, String.join(", ", missing));
        }
        LOG.info("Léman Pass: {} products, {} stops of {} stations", products.size(), stations.size(), names.size());
        return new LemanPass(products, stations);
    }

    static List<LemanPass.Product> readProducts(URL url) throws IOException {
        List<LemanPass.Product> products = new ArrayList<>();
        try (CSVReader reader = new CSVReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {
            Map<String, Integer> columns = header(reader.readNext(), url);
            List<String> required = new ArrayList<>(PRODUCT_COLUMNS);
            for (String profile : LemanPass.PROFILES) {
                required.add(profile + "_2");
                required.add(profile + "_1");
            }
            check(columns, required, url);

            String[] row;
            while ((row = reader.readNext()) != null) {
                if (row.length == 1 && row[0].isBlank()) {
                    continue;
                }
                double[] prices = new double[2 * LemanPass.PROFILES.size()];
                for (int p = 0; p < LemanPass.PROFILES.size(); p++) {
                    prices[2 * p] = price(cell(row, columns, LemanPass.PROFILES.get(p) + "_2"));
                    prices[2 * p + 1] = price(cell(row, columns, LemanPass.PROFILES.get(p) + "_1"));
                }
                Set<String> cover = places(cell(row, columns, "cover"));
                products.add(new LemanPass.Product(Integer.parseInt(cell(row, columns, "product_id").trim()),
                        cell(row, columns, "label"), places(cell(row, columns, "pieces")),
                        places(cell(row, columns, "end_a")), places(cell(row, columns, "end_b")),
                        cover.isEmpty() ? null : cover, prices));
            }
        } catch (CsvValidationException | NumberFormatException e) {
            throw new IOException("Invalid Léman Pass products in " + url + ": " + e.getMessage(), e);
        }
        return products;
    }

    static Map<String, LemanPass.Station> readStations(URL url) throws IOException {
        Map<String, LemanPass.Station> stations = new HashMap<>();
        try (CSVReader reader = new CSVReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {
            Map<String, Integer> columns = header(reader.readNext(), url);
            check(columns, STATION_COLUMNS, url);

            String[] row;
            while ((row = reader.readNext()) != null) {
                if (row.length == 1 && row[0].isBlank()) {
                    continue;
                }
                String stopId = cell(row, columns, "stop_id").trim();
                LemanPass.Station station = new LemanPass.Station(cell(row, columns, "station"),
                        cell(row, columns, "zone").trim(), cell(row, columns, "country").trim());
                stations.put(stopId, station);
            }
        } catch (CsvValidationException e) {
            throw new IOException("Invalid Léman Pass stations in " + url + ": " + e.getMessage(), e);
        }
        return stations;
    }

    private static Map<String, Integer> header(String[] header, URL url) throws IOException {
        if (header == null) {
            throw new IOException("Empty Léman Pass file (no header): " + url);
        }
        Map<String, Integer> columns = new HashMap<>();
        for (int i = 0; i < header.length; i++) {
            columns.put(header[i].replace("﻿", "").trim(), i);
        }
        return columns;
    }

    private static void check(Map<String, Integer> columns, List<String> required, URL url) throws IOException {
        List<String> missing = required.stream().filter(c -> !columns.containsKey(c)).toList();
        if (!missing.isEmpty()) {
            throw new IOException("Missing columns " + missing + " in " + url);
        }
    }

    private static String cell(String[] row, Map<String, Integer> columns, String column) {
        int index = columns.get(column);
        return index < row.length ? row[index] : "";
    }

    private static double price(String text) {
        return text == null || text.isBlank() ? Double.NaN : Double.parseDouble(text.trim());
    }

    /** A set of places separated by "|". */
    static Set<String> places(String text) {
        Set<String> places = new LinkedHashSet<>();
        if (text != null) {
            Arrays.stream(text.split("\\|")).map(String::trim).filter(p -> !p.isEmpty()).forEach(places::add);
        }
        return Set.copyOf(places);
    }
}
