// Acquires a versioned, shared native router from a release or pinned source without changing the
// scenario.
package org.eqasim.switzerland.ch_cmdp.rustRouting;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;

import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipFile;

final class RustRouterInstallation {
    private static final Logger LOG = LogManager.getLogger(RustRouterInstallation.class);
    private static final String REPOSITORY =
            "https://github.com/Dib-AEK/MultiModalMatsimRouter.git";
    private static final long MAX_ARCHIVE_BYTES = 100 * 1024 * 1024;
    private static final long MAX_LIBRARY_BYTES = 64 * 1024 * 1024;

    record Platform(String id, String library) {
        static Platform current() {
            return of(System.getProperty("os.name"), System.getProperty("os.arch"));
        }

        static Platform of(String os, String arch) {
            String cpu =
                    switch (arch.toLowerCase(Locale.ROOT)) {
                        case "amd64", "x86_64" -> "x86_64";
                        case "aarch64", "arm64" -> "aarch64";
                        default ->
                                throw new IllegalArgumentException(
                                        "Unsupported Rust router architecture: " + arch);
                    };
            String name = os.toLowerCase(Locale.ROOT);
            if (name.startsWith("windows"))
                return new Platform("windows-" + cpu, "matsim_pt_router.dll");
            if (name.contains("mac") || name.contains("darwin"))
                return new Platform("macos-" + cpu, "libmatsim_pt_router.dylib");
            if (name.contains("linux"))
                return new Platform("linux-" + cpu, "libmatsim_pt_router.so");
            throw new IllegalArgumentException("Unsupported Rust router operating system: " + os);
        }
    }

    private RustRouterInstallation() {}

    /** Explicit inputs resolve against the MATSim config, never the process working directory. */
    static Path inputPath(Config config, String value) throws Exception {
        return Path.of(ConfigGroup.getInputFileURL(config.getContext(), value).toURI())
                .toAbsolutePath();
    }

    /** No network or toolchain is touched when a supplied/bundled/system library exists. */
    static Path resolve(Config config, RustRoutingConfigGroup settings) throws Exception {
        if (!settings.libraryPath.isBlank()) {
            Path explicit = inputPath(config, settings.libraryPath);
            if (!Files.isRegularFile(explicit))
                throw new IOException("Missing explicit Rust library: " + explicit);
            return explicit;
        }
        Platform platform = Platform.current();
        try (var resource = bundledLibrary(platform)) {
            if (resource != null) {
                Path directory = Files.createTempDirectory("eqasim-rust-native-");
                Path library = directory.resolve(platform.library());
                directory.toFile().deleteOnExit();
                library.toFile().deleteOnExit();
                Files.copy(resource, library);
                return library;
            }
        }
        for (String directory :
                System.getProperty("java.library.path", "").split(java.io.File.pathSeparator)) {
            if (directory.isBlank()) continue;
            Path candidate = Path.of(directory).resolve(platform.library());
            if (Files.isRegularFile(candidate)) return candidate.toAbsolutePath();
        }
        if (!settings.autoInstall)
            throw new IOException(
                    "No Rust library supplied/bundled; set swissRustRouting.libraryPath or enable"
                            + " autoInstall");
        Path root =
                settings.installationDirectory.isBlank()
                        ? Path.of(System.getProperty("user.home"), ".eqasim", "rust-router")
                        : inputPath(config, settings.installationDirectory);
        return install(config, settings, platform, root);
    }

    /** New standalone jars namespace by platform; retain compatibility with older bundled jars. */
    private static java.io.InputStream bundledLibrary(Platform platform) {
        var resource =
                NativePtRouter.class.getResourceAsStream(
                        "/native/" + platform.id() + "/" + platform.library());
        return resource != null
                ? resource
                : NativePtRouter.class.getResourceAsStream("/native/" + platform.library());
    }

    /**
     * The JVM monitor and file lock serialize first installation across threads and simulations.
     */
    static synchronized Path install(
            Config config, RustRoutingConfigGroup settings, Platform platform, Path root)
            throws Exception {
        if (!settings.version.matches("v[0-9]+\\.[0-9]+\\.[0-9]+(?:-[A-Za-z0-9.-]+)?"))
            throw new IllegalArgumentException(
                    "Rust router version must be a release tag such as v0.2.0");
        Path directory = root.resolve(settings.version).resolve(platform.id()).toAbsolutePath();
        if (!settings.sourceDirectory.isBlank()) {
            // A development checkout must never replace the cached official release.
            String sourceKey =
                    HexFormat.of()
                            .formatHex(
                                    MessageDigest.getInstance("SHA-256")
                                            .digest(
                                                    inputPath(config, settings.sourceDirectory)
                                                            .normalize()
                                                            .toString()
                                                            .getBytes(
                                                                    java.nio.charset
                                                                            .StandardCharsets
                                                                            .UTF_8)));
            directory = directory.resolve("local-" + sourceKey.substring(0, 16));
        }
        Files.createDirectories(directory);
        try (var channel =
                        FileChannel.open(
                                directory.resolve("installation.lock"),
                                StandardOpenOption.CREATE,
                                StandardOpenOption.WRITE);
                var lock = channel.lock()) {
            Path checksum = directory.resolve("installed.sha256");
            if (settings.sourceDirectory.isBlank() && Files.isRegularFile(checksum)) {
                String recorded = Files.readString(checksum).trim();
                if (recorded.matches("[a-f0-9]{64}")) {
                    Path cached = directory.resolve(recorded).resolve(platform.library());
                    if (Files.isRegularFile(cached) && sha256(cached).equals(recorded)) {
                        LOG.info(
                                "Using cached Rust PT router {} ({})",
                                settings.version,
                                platform.id());
                        return cached;
                    }
                }
            }
            Path staged = Files.createTempFile(directory, "library-", ".part");
            try {
                if (!settings.sourceDirectory.isBlank()) {
                    buildSource(
                            inputPath(config, settings.sourceDirectory),
                            directory,
                            staged,
                            platform,
                            RustRouterInstallation::run);
                } else {
                    try {
                        downloadRelease(settings.version, platform, directory, staged);
                    } catch (IOException e) {
                        if (!settings.allowSourceBuild) throw e;
                        LOG.info(
                                "Compiled Rust package unavailable ({}); building {} from source",
                                e.getMessage(),
                                settings.version);
                        Path source =
                                checkout(settings.version, directory, RustRouterInstallation::run);
                        buildSource(
                                source, directory, staged, platform, RustRouterInstallation::run);
                    }
                }
                String digest = sha256(staged);
                Path library = directory.resolve(digest).resolve(platform.library());
                Files.createDirectories(library.getParent());
                // Content-addressed libraries are immutable, including while another JVM uses them.
                if (!Files.isRegularFile(library) || !sha256(library).equals(digest))
                    Files.move(
                            staged,
                            library,
                            StandardCopyOption.REPLACE_EXISTING,
                            StandardCopyOption.ATOMIC_MOVE);
                // This marker is written last. An interrupted install is never considered ready.
                Files.writeString(checksum, digest + "\n");
                LOG.info("Installed Rust PT router {} at {}", settings.version, library);
                return library;
            } finally {
                Files.deleteIfExists(staged);
            }
        }
    }

    /**
     * Prefer a compiled release. A token is read only from the environment, never from
     * config/output files.
     */
    private static void downloadRelease(
            String version, Platform platform, Path directory, Path library) throws Exception {
        String assetName =
                "MultiModalMatsimRouter-" + version.substring(1) + "-" + platform.id() + ".zip";
        var client =
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(20))
                        .followRedirects(HttpClient.Redirect.NEVER)
                        .build();
        URI api =
                URI.create(
                        "https://api.github.com/repos/Dib-AEK/MultiModalMatsimRouter/releases/tags/"
                                + version);
        byte[] body;
        try (var input = request(client, api, "application/vnd.github+json")) {
            body = input.readNBytes(2 * 1024 * 1024 + 1);
        }
        if (body.length > 2 * 1024 * 1024)
            throw new IOException("Oversized GitHub release metadata");
        JsonNode assets = new ObjectMapper().readTree(body).path("assets");
        JsonNode asset = null;
        for (var candidate : assets)
            if (assetName.equals(candidate.path("name").asText())) asset = candidate;
        if (asset == null) throw new IOException("No " + platform.id() + " binary in " + version);
        String digest = asset.path("digest").asText();
        if (!digest.matches("sha256:[a-f0-9]{64}"))
            throw new IOException("Release asset has no SHA-256 digest");
        Path archive = Files.createTempFile(directory, "download-", ".zip");
        try {
            LOG.info("Downloading Rust PT router {} for {}", version, platform.id());
            try (var input =
                            request(
                                    client,
                                    URI.create(asset.path("url").asText()),
                                    "application/octet-stream");
                    var output = Files.newOutputStream(archive)) {
                long copied =
                        input.transferTo(
                                new java.io.FilterOutputStream(output) {
                                    private long total;

                                    @Override
                                    public void write(byte[] bytes, int offset, int length)
                                            throws IOException {
                                        if ((total += length) > MAX_ARCHIVE_BYTES)
                                            throw new IOException("Oversized native archive");
                                        out.write(bytes, offset, length);
                                    }

                                    @Override
                                    public void write(int value) throws IOException {
                                        if (++total > MAX_ARCHIVE_BYTES)
                                            throw new IOException("Oversized native archive");
                                        out.write(value);
                                    }
                                });
                if (copied != asset.path("size").asLong())
                    throw new IOException("Incomplete native archive");
            }
            extractLibrary(
                    archive,
                    library,
                    assetName.substring(0, assetName.length() - 4),
                    platform,
                    digest.substring(7));
        } finally {
            Files.deleteIfExists(archive);
        }
    }

    /** Redirects may carry signed URLs; never forward a GitHub token to an asset host. */
    private static java.io.InputStream request(HttpClient client, URI uri, String accept)
            throws IOException, InterruptedException {
        for (int redirects = 0; redirects < 6; redirects++) {
            if (!uri.getScheme().equals("https"))
                throw new IOException("Native download requires HTTPS");
            var builder =
                    HttpRequest.newBuilder(uri)
                            .timeout(Duration.ofMinutes(3))
                            .header("Accept", accept)
                            .header("User-Agent", "eqasim-rust-router");
            String token = System.getenv("GITHUB_TOKEN");
            if (token == null || token.isBlank()) token = System.getenv("GH_TOKEN");
            if ("api.github.com".equals(uri.getHost()) && token != null && !token.isBlank())
                builder.header("Authorization", "Bearer " + token);
            var response =
                    client.send(builder.GET().build(), HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() == 200) return response.body();
            response.body().close();
            if (Set.of(301, 302, 303, 307, 308).contains(response.statusCode())) {
                uri =
                        uri.resolve(
                                response.headers()
                                        .firstValue("location")
                                        .orElseThrow(
                                                () ->
                                                        new IOException(
                                                                "Missing download redirect")));
            } else
                throw new IOException(
                        "GitHub release HTTP "
                                + response.statusCode()
                                + "; private releases require GITHUB_TOKEN/GH_TOKEN with repository"
                                + " access");
        }
        throw new IOException("Too many native download redirects");
    }

    /**
     * Extract exactly one known library to a supplied path; archive paths are never used as
     * destinations.
     */
    static void extractLibrary(
            Path archive, Path destination, String prefix, Platform platform, String expected)
            throws Exception {
        if (!sha256(archive).equals(expected))
            throw new IOException("Native archive SHA-256 mismatch");
        String member = prefix + "/native/" + platform.library();
        try (var zip = new ZipFile(archive.toFile())) {
            var entries =
                    zip.stream()
                            .filter(e -> e.getName().equals(member) && !e.isDirectory())
                            .toList();
            if (entries.size() != 1)
                throw new IOException("Expected exactly one native library in release archive");
            try (var input = zip.getInputStream(entries.getFirst());
                    var output = Files.newOutputStream(destination)) {
                byte[] bytes = input.readNBytes((int) MAX_LIBRARY_BYTES + 1);
                if (bytes.length == 0 || bytes.length > MAX_LIBRARY_BYTES)
                    throw new IOException("Invalid native library size");
                output.write(bytes);
            }
        }
    }

    @FunctionalInterface
    interface ProcessRunner {
        void run(List<String> command, Path workingDirectory, Path log) throws Exception;
    }

    /** Clone into a temporary directory; never reset or overwrite a user's existing checkout. */
    static Path checkout(String version, Path directory, ProcessRunner runner) throws Exception {
        Path source = directory.resolve("source");
        if (Files.isRegularFile(source.resolve("Cargo.toml"))) return source;
        Path temporary = Files.createTempDirectory(directory, "checkout-");
        try {
            Path clone = temporary.resolve("source");
            runner.run(
                    List.of(
                            "git",
                            "clone",
                            "--depth",
                            "1",
                            "--single-branch",
                            "--branch",
                            version,
                            REPOSITORY,
                            clone.toString()),
                    directory,
                    directory.resolve("installation.log"));
            Files.move(clone, source, StandardCopyOption.ATOMIC_MOVE);
            return source;
        } finally {
            try (var paths = Files.walk(temporary)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList())
                    Files.deleteIfExists(path);
            }
        }
    }

    /** Cargo output is isolated in the installation cache, even when using an existing checkout. */
    static void buildSource(
            Path source, Path directory, Path destination, Platform platform, ProcessRunner runner)
            throws Exception {
        if (!Files.isRegularFile(source.resolve("Cargo.toml")))
            throw new IOException("Missing Rust Cargo.toml in " + source);
        Path target = directory.resolve("build");
        Path cargo =
                Path.of(
                        System.getProperty("user.home"),
                        ".cargo",
                        "bin",
                        platform.id().startsWith("windows-") ? "cargo.exe" : "cargo");
        String configuredCargo = System.getProperty("eqasim.rust.cargo", "cargo");
        String executable =
                configuredCargo.equals("cargo") && Files.isRegularFile(cargo)
                        ? cargo.toString()
                        : configuredCargo;
        LOG.info(
                "Building Rust PT router from {}; build log: {}",
                source,
                directory.resolve("installation.log"));
        runner.run(
                List.of(
                        executable,
                        "build",
                        "--release",
                        "--locked",
                        "--manifest-path",
                        source.resolve("Cargo.toml").toString(),
                        "--target-dir",
                        target.toString(),
                        "-p",
                        "matsim-pt-router-jni"),
                directory,
                directory.resolve("installation.log"));
        Files.copy(
                target.resolve("release").resolve(platform.library()),
                destination,
                StandardCopyOption.REPLACE_EXISTING);
    }

    private static void run(List<String> command, Path directory, Path log) throws Exception {
        var builder =
                new ProcessBuilder(command)
                        .directory(directory.toFile())
                        .redirectErrorStream(true)
                        .redirectOutput(ProcessBuilder.Redirect.appendTo(log.toFile()));
        builder.environment().put("GIT_TERMINAL_PROMPT", "0");
        builder.environment().put("GCM_INTERACTIVE", "Never");
        builder.environment().remove("GITHUB_TOKEN");
        builder.environment().remove("GH_TOKEN");
        Process process;
        try {
            process = builder.start();
        } catch (IOException e) {
            throw new IOException(
                    "Cannot start "
                            + Path.of(command.getFirst()).getFileName()
                            + "; install Git/Rust and a native linker or provide a compiled"
                            + " library",
                    e);
        }
        try {
            if (!process.waitFor(30, TimeUnit.MINUTES))
                throw new IOException("Rust setup timed out; see " + log);
            if (process.exitValue() != 0)
                throw new IOException(
                        "Rust setup process failed (exit " + process.exitValue() + "); see " + log);
        } finally {
            if (process.isAlive()) {
                process.descendants().forEach(ProcessHandle::destroyForcibly);
                process.destroyForcibly();
            }
        }
    }

    static String sha256(Path file) throws Exception {
        var digest = MessageDigest.getInstance("SHA-256");
        try (var input = Files.newInputStream(file)) {
            byte[] buffer = new byte[65536];
            for (int count; (count = input.read(buffer)) != -1; ) digest.update(buffer, 0, count);
        }
        return HexFormat.of().formatHex(digest.digest());
    }
}
