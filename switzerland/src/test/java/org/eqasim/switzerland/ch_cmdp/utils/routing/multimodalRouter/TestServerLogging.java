package org.eqasim.switzerland.ch_cmdp.utils.routing.multimodalRouter;

import static org.junit.Assert.*;
import java.nio.file.*;
import org.junit.Test;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.eqasim.switzerland.ch_cmdp.utils.routing.multimodalRouter.server.ServerLogging;

public class TestServerLogging {
    @Test
    public void initializationAndNativePreparationGoOnlyToTheirSeparateFile() throws Exception {
        var context = (LoggerContext) LogManager.getContext(false);
        var root = context.getConfiguration().getRootLogger();
        var original = new java.util.ArrayList<>(root.getAppenders().values());
        Path directory = Files.createTempDirectory("router-logs-");
        Path main = directory.resolve("router.log"), init = directory.resolve("loading.log");
        try {
            ServerLogging.configure(main.toString(), init.toString());
            var logger = LogManager.getLogger("router-log-test");
            var loader = new Thread(() -> logger.info("NETWORK_LOADING_MARKER"), "multi-mode-router-loader");
            loader.start(); loader.join();
            var nativeLoader = new Thread(() -> logger.info("NATIVE_READY_MARKER"), "rust-pt-preprocessing");
            nativeLoader.start(); nativeLoader.join();
            logger.info("REQUEST_PROGRESS_MARKER");
            assertTrue(Files.readString(init).contains("NETWORK_LOADING_MARKER"));
            assertTrue(Files.readString(init).contains("NATIVE_READY_MARKER"));
            assertFalse(Files.readString(init).contains("REQUEST_PROGRESS_MARKER"));
            assertTrue(Files.readString(main).contains("REQUEST_PROGRESS_MARKER"));
            assertFalse(Files.readString(main).contains("NETWORK_LOADING_MARKER"));
            assertFalse(Files.readString(main).contains("NATIVE_READY_MARKER"));
        } finally {
            for (var appender : java.util.List.copyOf(root.getAppenders().values())) {
                root.removeAppender(appender.getName()); appender.stop();
            }
            original.forEach(appender -> root.addAppender(appender, null, null));
            context.updateLoggers();
            Files.deleteIfExists(main); Files.deleteIfExists(init); Files.deleteIfExists(directory);
        }
    }
}
