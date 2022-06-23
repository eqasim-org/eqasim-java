package org.eqasim.examples.Drafts.GBFSUtilsDrafts;


import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Random;

/**
 *Class creates GBFS Json files
 */
public class CreateGBFS {
    public static void main(String[] args) {
        Network fullNetwork = NetworkUtils.createNetwork();
        (new MatsimNetworkReader(fullNetwork)).readFile("C:\\Users\\juan_\\Desktop\\TUM\\Semester 4\\Matsim\\eqasim-java-develop\\ile_de_france\\src\\main\\resources\\corsica\\corsica_network.xml");
        double[] bounds = NetworkUtils.getBoundingBox(fullNetwork.getNodes().values());


        createGBFSVehicleTypes();
//        createStations(11,2,"2",10,bounds[2],bounds[3],bounds[0],bounds[1]);
        createStationsStatus(10);
        createGeoFencingZone(5,15.3,85.2,0.0,23.2);
    }

    /**
     * creates station status based on a number of stations
     * @param numberOfStations
     */
    public static void createStationsStatus(int numberOfStations){
        JSONObject obj=new JSONObject();
        obj.put("last_updated", 10);
        obj.put("ttl",3);
        obj.put("version",35);
        JSONObject data=new JSONObject();
        JSONArray stationsArray=new JSONArray();
        Random r=new Random();

        for (int i=0;i<numberOfStations;i++){
            int numVeh = r.nextInt(30-0) +0 ;
            JSONObject stationStatus=createStationStatus(String.valueOf(i),true,true,true,15,3,0,"1","2","3",numVeh,0);
            stationsArray.add(stationStatus);
        }
        data.put("stations",stationsArray);
        obj.put("data",data);
        try{
            FileWriter file = new FileWriter("stationsStatus.json");
            file.write(obj.toJSONString());
            file.close();
        }catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }
    public static JSONObject createVehicleStatus(JSONArray vehicles){
        JSONObject obj=new JSONObject();
        obj.put("last_updated", 1640887163);
        obj.put("ttl",0);
        obj.put("version","3.0");
        JSONObject data=new JSONObject();
        data.put("vehicles",vehicles);
        obj.put("data",data);
        return obj;

    }

    public static JSONObject createFreeVehicle(String id,String lastReport, Double lat,Double lon, String isReserved,String isDisabled,String vehicleType){
        JSONObject obj= new JSONObject();

        obj.put("last_reported",lastReport);
        obj.put("lat",lat);
        obj.put("lon",lon);
        obj.put("vehicle_id",id);
        obj.put("is_reserved",isReserved);
        obj.put("is_reserved",isReserved);
        obj.put("vehicle_type_id",vehicleType);

        return(obj);
    }
    public static JSONArray createFreeVehicles(Network network,int numberOfVehicles,String mode, String fromCrs){
        JSONArray vehiclesArray=new JSONArray();
        Object[] nodesKeys =network.getNodes().keySet().toArray();
//        CoordinateTransformation ct= TransformationFactory.getCoordinateTransformation(fromCrs,TransformationFactory.WGS84);
        for ( int i=0; i<=numberOfVehicles;i++){
            Object nodeKey = nodesKeys[new Random().nextInt(nodesKeys.length)];
            Coord coordNode=network.getNodes().get(nodeKey).getCoord();
//            coordNode=ct.transform(coordNode);
             JSONObject vehicle=createFreeVehicle(String.valueOf(i),"proxy", coordNode.getY(), coordNode.getX(), "false","false",mode);
             vehiclesArray.add(vehicle);

        };

        return( vehiclesArray);
    }
    public static JSONObject createVehDockCapacity(int cap1,int cap2,int cap3,String id1,
                                                   String id2, String id3, int numbVehTypes){
        JSONObject obj=new JSONObject();
        switch(numbVehTypes){
            case 0:
                break;
            case 1:
                obj.put(id1,cap1);
                break;
            case 2:
                obj.put(id1,cap1);
                obj.put(id2,cap2);
                break;
            case 3:
                obj.put(id1,cap1);
                obj.put(id2,cap2);
                obj.put(id3,cap3);
        }
        return(obj);
    }

    public static JSONObject createStaticStation(String id,String name,Double lat, Double longitude,String openingHours,
                                                 String parkingType,Boolean parkingHoop,String contactPhone,
                                                 Boolean chargingStation, JSONObject vehTypeDockCapacity,int numVeh){

        JSONObject obj= new JSONObject();
        int docCapacity=3;
        if(numVeh!=0){docCapacity= (int) Math.round(numVeh*2.5);
        };
        obj.put("id",id);
        obj.put("name",name);
        obj.put("lat",lat);
        obj.put("lon",longitude);
        obj.put("station_opening_hours",openingHours);
        obj.put("parking_hoop",parkingHoop);
        obj.put("parking_type",parkingType);
        obj.put("contact_phone",contactPhone);
        obj.put("is_charging_station",chargingStation);
        obj.put("capacity",docCapacity);
        obj.put("vehicle_type_dock_capacity",vehTypeDockCapacity);
        return(obj);




    }

    public static void createGBFSVehicleTypes(){
        JSONObject obj = new JSONObject();
        //*
        obj.put("last_updated",0);
        obj.put("ttl",0);
        obj.put("version",3.0);

        JSONArray pricingPlans=new JSONArray();
        JSONObject p1= (JSONObject) new JSONObject().put("0","pricing 1");
        JSONObject p2= (JSONObject) new JSONObject().put("1","pricing 2");
        JSONObject p3= (JSONObject) new JSONObject().put("2","pricing 3");
        pricingPlans.add(p1);
        pricingPlans.add(p2);
        pricingPlans.add(p3);
        JSONObject vehAssets=createVehicleAssets("aa.com","ssss.com","15-01-2022");
        JSONObject vehTypes=new JSONObject();
        JSONObject vehiclesType1 = createVehicleType("1","bike","manual",
                "a",30,"none", vehAssets,"pricing 1",
                pricingPlans,2);
        JSONObject vehiclesType2 = createVehicleType("2","pedelec","electric",
                "pedelec",50,"none", vehAssets,"pricing 2",
                pricingPlans,2);

        vehTypes.put(0,vehiclesType1);
        vehTypes.put(1,vehiclesType2);
        obj.put("data",vehTypes);

        System.out.println(obj);
        try{
            FileWriter file = new FileWriter("output.json");
            file.write(obj.toJSONString());
            file.close();
        }catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }



    }
    public static JSONObject createVehicleType(String id,String form,String propulsion,String name,Integer reserveTime,String returnConstraint
            ,JSONObject vehicleAssets,String defaultPricing,JSONArray pricingPlans,int wheels){
        JSONObject vehType=new JSONObject();
        vehType.put("vehicle_type_id",id);
        vehType.put( "form_factor",form);

        vehType.put( "name",name);
        vehType.put("wheel_count",wheels);
        vehType.put("default_reserve_time",reserveTime);
        vehType.put("return_constraint",returnConstraint);
        // Create Array
        vehType.put("vehicle_assets",vehicleAssets);

        vehType.put("default_pricing_plan_id",defaultPricing);
        vehType.put("princing_plan_ids",pricingPlans);
        return(vehType);

    }
    public static JSONObject createVehicleAssets(String iconUrl, String iconURLDark, String iconLastMod){
        JSONObject vehAssets=new JSONObject();
        vehAssets.put("icon_url",iconUrl);
        vehAssets.put("icon_url_dark",iconURLDark);
        vehAssets.put( "icon_last_modified",iconLastMod);

        return(vehAssets);

    }


    public static JSONObject createStationStatus(String id, boolean isInstalled,boolean isRenting,boolean isReturning,Integer lastReported,
                                                 Integer numDocksAvai,Integer numDocksDisa, String id1,String id2,String id3,
                                                 int numVehAva, int numVehDis){
        JSONObject obj=new JSONObject();
        obj.put("station_id",id);
        obj.put("is_installed",isInstalled);
        obj.put("is_renting",isRenting);
        obj.put("is_returning",isReturning);
        obj.put("last_reported",lastReported);
        obj.put("num_docks_available",numDocksAvai);
        obj.put("num_docks_disabled",numDocksDisa);
        JSONArray vehicle_docks_available=new JSONArray();
        JSONArray vehicle_type_ids =null;
        JSONObject dock0 = new JSONObject();
        JSONObject dock1 = new JSONObject();
        JSONObject dock2 = new JSONObject();
        int dType0=0;
        int dType1=0;
        int dType2=0;

        dock0 = new JSONObject();
        vehicle_type_ids = createVehicleTypeIds(id1, id2, id3);
        dock0.put("vehicle_type_ids", vehicle_type_ids);
        dock0.put("count",dType0);
        dock1 = new JSONObject();
        vehicle_type_ids = createVehicleTypeIds(id1, id2, id3);
        dock1.put("vehicle_type_ids", vehicle_type_ids);
        dock1.put("count",dType1);
        dock2 = new JSONObject();
        vehicle_type_ids = createVehicleTypeIds(id1, id2, id3);
        dock2.put("vehicle_type_ids", vehicle_type_ids);
        dock2.put("count",dType2);



        obj.put("num_vehicles_available",numVehAva);
        obj.put("num_vehicles_disabled",numVehDis);




        return(obj);
        }

        public static JSONArray createVehicleTypeStation(String id1,String id2,String id3,Integer numVehAva){
        Integer min=0;
        int max=1;
        if(numVehAva>0){
            max=numVehAva;
        }

        Random r= new Random();
        int nID1=0;
        int nID2=0;
        int nID3=0;
        do{
             nID1=r.nextInt(max-min)+min;
             nID2=r.nextInt(max-min)+min;
             nID3=r.nextInt(max-min)+min;
        }while(nID1+nID2+nID3!=max);

        JSONObject  iD1=  new JSONObject();
        iD1.put(id1,nID1);
        JSONObject  iD2=  new JSONObject();
        iD2.put(id3,nID2);
        JSONObject  iD3=  new JSONObject();
        iD2.put(id3,nID3);
        JSONArray vehType=new JSONArray();
        vehType.add(iD1);
        vehType.add(iD2);
        vehType.add(iD3);
        return(vehType);

        }
        public static JSONArray createVehicleTypeIds(String id1, String id2, String id3){
        JSONArray vehType_ids=new JSONArray();
        vehType_ids.add(0,id1);
        vehType_ids.add(1,id2);
        vehType_ids.add(2,id3);
        return(vehType_ids);

        }
        public static JSONObject createVehicleFreeFloating(String id, Integer lastReported,Double latitude,Double longitude,
                                    Boolean isReserved,Boolean isDisabled, String vehTypeId){
        JSONObject obj=new JSONObject();
        obj.put("vehicle_id",id);
        obj.put("last_reported",lastReported);
        obj.put("lat",latitude);
        obj.put("lon",longitude);
        obj.put("is_reserved",isReserved);
        obj.put("is_disabled",isDisabled);
        obj.put("vehicle_type_id", vehTypeId);
        return(obj);

        }

    public static JSONObject createVehicleStationBased(String id, Integer lastReported,Double range,Integer stationID,
                                                       Boolean isReserved,Boolean isDisabled, String vehTypeId){
        JSONObject obj=new JSONObject();
        obj.put("vehicle_id",id);
        obj.put("last_reported",lastReported);
        obj.put("current_range_meters",range);
        obj.put("station_id",stationID);
        obj.put("is_reserved",isReserved);
        obj.put("is_disabled",isDisabled);
        obj.put("vehicle_type_id", vehTypeId);
        return(obj);

    }
    public static JSONObject  createVehiclesFile(){
        JSONObject obj=new JSONObject();
        obj.put("last_updated",1235);
        obj.put("ttl",0);
        obj.put("version","3.0");
        JSONObject data=new JSONObject();
        JSONArray vehicles=new JSONArray();
        vehicles.add(createVehicleFreeFloating("1",0,10.0,15.3,true,false,"t1"));
        vehicles.add(createVehicleFreeFloating("2",0,10.0,19.5,true,false,"t1"));
        vehicles.add(createVehicleStationBased("3",0,351.2,1,false,false,"t1"));
        data.put("vehicles",vehicles);
        obj.put("data",data);
        return(obj);

    }
     public static JSONObject createGeoFencingZone(int numCorners,Double xMax,Double yMax,Double xMin,Double yMin){
        JSONObject obj=new JSONObject();
        obj.put("last_updated",1235);
        obj.put("ttl",0);
        obj.put("version","3.0");
        JSONObject data= new JSONObject();
        JSONObject geofencing_zones=new JSONObject();
        JSONArray features=new JSONArray();
        JSONObject geofence=new JSONObject();
        geofence.put("type","Feature");
        JSONObject geometry= new JSONObject();
        geometry.put("type","MultiPolygon");
        JSONArray coords= new JSONArray();
        JSONArray currentCoord=new JSONArray();
        HashMap<Integer,JSONArray> mapCoords=new HashMap<>();
        for (int i=0;i< numCorners;i++){
            currentCoord=new JSONArray();
            Double xCoord= (Math.random() * ((xMax - xMin) + 1)) + xMin;
            Double yCoord= (Math.random() * ((yMax - yMin) + 1)) + yMin;
            currentCoord.add(xCoord);
            currentCoord.add(yCoord);
            mapCoords.put(i,currentCoord);


        }
        for ( Integer i=0;i<numCorners;i++){
            JSONArray mapCoord=mapCoords.get(i);
            coords.add(mapCoord);
        }
        geometry.put("coordinates",coords);
        geofence.put("geometry",geometry);
        features.add(geofence);
        geofencing_zones.put("geofencing_zones",features);
        geofencing_zones.put("type","FeatureCollection");
        data.put("geofencing_zones",geofencing_zones);
        obj.put("data",data);

         System.out.println(obj);
         try{
             FileWriter file = new FileWriter("geoFencingZones.geojson");
             file.write(obj.toJSONString());
             file.close();
         }catch (IOException e) {
             // TODO Auto-generated catch block
             e.printStackTrace();
         }
         return(obj);
     }

    }




