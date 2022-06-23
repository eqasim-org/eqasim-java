package org.eqasim.examples.SMMFramework.GBFSUtils;

import org.eqasim.examples.corsica_drt.Drafts.GBFSUtilsDrafts.CreateGBFS;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.opengis.feature.simple.SimpleFeature;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;



/**
 * Class provides the  utils functions to  generate a random Service in Corsica
 */
public class RandomStationServiceCorsica {
    // Constants
    private static final String FREE_FLOATING="Free-floating";
    private static final String STATION_BASED="Station-Based";


    /**
     * Method calculate the  a Map of Number of vehicles  in each polygon based on density of vehicle per 1000 inhabitant and population in polygon
     * @param polygons region zones
     * @param densityVeh desired vehicle density
     * @param attrString name of attribute id in shape file for zones
     * @param population MATSim Population
     * @return
     */
    public static Map<String,Integer>calculateNumberofVehicles(Map<String, Geometry> polygons, double densityVeh, String attrString, Population population){
        Map<String, Integer> shapeMap = new HashMap<>();
        int vehiclesCounter=0;
        // Initialize the map of zones with 0 persons
        for(String key: polygons.keySet()){
            shapeMap.put(key,0);
        }
        // Iterates for every person in population and finds the zone to which it  home belongs
        for(Id<Person> id:population.getPersons().keySet()){
            Person person=population.getPersons().get(id);
            Plan plan=person.getSelectedPlan();
            for(PlanElement planElement: plan.getPlanElements()){
                if(planElement instanceof Activity){
                    Activity act=(Activity) planElement;
                    if(act.getType().equals("home")){
                        Coord coordHome=act.getCoord();
                        String idZone=idResidentialArea(coordHome,polygons);
                        // if the person lives in a zone in the polygons, adds a person to the zone
                        if(!idZone.equals("None")) {
                            int outdatedPopNumber=shapeMap.get(idZone);
                            shapeMap.replace(idZone,outdatedPopNumber+1);

                        }

                    }


                }
            }

        }
        // Based on the Population per zone calculate number of vehicles
        for(String id:shapeMap.keySet()){
            int populationArea=shapeMap.get(id);

            double veh=(populationArea/100)*densityVeh;
            vehiclesCounter+=veh;
            shapeMap.replace(id, (int) Math.round(veh));


        }
        //Returns the map of polygons ids and number of Vehicles in Zone
        return shapeMap;
    }
    //

    /**
     * Method calculate the Map of Number of stations  in each polygon based on density of station  per km2 and area
     * @param filename Shape file string
     * @param densityVeh desired vehicle density
     * @param attrString name of attribute id in shape file for zones
     * @param vehZone   desired vehicle density
     * @return
     */
    public static Map<String, Integer> calculateNumberofStations(String filename, Integer densityVeh,String attrString, Map<String,Integer> vehZone) {
        // Initializes return element
        Map<String, Integer> shapeMap = new HashMap<>();
        int stationCounter=0;
        // Reads every polygon from shapfile and calculate the number of stations
        for (SimpleFeature ft : ShapeFileReader.getAllFeatures(filename)) {
            GeometryFactory geometryFactory = new GeometryFactory();
            WKTReader wktReader = new WKTReader(geometryFactory);
            Geometry geometry;
            try {
                Double area=  (Double)ft.getAttribute("AREA");
                int vehicles= (int) (area/(1000*1000))*densityVeh;

                geometry = wktReader.read((ft.getAttribute("the_geom")).toString());
                // Verify with the number of vehicles in the Zone, if no vehicles then omit the station
                if(vehZone.get(ft.getAttribute(attrString).toString())>0){
                    shapeMap.put(ft.getAttribute(attrString).toString(), vehicles);
                    stationCounter+=vehicles;
                }
                else{
                    shapeMap.put(ft.getAttribute(attrString).toString(),0);

                }
            } catch (ParseException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        // Returns the map with number of vehicles per  polygon
        return shapeMap;
    }


    // Identifies the polygon in which a Coordinate is located; returns the id of polygon
    private static String idResidentialArea(Coord coord, Map<String, Geometry> polygons) {

        Point p;
        p = MGC.xy2Point(coord.getX(), coord.getY());
        String keyFinal="None";
        for( String key:polygons.keySet()){
            Geometry poly= (Geometry) polygons.get(key);
            if(poly.contains(p)){
                keyFinal=key;
                break;
            }
        }

        return keyFinal;
    }
    // Method creates the locations of the sharing vehicles in each zone
    // in case the service is trying to grow the vehicle density the last position of vehicles must be parsed as lastVehicle, in case it is null
    // the allocation of vehicles is random
    // Returns the Map of each Vehicle and the object
    public static HashMap<String,Vehicle>createVehiclesMap(HashMap<String,HashMap<String,Vehicle>>lastVehicle,int numVeh, String idZone,Geometry polygon){
        HashMap<String,Vehicle> mapVehicles=new HashMap<>();
        // Random Assignment of vehicles
        if(lastVehicle==null){
            for(int i=1; i<=numVeh;i++){
                Coord vehCoord=drawRandomPointFromGeometry(polygon);
                String id=String.valueOf(i)+"_"+idZone;
                Vehicle tempVehicle= new Vehicle(id,vehCoord);
                mapVehicles.put(id,tempVehicle);
            }

        }else{
            // If there are previous vehicles
            HashMap<String,Vehicle>stationsInZoneP=lastVehicle.get(idZone);
            mapVehicles=stationsInZoneP;
            int existingVeh=mapVehicles.size();
            // Calculate missing Vehicles  for new density
            int newVehicles=numVeh-existingVeh;
            // if there are more vehicles add them
            if(newVehicles>0){

                for(int i=existingVeh+1;i<=numVeh;i++){
                    Coord vehCoord=drawRandomPointFromGeometry(polygon);
                    String id=String.valueOf(i)+"_"+idZone;
                    Vehicle tempVehicle= new Vehicle(id,vehCoord);
                    mapVehicles.put(id,tempVehicle);
                }

            }


        }
        return mapVehicles;
    }


    /**
     * Method creates the locations of the sharing stations in each zone
     * @param lastStations stations of existing service
     * @param numVeh        number of vehicles desired
     * @param numStations   number of stations desired
     * @param idZone        id Identifier of zone
     * @param polygon      polygon of zone
     * @return the Map of stations per polygon in zone
     */
    public static HashMap<String,Station>createStationMap(HashMap<String,HashMap<String,Station>>lastStations,int numVeh,int numStations, String idZone,Geometry polygon){
        HashMap<String,Station> mapStations=new HashMap<>();
        // Random Assignment of stations
        if(lastStations==null){
            List<Integer> distributedVeh=RandomSum.n_random(numVeh,numStations);
            for(int i=1; i<=numStations;i++){
                Coord stationCoord=drawRandomPointFromGeometry(polygon);
                Integer numVehStation=distributedVeh.get(i-1);
                Station tempStation= new Station(numVehStation,0,stationCoord);
                mapStations.put(String.valueOf(i)+"_"+idZone,tempStation);
            }

        }else{
            // If there are previous stations
            HashMap<String,Station>stationsInZoneP=lastStations.get(idZone);
            int existingVeh = 0;
            int existingStations = 0;
            int newStations =0;
            if(stationsInZoneP!=null) {
                mapStations = stationsInZoneP;
                existingVeh = 0;
                // Calculate missing Vehicles and Stations for new densities
                existingStations = stationsInZoneP.size();
                newStations = numStations - existingStations;
                for (String key : stationsInZoneP.keySet()) {
                    existingVeh += stationsInZoneP.get(key).getNumVeh();
                }
            }else{
                stationsInZoneP=new HashMap<>();
                mapStations = stationsInZoneP;
                existingVeh = 0;
                // Calculate missing Vehicles and Stations for new densities
                existingStations = stationsInZoneP.size();
                newStations = numStations - existingStations;
                for (String key : stationsInZoneP.keySet()) {
                    existingVeh += stationsInZoneP.get(key).getNumVeh();
                }
            }
            // If there are more stations due to increase of density
            if(newStations>0){
                List<Integer> distributedVeh=RandomSum.n_random(numVeh-existingVeh,newStations);
                for(int i=existingStations+1;i<=numStations;i++){
                    Coord stationCoord=drawRandomPointFromGeometry(polygon);
                    Integer numVehStation=distributedVeh.get((numStations-existingStations)-1);
                    Station tempStation= new Station(numVehStation,0,stationCoord);
                    mapStations.put(String.valueOf(i)+"_"+idZone,tempStation);
                }

            }
            // if there are more stations add them
            if(newStations==0){
                List<Integer> distributedVeh=RandomSum.n_random(numVeh-existingVeh,stationsInZoneP.size());
                int i=0;
                for(String key:stationsInZoneP.keySet()){

                    int lastNum=stationsInZoneP.get(key).getNumVeh();
                    stationsInZoneP.get(key).setNumVeh(lastNum+distributedVeh.get(i));
                    i+=1;
                }
            }

        }
        return mapStations;
    }

    /**
     *  Method creates the GBFS Files for a station based service in Corsica
     * @param stations number off stations per zone
     * @param numberOfVehicles number of vehicles per zone
     * @param polygons region zones
     * @param scenario Matsim Scenatio
     * @param vehDensity Density of the service(1/(1000)inhabitants
     * @param stationDensity density of stations (1/km2)
     * @param lastStations if there is a previous station based service
     * @return map of stations in the service
     */
    public static HashMap<String,HashMap<String,Station>> createGBFSStationBased2(Map<String,Integer> stations,Map<String,Integer> numberOfVehicles,Map<String,Geometry>polygons,Scenario scenario,String vehDensity,String stationDensity,HashMap<String,HashMap<String,Station>> lastStations){
        // Initialize   Station Status GBFS as JSON object
        JSONObject obj=new JSONObject();
        // Fills default dummy values
        obj.put("last_updated", 10);
        obj.put("ttl",3);
        obj.put("version",35);
        //Create the  objects to save the stations and their current status
        JSONObject data=new JSONObject();
        JSONArray stationsArray=new JSONArray();

        // Initialize   Station information  GBFS as JSON object
        JSONObject stationsGBFS=new JSONObject();
        // Fills default dummy values
        stationsGBFS.put("last_updated", 30052022);
        stationsGBFS.put("ttl",1);
        stationsGBFS.put("version",3);
        //Create the  objects to save the stations and their current status
        JSONObject dataSGBFS=new JSONObject();
        JSONArray stationsSGBFS=new JSONArray();
        HashMap<String,HashMap<String,Station>> returnMap=new HashMap<>();


        // Runs Through all the polygons and assign stations and vehicles
        for(String idZone:polygons.keySet()){
            Integer numberofVehZone=numberOfVehicles.get(idZone);
            int numberofStations=stations.get(idZone);
            Geometry polygon=polygons.get(idZone);
            HashMap<String,Station>stationsInZone=createStationMap(lastStations,numberofVehZone,numberofStations,idZone,polygon);
            // Creates  the stations status and information in the polygon
            for(String key:stationsInZone.keySet()){
                Station tempStation=stationsInZone.get(key);

                // Fills the station with dummy values but the  number of vehicles in station and number of docks available
                stationsSGBFS.add(CreateGBFS.createStaticStation(key,key,
                        tempStation.getCoord().getY(),tempStation.getCoord().getX(),
                        "Su-Th 05:00-22:00; Fr-Sa 05:00-01:00",
                        "underground_parking",false,"true",true,
                        CreateGBFS.createVehDockCapacity(2,9,1,"b","a","c",3),tempStation.getNumVeh()));

                JSONObject stationStatus= CreateGBFS.createStationStatus(key,true,true,true,15,1,0,"1","2","3",tempStation.getNumVeh(),0);
                stationsArray.add(stationStatus);




            }
            returnMap.put(idZone,stationsInZone);

        }

        // Adds the station status data to the station status Object
        data.put("stations",stationsArray);
        obj.put("data",data);

        // Adds the station information data to the station information Object
        dataSGBFS.put("stations",stationsSGBFS);
        stationsGBFS.put("data",dataSGBFS);

        // Saves the Station Status GBFS
        try{
            FileWriter file = new FileWriter("./UpdatedGBFSInputs/StationStatus_SD_"+stationDensity+"_VD_"+vehDensity+".json");
            file.write(obj.toJSONString());
            file.close();
        }catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        // Saves the Station Information GBFS
        try{
            FileWriter file = new FileWriter("./UpdatedGBFSInputs/StationInformation_SD_"+stationDensity+"_VD_"+vehDensity+".json");
            file.write(stationsGBFS.toJSONString());
            file.close();
        }catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return returnMap;
    }

    /**
     * Method  creathes the GBFS file free floating
     * @param vehicles number of vehicles per zone
     * @param polygons region zones
     * @param scenario Matsim Scenario
     * @param vehDensity Density of the service(1/(1000)inhabitants
     * @param lastVehicles If there is an existing service, the last position of vehicles
     * @return Hashmap of the Vehicles of the service
     */
    public static HashMap<String,HashMap<String,Vehicle>> createGBFSFreeFloating(Map<String,Integer>vehicles,Map<String,Geometry>polygons,Scenario scenario,String vehDensity,HashMap<String,HashMap<String,Vehicle>>lastVehicles) {
        JSONArray vehiclesArray = new JSONArray();
        Integer sumVeh=0;
        HashMap<String,HashMap<String,Vehicle>> returnMap=new HashMap<>();
        for (String idZone : polygons.keySet()) {
            Geometry polygon = polygons.get(idZone);
            Integer numberofVehicles = vehicles.get(idZone);
            HashMap<String,Vehicle>vehiclesInZone=createVehiclesMap(lastVehicles,numberofVehicles,idZone,polygon);
            for (String key:vehiclesInZone.keySet()) {
                Vehicle tempVeh=vehiclesInZone.get(key);

                JSONObject vehicle = CreateGBFS.createFreeVehicle( tempVeh.id,"proxy",tempVeh.getCoord().getY(), tempVeh.getCoord().getX(), "false", "false", "bike");
                vehiclesArray.add(vehicle);

            }
            returnMap.put(idZone,vehiclesInZone);
        }


        JSONObject vehicleStatus=CreateGBFS.createVehicleStatus(vehiclesArray);
        try{
            FileWriter file = new FileWriter("./UpdatedGBFSInputs/VehicleStatus_VD_"+vehDensity+".json");
            file.write(vehicleStatus.toJSONString());
            file.close();
        }catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return  returnMap;
    }
    public static HashMap<String, HashMap<String, Station>>createStationMapBsedOnPUT(Scenario scenario, Map<String,Geometry>polygons){
        HashMap<String, HashMap<String, Station>> mapStations=new HashMap<>();
        Map<Id<TransitStopFacility>, TransitStopFacility> transitStops=  scenario.getTransitSchedule().getFacilities();
        int i =0;
        for(TransitStopFacility facility : transitStops.values()){
            Coord stationCoord=facility.getCoord();
            String zoneId=idResidentialArea(stationCoord,polygons);
            Station stTemp=new Station(3,0,stationCoord);
            if(zoneId!=null){
                HashMap<String,Station>zone=mapStations.get(zoneId);
                if(zone!=null){
                    System.out.println(facility.getName());
                    if(!zone.keySet().contains(facility.getName())){
                        zone.put(facility.getName()+facility.getLinkId().toString(),stTemp);
                    }


                }else{
                    HashMap<String,Station>zoneMap=new HashMap<>();
                    zoneMap.put(facility.getName()+facility.getLinkId().toString(),stTemp);
                    System.out.println(facility.getName());
                    mapStations.put(zoneId,zoneMap);
                }
                i=i++;

            }

        }
        return mapStations;
    }


    /**
     *
     * External Methods
     */
    // Extracted from Raoul Rothfeld's code for MATSim  TUM Class
    public static Coord drawRandomPointFromGeometry(Geometry g){
        Random rnd = MatsimRandom.getLocalInstance();
        Point p;
        double x, y;
        do {
            x = g.getEnvelopeInternal().getMinX()
                    + rnd.nextDouble() * (g.getEnvelopeInternal().getMaxX() - g.getEnvelopeInternal().getMinX());
            y = g.getEnvelopeInternal().getMinY()
                    + rnd.nextDouble() * (g.getEnvelopeInternal().getMaxY() - g.getEnvelopeInternal().getMinY());
            p = MGC.xy2Point(x, y);
        } while (!g.contains(p));
        Coord coord = new Coord(p.getX(), p.getY());
        return coord;
    }
    // Read in shapefile From Raoul Rothfeld MATSim Class
    public static Map<String, Geometry> readShapeFile(String filename, String attrString) {
        Map<String, Geometry> shapeMap = new HashMap<>();
        for (SimpleFeature ft : ShapeFileReader.getAllFeatures(filename)) {
            GeometryFactory geometryFactory = new GeometryFactory();
            WKTReader wktReader = new WKTReader(geometryFactory);
            Geometry geometry;
            try {
                geometry = wktReader.read((ft.getAttribute("the_geom")).toString());
                shapeMap.put(ft.getAttribute(attrString).toString(), geometry);
            } catch (ParseException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return shapeMap;
    }
}
