package org.eqasim.switzerland.ch_cmdp.memory;

import java.nio.file.*;
import java.util.*;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.matsim.api.core.v01.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.households.Household;
import static org.junit.Assert.*;

public class TestScenarioAttributeArchive {
    @Rule public TemporaryFolder temp = new TemporaryFolder();
    private enum Category { FIRST, SECOND }

    @Test public void stableObjectsRoundTripTypesAndLongValuesWithoutRestoringOtherObjects() throws Exception {
        var scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        var person = scenario.getPopulation().getFactory().createPerson(Id.createPersonId("person"));
        scenario.getPopulation().addPerson(person);
        var values = new LinkedHashMap<String, Object>();
        values.put("string", "?cole ??"); values.put("integer", -7); values.put("long", Long.MAX_VALUE);
        values.put("double", -1.25); values.put("float", 2.5f); values.put("yes", true); values.put("no", false);
        values.put("short", (short) -3); values.put("byte", (byte) 7); values.put("char", '?');
        values.put("null", null); values.put("enum", Category.SECOND);
        values.put("longString", "metadata".repeat(20_000));
        values.put("doubles", new double[]{0, -1, Double.NaN, Double.POSITIVE_INFINITY});
        values.put("integers", new int[]{1, -2, Integer.MAX_VALUE}); values.put("longs", new long[]{0, Long.MIN_VALUE});
        var bytes = new byte[2_500_000]; new Random(19).nextBytes(bytes); values.put("bytes", bytes);
        values.put("coordinate", new Coord(1, 2, 3)); values.put("map", Map.of("key", "value"));
        values.forEach(person.getAttributes()::putAttribute);
        person.getAttributes().putAttribute("age", 40);
        person.getAttributes().putAttribute("home_x", 100.0); person.getAttributes().putAttribute("home_y", 200.0);
        person.getAttributes().putAttribute("home_coordinate_x", 100.0); person.getAttributes().putAttribute("home_coordinate_y", 200.0);
        var plan = scenario.getPopulation().getFactory().createPlan();
        var activity = scenario.getPopulation().getFactory().createActivityFromCoord("home", new Coord(0, 0));
        activity.getAttributes().putAttribute("discardedActivity", 1); plan.addActivity(activity);
        var leg = scenario.getPopulation().getFactory().createLeg("walk");
        leg.getAttributes().putAttribute("discardedLeg", 2); plan.addLeg(leg); person.addPlan(plan);
        var network = scenario.getNetwork();
        var a = network.getFactory().createNode(Id.createNodeId("a"), new Coord(0, 0));
        var b = network.getFactory().createNode(Id.createNodeId("b"), new Coord(1, 0)); network.addNode(a); network.addNode(b);
        var link = network.getFactory().createLink(Id.createLinkId("road"), a, b); network.addLink(link);
        link.getAttributes().putAttribute("metadata", "road metadata"); link.getAttributes().putAttribute("penalty", 0.2);
        a.getAttributes().putAttribute("discardedNode", "node");
        var household = scenario.getHouseholds().getFactory().createHousehold(Id.create("household", Household.class));
        household.getMemberIds().add(person.getId()); household.getAttributes().putAttribute("metadata", 42);
        scenario.getHouseholds().getHouseholds().put(household.getId(), household);

        var archive = ScenarioAttributeCleaner.cleanAndArchive(scenario, temp.getRoot().toPath());
        assertTrue(Files.size(archive.file()) > 2_000_000); // Exercises multiple compressed-memory segments.
        assertEquals(values.size() + 4, archive.attributeCount());
        for (String name : values.keySet()) assertFalse(person.getAttributes().getAsMap().containsKey(name));
        assertNull(person.getAttributes().getAttribute("home_coordinate_x"));
        assertNull(link.getAttributes().getAttribute("metadata")); assertTrue(household.getAttributes().isEmpty());
        archive.restore();
        for (var entry : values.entrySet()) {
            Object actual = person.getAttributes().getAttribute(entry.getKey()), expected = entry.getValue();
            if (expected instanceof double[] x) assertArrayEquals(x, (double[]) actual, 0);
            else if (expected instanceof int[] x) assertArrayEquals(x, (int[]) actual);
            else if (expected instanceof long[] x) assertArrayEquals(x, (long[]) actual);
            else if (expected instanceof byte[] x) assertArrayEquals(x, (byte[]) actual);
            else if (expected instanceof Coord coord) { assertEquals(coord.getZ(), ((Coord) actual).getZ(), 0); }
            else assertEquals(expected, actual);
        }
        assertTrue(person.getAttributes().getAsMap().containsKey("null"));
        assertEquals(100.0, person.getAttributes().getAttribute("home_coordinate_x"));
        assertEquals("road metadata", link.getAttributes().getAttribute("metadata"));
        assertEquals(42, household.getAttributes().getAttribute("metadata"));
        assertTrue(a.getAttributes().isEmpty()); assertTrue(activity.getAttributes().isEmpty()); assertTrue(leg.getAttributes().isEmpty());
        assertEquals(archive.attributeCount(), archive.restoredAttributeCount());
        archive.restore(); // Idempotent even if final-writer and shutdown paths both invoke it.
        Path file = archive.file(); archive.notifyShutdown(new ShutdownEvent(null, false, 1, null));
        assertFalse(Files.exists(file)); assertFalse(Files.exists(file.getParent()));
    }

    @Test public void currentValuesWinDeletedObjectsAreSkippedAndNewPeopleAreUntouched() throws Exception {
        var scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        for (String id : List.of("stable", "deleted")) {
            var person = scenario.getPopulation().getFactory().createPerson(Id.createPersonId(id));
            person.getAttributes().putAttribute("metadata", "original"); scenario.getPopulation().addPerson(person);
        }
        var stable = scenario.getPopulation().getPersons().get(Id.createPersonId("stable"));
        stable.getAttributes().putAttribute("wasNull", "original");
        var archive = ScenarioAttributeCleaner.cleanAndArchive(scenario, temp.getRoot().toPath());
        stable.getAttributes().putAttribute("metadata", "updated"); stable.getAttributes().putAttribute("wasNull", null);
        scenario.getPopulation().removePerson(Id.createPersonId("deleted"));
        var clone = scenario.getPopulation().getFactory().createPerson(Id.createPersonId("new-clone")); scenario.getPopulation().addPerson(clone);
        archive.restore();
        assertEquals("updated", stable.getAttributes().getAttribute("metadata"));
        assertNull(stable.getAttributes().getAttribute("wasNull")); assertTrue(clone.getAttributes().isEmpty());
        assertEquals(0, archive.restoredAttributeCount());
    }

    @Test public void unsupportedValuesStayInMemoryAndEmptyArchivesCreateNoFiles() throws Exception {
        var scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        var person = scenario.getPopulation().getFactory().createPerson(Id.createPersonId("person")); scenario.getPopulation().addPerson(person);
        var unsupported = new Object(); person.getAttributes().putAttribute("opaque", unsupported);
        var archive = ScenarioAttributeCleaner.cleanAndArchive(scenario, temp.getRoot().toPath());
        assertSame(unsupported, person.getAttributes().getAttribute("opaque"));
        assertNull(archive.file()); assertEquals(0, archive.attributeCount()); archive.restore();
        try (var files = Files.list(temp.getRoot().toPath())) { assertEquals(0, files.count()); }
    }

    @Test public void unexpectedShutdownRetainsArchiveAndTruncationPreventsSilentOutput() throws Exception {
        var scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        var person = scenario.getPopulation().getFactory().createPerson(Id.createPersonId("person"));
        person.getAttributes().putAttribute("metadata", "original"); scenario.getPopulation().addPerson(person);
        var archive = ScenarioAttributeCleaner.cleanAndArchive(scenario, temp.getRoot().toPath());
        archive.notifyShutdown(new ShutdownEvent(null, true, 0, new IllegalStateException("failure")));
        assertTrue(Files.exists(archive.file())); assertNull(person.getAttributes().getAttribute("metadata"));
        byte[] bytes = Files.readAllBytes(archive.file()); Files.write(archive.file(), Arrays.copyOf(bytes, bytes.length / 2));
        assertThrows(IllegalStateException.class, archive::restore); assertTrue(Files.exists(archive.file()));
    }
}
