package org.matsim.analysis;

import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.util.TravelTime;

import static org.junit.Assert.*;

public class TestLazyCalcLinkStats {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    /** Load only the original calculator from the pinned dependency, sharing all API types.
     * This compares actual upstream output, rather than duplicating its averaging formulas.
     */
    private URLClassLoader upstream() {
        return new URLClassLoader(new java.net.URL[] {
                Network.class.getProtectionDomain().getCodeSource().getLocation() },
                getClass().getClassLoader()) {
            @Override protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (!name.equals(CalcLinkStats.class.getName()) && !name.startsWith(CalcLinkStats.class.getName() + "$"))
                    return super.loadClass(name, resolve);
                var c = findLoadedClass(name);
                if (c == null) c = findClass(name);
                if (resolve) resolveClass(c);
                return c;
            }
        };
    }

    @Test public void rowsAreLazyAndAveragingOutputMatchesPinnedMatsim() throws Exception {
        var network = network();
        var lazy = new CalcLinkStats(network);
        assertEquals(0, rows(lazy));
        assertEquals(0, lazy.getAvgLinkVolumes(Id.createLinkId("a")).length);
        assertEquals(0, rows(lazy));
        try (var loader = upstream()) {
            var type = loader.loadClass(CalcLinkStats.class.getName());
            var eager = type.getConstructor(Network.class).newInstance(network);
            assertNotSame(CalcLinkStats.class, type);
            for (int iteration = 0; iteration < 3; iteration++) {
                var volumes = volumes(network, iteration);
                final int day = iteration;
                TravelTime times = (link, time, person, vehicle) -> 10 + day * 5 + time / 3600;
                lazy.addData(volumes, times);
                type.getMethod("addData", VolumesAnalyzer.class, TravelTime.class).invoke(eager, volumes, times);
                compareFiles(lazy, eager, "iteration-" + iteration);
                for (var id : network.getLinks().keySet()) {
                    assertArrayEquals((double[]) type.getMethod("getAvgLinkVolumes", Id.class).invoke(eager, id),
                            lazy.getAvgLinkVolumes(id), 0);
                    var avgTimes = type.getDeclaredMethod("getAvgTravelTimes", Id.class);
                    avgTimes.setAccessible(true);
                    assertArrayEquals((double[]) avgTimes.invoke(eager, id), lazy.getAvgTravelTimes(id), 0);
                }
            }
            assertEquals(2, rows(lazy));
            lazy.reset();
            type.getMethod("reset").invoke(eager);
            assertEquals(0, rows(lazy));
            compareFiles(lazy, eager, "empty-after-reset");
        }
    }

    @Test public void explicitWritesReadsAndScalingMatchPinnedMatsim() throws Exception {
        var network = network();
        var lazy = new CalcLinkStats(network, 2.0);
        try (var loader = upstream()) {
            var type = loader.loadClass(CalcLinkStats.class.getName());
            var eager = type.getConstructor(Network.class, double.class).newInstance(network, 2.0);
            compareFiles(lazy, eager, "empty");
            var producer = new CalcLinkStats(network);
            producer.addData(volumes(network, 0), (link, time, person, vehicle) -> 12);
            var input = temporary.newFile("input.txt").toPath();
            producer.writeFile(input.toString());
            readBoth(lazy, eager, input);
            compareFiles(lazy, eager, "read-scaled");

            // The legacy format has no origId column and integer hourly averages.
            var legacy = temporary.newFile("legacy.txt").toPath();
            var lines = Files.readAllLines(input);
            for (int i = 1; i < lines.size(); i++) {
                var fields = new java.util.ArrayList<>(java.util.List.of(lines.get(i).split("\\t", -1)));
                fields.remove(1);
                for (int h = 0; h < 24; h++) fields.set(7 + h * 3, Integer.toString((int) Double.parseDouble(fields.get(7 + h * 3))));
                lines.set(i, String.join("\t", fields));
            }
            Files.write(legacy, lines);
            readBoth(lazy, eager, legacy);
            compareFiles(lazy, eager, "read-legacy");

            Files.writeString(input, "");
            readBoth(lazy, eager, input);
            compareFiles(lazy, eager, "read-empty");
            Files.writeString(input, "header\n");
            readBoth(lazy, eager, input);
            assertEquals(24, lazy.getAvgLinkVolumes(Id.createLinkId("a")).length);
            compareFiles(lazy, eager, "read-header-only");
        }
    }

    @Test public void usesResetTimeLinkSetEvenIfNetworkGrowsBeforeFirstData() throws Exception {
        var network = network();
        var lazy = new CalcLinkStats(network);
        try (var loader = upstream()) {
            var type = loader.loadClass(CalcLinkStats.class.getName());
            var eager = type.getConstructor(Network.class).newInstance(network);
            var a = network.getLinks().get(Id.createLinkId("a"));
            NetworkUtils.createAndAddLink(network, Id.createLinkId("later"), a.getFromNode(), a.getToNode(), 50, 10, 900, 1);
            var volumes = volumes(network, 2);
            TravelTime times = (link, time, person, vehicle) -> 15;
            lazy.addData(volumes, times);
            type.getMethod("addData", VolumesAnalyzer.class, TravelTime.class).invoke(eager, volumes, times);
            compareFiles(lazy, eager, "network-growth");
            assertEquals(2, rows(lazy));
            lazy.reset();
            assertEquals(0, rows(lazy));
            lazy.addData(volumes, times);
            assertEquals(3, rows(lazy));
        }
    }

    private void readBoth(CalcLinkStats lazy, Object eager, Path input) throws Exception {
        lazy.readFile(input.toString());
        eager.getClass().getMethod("readFile", String.class).invoke(eager, input.toString());
    }

    private void compareFiles(CalcLinkStats lazy, Object eager, String name) throws Exception {
        var actual = temporary.newFile(name + "-lazy.txt").toPath();
        var expected = temporary.newFile(name + "-original.txt").toPath();
        lazy.writeFile(actual.toString());
        eager.getClass().getMethod("writeFile", String.class).invoke(eager, expected.toString());
        assertEquals(name, Files.readString(expected), Files.readString(actual));
    }

    private int rows(CalcLinkStats stats) throws Exception {
        var field = CalcLinkStats.class.getDeclaredField("linkData");
        field.setAccessible(true);
        return ((Map<?, ?>) field.get(stats)).size();
    }

    private static VolumesAnalyzer volumes(Network network, int day) {
        return new VolumesAnalyzer(3600, 24 * 3600 - 1, network) {
            @Override public double[] getVolumesPerHourForLink(Id<Link> id) {
                double[] result = new double[24];
                // Include both varying positive volumes and a completely unused link.
                if (id.toString().equals("a")) for (int h = 0; h < 24; h++) result[h] = (day + h) % 5;
                return result;
            }
        };
    }

    private static Network network() {
        var network = NetworkUtils.createNetwork();
        var a = NetworkUtils.createAndAddNode(network, Id.createNodeId("a"), new Coord(0, 0));
        var b = NetworkUtils.createAndAddNode(network, Id.createNodeId("b"), new Coord(100, 0));
        NetworkUtils.createAndAddLink(network, Id.createLinkId("a"), a, b, 100, 10, 900, 1);
        NetworkUtils.createAndAddLink(network, Id.createLinkId("b"), b, a, 100, 10, 900, 1);
        return network;
    }
}
