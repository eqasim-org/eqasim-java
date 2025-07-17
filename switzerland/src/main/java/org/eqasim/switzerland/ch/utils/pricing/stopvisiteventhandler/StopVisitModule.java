package org.eqasim.switzerland.ch.utils.pricing.stopvisiteventhandler;

import org.eqasim.switzerland.ch.utils.pricing.inputs.zonal.ZonalRegistry;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.AbstractModule;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class StopVisitModule extends AbstractModule {

    ZonalRegistry zonalRegistry;

    public StopVisitModule(ZonalRegistry zonalRegistry){
        this.zonalRegistry = zonalRegistry;
    }

    @Override
	public void install() {
		addControlerListenerBinding().to(StopVisitAnalyzer.class);
		
	}

    @Provides
    @Singleton
    public StopVisitLogger provideStopVisitLogger(Scenario scenario) {
        return new StopVisitLogger("100", zonalRegistry);
    }
    
}
