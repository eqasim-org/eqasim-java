package org.eqasim.switzerland.ch_cmdp.utils.routing.multimodalRouter.server;

import java.nio.file.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.*;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.apache.logging.log4j.core.layout.PatternLayout;

/** CLI-process logging: initialization threads can write to a separate file, including MATSim/native preparation. */
public final class ServerLogging {
    private ServerLogging() {}
    public static void configure(String logFile, String initializationFile) throws java.io.IOException {
        if (logFile != null && initializationFile != null && Path.of(logFile).toAbsolutePath().normalize().equals(
                Path.of(initializationFile).toAbsolutePath().normalize()))
            throw new IllegalArgumentException("log-file and initialization-log-file must be different");
        var context = (LoggerContext) LogManager.getContext(false);
        var config = context.getConfiguration();
        var root = config.getRootLogger();
        if (logFile != null) {
            for (String name : java.util.List.copyOf(root.getAppenders().keySet())) root.removeAppender(name);
            var appender = file("router-log", logFile);
            config.addAppender(appender); root.addAppender(appender, null, null);
        }
        if (initializationFile != null) {
            // Initialization is always eager on the loader and native preparation threads.
            for (var appender : root.getAppenders().values())
                if (appender instanceof org.apache.logging.log4j.core.filter.AbstractFilterable filterable)
                    filterable.addFilter(new InitializationFilter(false));
            var appender = file("router-initialization", initializationFile);
            appender.addFilter(new InitializationFilter(true));
            config.addAppender(appender); root.addAppender(appender, null, null);
        }
        context.updateLoggers();
    }
    private static FileAppender file(String name, String path) throws java.io.IOException {
        Path target = Path.of(path).toAbsolutePath().normalize();
        Files.createDirectories(target.getParent());
        var result = FileAppender.newBuilder().setName(name).withFileName(target.toString()).withAppend(true)
                .setLayout(PatternLayout.newBuilder().withPattern("%d{yyyy-MM-dd HH:mm:ss} %-5level %c{1}: %msg%n").build()).build();
        if (result == null) throw new java.io.IOException("Cannot open log file: " + target);
        result.start(); return result;
    }
    private static final class InitializationFilter extends AbstractFilter {
        private final boolean initialization;
        InitializationFilter(boolean initialization) { this.initialization = initialization; }
        @Override public Result filter(LogEvent event) {
            boolean loading = event.getThreadName().equals("multi-mode-router-loader")
                    || event.getThreadName().equals("rust-pt-preprocessing");
            return loading == initialization ? Result.NEUTRAL : Result.DENY;
        }
    }
}
