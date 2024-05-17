package org.eqasim.core.analysis.cba.run;

import com.google.common.base.Preconditions;
import org.eqasim.core.analysis.cba.CbaConfigGroup;
import org.eqasim.core.analysis.cba.CbaModule;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

public class RunCbaScenario {

    public static void run(String configFile) {
        Config config = ConfigUtils.loadConfig(configFile, new CbaConfigGroup(), new MultiModeDrtConfigGroup(), new DvrpConfigGroup(),
                new OTFVisConfigGroup());
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
        createControler(config).run();
    }

    public static void main(String[] args) {
        Preconditions.checkArgument(args.length == 1,
                "RunDrtScenario needs one argument: path to the configuration file");
        RunCbaScenario.run(args[0]);
    }

    public static Controler createControler(Config config) {
        Controler controler = DrtControlerCreator.createControler(config, false);
        controler.addOverridingModule(new CbaModule());
        return controler;
    }
}
