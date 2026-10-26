package org.eqasim.switzerland.ch_cmdp.routing.pt_cache;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.utils.objectattributes.attributable.Attributes;

/** Lossless binary itinerary essentials. Every decode creates fresh plan/route objects. */
final class PtItineraryCodec {
    private PtItineraryCodec() {}

    static byte[] encode(List<? extends PlanElement> elements) throws IOException {
        var bytes = new ByteArrayOutputStream(512);
        var out = new DataOutputStream(bytes);
        out.writeInt(1);
        out.writeInt(elements == null ? -1 : elements.size());
        if (elements != null) for (var element : elements) {
            if (element instanceof Leg leg) {
                out.writeByte(1); text(out, leg.getMode()); text(out, leg.getRoutingMode());
                time(out, leg.getDepartureTime()); time(out, leg.getTravelTime());
                var route = leg.getRoute(); out.writeBoolean(route != null);
                if (route != null) {
                    text(out, route.getRouteType()); text(out, id(route.getStartLinkId())); text(out, id(route.getEndLinkId()));
                    text(out, route.getRouteDescription()); out.writeDouble(route.getDistance()); time(out, route.getTravelTime());
                    out.writeBoolean(route instanceof NetworkRoute);
                    if (route instanceof NetworkRoute network) {
                        out.writeInt(network.getLinkIds().size());
                        for (var link : network.getLinkIds()) text(out, link.toString());
                        text(out, id(network.getVehicleId())); out.writeDouble(network.getTravelCost());
                    }
                }
            } else if (element instanceof Activity activity) {
                out.writeByte(2); text(out, activity.getType()); coord(out, activity.getCoord());
                text(out, id(activity.getLinkId())); text(out, id(activity.getFacilityId()));
                time(out, activity.getStartTime()); time(out, activity.getEndTime()); time(out, activity.getMaximumDuration());
            } else throw new IOException("Unsupported PT plan element " + element.getClass());
            attributes(out, element.getAttributes());
        }
        return bytes.toByteArray();
    }

    static List<? extends PlanElement> decode(byte[] bytes, PopulationFactory factory) throws IOException {
        var in = new DataInputStream(new ByteArrayInputStream(bytes));
        if (in.readInt() != 1) throw new IOException("Unsupported PT cache record version");
        int count = in.readInt();
        if (count == -1) return null;
        if (count < 0 || count > bytes.length) throw new IOException("Invalid PT element count");
        var result = new ArrayList<PlanElement>(count);
        for (int i = 0; i < count; i++) {
            PlanElement element;
            int type = in.readUnsignedByte();
            if (type == 1) {
                var leg = factory.createLeg(text(in)); leg.setRoutingMode(text(in));
                Double departure = time(in), duration = time(in);
                if (departure != null) leg.setDepartureTime(departure);
                if (duration != null) leg.setTravelTime(duration);
                if (in.readBoolean()) {
                    String routeType = text(in), from = text(in), to = text(in), description = text(in);
                    var routes = factory.getRouteFactories();
                    var route = routes.createRoute(routes.getRouteClassForType(routeType),
                            from == null ? null : Id.createLinkId(from), to == null ? null : Id.createLinkId(to));
                    if (!routeType.equals(route.getRouteType())) throw new IOException("Unsupported cached route type " + routeType);
                    double distance = in.readDouble(); Double travelTime = time(in);
                    route.setRouteDescription(description);
                    if (in.readBoolean()) {
                        if (!(route instanceof NetworkRoute network)) throw new IOException("Expected a network route");
                        int links = in.readInt();
                        if (links < 0 || links > bytes.length) throw new IOException("Invalid link count");
                        var ids = new ArrayList<Id<org.matsim.api.core.v01.network.Link>>(links);
                        for (int j = 0; j < links; j++) ids.add(Id.createLinkId(text(in)));
                        network.setLinkIds(route.getStartLinkId(), ids, route.getEndLinkId());
                        String vehicle = text(in); network.setVehicleId(vehicle == null ? null : Id.createVehicleId(vehicle));
                        network.setTravelCost(in.readDouble());
                    }
                    route.setDistance(distance);
                    if (travelTime != null) route.setTravelTime(travelTime); else route.setTravelTimeUndefined();
                    leg.setRoute(route);
                }
                element = leg;
            } else if (type == 2) {
                String purpose = text(in); Coord coordinate = coord(in);
                var activity = factory.createActivityFromCoord(purpose, coordinate);
                String link = text(in), facility = text(in);
                if (link != null) activity.setLinkId(Id.createLinkId(link));
                if (facility != null) activity.setFacilityId(Id.create(facility, org.matsim.facilities.ActivityFacility.class));
                Double start = time(in), end = time(in), duration = time(in);
                if (start != null) activity.setStartTime(start);
                if (end != null) activity.setEndTime(end);
                if (duration != null) activity.setMaximumDuration(duration);
                element = activity;
            } else throw new IOException("Invalid PT element type");
            attributes(in, element.getAttributes()); result.add(element);
        }
        if (in.available() != 0) throw new IOException("Trailing PT cache data");
        return result;
    }

    private static void time(DataOutputStream out, OptionalTime time) throws IOException {
        out.writeBoolean(time.isDefined()); if (time.isDefined()) out.writeDouble(time.seconds());
    }
    private static Double time(DataInputStream in) throws IOException { return in.readBoolean() ? in.readDouble() : null; }
    private static String id(Id<?> id) { return id == null ? null : id.toString(); }
    private static void text(DataOutputStream out, String text) throws IOException { PtRouteKey.text(out, text); }
    private static String text(DataInputStream in) throws IOException {
        int length = in.readInt(); if (length == -1) return null;
        if (length < 0 || length > in.available()) throw new IOException("Invalid PT string length");
        return new String(in.readNBytes(length), StandardCharsets.UTF_8);
    }
    private static void coord(DataOutputStream out, Coord c) throws IOException {
        out.writeBoolean(c != null); if (c == null) return;
        out.writeDouble(c.getX()); out.writeDouble(c.getY()); out.writeBoolean(c.hasZ());
        if (c.hasZ()) out.writeDouble(c.getZ());
    }
    private static Coord coord(DataInputStream in) throws IOException {
        if (!in.readBoolean()) return null;
        double x = in.readDouble(), y = in.readDouble();
        return in.readBoolean() ? new Coord(x, y, in.readDouble()) : new Coord(x, y);
    }
    private static void attributes(DataOutputStream out, Attributes attributes) throws IOException {
        var map = attributes.getAsMap(); out.writeInt(map.size());
        for (var entry : map.entrySet()) {
            text(out, entry.getKey()); Object value = entry.getValue();
            if (value == null) out.writeByte(0);
            else if (value instanceof String s) { out.writeByte(1); text(out, s); }
            else if (value instanceof Double d) { out.writeByte(2); out.writeDouble(d); }
            else if (value instanceof Integer n) { out.writeByte(3); out.writeInt(n); }
            else if (value instanceof Long n) { out.writeByte(4); out.writeLong(n); }
            else if (value instanceof Boolean b) { out.writeByte(5); out.writeBoolean(b); }
            else if (value instanceof Float f) { out.writeByte(6); out.writeFloat(f); }
            else if (value instanceof Short s) { out.writeByte(7); out.writeShort(s); }
            else if (value instanceof Byte b) { out.writeByte(8); out.writeByte(b); }
            else throw new IOException("Unsupported PT output attribute " + value.getClass());
        }
    }
    private static void attributes(DataInputStream in, Attributes attributes) throws IOException {
        int count = in.readInt();
        if (count < 0 || count > in.available()) throw new IOException("Invalid PT attribute count");
        for (int i = 0; i < count; i++) {
            String key = text(in);
            Object value = switch (in.readUnsignedByte()) {
                case 0 -> null; case 1 -> text(in); case 2 -> in.readDouble(); case 3 -> in.readInt();
                case 4 -> in.readLong(); case 5 -> in.readBoolean(); case 6 -> in.readFloat();
                case 7 -> in.readShort(); case 8 -> in.readByte();
                default -> throw new IOException("Invalid PT attribute type");
            };
            attributes.putAttribute(key, value);
        }
    }
}
