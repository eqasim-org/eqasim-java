package org.eqasim.switzerland.ch_cmdp.dsim;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalInt;
import org.apache.logging.log4j.LogManager;

/** Supplies Agrona/Fory module access without requiring users to change their launch command. */
public final class DsimJvmSupport {
    private DsimJvmSupport() { }

    static final List<String> MODULE_OPTIONS = List.of(
            "--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED",
            "--add-opens=java.base/java.lang.invoke=ALL-UNNAMED");

    public static boolean hasModuleAccess() {
        var base = Object.class.getModule();
        var application = DsimJvmSupport.class.getModule();
        return base.isOpen("jdk.internal.misc", application) && base.isOpen("java.lang.invoke", application);
    }

    static List<String> command(String javaHome, List<String> vmArguments, String classPath,
            String mainClass, String[] arguments) {
        var command = new ArrayList<String>();
        command.add(Path.of(javaHome, "bin", System.getProperty("os.name").startsWith("Windows")
                ? "java.exe" : "java").toString());
        for (var argument : vmArguments) {
            // The parent already owns the debugger's listening port. To debug the actual
            // simulation in an IDE, supply MODULE_OPTIONS in its VM options (no relaunch).
            if (!argument.startsWith("-agentlib:jdwp") && !argument.startsWith("-Xrunjdwp")) {
                command.add(argument);
            }
        }
        command.addAll(MODULE_OPTIONS);
        command.add("-cp");
        command.add(classPath);
        command.add(mainClass);
        command.addAll(Arrays.asList(arguments));
        return command;
    }

    /** Empty if already configured; otherwise runs the simulation child and returns its exit code. */
    public static OptionalInt runWithModuleAccess(String mainClass, String[] arguments) throws IOException {
        if (hasModuleAccess()) return OptionalInt.empty();
        var vmArguments = ManagementFactory.getRuntimeMXBean().getInputArguments();
        var log = LogManager.getLogger(DsimJvmSupport.class);
        log.info("Starting DSim with the required JVM module access; keeping Java, heap settings, "
                + "classpath and simulation arguments. Output is inherited from this process.");
        if (vmArguments.stream().anyMatch(a -> a.startsWith("-agentlib:jdwp") || a.startsWith("-Xrunjdwp"))) {
            log.warn("The DSim child does not reuse the parent's debugger port. For IDE debugging, add {} "
                    + "to the run configuration's VM options to avoid relaunching.", MODULE_OPTIONS);
        }
        var child = new ProcessBuilder(command(System.getProperty("java.home"), vmArguments,
                System.getProperty("java.class.path"), mainClass, arguments)).inheritIO().start();
        var shutdown = new Thread(() -> { if (child.isAlive()) child.destroy(); }, "dsim-launcher-shutdown");
        Runtime.getRuntime().addShutdownHook(shutdown);
        try {
            return OptionalInt.of(child.waitFor());
        } catch (InterruptedException e) {
            child.destroyForcibly();
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while waiting for DSim", e);
        } finally {
            Runtime.getRuntime().removeShutdownHook(shutdown);
        }
    }
}
