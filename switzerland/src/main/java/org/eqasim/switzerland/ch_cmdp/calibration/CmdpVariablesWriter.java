package org.eqasim.switzerland.ch_cmdp.calibration;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Map;
import org.eqasim.core.components.calibration.writer.StandardVariablesWriter;


public class CmdpVariablesWriter extends StandardVariablesWriter {

    public CmdpVariablesWriter() {
        super();
    }

    @Override
    protected void writeHeader(BufferedWriter writer, String mode) {
        try {
            String commonHeader = "person_id;trip_index;departure_time;utility;" +
                    "euclideanDistance_km;" +
                    "age;sex;region;retired;junior;lowIncome;income;canton;" +
                    "originHome;" +
                    "destinationWork;destinationHome;destinationEducation;destinationShopping;destinationLeisure;destinationOther;" +
                    "urbanDestination;urbancoreDestination;" +
                    "shortDistance;longDistance;";

            switch (mode) {
                case "pt" -> writer.write(commonHeader +
                        "goodPtService;mediumPtService;" +
                        "accessEgressTime_min;inVehicleTime_min;waitingTime_min;numberOfLineSwitches;" +
                        "cost_MU\n");

                case "car" -> writer.write(commonHeader +
                        "carOwnershipRatio;" +
                        "suburbanDestination;" +
                        "travelTime_min;parking_searching_duration_min;" +
                        "cost_MU;parking_cost_MU\n");

                case "bike", "walk" -> writer.write(commonHeader +
                        "travelTime_min\n");

                case "car_passenger" -> writer.write(commonHeader +
                        "drivingLicense;veryLongDistance;carOwnershipRatio;hasCar;" +
                        "travelTime_min\n");

                default -> System.err.println("Unknown mode: " + mode);
            }
            writer.flush();
        } catch (IOException e) {
            System.err.println("Failed to write the header for mode \"" + mode + "\": " + e.getMessage());
        }
    }

    @Override
    public void writeVariables(String mode, String personId, int tripIndex, double departureTime,
                               double utility, Map<String, String> attributes) {
        if (!initiated) {
            return;
        }

        synchronized (LOCK) {
            try {
                StringBuilder sb = new StringBuilder();

                // Common attributes (must match commonHeader ordering)
                sb.append(String.format("%s;%d;%d;%.3f;%.4f;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s",
                        personId,
                        tripIndex,
                        (int) departureTime,
                        utility,
                        Double.parseDouble(attributes.get("euclideanDistance_km")),
                        attributes.get("age"),
                        attributes.get("sex"),
                        attributes.get("region"),
                        attributes.get("retired"),
                        attributes.get("junior"),
                        attributes.get("lowIncome"),
                        attributes.get("income"),
                        attributes.get("canton"),
                        attributes.get("originHome"),
                        attributes.get("destinationWork"),
                        attributes.get("destinationHome"),
                        attributes.get("destinationEducation"),
                        attributes.get("destinationShopping"),
                        attributes.get("destinationLeisure"),
                        attributes.get("destinationOther"),
                        attributes.get("urbanDestination"),
                        attributes.get("urbancoreDestination"),
                        attributes.get("shortDistance"),
                        attributes.get("longDistance")
                ));

                switch (mode) {
                    case "pt":
                        sb.append(String.format(";%s;%s;%s;%s;%s;%s;%s\n",
                                attributes.get("goodPtService"),
                                attributes.get("mediumPtService"),
                                attributes.get("accessEgressTime_min"),
                                attributes.get("inVehicleTime_min"),
                                attributes.get("waitingTime_min"),
                                attributes.get("numberOfLineSwitches"),
                                attributes.get("cost_MU")
                        ));
                        ptWriter.write(sb.toString());
                        ptWriter.flush();
                        break;

                    case "car":
                        sb.append(String.format(";%s;%s;%s;%s;%s;%s\n",
                                attributes.get("carOwnershipRatio"),
                                attributes.get("suburbanDestination"),
                                attributes.get("travelTime_min"),
                                attributes.get("parking_searching_duration_min"),
                                attributes.get("cost_MU"),
                                attributes.get("parking_cost_MU")
                        ));
                        carWriter.write(sb.toString());
                        carWriter.flush();
                        break;

                    case "bike":
                        sb.append(String.format(";%s\n", attributes.get("travelTime_min")));
                        bikeWriter.write(sb.toString());
                        bikeWriter.flush();
                        break;

                    case "walk":
                        sb.append(String.format(";%s\n", attributes.get("travelTime_min")));
                        walkWriter.write(sb.toString());
                        walkWriter.flush();
                        break;

                    case "car_passenger":
                        sb.append(String.format(";%s;%s;%s;%s;%s\n",
                                attributes.get("drivingLicense"),
                                attributes.get("veryLongDistance"),
                                attributes.get("carOwnershipRatio"),
                                attributes.get("hasCar"),
                                attributes.get("travelTime_min")
                        ));
                        cpWriter.write(sb.toString());
                        cpWriter.flush();
                        break;

                    default:
                        System.err.println("Unknown mode: " + mode);
                }
            } catch (IOException e) {
                System.err.println("Failed to write to CSV: " + e.getMessage());
            }
        }
    }

}
