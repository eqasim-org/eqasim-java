package org.eqasim.examples.SMMFramework.generalizedSMMModeChoice.testingUtils;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.core.utils.misc.Time;
import org.matsim.utils.objectattributes.attributable.Attributes;

public class LegImpl2 implements Leg {

    private Route route = null;

    private OptionalTime depTime = OptionalTime.undefined();
    private OptionalTime travTime = OptionalTime.undefined();
    private String mode;

    private final Attributes attributes = new Attributes();

    /* deliberately package */
    public LegImpl2(final String transportMode) {
        this.mode = transportMode;
    }

    @Override
    public final String getMode() {
        return this.mode;
    }

    @Override
    public final void setMode(String transportMode) {
        this.mode = transportMode;
        TripStructureUtils.setRoutingMode( this, null );
//		TripStructureUtils.setRoutingMode( this, null ); // setting routingMode to null leads to exceptions in AttributesXmlWriterDelegate.writeAttributes() : Class<?> clazz = objAttribute.getValue().getClass();
        // (yyyy or maybe "transportMode" instead of "null"?? kai, oct'19)
    }

    @Override
    public final OptionalTime getDepartureTime() {
        return this.depTime;
    }

    @Override
    public final void setDepartureTime(final double depTime) {
        this.depTime = OptionalTime.defined(depTime);
    }

    @Override
    public void setDepartureTimeUndefined() {
        this.depTime = OptionalTime.undefined();
    }

    @Override
    public final OptionalTime getTravelTime() {
        return this.travTime;
    }

    @Override
    public final void setTravelTime(final double travTime) {
        this.travTime = OptionalTime.defined(travTime);
    }

    @Override
    public void setTravelTimeUndefined() {
        this.travTime = OptionalTime.undefined();
    }

    @Override
    public Route getRoute() {
        return this.route;
    }

    @Override
    public final void setRoute(Route route) {
        this.route = route;
    }

    @Override
    public final String toString() {
        return "leg [mode="
                + this.getMode()
                + "]"
                + "[depTime="
                + Time.writeTime(this.getDepartureTime())
                + "]"
                + "[travTime="
                + Time.writeTime(this.getTravelTime())
                + "]"
                + "[arrTime="
                + (depTime.isDefined() && travTime.isDefined()?
                Time.writeTime(depTime.seconds() + travTime.seconds()) :
                Time.writeTime(OptionalTime.undefined()))
                + "]"
                + "[route="
                + this.route
                + "]";
    }


    @Override
    public Attributes getAttributes() {
        return attributes;
    }



}
