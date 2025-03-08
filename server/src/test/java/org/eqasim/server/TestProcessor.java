package org.eqasim.server;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.ExecutionException;

import org.apache.commons.io.FileUtils;
import org.eqasim.core.simulation.EqasimConfigurator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.matsim.core.config.CommandLine.ConfigurationException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

public class TestProcessor {
    @Before
    public void setUp() throws IOException {
        new File("processor_test").mkdirs();
    }

    @After
    public void tearDown() throws IOException {
        FileUtils.deleteDirectory(new File("processor_test"));
    }

    @Test
    public void testProcessor() throws JsonParseException, JsonMappingException, ConfigurationException, IOException,
            InterruptedException, ExecutionException {
        URL scenarioUrl = EqasimConfigurator.class.getClassLoader().getResource("melun");
        String configPath = new File(scenarioUrl.getPath(), "config.xml").getAbsolutePath();

        URL resourcesUrl = getClass().getClassLoader().getResource("processor");
        String inputPath = new File(resourcesUrl.getPath(), "input.json").getAbsolutePath();
        String expectedOutputPath = new File(resourcesUrl.getPath(), "output.json").getAbsolutePath();
        String outputPath = new File("processor_test/output.json").getPath();

        // uncomment to regenerate
        outputPath = expectedOutputPath;

        RunProcessor.main(new String[] {
                "--config-path", configPath,
                "--input-path", inputPath,
                "--output-path", outputPath,
                "--threads", "4",
                "--eqasim-configurator", "org.eqasim.server.ServerTestConfigurator"
        });

        assertTrue(FileUtils.contentEquals(new File(expectedOutputPath), new File(outputPath)));
    }
}