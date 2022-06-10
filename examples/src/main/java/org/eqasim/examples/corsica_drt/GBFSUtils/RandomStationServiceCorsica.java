package org.eqasim.examples.corsica_drt.GBFSUtils;

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
import org.opengis.feature.simple.SimpleFeature;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.eqasim.examples.corsica_drt.GBFSUtils.CreateGBFS.createStationStatus;
import static org.eqasim.examples.corsica_drt.GBFSUtils.CreateGBFS.createVehDockCapacity;

public class RandomStationServiceCorsica {
    private static final String FREE_FLOATING="Free-floating";
    private static final String STATION_BASED="Station-Based";

    private String residentialAreas;
    private String  configFile;
    private String serviceMode;
    private String serviceCode;
    private Integer stationDensity;
    private Integer vehicleDensity;
    private String mode;


    public RandomStationServiceCorsica(String residentialAreas, String configFile, String serviceMode, String serviceCode, Integer stationDensity, Integer vehicleDensity, String mode) {
        this.residentialAreas = residentialAreas;
        this.configFile = configFile;
        this.serviceMode = serviceMode;
        this.serviceCode = serviceCode;
        this.stationDensity = stationDensity;
        this.vehicleDensity = vehicleDensity;
        this.mode = mode;
    }






     public static Map<String,Integer>calculateNumberofVehicles(Map<String, Geometry> polygons, double densityVeh, String attrString, Population population){
         Map<String, Integer> shapeMap = new HashMap<>();
         int vehiclesCounter=0;
         for(String key: polygons.keySet()){
             shapeMap.put(key,0);
         }
         for(Id<Person> id:population.getPersons().keySet()){
             Person person=population.getPersons().get(id);
             Plan plan=person.getSelectedPlan();
             for(PlanElement planElement: plan.getPlanElements()){
                 if(planElement instanceof Activity){
                     Activity act=(Activity) planElement;
                     if(act.getType().equals("home")){
                         Coord coordHome=act.getCoord();
                         String idZone=idResidentialArea(coordHome,polygons);
                         if(!idZone.equals("None")) {
                             int outdatedPopNumber=shapeMap.get(idZone);
                             shapeMap.replace(idZone,outdatedPopNumber+1);

                         }

                     }


                 }
             }

         }
         for(String id:shapeMap.keySet()){
             int populationArea=shapeMap.get(id);
             double veh=(populationArea/10)*densityVeh;

             vehiclesCounter+=veh;
             shapeMap.replace(id, (int) Math.round(veh));


         }
         System.out.println("for veh density: "+ String.valueOf(densityVeh)+" there are "+ String.valueOf(vehiclesCounter)+ " in Corsica");
    return shapeMap;
     }

    public static Map<String, Integer> calculateNumberofStations(String filename, Integer densityVeh,String attrString, Map<String,Integer> vehZone) {
        Map<String, Integer> shapeMap = new HashMap<>();
        int stationCounter=0;
        for (SimpleFeature ft : ShapeFileReader.getAllFeatures(filename)) {
            GeometryFactory geometryFactory = new GeometryFactory();
            WKTReader wktReader = new WKTReader(geometryFactory);
            Geometry geometry;
            try {
                Double area=  (Double)ft.getAttribute("AREA");
                int vehicles= (int) (area/(1000*1000))*densityVeh;

                geometry = wktReader.read((ft.getAttribute("the_geom")).toString());
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
        System.out.println("for station density: "+ String.valueOf(densityVeh) +" there are "+ String.valueOf(stationCounter)+ " in Corsica");
        return shapeMap;
    }

    // Read in shapefile (exact functioning can be ignored for now)
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
//    // Method creates a  Proxy GBFS  for a station based service based on residential areas, number of vehicles and number of stations

    public static HashMap<String,Vehicle>createVehiclesMap(HashMap<String,HashMap<String,Vehicle>>lastVehicle,int numVeh, String idZone,Geometry polygon){
        HashMap<String,Vehicle> mapVehicles=new HashMap<>();

        if(lastVehicle==null){
            for(int i=1; i<=numVeh;i++){
                Coord vehCoord=drawRandomPointFromGeometry(polygon);
                String id=String.valueOf(i)+"_"+idZone;
                Vehicle tempVehicle= new Vehicle(id,vehCoord);
                mapVehicles.put(id,tempVehicle);
            }

        }else{
            HashMap<String,Vehicle>stationsInZoneP=lastVehicle.get(idZone);
            mapVehicles=stationsInZoneP;
            int existingVeh=mapVehicles.size();
            int newVehicles=numVeh-existingVeh;

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


    public static HashMap<String,Station>createStationMap(HashMap<String,HashMap<String,Station>>lastStations,int numVeh,int numStations, String idZone,Geometry polygon){
        HashMap<String,Station> mapStations=new HashMap<>();

        if(lastStations==null){



            List<Integer> distributedVeh=RandomSum.n_random(numVeh,numStations);
            for(int i=1; i<=numStations;i++){
                Coord stationCoord=drawRandomPointFromGeometry(polygon);
                Integer numVehStation=distributedVeh.get(i-1);
                Station tempStation= new Station(numVehStation,0,stationCoord);
                mapStations.put(String.valueOf(i)+"_"+idZone,tempStation);
            }

        }else{
            HashMap<String,Station>stationsInZoneP=lastStations.get(idZone);
            mapStations=stationsInZoneP;
            int existingVeh=0;
            int existingStations=stationsInZoneP.size();
            int newStations=numStations-existingStations;
            for(String key: stationsInZoneP.keySet()){
                existingVeh+=stationsInZoneP.get(key).getNumVeh();
            }
            if(newStations>0){
                List<Integer> distributedVeh=RandomSum.n_random(numVeh-existingVeh,newStations);
                for(int i=existingStations+1;i<=numStations;i++){
                    Coord stationCoord=drawRandomPointFromGeometry(polygon);
                    Integer numVehStation=distributedVeh.get((numStations-existingStations)-1);
                    Station tempStation= new Station(numVehStation,0,stationCoord);
                    mapStations.put(String.valueOf(i)+"_"+idZone,tempStation);
                }

            }
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
                                createVehDockCapacity(2,9,1,"b","a","c",3),tempStation.getNumVeh()));

                        JSONObject stationStatus=createStationStatus(key,true,true,true,15,1,0,"1","2","3",tempStation.getNumVeh(),0);
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
                FileWriter file = new FileWriter("./GBFSInputs/StationStatus_SD_"+stationDensity+"_VD_"+vehDensity+".json");
                file.write(obj.toJSONString());
                file.close();
            }catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            // Saves the Station Information GBFS
            try{
                FileWriter file = new FileWriter("./GBFSInputs/StationInformation_SD_"+stationDensity+"_VD_"+vehDensity+".json");
                file.write(stationsGBFS.toJSONString());
                file.close();
            }catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        return returnMap;
        }

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
            FileWriter file = new FileWriter("./GBFSInputs/VehicleStatus_VD_"+vehDensity+".json");
            file.write(vehicleStatus.toJSONString());
            file.close();
        }catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
       return  returnMap;
    }

    public static void createGBFSFreeFloting(Map<String,Integer>vehicles,Map<String,Geometry>polygons,Scenario scenario,String vehDensity) {
        JSONArray vehiclesArray = new JSONArray();
        Integer sumVeh=0;
        for (String idZone : polygons.keySet()) {
            Geometry polygon = polygons.get(idZone);
            Integer numberofVehicles = vehicles.get(idZone);
            sumVeh+=numberofVehicles;
            for (int i = 1; i <= numberofVehicles; i++) {
                Coord vehicleCoord = drawRandomPointFromGeometry(polygon);

                JSONObject vehicle = CreateGBFS.createFreeVehicle(String.valueOf(i)+"_"+idZone, "proxy", vehicleCoord.getY(), vehicleCoord.getX(), "false", "false", "bike");
                vehiclesArray.add(vehicle);
                i=i;

            }
        }

        JSONObject vehicleStatus=CreateGBFS.createVehicleStatus(vehiclesArray);
        try{
            FileWriter file = new FileWriter("./GBFSInputs/VehicleStatus_VD_"+vehDensity+".json");
            file.write(vehicleStatus.toJSONString());
            file.close();
        }catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

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
}
