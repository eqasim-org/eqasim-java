package org.eqasim.examples.Drafts.GBFSUtilsDrafts;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eqasim.examples.SMMFramework.GBFSUtils.Station;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class GBFSToESRI {
    private SimpleFeatureBuilder stationBuilder;
    private final CoordinateReferenceSystem crs;


    private double outputSample = 1;
    private double actBlurFactor = 0;
    private double legBlurFactor = 0;
    private final String outputDir;
    private boolean writeActs = true;
    private boolean writeLegs = true;
    private ArrayList<Plan> outputSamplePlans;

    private final GeometryFactory geofac;

    public static void main(String[] args) throws IOException {
        HashMap<String, Station>stationsMap=readGBFSStationBased("C:\\Users\\juan_\\Desktop\\TUM\\Semester5\\Thesis\\eqasimMicromobility\\GBFSInputs\\StationInformation_SD_2_VD_6.json","C:\\Users\\juan_\\Desktop\\TUM\\Semester5\\Thesis\\eqasimMicromobility\\GBFSInputs\\StationStatus_SD_2_VD_6.json");
        HashMap<String,Station>stationsMap4=readGBFSStationBased("C:\\Users\\juan_\\Desktop\\TUM\\Semester5\\Thesis\\eqasimMicromobility\\GBFSInputs\\StationInformation_SD_2_VD_8.json","C:\\Users\\juan_\\Desktop\\TUM\\Semester5\\Thesis\\eqasimMicromobility\\GBFSInputs\\StationStatus_SD_2_VD_8.json");
        HashMap<String,Station>stationsMap6=readGBFSStationBased("C:\\Users\\juan_\\Desktop\\TUM\\Semester5\\Thesis\\eqasimMicromobility\\GBFSInputs\\StationInformation_SD_2_VD_10.json","C:\\Users\\juan_\\Desktop\\TUM\\Semester5\\Thesis\\eqasimMicromobility\\GBFSInputs\\StationStatus_SD_2_VD_10.json");
//        HashMap<String,Station>stationsMap8=readGBFSStationBased("C:\\Users\\juan_\\Desktop\\TUM\\Semester5\\Thesis\\eqasimMicromobility\\GBFSInputs\\StationInformation_SD_8_VD_6.json","C:\\Users\\juan_\\Desktop\\TUM\\Semester5\\Thesis\\eqasimMicromobility\\GBFSInputs\\StationStatus_SD_2_VD_6.json");
        String x="x";

    }

    public GBFSToESRI(CoordinateReferenceSystem crs,String outputDir) {
        this.crs = crs;
        this.outputDir = outputDir;
        this.geofac = new GeometryFactory();
        initFeatureType();
    }

    private void initFeatureType() {
        SimpleFeatureTypeBuilder actBuilder = new SimpleFeatureTypeBuilder();
        actBuilder.setName("activity");
        actBuilder.setCRS(this.crs);
        actBuilder.add("the_geom", Point.class);
        actBuilder.add("Station_ID", String.class);
        actBuilder.add("VehCapacity", String.class);
        actBuilder.add("Docks", String.class);


        this.stationBuilder = new SimpleFeatureBuilder(actBuilder.buildFeatureType());
    }


    public static HashMap<String,Station> readGBFSStationBased(String stations,
            String stationStatus) throws IOException {
        HashMap<String, Station> stationsMap = new HashMap<>();
        BufferedReader reader = IOUtils.getBufferedReader(stations);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(reader);
        JsonNode stationsNode = rootNode.findPath("stations");
        JsonNode docksCapacity = rootNode.findPath("vehicle_type_dock_capacity");
        System.out.println(docksCapacity);
        if (stationsNode.isArray()) {
            Iterator var26 = stationsNode.iterator();

            while (var26.hasNext()) {
                JsonNode stationNode = (JsonNode) var26.next();
                String stationId = stationNode.findValue("id").asText();

                int capacity = stationNode.findValue("capacity").asInt();
                Coord coord = new Coord(stationNode.findValue("lon").asDouble(), stationNode.findValue("lat").asDouble());
                Station tempStation=new Station(0,capacity,coord);
                stationsMap.put(stationId,tempStation);
                System.out.println(coord);

            }
        }

        reader = IOUtils.getBufferedReader(stationStatus);
        mapper = new ObjectMapper();
        rootNode = mapper.readTree(reader);
        stationsNode = rootNode.findPath("stations");
        int vehicleIndex = 0;
        if (stationsNode.isArray()) {
            Iterator var29 = stationsNode.iterator();

            while (var29.hasNext()) {
                JsonNode stationNode = (JsonNode) var29.next();
                String stationId = stationNode.findValue("station_id").asText();
                int numberOfBikes = stationNode.findValue("num_vehicles_available").asInt();
                stationsMap.get(stationId).setNumVeh(numberOfBikes);
            }
        }
        return stationsMap;
    }


}
