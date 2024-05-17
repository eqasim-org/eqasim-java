package org.eqasim.core.analysis.cba.analyzers.drtAnalysis;

import org.eqasim.core.analysis.cba.CbaAnalysis;
import org.eqasim.core.analysis.cba.CbaConfigGroup;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.drt.speedup.DrtSpeedUp;
import org.matsim.contrib.drt.speedup.DrtSpeedUpParams;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.core.config.groups.ControlerConfigGroup;


public class DrtAnalysisModule extends AbstractDvrpModeModule {

    private final DrtAnalyzerConfigGroup configGroup;
    private final CbaConfigGroup cbaConfigGroup;

    public DrtAnalysisModule(DrtAnalyzerConfigGroup configGroup, CbaConfigGroup cbaConfigGroup) {
        super(configGroup.getMode());
        this.configGroup = configGroup;
        this.cbaConfigGroup = cbaConfigGroup;
    }

    @Override
    public void install() {
        DrtSpeedUpParams drtSpeedUpParams = null;
        for(DrtConfigGroup drtConfigGroup : ((MultiModeDrtConfigGroup) getConfig().getModules().get(MultiModeDrtConfigGroup.GROUP_NAME)).getModalElements()) {
            if(drtConfigGroup.getMode().equals(this.configGroup.getMode())) {
                drtSpeedUpParams = drtConfigGroup.getDrtSpeedUpParams().orElse(null);
            }
        }
        DrtSpeedUpParams finalDrtSpeedUpParams = drtSpeedUpParams;
        installQSimModule(new AbstractDvrpModeQSimModule(this.getMode()) {
            @Override
            protected void configureQSim() {
                if(this.getIterationNumber() % cbaConfigGroup.getOutputFrequency() != 0) {
                    return;
                }
                if(finalDrtSpeedUpParams != null && DrtSpeedUp.isTeleportDrtUsers(finalDrtSpeedUpParams, (ControlerConfigGroup) getConfig().getModules().get(ControlerConfigGroup.GROUP_NAME), this.getIterationNumber())) {
                    return;
                }
                addMobsimScopeEventHandlerBinding().toProvider(modalProvider(getter -> {
                    Network network = getter.get(Network.class);
                    Fleet fleet = getter.getModal(Fleet.class);
                    CbaAnalysis cbaAnalysis = getter.get(CbaAnalysis.class);
                    DrtAnalyzer drtAnalyzer = new DrtAnalyzer(this.getMode(), network, this.getScheme(), fleet, configGroup);
                    cbaAnalysis.addSingleIterationAnalyzer(drtAnalyzer);
                    return drtAnalyzer;
                }));
            }

            private DrtConfigGroup.OperationalScheme getScheme(){
                MultiModeDrtConfigGroup multiModeDrtConfigGroup = (MultiModeDrtConfigGroup) getConfig().getModules().get(MultiModeDrtConfigGroup.GROUP_NAME);
                DrtConfigGroup.OperationalScheme scheme = DrtConfigGroup.OperationalScheme.stopbased;
                for(DrtConfigGroup drtConfigGroup : multiModeDrtConfigGroup.getModalElements()) {
                    if(drtConfigGroup.getMode().equals(this.getMode())) {
                        scheme = drtConfigGroup.operationalScheme;
                    }
                }
                return scheme;
            }
        });
    }
}