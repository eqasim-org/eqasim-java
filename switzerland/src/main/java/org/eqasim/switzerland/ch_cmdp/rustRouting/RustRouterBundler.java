// Embeds configured platform libraries so the standalone jar runs on workstation and cluster
// without setup.
package org.eqasim.switzerland.ch_cmdp.rustRouting;

import org.matsim.core.config.ConfigUtils;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public final class RustRouterBundler {
    private RustRouterBundler() {}

    /** A supplied binary avoids compilation; an existing source checkout is built as-is. */
    @SuppressWarnings("null")
    public static void main(String[] args) throws Exception {
        Map<String, String> options = new HashMap<>();
        for (String argument : args) {
            int separator = argument.indexOf('=');
            if (!argument.startsWith("--") || separator < 3)
                throw new IllegalArgumentException("Expected --name=value bundler arguments");
            String key = argument.substring(2, separator);
            if (!Set.of("output", "source", "library", "version", "platforms", "native-directory")
                            .contains(key)
                    || options.put(key, argument.substring(separator + 1)) != null)
                throw new IllegalArgumentException("Unknown or duplicate bundler option: " + key);
        }
        if (!options.containsKey("output") || options.get("output").isBlank())
            throw new IllegalArgumentException("Missing native resource output directory");
        Path output = Path.of(options.get("output")).toAbsolutePath();
        var host = RustRouterInstallation.Platform.current();
        String version = options.getOrDefault("version", "latest");
        var platforms = platforms(options.getOrDefault("platforms", "host,linux-x86_64"), host);
        Path cache = Path.of(System.getProperty("user.home"), ".eqasim", "rust-router");
        // Validate explicit overrides before attempting optional acquisition.
        for (var platform : platforms) supplied(options, platform, host);
        LatestRustRouterBuild.Source latest = null;
        if (version.equals("latest") && platforms.stream().anyMatch(p -> supplied(options, p, host) == null)
                && toolchainAvailable(cache, RustRouterInstallation::run)) {
            try {
                latest = LatestRustRouterBuild.resolve(cache.resolve("main-builds"),
                        options.getOrDefault("source", ""), RustRouterInstallation::run);
                System.out.println("Building Rust PT router revision " + latest.revision());
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw interrupted;
            } catch (Exception failure) {
                System.out.println("[WARNING] Latest Rust source unavailable: " + failure.getMessage());
            }
        }
        List<String> bundledPlatforms = new ArrayList<>();
        final var source = latest;
        String[] releaseVersion = {version.equals("latest") ? null : version};
        for (var platform : platforms) {
            Path explicit = supplied(options, platform, host);
            Library library = explicit != null ? new Library(explicit,
                    version.equals("latest") ? "explicit-library" : version)
                    : optionalLibrary(platform,
                            source == null ? null : () -> new Library(LatestRustRouterBuild.build(
                                    source, platform, host, RustRouterInstallation::run), source.revision()),
                            () -> {
                                if (releaseVersion[0] == null)
                                    releaseVersion[0] = RustRouterInstallation.latestReleaseVersion();
                                return new Library(acquire(releaseVersion[0], platform), releaseVersion[0]);
                            });
            if (library == null) {
                removeGenerated(output, platform);
                System.out.println("[WARNING] Rust PT routing is not bundled for " + platform.id()
                        + "; Maven will continue. Use SwissRailRaptor or supply a native library to use Rust on this platform.");
            } else {
                Path bundled = bundle(output, library.path(), platform, library.version());
                bundledPlatforms.add(platform.id());
                System.out.println("Bundled Rust PT router for " + platform.id() + " (" + library.version() + "): " + bundled);
            }
        }
        writePlatforms(output, bundledPlatforms);
    }

    record Library(Path path, String version) {}
    @FunctionalInterface interface LibraryAcquisition { Library get() throws Exception; }

    /** Optional Rust acquisition never turns an absent compiler/download into a Maven failure. */
    static Library optionalLibrary(RustRouterInstallation.Platform platform,
            LibraryAcquisition source, LibraryAcquisition release) throws InterruptedException {
        for (var acquisition : Arrays.asList(source, release)) {
            if (acquisition == null) continue;
            try {
                return acquisition.get();
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw interrupted;
            } catch (Exception failure) {
                System.out.println("[WARNING] Rust PT library unavailable for " + platform.id() + ": " + failure.getMessage());
            }
        }
        return null;
    }

    /** Probe compiler availability before cloning or installing cross-compilation tools. */
    static boolean toolchainAvailable(Path root, RustRouterInstallation.ProcessRunner runner) throws Exception {
        Files.createDirectories(root);
        Path log = Files.createTempFile(root, "toolchain-", ".log");
        try {
            for (String tool : List.of("cargo", "rustc")) {
                Path standard = Path.of(System.getProperty("user.home"), ".cargo", "bin",
                        System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win") ? tool + ".exe" : tool);
                String configured = tool.equals("cargo") ? System.getProperty("eqasim.rust.cargo", tool) : tool;
                String executable = configured.equals(tool) && Files.isRegularFile(standard) ? standard.toString() : configured;
                runner.run(List.of(executable, "--version"), root, log);
            }
            return true;
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw interrupted;
        } catch (Exception absent) {
            System.out.println("[WARNING] Rust/Cargo toolchain unavailable; trying precompiled Rust PT releases.");
            return false;
        } finally {
            Files.deleteIfExists(log);
        }
    }

    /** Remove only known generated resources so a failed incremental build cannot bundle stale Rust. */
    static void removeGenerated(Path output, RustRouterInstallation.Platform platform) throws IOException {
        Path directory = output.resolve("native").resolve(platform.id());
        Files.deleteIfExists(directory.resolve(platform.library()));
        Files.deleteIfExists(directory.resolve("build.properties"));
    }

    /** Advertise only libraries actually present, including the valid empty-platform case. */
    static void writePlatforms(Path output, List<String> platforms) throws IOException {
        Path directory = output.resolve("native");
        Files.createDirectories(directory);
        for (String platform : List.of("windows-x86_64", "windows-aarch64", "linux-x86_64",
                "linux-aarch64", "macos-x86_64", "macos-aarch64")) {
            if (!platforms.contains(platform)) removeGenerated(output, RustRouterInstallation.Platform.named(platform));
        }
        for (String legacy : List.of("matsim_pt_router.dll", "libmatsim_pt_router.so", "libmatsim_pt_router.dylib"))
            Files.deleteIfExists(directory.resolve(legacy));
        Files.writeString(directory.resolve("platforms.properties"),
                "# Native platforms embedded by the standalone build.\nplatforms=" + String.join(",", platforms) + "\n");
    }

    /** An explicit binary override wins; a missing requested override must never fall back. */
    static Path supplied(Map<String, String> options, RustRouterInstallation.Platform platform,
            RustRouterInstallation.Platform host) {
        String directory = options.getOrDefault("native-directory", "");
        Path path = !directory.isBlank()
                ? Path.of(directory).resolve(platform.id()).resolve(platform.library())
                : platform.equals(host) && !options.getOrDefault("library", "").isBlank()
                        ? Path.of(options.get("library")) : null;
        if (path != null && !Files.isRegularFile(path))
            throw new IllegalArgumentException("Missing explicitly supplied Rust library: " + path);
        return path == null ? null : path.toAbsolutePath();
    }

    /** Resolve/deduplicate explicit platforms; foreign targets must use precompiled libraries. */
    static List<RustRouterInstallation.Platform> platforms(
            String configured, RustRouterInstallation.Platform host) {
        var result = new LinkedHashSet<RustRouterInstallation.Platform>();
        for (String item : configured.split(",", -1)) {
            String name = item.trim();
            result.add(name.equals("host") ? host : RustRouterInstallation.Platform.named(name));
        }
        return List.copyOf(result);
    }

    /** This fallback is binary-only: it must work without Cargo, rustc, rustup or Git. */
    private static Path acquire(String version, RustRouterInstallation.Platform platform) throws Exception {
        var settings = new RustRoutingConfigGroup();
        settings.version = version;
        settings.allowSourceBuild = false;
        return RustRouterInstallation.install(ConfigUtils.createConfig(), settings, platform,
                Path.of(System.getProperty("user.home"), ".eqasim", "rust-router"));
    }

    /** Namespaced resources avoid selecting the wrong architecture when multiple builds coexist. */
    static Path bundle(
            Path output, Path library, RustRouterInstallation.Platform platform, String version)
            throws Exception {
        if (!Files.isRegularFile(library) || Files.size(library) == 0)
            throw new IOException("Missing or empty Rust JNI library: " + library);
        Path directory = output.resolve("native").resolve(platform.id());
        Files.createDirectories(directory);
        Path bundled = directory.resolve(platform.library());
        if (!library.toAbsolutePath().normalize().equals(bundled.toAbsolutePath().normalize()))
            Files.copy(library, bundled, StandardCopyOption.REPLACE_EXISTING);
        Files.writeString(
                directory.resolve("build.properties"),
                "# Identifies the native library embedded by Maven.\nversion="
                        + version
                        + "\nplatform="
                        + platform.id()
                        + "\nsha256="
                        + RustRouterInstallation.sha256(bundled)
                        + "\n");
        // Remove only known legacy generated files, not other platforms' namespaced binaries.
        for (String legacy :
                List.of(
                        "matsim_pt_router.dll",
                        "libmatsim_pt_router.so",
                        "libmatsim_pt_router.dylib"))
            Files.deleteIfExists(output.resolve("native").resolve(legacy));
        return bundled;
    }
}
