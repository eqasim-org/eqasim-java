package org.eqasim.core.analysis.sociodemographics;

import org.apache.log4j.Logger;
import org.matsim.core.config.CommandLine;
import org.matsim.households.Households;
import org.matsim.households.HouseholdsImpl;
import org.matsim.households.HouseholdsReaderV10;

import java.io.IOException;

public class RunReadHouseholdsFromHouseholds {
    private static final Logger log = Logger.getLogger( RunReadHouseholdsFromHouseholds.class );

    public static void main(String[] args) throws CommandLine.ConfigurationException, IOException {
        CommandLine cmd = new CommandLine.Builder(args) //
                .requireOptions("input-path", "output-path") //
                .build();

        new RunReadHouseholdsFromHouseholds().run(cmd.getOptionStrict("input-path"), cmd.getOptionStrict("output-path"));
    }

    private void run(String inputPath, String outputPath) throws IOException {

        log.info("Reading in the households ...");
        Households households = new HouseholdsImpl();
        new HouseholdsReaderV10(households).readFile(inputPath);

        log.info("Extracting household information ...");
        HouseholdInfo householdInfo = new HouseholdsReader().read(households);

        log.info("Writing household information to csv ...");
        new HouseholdsWriter(householdInfo).write(outputPath);

        log.info("Done.");
    }

}

