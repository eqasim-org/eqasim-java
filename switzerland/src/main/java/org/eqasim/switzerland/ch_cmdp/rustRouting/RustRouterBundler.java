// Acquires and embeds the host's Rust JNI library during the normal standalone Maven build.
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
            if (!Set.of("output", "source", "library", "version").contains(key)
                    || options.put(key, argument.substring(separator + 1)) != null)
                throw new IllegalArgumentException("Unknown or duplicate bundler option: " + key);
        }
        if (!options.containsKey("output") || options.get("output").isBlank())
            throw new IllegalArgumentException("Missing native resource output directory");
        Path output = Path.of(options.get("output")).toAbsolutePath();
        var platform = RustRouterInstallation.Platform.current();
        String version = options.getOrDefault("version", "v0.2.0");
        Path library;
        String explicit = options.getOrDefault("library", "");
        if (!explicit.isBlank()) {
            library = Path.of(explicit).toAbsolutePath();
        } else {
            var settings = new RustRoutingConfigGroup();
            settings.version = version;
            String source = options.getOrDefault("source", "");
            if (!source.isBlank() && Files.isRegularFile(Path.of(source).resolve("Cargo.toml")))
                settings.sourceDirectory = Path.of(source).toAbsolutePath().toString();
            library =
                    RustRouterInstallation.install(
                            ConfigUtils.createConfig(),
                            settings,
                            platform,
                            Path.of(System.getProperty("user.home"), ".eqasim", "rust-router"));
        }
        Path bundled = bundle(output, library, platform, version);
        System.out.println("Bundled Rust PT router for " + platform.id() + ": " + bundled);
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
