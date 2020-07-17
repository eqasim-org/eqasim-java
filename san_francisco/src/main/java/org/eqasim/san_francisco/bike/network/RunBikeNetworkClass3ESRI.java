package org.eqasim.san_francisco.bike.network;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.CommandLine;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.filter.NetworkFilterManager;
import org.matsim.core.network.filter.NetworkLinkFilter;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.utils.gis.matsim2esri.network.Links2ESRIShape;

public class RunBikeNetworkClass3ESRI {
    public static void main(String[] args) throws CommandLine.ConfigurationException {
        CommandLine cmd = new CommandLine.Builder(args) //
                .requireOptions("input-path", "output-path", "coord-system") //
                .build();

        Network network = NetworkUtils.createNetwork();
        new MatsimNetworkReader(network).readFile(cmd.getOptionStrict("input-path"));

        NetworkFilterManager networkFilterManager = new NetworkFilterManager(network);

        NetworkLinkFilter networkLinkFilter = new NetworkLinkFilter() {

            @Override
            public boolean judgeLink(Link l) {
                return (l.getAllowedModes().contains(TransportMode.bike) &&
                        l.getAttributes().getAttribute("bikeFacilityClass").toString().equals("class_3"));
            }
        };

        networkFilterManager.addLinkFilter(networkLinkFilter);

        Network bikeNetwork = networkFilterManager.applyFilters();

        new Links2ESRIShape(bikeNetwork, cmd.getOptionStrict("output-path"), cmd.getOptionStrict("coord-system")).write();

    }
}
