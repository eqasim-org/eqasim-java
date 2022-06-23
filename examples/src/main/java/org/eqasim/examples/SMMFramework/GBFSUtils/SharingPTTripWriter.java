package org.eqasim.examples.SMMFramework.GBFSUtils;

import org.matsim.analysis.TripsAndLegsCSVWriter;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.router.TripStructureUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Class adapts the MATSIM trip writer to add information odf SMM multimodal legs into trip
 */
public class SharingPTTripWriter implements TripsAndLegsCSVWriter.CustomTripsWriterExtension {

    public SharingPTTripWriter() {
    }

    @Override
    public String[] getAdditionalTripHeader() {
        String [] returnHeaders=new String[]{"Access_Sharing","Egress_Sharing",
                "Access_Sharing_Time","Egress_Sharing_Time","Access_Sharing_Distance","Egress_Sharing_Distance",
                "Access_Sharing_Destination_x","Access_Sharing_Destination_y","Egress_Sharing_Origin_x","Egress_Sharing_Origin_y"};
        return returnHeaders;
    }

    @Override
    public List<String> getAdditionalTripColumns(TripStructureUtils.Trip trip) {
        List<String> additional=new ArrayList<>();
        List<PlanElement> planElementList=trip.getTripElements();

        for(PlanElement element:planElementList){
            if(element instanceof Activity){
                Activity act=(Activity) element;
                System.out.println(act.getType());
            }
        }
       Integer sharingPTInt=findActivityIndex(planElementList,"SharingPT interaction");
       Integer PTSharingInt=findActivityIndex(planElementList,"PTSharing interaction");
        Activity sharingPT= null;
        Activity pTSharing=null;

       if(sharingPTInt!=null){
           additional.add("Yes");
           sharingPT= (Activity) planElementList.get(sharingPTInt);

       }else{
           additional.add("No");
       }
        if(PTSharingInt!=null){
            additional.add("Yes");
            pTSharing=(Activity) planElementList.get(PTSharingInt);

        }else{
            additional.add("No");
        }
        additional.add(String.valueOf(calculateAccessTime(planElementList,sharingPTInt)));
        additional.add(String.valueOf(calculateEgressTime(planElementList,PTSharingInt)));
        additional.add(String.valueOf(calculateAccessDist(planElementList,sharingPTInt)));
        additional.add(String.valueOf(calculateEgressDist(planElementList,PTSharingInt)));
        if(sharingPT!=null){
            additional.add(String.valueOf(sharingPT.getCoord().getX()));
            additional.add(String.valueOf(sharingPT.getCoord().getY()));
        }else{
            additional.add("None");
            additional.add("None");
        }
        if(pTSharing!=null){
            additional.add(String.valueOf(pTSharing.getCoord().getX()));
            additional.add(String.valueOf(pTSharing.getCoord().getY()));
        }else{
            additional.add("None");
            additional.add("None");
        }
        return additional;

    }

    public Double calculateAccessDist(List<PlanElement>list, Integer index) {
        Double accessDist = 0.0;
        if (index != null){
            for (int i = index; i > 0; i--) {
                PlanElement element = list.get(i);
                if (element instanceof Leg) {
                    Leg elementL = (Leg) element;
                    accessDist += elementL.getRoute().getDistance();
                }
            }
        }
        return accessDist;
    }
    public Double calculateAccessTime(List<PlanElement>list, Integer index){
        Double accessTime=0.0;
        if (index != null){
            for(int i=index;i>0;i--) {
                PlanElement element = list.get(i);
                if (element instanceof Leg) {
                    Leg elementL = (Leg) element;
                    accessTime+= elementL.getRoute().getTravelTime().seconds();
                }
            }
        }
        return accessTime;
    }

    public Double calculateEgressDist(List<PlanElement>list, Integer index){
        Double egressDist=0.0;
        if (index != null) {
            for (int i = index; i < list.size(); i++) {
                PlanElement element = list.get(i);
                if (element instanceof Leg) {
                    Leg elementL = (Leg) element;
                    egressDist += elementL.getRoute().getDistance();
                }
            }
        }
        return egressDist;
    }
    public Double calculateEgressTime(List<PlanElement>list, Integer index){
        Double egressTime=0.0;
        if (index != null) {
            for (int i = index; i < list.size(); i++) {
                PlanElement element = list.get(i);
                if (element instanceof Leg) {
                    Leg elementL = (Leg) element;
                    egressTime += elementL.getRoute().getTravelTime().seconds();
                }
            }
        }
        return egressTime;
    }
    public Integer findActivityIndex(List<PlanElement>list, String activityType){
        Integer index=null;

            for (int i = 0; i < list.size(); i++) {
                PlanElement element = list.get(i);
                if (element instanceof Activity) {
                    Activity act = (Activity) element;
                    if (act.getType().equals(activityType)) {
                        index = i;
                    }
                }
            }

        return index;
    }
}
