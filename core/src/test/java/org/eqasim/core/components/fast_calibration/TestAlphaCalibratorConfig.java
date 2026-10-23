package org.eqasim.core.components.fast_calibration;

import static org.junit.Assert.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;

public class TestAlphaCalibratorConfig {
    @Rule public TemporaryFolder temp = new TemporaryFolder();

    @Test
    public void relativeInputIsReadFromConfigDirectoryAndRemainsRelativeWhenWritten() throws Exception {
        Path directory = temp.newFolder("scenario with spaces").toPath();
        Path input = directory.resolve("targets.csv");
        Files.writeString(input, "canton,car\nGE,0.4\n");
        Path configPath = directory.resolve("config.xml");
        var group = new AlphaCalibratorConfig();
        group.setFilePath("targets.csv");
        new ConfigWriter(ConfigUtils.createConfig(group)).write(configPath.toString());

        var config = ConfigUtils.loadConfig(configPath.toString(), new AlphaCalibratorConfig());
        var loaded = AlphaCalibratorConfig.getOrCreate(config);
        assertEquals("canton,car\nGE,0.4\n", Files.readString(Path.of(loaded.getResolvedFilePath(config))));
        assertEquals("targets.csv", loaded.getFilePath());
    }

    @Test
    public void absoluteAndEmptyPathsRemainSupported() throws Exception {
        var config = ConfigUtils.createConfig();
        var group = AlphaCalibratorConfig.getOrCreate(config);
        assertEquals("", group.getResolvedFilePath(config));
        Path input = temp.newFile("absolute targets.csv").toPath().toAbsolutePath();
        group.setFilePath(input.toString());
        assertEquals(input, Path.of(group.getResolvedFilePath(config)));
    }
}
