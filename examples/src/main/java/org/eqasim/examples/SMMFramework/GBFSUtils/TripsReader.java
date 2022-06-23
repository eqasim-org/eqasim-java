package org.eqasim.examples.SMMFramework.GBFSUtils;

import com.google.common.io.Resources;
import org.eqasim.core.components.EqasimMainModeIdentifier;
import org.eqasim.ile_de_france.IDFConfigurator;
import org.matsim.analysis.TripsAndLegsCSVWriter;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;

import java.net.URL;
import java.util.HashMap;
import java.util.List;

public class TripsReader {


    public static void main(String[] args) {
        Population pop= PopulationUtils.readPopulation("C:\\Users\\juan_\\Desktop\\TUM\\Semester5\\Thesis\\FrameworkCommon\\ComputationalSMMResults\\Scenario6\\output_plans.xml.gz");

   IdMap<Person,Plan> ptShare= new IdMap<>(Person.class);
        for(Id<Person> personKey:pop.getPersons().keySet()){
            Person person=pop.getPersons().get(personKey);
            Plan plan= person.getSelectedPlan();
            List<PlanElement> elements =plan.getPlanElements();
            for(int i=0;i< elements.size();i++){
                PlanElement element=elements.get(i);
                if(element instanceof Activity){
                    Activity act=(Activity) element;
                    System.out.println(act.getType());
                    if((act.getType().equals("PTSharing interaction"))||(act.getType().equals("SharingPT interaction"))){

//                        act.setType("PTSharing interaction");
                        act.getType();
                        if(!ptShare.containsKey(person.getId())){
                            ptShare.put(person.getId(), plan);
                        }

                    }
                }
            }

   }
        HashMap<String, TripStructureUtils.Trip>tripList=new HashMap<>();
        for (Id<Person> key:ptShare.keySet()){
            Person person=pop.getPersons().get(key);
            Plan plan= person.getSelectedPlan();
            List<PlanElement> elements =plan.getPlanElements();
            List<TripStructureUtils.Trip>tripsPlan=TripStructureUtils.getTrips(plan);
            for(int i=0;i<tripsPlan.size();i++){
                TripStructureUtils.Trip trip=tripsPlan.get(i);
                tripList.put((person.getAttributes().getAttribute("censusPersonId")+"_"+String.valueOf(i)),trip);
            }
        }

        for(String key:tripList.keySet()){
            TripStructureUtils.Trip trip=tripList.get(key);
//            List<Legs>
        }
   
        URL configUrl = Resources.getResource("corsica/corsica_config.xml");
        Config config = ConfigUtils.loadConfig(configUrl, IDFConfigurator.getConfigGroups());
        Scenario scenario = ScenarioUtils.createScenario(config);
        scenario= ScenarioUtils.loadScenario(config);
        TripsAndLegsCSVWriter tripsReaderWriter= new TripsAndLegsCSVWriter(scenario,new SharingPTTripWriter(),new NoLegsWriter() ,new EqasimMainModeIdentifier());
        tripsReaderWriter.write( ptShare,"tripsTry.csv","legsTry");

        String x="uwuw";
    }
    
}
