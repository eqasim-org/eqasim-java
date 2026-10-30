package org.eqasim.switzerland.ch_cmdp.utils.routing;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.TreeMap;

import com.fasterxml.jackson.databind.ObjectMapper;

/** Disk-backed input partitions and indexed output, avoiding whole-request trip/path retention. */
final class AssignmentSpool implements AutoCloseable {
    private static final ObjectMapper JSON = new ObjectMapper();
    final Map<Integer, Bin> bins = new TreeMap<>();
    private final Path directory;
    private final List<Path> files = new ArrayList<>();
    private final List<Rows> open = new ArrayList<>();
    private final LinkedHashSet<Rows> handles = new LinkedHashSet<>();
    private final Rows results;
    private final RandomAccessFile resultOffsets;

    public record Input(long index, Map<String, String> row) {}
    public record Output(MultiModeRouter.RoutedTrip result, int[] path, boolean truck) {}

    final class Rows implements AutoCloseable {
        RandomAccessFile data;
        RandomAccessFile offsets;
        final Path dataPath;
        final Path offsetsPath;
        long count;

        Rows(String name) throws IOException {
            dataPath = directory.resolve(name + ".data");
            offsetsPath = directory.resolve(name + ".offsets");
            files.add(dataPath); files.add(offsetsPath);
            open.add(this);
        }

        private void ensureOpen() throws IOException {
            if (data == null) {
                data = new RandomAccessFile(dataPath.toFile(), "rw");
                try { offsets = new RandomAccessFile(offsetsPath.toFile(), "rw"); }
                catch (IOException e) { data.close(); data = null; throw e; }
            }
            handles.remove(this); handles.add(this);
            if (handles.size() > 32) handles.iterator().next().close();
        }

        long append(Object value) throws IOException {
            ensureOpen();
            long position = data.length();
            byte[] bytes = JSON.writeValueAsBytes(value);
            data.seek(position);
            data.writeInt(bytes.length);
            data.write(bytes);
            offsets.seek(count++ * Long.BYTES);
            offsets.writeLong(position);
            return position;
        }

        <T> T at(long position, Class<T> type) throws IOException {
            ensureOpen();
            data.seek(position);
            byte[] bytes = new byte[data.readInt()];
            data.readFully(bytes);
            return JSON.readValue(bytes, type);
        }

        <T> T row(long index, Class<T> type) throws IOException {
            ensureOpen();
            offsets.seek(index * Long.BYTES);
            return at(offsets.readLong(), type);
        }

        long[] shuffled(long seed) throws IOException {
            ensureOpen();
            long[] positions = new long[Math.toIntExact(count)];
            offsets.seek(0);
            for (int i = 0; i < positions.length; i++) positions[i] = offsets.readLong();
            var random = new SplittableRandom(seed);
            for (int i = positions.length - 1; i > 0; i--) {
                int j = random.nextInt(i + 1);
                long value = positions[i]; positions[i] = positions[j]; positions[j] = value;
            }
            return positions;
        }

        public void close() throws IOException {
            handles.remove(this);
            try { if (data != null) data.close(); }
            finally {
                data = null;
                try { if (offsets != null) offsets.close(); } finally { offsets = null; }
            }
        }
    }

    final class Bin {
        final Rows drivers;
        final Rows passengers;
        Bin(int bin) throws IOException {
            drivers = new Rows("bin-" + bin + "-drivers");
            passengers = new Rows("bin-" + bin + "-passengers");
        }
    }

    AssignmentSpool(Path parent) throws IOException {
        directory = Files.createTempDirectory(parent, ".marouter-");
        RandomAccessFile offsets = null;
        try {
            results = new Rows("results");
            offsets = file("result-index");
            resultOffsets = offsets;
        } catch (IOException e) {
            if (offsets != null) offsets.close();
            for (Rows rows : open) rows.close();
            for (Path path : files) Files.deleteIfExists(path);
            Files.deleteIfExists(directory);
            throw e;
        }
    }

    void add(int bin, boolean passenger, long index, Map<String, String> row) throws IOException {
        Bin target = bins.get(bin);
        if (target == null) { target = new Bin(bin); bins.put(bin, target); }
        (passenger ? target.passengers : target.drivers).append(new Input(index, row));
    }

    void put(long index, MultiModeRouter.RoutedTrip result, boolean truck) throws IOException {
        long position = results.append(new Output(result, result.assignmentPath, truck));
        resultOffsets.seek(index * Long.BYTES);
        resultOffsets.writeLong(position);
    }

    Output get(long index) throws IOException {
        resultOffsets.seek(index * Long.BYTES);
        return results.at(resultOffsets.readLong(), Output.class);
    }

    private RandomAccessFile file(String name) throws IOException {
        Path path = directory.resolve(name);
        files.add(path);
        return new RandomAccessFile(path.toFile(), "rw");
    }

    public void close() throws IOException {
        IOException failure = null;
        try { resultOffsets.close(); } catch (IOException e) { failure = e; }
        for (Rows rows : open) {
            try { rows.close(); } catch (IOException e) { if (failure == null) failure = e; else failure.addSuppressed(e); }
        }
        for (Path path : files) {
            try { Files.deleteIfExists(path); } catch (IOException e) { if (failure == null) failure = e; else failure.addSuppressed(e); }
        }
        try { Files.deleteIfExists(directory); } catch (IOException e) { if (failure == null) failure = e; else failure.addSuppressed(e); }
        if (failure != null) throw failure;
    }
}
