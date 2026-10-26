package org.eqasim.switzerland.ch_cmdp.routing.pt_cache;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.router.RoutingRequest;
import org.matsim.facilities.Facility;

/** Exact routing inputs, including person/request/facility attributes. No time bins. */
final class PtRouteKey {
    final byte[] bytes;
    private final int hash;
    private static final ThreadLocal<MessageDigest> HASH = ThreadLocal.withInitial(() -> {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    });

    PtRouteKey(byte[] bytes) { this.bytes = bytes; hash = Arrays.hashCode(bytes); }

    static PtRouteKey of(RoutingRequest request, long generation) throws IOException {
        var buffer = new ByteArrayOutputStream(512);
        var out = new DataOutputStream(buffer);
        out.writeLong(generation);
        out.writeDouble(request.getDepartureTime());
        facility(out, request.getFromFacility());
        facility(out, request.getToFacility());
        var person = request.getPerson();
        value(out, person == null ? null : person.getId());
        value(out, person == null ? null : person.getAttributes().getAsMap());
        value(out, request.getAttributes() == null ? null : request.getAttributes().getAsMap());
        return new PtRouteKey(HASH.get().digest(buffer.toByteArray()));
    }

    private static void facility(DataOutputStream out, Facility facility) throws IOException {
        boolean linkWrapper = facility instanceof org.matsim.core.router.LinkWrapperFacilityI;
        boolean wrapper = linkWrapper
                // This standard MATSim wrapper is package-private; its optional metadata getters throw.
                || facility.getClass().getName().equals("org.matsim.facilities.ActivityWrapperFacility");
        Object facilityId = null;
        try {
            if (!linkWrapper && facility instanceof org.matsim.api.core.v01.Identifiable<?> identified) facilityId = identified.getId();
        } catch (UnsupportedOperationException absent) { /* Link/activity wrappers have no facility ID. */ }
        value(out, facilityId);
        value(out, facility.getLinkId());
        value(out, facility.getCoord());
        Map<String, Object> custom;
        try { custom = wrapper ? null : facility.getCustomAttributes(); }
        catch (UnsupportedOperationException absent) { custom = null; }
        value(out, custom);
        Object attributes = null;
        try {
            if (facility instanceof org.matsim.utils.objectattributes.attributable.Attributable a)
                attributes = a.getAttributes().getAsMap();
        } catch (UnsupportedOperationException absent) { /* Optional wrapper metadata. */ }
        value(out, attributes);
    }

    private static void value(DataOutputStream out, Object value) throws IOException {
        if (value == null) { out.writeByte(0); return; }
        if (value instanceof String s) { out.writeByte(1); text(out, s); }
        else if (value instanceof Number || value instanceof Boolean || value instanceof Character
                || value instanceof Id<?> || value instanceof Enum<?>) {
            out.writeByte(2); text(out, value.getClass().getName()); text(out, value.toString());
        } else if (value instanceof Coord c) {
            out.writeByte(3); out.writeDouble(c.getX()); out.writeDouble(c.getY());
            out.writeBoolean(c.hasZ()); if (c.hasZ()) out.writeDouble(c.getZ());
        } else if (value instanceof org.matsim.vehicles.PersonVehicles vehicles) {
            out.writeByte(8); value(out, vehicles.getModeVehicles());
        } else if (value instanceof org.matsim.vehicles.PersonVehicleTypes types) {
            out.writeByte(9); value(out, types.getModeVehicleTypes());
        } else if (value instanceof Map<?, ?> map) {
            out.writeByte(4); out.writeInt(map.size());
            var sorted = new TreeMap<String, Object>();
            for (var entry : map.entrySet()) {
                if (!(entry.getKey() instanceof String key)) throw new IOException("Non-string attribute map key");
                sorted.put(key, entry.getValue());
            }
            for (var entry : sorted.entrySet()) { text(out, entry.getKey()); value(out, entry.getValue()); }
        } else if (value instanceof Collection<?> collection) {
            out.writeByte(value instanceof Set<?> ? 5 : 6); out.writeInt(collection.size());
            if (value instanceof Set<?>) {
                var encoded = new ArrayList<byte[]>();
                for (Object member : collection) {
                    var buffer = new ByteArrayOutputStream(); value(new DataOutputStream(buffer), member);
                    encoded.add(buffer.toByteArray());
                }
                encoded.sort(Arrays::compareUnsigned);
                for (byte[] member : encoded) { out.writeInt(member.length); out.write(member); }
            } else for (Object member : collection) value(out, member);
        } else if (value.getClass().isArray()) {
            out.writeByte(7); text(out, value.getClass().getName());
            int length = java.lang.reflect.Array.getLength(value); out.writeInt(length);
            for (int i = 0; i < length; i++) value(out, java.lang.reflect.Array.get(value, i));
        } else throw new IOException("Unsupported routing attribute: " + value.getClass().getName());
    }

    static void text(DataOutputStream out, String text) throws IOException {
        if (text == null) { out.writeInt(-1); return; }
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8); out.writeInt(bytes.length); out.write(bytes);
    }

    @Override public int hashCode() { return hash; }
    @Override public boolean equals(Object other) {
        return other instanceof PtRouteKey key && Arrays.equals(bytes, key.bytes);
    }
}
