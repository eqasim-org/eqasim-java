package org.eqasim.switzerland.ch_cmdp.dsim;

import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class TestDsimJvmSupport {
    @Test public void preservesHeapPropertiesAndArgumentsWithoutShellQuoting() {
        var args = new String[] {"--config-path", "scenario with spaces/config.xml", "--use-dsim", "true"};
        var command = DsimJvmSupport.command("jdk with spaces", List.of("-Xmx12g", "-Dexample=a b"),
                "jar with spaces.jar", "example.Main", args);
        assertTrue(command.getFirst().contains("jdk with spaces"));
        assertEquals(List.of("-Xmx12g", "-Dexample=a b"), command.subList(1, 3));
        assertTrue(command.containsAll(DsimJvmSupport.MODULE_OPTIONS));
        assertEquals("jar with spaces.jar", command.get(command.indexOf("-cp") + 1));
        assertEquals(List.of(args), command.subList(command.size() - args.length, command.size()));
    }

    @Test public void doesNotReuseDebuggerPort() {
        var command = DsimJvmSupport.command("jdk", List.of("-Xmx12g", "-agentlib:jdwp=address=5005"),
                "app.jar", "example.Main", new String[0]);
        assertTrue(command.contains("-Xmx12g"));
        assertFalse(command.stream().anyMatch(a -> a.startsWith("-agentlib:jdwp")));
    }
}
