package org.eqasim.switzerland.ch_cmdp.utils.link_stats;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;

public class TestRunDetailedLinkStatistics {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    private Path gzip(String name, String content) throws Exception {
        Path path = temporary.getRoot().toPath().resolve(name);
        try (var stream = new GZIPOutputStream(Files.newOutputStream(path))) {
            stream.write(content.getBytes(StandardCharsets.UTF_8));
        }
        return path;
    }

    @Test
    public void replaysFilesWithoutNetworkOrVehiclesAndAppliesCutoff() throws Exception {
        // Resolve test DTDs from MATSim's jar, so this test never depends on HTTP.
        String populationDtd = org.matsim.core.config.ConfigUtils.class.getResource("/dtd/population_v6.dtd").toExternalForm();
        String scheduleDtd = org.matsim.core.config.ConfigUtils.class.getResource("/dtd/transitSchedule_v2.dtd").toExternalForm();
        Path population = gzip("plans.xml.gz", """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE population SYSTEM "http://www.matsim.org/files/dtd/population_v6.dtd">
                <population>
                  <person id="french">
                    <attributes><attribute name="isExternalFR" class="java.lang.Boolean">true</attribute></attributes>
                    <plan selected="yes"><activity type="home" x="0" y="0"/></plan>
                  </person>
                  <person id="freight">
                    <attributes><attribute name="isFreight" class="java.lang.Boolean">true</attribute></attributes>
                    <plan selected="yes"><activity type="home" x="0" y="0"/></plan>
                  </person>
                </population>
                """.replace("http://www.matsim.org/files/dtd/population_v6.dtd", populationDtd));
        Path schedule = temporary.getRoot().toPath().resolve("schedule.xml");
        Files.writeString(schedule, """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE transitSchedule SYSTEM "http://www.matsim.org/files/dtd/transitSchedule_v2.dtd">
                <transitSchedule>
                  <transitStops>
                    <stopFacility id="stop" x="0" y="0" linkRefId="link"/>
                  </transitStops>
                  <transitLine id="line">
                    <transitRoute id="bus">
                      <transportMode>bus</transportMode>
                      <routeProfile><stop refId="stop" departureOffset="00:00:00"/></routeProfile>
                      <route><link refId="link"/></route>
                      <departures/>
                    </transitRoute>
                    <transitRoute id="train">
                      <transportMode>rail</transportMode>
                      <routeProfile><stop refId="stop" departureOffset="00:00:00"/></routeProfile>
                      <route><link refId="link"/></route>
                      <departures/>
                    </transitRoute>
                  </transitLine>
                </transitSchedule>
                """.replace("http://www.matsim.org/files/dtd/transitSchedule_v2.dtd", scheduleDtd));
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\"?><events version=\"1.0\">\n");
        for (String route : new String[] { "bus", "train" }) {
            xml.append("<event time=\"0\" type=\"TransitDriverStarts\" driverId=\"driver\" vehicleId=\"")
                    .append(route).append("\" transitLineId=\"line\" transitRouteId=\"").append(route)
                    .append("\" departureId=\"dep\"/>\n");
        }
        String[][] vehicles = { { "car", "french", "car" }, { "truck", "freight", "car" },
                { "bike", "french", "bike" }, { "bus", "driver", "pt" }, { "train", "driver", "pt" } };
        for (String[] vehicle : vehicles) {
            xml.append("<event time=\"1\" type=\"vehicle enters traffic\" person=\"").append(vehicle[1])
                    .append("\" link=\"link\" vehicle=\"").append(vehicle[0]).append("\" networkMode=\"")
                    .append(vehicle[2]).append("\" relativePosition=\"1.0\"/>\n");
        }
        for (int time : new int[] { 2, 108000 }) {
            for (String[] vehicle : vehicles) {
                xml.append("<event time=\"").append(time).append("\" type=\"entered link\" link=\"link\" vehicle=\"")
                        .append(vehicle[0]).append("\"/>\n");
            }
        }
        xml.append("</events>");
        Path events = gzip("events.xml.gz", xml.toString());
        Path output = temporary.getRoot().toPath().resolve("nested/output_detailed_link_statistics.csv.gz");
        RunDetailedLinkStatistics.main(new String[] {
                "--events-path", events.toString(), "--population-path", population.toString(),
                "--transit-schedule-path", schedule.toString(), "--output-path", output.toString() });
        try (var stream = new GZIPInputStream(Files.newInputStream(output))) {
            var rows = new String(stream.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
            assertEquals(2, rows.size());
            assertEquals("link_id,hour,start_time_s,end_time_s,cars,trucks,bikes,buses,"
                    + "cars_french,cars_crossborder,cars_swiss,cars_unknown", rows.get(0));
            assertEquals("\"link\",0,0,3600,2,2,2,2,2,0,0,0", rows.get(1));
        }
    }
}
