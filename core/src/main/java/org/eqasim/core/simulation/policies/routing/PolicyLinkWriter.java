package org.eqasim.core.simulation.policies.routing;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.IdSet;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.utils.io.IOUtils;

public class PolicyLinkWriter {
    static public final String PATTERN = "policy_links.{policy}.csv";

    private final OutputDirectoryHierarchy outputHierarchy;

    public PolicyLinkWriter(OutputDirectoryHierarchy outputHierarchy) {
        this.outputHierarchy = outputHierarchy;
    }

    public void write(String policy, IdSet<Link> links) {
        String outputPath = outputHierarchy.getOutputFilename(PATTERN.replace("{policy}", policy));

        try {
            BufferedWriter writer = IOUtils.getBufferedWriter(outputPath);
            writer.write("link_id\n");
            writer.write(links.stream().map(id -> id.toString()).collect(Collectors.joining("\n")));
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
