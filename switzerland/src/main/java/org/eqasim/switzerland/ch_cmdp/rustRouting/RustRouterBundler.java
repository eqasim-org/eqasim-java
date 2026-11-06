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
        String version = options.getOrDefault("version", "v0.2.0");
        var platforms = platforms(options.getOrDefault("platforms", "host,linux-x86_64"), host);
        for (var platform : platforms) {
            Path library = acquire(options, version, platform, host);
            Path bundled = bundle(output, library, platform, version);
            System.out.println("Bundled Rust PT router for " + platform.id() + ": " + bundled);
        }
        Files.writeString(
                output.resolve("native/platforms.properties"),
                "# Native platforms embedded by the standalone build.\nplatforms="
                        + String.join(
                                ",",
                                platforms.stream()
                                        .map(RustRouterInstallation.Platform::id)
                                        .toList())
                        + "\n");
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

    private static Path acquire(
            Map<String, String> options,
            String version,
            RustRouterInstallation.Platform platform,
            RustRouterInstallation.Platform host)
            throws Exception {
        Path library;
        String directory = options.getOrDefault("native-directory", "");
        if (!directory.isBlank()) {
            Path candidate = Path.of(directory).resolve(platform.id()).resolve(platform.library());
            if (Files.isRegularFile(candidate)) return candidate.toAbsolutePath();
        }
        String explicit = options.getOrDefault("library", "");
        if (platform.equals(host) && !explicit.isBlank()) {
            library = Path.of(explicit).toAbsolutePath();
        } else {
            var settings = new RustRoutingConfigGroup();
            settings.version = version;
            // A normal Cargo build produces the host binary, never a foreign-platform binary.
            settings.allowSourceBuild = platform.equals(host);
            String source = options.getOrDefault("source", "");
            if (platform.equals(host)
                    && !source.isBlank()
                    && Files.isRegularFile(Path.of(source).resolve("Cargo.toml")))
                settings.sourceDirectory = Path.of(source).toAbsolutePath().toString();
            try {
                library =
                        RustRouterInstallation.install(
                                ConfigUtils.createConfig(),
                                settings,
                                platform,
                                Path.of(System.getProperty("user.home"), ".eqasim", "rust-router"));
            } catch (Exception failure) {
                throw new IOException(
                        "Cannot bundle Rust router for "
                                + platform.id()
                                + ". Supply rust.router.nativeDirectory/<platform>/<library> or"
                                + " authenticate GitHub on the build machine. The build will not"
                                + " omit a requested platform. For a deliberately host-only jar set"
                                + " -Drust.router.platforms=host.",
                        failure);
            }
        }
        return library;
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
