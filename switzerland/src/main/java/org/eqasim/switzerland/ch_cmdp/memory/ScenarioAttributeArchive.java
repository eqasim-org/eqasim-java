package org.eqasim.switzerland.ch_cmdp.memory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import net.jpountz.lz4.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.households.Household;
import org.matsim.utils.objectattributes.ObjectAttributesConverter;
import org.matsim.utils.objectattributes.attributable.Attributes;

/** Removed metadata is compressed during cleanup, saved once, and restored before final scenario export. */
public final class ScenarioAttributeArchive implements ShutdownListener {
    private static final Logger LOG = LogManager.getLogger(ScenarioAttributeArchive.class);
    private static final int MAGIC = 0x45514141, VERSION = 1, BLOCK_SIZE = 1 << 20;
    static final int PERSON = 1, LINK = 2, HOUSEHOLD = 3;
    private final Scenario scenario;
    private final Path file;
    private final long attributes;
    private boolean restored;
    private long restoredAttributes;

    private ScenarioAttributeArchive(Scenario scenario, Path file, long attributes) {
        this.scenario = scenario; this.file = file; this.attributes = attributes;
    }

    /** Uses the original MATSim writer as a delegate, so this also handles normal early termination. */
    public void install(Controler controller) {
        controller.addOverridingModule(AttributeRestoringDumpDataAtEnd.module(this));
    }

    /** Idempotent. Current values win; missing original objects are skipped. Clones have no archive records. */
    public synchronized void restore() {
        if (restored) return;
        long started = System.nanoTime(), read = 0, applied = 0;
        if (file != null) {
            try (var input = new DataInputStream(new LZ4BlockInputStream(
                    new BufferedInputStream(Files.newInputStream(file), BLOCK_SIZE),
                    LZ4Factory.fastestJavaInstance().fastDecompressor()))) {
                if (input.readInt() != MAGIC || input.readInt() != VERSION) throw new IOException("Invalid attribute archive header");
                var dictionary = new ArrayList<String>();
                var converters = new ObjectAttributesConverter();
                for (;;) {
                    int kind = input.readUnsignedByte();
                    if (kind == 0) break;
                    String id = readString(input);
                    Attributes target = target(kind, id);
                    int count = readSize(input);
                    for (int i = 0; i < count; i++) {
                        String name = readSymbol(input, dictionary);
                        Object value = readValue(input, dictionary, converters);
                        if (target != null && !target.getAsMap().containsKey(name)) {
                            target.putAttribute(name, value); applied++;
                        }
                        read++;
                    }
                }
                if (input.readLong() != attributes || read != attributes || input.read() != -1)
                    throw new IOException("Incomplete attribute archive");
            } catch (IOException | RuntimeException e) {
                throw new IllegalStateException("Could not restore scenario attributes from " + file + "; archive retained", e);
            }
        }
        restoredAttributes = applied; restored = true;
        LOG.info("Restored {} archived attributes ({} archived entries) in {} ms", applied, attributes,
                (System.nanoTime() - started) / 1_000_000.0);
    }

    private Attributes target(int kind, String id) throws IOException {
        return switch (kind) {
            case PERSON -> { var value = scenario.getPopulation().getPersons().get(Id.createPersonId(id)); yield value == null ? null : value.getAttributes(); }
            case LINK -> { var value = scenario.getNetwork().getLinks().get(Id.createLinkId(id)); yield value == null ? null : value.getAttributes(); }
            case HOUSEHOLD -> { var value = scenario.getHouseholds().getHouseholds().get(Id.create(id, Household.class)); yield value == null ? null : value.getAttributes(); }
            default -> throw new IOException("Unknown attribute owner type " + kind);
        };
    }

    @Override public void notifyShutdown(ShutdownEvent event) {
        if (event.isUnexpected()) {
            if (file != null) LOG.warn("Simulation ended unexpectedly; removed-attribute archive retained at {}", file);
            return;
        }
        // Also restore when dumpDataAtEnd=false, where the final-writer decorator is not registered as a core listener.
        restore();
        if (file != null) try {
            Files.deleteIfExists(file); Files.deleteIfExists(file.getParent());
        } catch (IOException e) { LOG.warn("Could not remove temporary attribute archive {}", file, e); }
    }

    Path file() { return file; }
    long attributeCount() { return attributes; }
    long restoredAttributeCount() { return restoredAttributes; }

    /** No entity maps or object values survive finish(): only the path and counters are kept during simulation. */
    static final class Builder implements AutoCloseable {
        private final CompressedChunks chunks = new CompressedChunks();
        private final Map<String, Integer> symbols = new HashMap<>();
        private final Set<String> unsupportedTypes = new HashSet<>();
        private final ObjectAttributesConverter converters = new ObjectAttributesConverter();
        private final DataOutputStream out;
        private long attributes;
        private boolean finished;

        Builder() throws IOException {
            out = new DataOutputStream(new LZ4BlockOutputStream(chunks, BLOCK_SIZE,
                    LZ4Factory.fastestJavaInstance().fastCompressor()));
            out.writeInt(MAGIC); out.writeInt(VERSION);
        }

        /** Filters unencodable values out of the removal list; they remain in the scenario rather than being lost. */
        void capture(int kind, String id, Attributes source, List<String> names) {
            var encoded = new ArrayList<Object>(names.size());
            var iterator = names.iterator();
            while (iterator.hasNext()) {
                String name = iterator.next(); Object value = source.getAttribute(name);
                if (!isBinaryType(value)) {
                    String text = converters.convertToString(value);
                    if (text == null) {
                        if (unsupportedTypes.add(value.getClass().getName()))
                            LOG.warn("Keeping attributes of type {} in memory because no archive converter is available", value.getClass().getName());
                        iterator.remove(); continue;
                    }
                    value = new Converted(value.getClass().getName(), text);
                }
                encoded.add(value);
            }
            if (names.isEmpty()) return;
            try {
                out.writeByte(kind); writeString(out, id); writeSize(out, names.size());
                for (int i = 0; i < names.size(); i++) {
                    symbol(names.get(i)); writeValue(encoded.get(i)); attributes++;
                }
            } catch (IOException e) { throw new UncheckedIOException("Could not capture removed scenario attributes", e); }
        }

        ScenarioAttributeArchive finish(Scenario scenario, Path temporaryParent) throws IOException {
            if (finished) throw new IllegalStateException("Attribute archive was already saved");
            finished = true; Path directory = null;
            try {
                out.writeByte(0); out.writeLong(attributes); out.close();
                if (attributes == 0) return new ScenarioAttributeArchive(scenario, null, 0);
                directory = temporaryParent == null ? Files.createTempDirectory("eqasim-scenario-attributes-")
                        : Files.createTempDirectory(temporaryParent, "eqasim-scenario-attributes-");
                Path archive = directory.resolve("removed-attributes.bin.lz4");
                try (var file = new BufferedOutputStream(Files.newOutputStream(archive), BLOCK_SIZE)) { chunks.writeTo(file); }
                LOG.info("Archived {} removed person/link/household attributes in {} bytes at {}", attributes, chunks.size, archive);
                return new ScenarioAttributeArchive(scenario, archive, attributes);
            } catch (IOException | RuntimeException e) {
                if (directory != null) {
                    try { Files.deleteIfExists(directory.resolve("removed-attributes.bin.lz4")); Files.deleteIfExists(directory); }
                    catch (IOException cleanup) { e.addSuppressed(cleanup); }
                }
                throw e;
            } finally { chunks.clear(); symbols.clear(); }
        }

        private void symbol(String value) throws IOException {
            Integer index = symbols.get(value);
            if (index == null) { writeSize(out, 0); writeString(out, value); symbols.put(value, symbols.size()); }
            else writeSize(out, index + 1);
        }

        private void writeValue(Object value) throws IOException {
            if (value == null) out.writeByte(0);
            else if (value instanceof String s) { out.writeByte(1); writeString(out, s); }
            else if (value instanceof Integer n) { out.writeByte(2); out.writeInt(n); }
            else if (value instanceof Long n) { out.writeByte(3); out.writeLong(n); }
            else if (value instanceof Double n) { out.writeByte(4); out.writeDouble(n); }
            else if (value instanceof Float n) { out.writeByte(5); out.writeFloat(n); }
            else if (value instanceof Boolean b) out.writeByte(b ? 6 : 7);
            else if (value instanceof Short n) { out.writeByte(8); out.writeShort(n); }
            else if (value instanceof Byte n) { out.writeByte(9); out.writeByte(n); }
            else if (value instanceof Character c) { out.writeByte(10); out.writeChar(c); }
            else if (value instanceof double[] values) { out.writeByte(11); writeSize(out, values.length); for (double n : values) out.writeDouble(n); }
            else if (value instanceof int[] values) { out.writeByte(12); writeSize(out, values.length); for (int n : values) out.writeInt(n); }
            else if (value instanceof long[] values) { out.writeByte(13); writeSize(out, values.length); for (long n : values) out.writeLong(n); }
            else if (value instanceof byte[] values) { out.writeByte(14); writeSize(out, values.length); out.write(values); }
            else if (value instanceof Coord coord) {
                out.writeByte(15); out.writeDouble(coord.getX()); out.writeDouble(coord.getY());
                out.writeBoolean(coord.hasZ()); if (coord.hasZ()) out.writeDouble(coord.getZ());
            } else if (value instanceof Converted converted) { out.writeByte(16); symbol(converted.type()); writeString(out, converted.text()); }
            else throw new IOException("Unprepared attribute value " + value.getClass());
        }

        @Override public void close() throws IOException {
            try { if (!finished) out.close(); } finally { chunks.clear(); symbols.clear(); }
        }
    }

    private record Converted(String type, String text) { }
    private static boolean isBinaryType(Object value) {
        return value == null || value instanceof String || value instanceof Integer || value instanceof Long
                || value instanceof Double || value instanceof Float || value instanceof Boolean || value instanceof Short
                || value instanceof Byte || value instanceof Character || value instanceof double[] || value instanceof int[]
                || value instanceof long[] || value instanceof byte[] || value instanceof Coord;
    }

    private static Object readValue(DataInputStream input, List<String> dictionary, ObjectAttributesConverter converters) throws IOException {
        return switch (input.readUnsignedByte()) {
            case 0 -> null;
            case 1 -> readString(input);
            case 2 -> input.readInt();
            case 3 -> input.readLong();
            case 4 -> input.readDouble();
            case 5 -> input.readFloat();
            case 6 -> true;
            case 7 -> false;
            case 8 -> input.readShort();
            case 9 -> input.readByte();
            case 10 -> input.readChar();
            case 11 -> { var values = new double[readSize(input)]; for (int i = 0; i < values.length; i++) values[i] = input.readDouble(); yield values; }
            case 12 -> { var values = new int[readSize(input)]; for (int i = 0; i < values.length; i++) values[i] = input.readInt(); yield values; }
            case 13 -> { var values = new long[readSize(input)]; for (int i = 0; i < values.length; i++) values[i] = input.readLong(); yield values; }
            case 14 -> { var values = new byte[readSize(input)]; input.readFully(values); yield values; }
            case 15 -> { double x = input.readDouble(), y = input.readDouble(); yield input.readBoolean() ? new Coord(x, y, input.readDouble()) : new Coord(x, y); }
            case 16 -> {
                String type = readSymbol(input, dictionary), text = readString(input);
                Object value = converters.convert(type, text);
                if (value == null) throw new IOException("Could not decode archived attribute type " + type);
                yield value;
            }
            default -> throw new IOException("Unknown archived attribute value type");
        };
    }

    private static String readSymbol(DataInputStream input, List<String> dictionary) throws IOException {
        int token = readSize(input);
        if (token == 0) { String value = readString(input); dictionary.add(value); return value; }
        if (token > dictionary.size()) throw new IOException("Invalid attribute archive dictionary reference");
        return dictionary.get(token - 1);
    }
    private static void writeString(DataOutputStream out, String text) throws IOException {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8); writeSize(out, bytes.length); out.write(bytes);
    }
    private static String readString(DataInputStream input) throws IOException {
        byte[] bytes = new byte[readSize(input)]; input.readFully(bytes); return new String(bytes, StandardCharsets.UTF_8);
    }
    private static void writeSize(DataOutputStream out, int value) throws IOException {
        while ((value & ~127) != 0) { out.writeByte((value & 127) | 128); value >>>= 7; }
        out.writeByte(value);
    }
    private static int readSize(DataInputStream input) throws IOException {
        int value = 0;
        for (int shift = 0; shift <= 28; shift += 7) {
            int next = input.readUnsignedByte();
            if (shift == 28 && (next & 0xf8) != 0) throw new IOException("Invalid archive length");
            value |= (next & 127) << shift;
            if ((next & 128) == 0) return value;
        }
        throw new IOException("Invalid archive length");
    }

    /** Segmented compressed bytes avoid a monolithic array, doubling copies, and a per-attribute object graph. */
    private static final class CompressedChunks extends OutputStream {
        private final List<byte[]> full = new ArrayList<>();
        private byte[] current = new byte[BLOCK_SIZE];
        private int used;
        private long size;
        @Override public void write(int value) {
            if (used == current.length) { full.add(current); current = new byte[BLOCK_SIZE]; used = 0; }
            current[used++] = (byte) value; size++;
        }
        @Override public void write(byte[] bytes, int offset, int length) {
            Objects.checkFromIndexSize(offset, length, bytes.length);
            while (length > 0) {
                if (used == current.length) { full.add(current); current = new byte[BLOCK_SIZE]; used = 0; }
                int count = Math.min(length, current.length - used);
                System.arraycopy(bytes, offset, current, used, count); used += count; size += count;
                offset += count; length -= count;
            }
        }
        void writeTo(OutputStream file) throws IOException {
            for (var chunk : full) file.write(chunk);
            file.write(current, 0, used);
        }
        void clear() { full.clear(); current = null; used = 0; }
    }
}
