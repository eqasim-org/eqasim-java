// Builds the latest immutable Rust revision for every bundled platform without reusing release binaries.
package org.eqasim.switzerland.ch_cmdp.rustRouting;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;

final class LatestRustRouterBuild {
    private static final String REPOSITORY = "https://github.com/matsim-eth/RustRouter.git";

    record Source(Path directory, Path cache, String revision, boolean reusable) {}

    private LatestRustRouterBuild() {}

    /** Resolve main on every Maven invocation; never update a user's working checkout. */
    static Source resolve(Path root, String local, RustRouterInstallation.ProcessRunner runner)
            throws Exception {
        Files.createDirectories(root);
        if (!local.isBlank()) {
            Path source = Path.of(local).toAbsolutePath().normalize();
            if (!Files.isRegularFile(source.resolve("Cargo.toml")))
                throw new IOException("Missing Rust Cargo.toml in " + source);
            return new Source(source, root.resolve("local"), "local-working-tree", false);
        }
        Path probe = Files.createTempFile(root, "main-", ".log");
        // Preserve the probe log on failure so authentication/network errors remain inspectable.
        runner.run(List.of("git", "ls-remote", REPOSITORY, "refs/heads/main"), root, probe);
        String revision = parseRevision(Files.readString(probe));
        Files.deleteIfExists(probe);
        Path cache = root.resolve(revision);
        Files.createDirectories(cache);
        try (var channel = FileChannel.open(cache.resolve("source.lock"),
                        StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                var lock = channel.lock()) {
            Path source = cache.resolve("source");
            if (!Files.isRegularFile(source.resolve("Cargo.toml"))) {
                Path temporary = Files.createTempDirectory(cache, "checkout-");
                Path log = cache.resolve("source.log");
                try {
                    runner.run(List.of("git", "init", temporary.toString()), cache, log);
                    runner.run(List.of("git", "fetch", "--depth", "1", REPOSITORY, revision), temporary, log);
                    runner.run(List.of("git", "checkout", "--detach", "FETCH_HEAD"), temporary, log);
                    if (!Files.isRegularFile(temporary.resolve("Cargo.toml")))
                        throw new IOException("Rust checkout has no Cargo.toml; see " + log);
                    Files.move(temporary, source, StandardCopyOption.ATOMIC_MOVE);
                } finally {
                    if (Files.exists(temporary)) {
                        try (var paths = Files.walk(temporary)) {
                            for (Path path : paths.sorted(Comparator.reverseOrder()).toList())
                                Files.deleteIfExists(path);
                        }
                    }
                }
            }
            return new Source(source, cache, revision, true);
        }
    }

    static String parseRevision(String response) throws IOException {
        for (String line : response.lines().toList()) {
            String[] fields = line.trim().split("\\s+");
            if (fields.length == 2 && fields[0].matches("[a-f0-9]{40}")
                    && fields[1].equals("refs/heads/main")) return fields[0];
        }
        throw new IOException("Cannot resolve Rust main revision; refusing to bundle an older release");
    }

    /** Cache only validated, commit-specific outputs; local overrides are rebuilt every time. */
    static Path build(Source source, RustRouterInstallation.Platform platform,
            RustRouterInstallation.Platform host, RustRouterInstallation.ProcessRunner runner)
            throws Exception {
        Path directory = source.cache().resolve(platform.id());
        Files.createDirectories(directory);
        try (var channel = FileChannel.open(directory.resolve("build.lock"),
                        StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                var lock = channel.lock()) {
            Path library = directory.resolve(platform.library());
            Path checksum = directory.resolve("built.sha256");
            if (source.reusable() && Files.isRegularFile(library) && Files.isRegularFile(checksum)
                    && RustRouterInstallation.sha256(library).equals(Files.readString(checksum).trim()))
                return library;
            Files.deleteIfExists(checksum);
            if (platform.equals(host)) {
                RustRouterInstallation.buildSource(source.directory(), directory, library, platform, runner);
            } else {
                crossBuild(source.directory(), directory, library, platform, host, runner);
            }
            Files.writeString(checksum, RustRouterInstallation.sha256(library) + "\n");
            return library;
        }
    }

    /** Provision isolated Python-packaged Zig tools for portable Linux/glibc 2.17 binaries. */
    private static void crossBuild(Path source, Path directory, Path library,
            RustRouterInstallation.Platform platform, RustRouterInstallation.Platform host,
            RustRouterInstallation.ProcessRunner runner) throws Exception {
        if (!platform.id().equals("linux-x86_64"))
            throw new IOException("Automatic cross-build unsupported for " + platform.id()
                    + "; supply rust.router.nativeDirectory or build on that platform");
        Path tools = directory.getParent().getParent().resolve("cross-tools-v1-" + host.id());
        Files.createDirectories(tools);
        Path log = directory.resolve("installation.log");
        boolean windows = host.id().startsWith("windows-");
        Path python = tools.resolve(windows ? "venv/Scripts/python.exe" : "venv/bin/python");
        try (var channel = FileChannel.open(tools.resolve("setup.lock"),
                        StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                var lock = channel.lock()) {
            if (!Files.isRegularFile(tools.resolve("ready"))) {
                runner.run(List.of(windows ? "python" : "python3", "-m", "venv", tools.resolve("venv").toString()), directory, log);
                runner.run(List.of(python.toString(), "-m", "pip", "install",
                        "ziglang==0.17.1", "cargo-zigbuild==0.24.1"), directory, log);
                Files.writeString(tools.resolve("ready"), "ziglang=0.17.1\ncargo-zigbuild=0.24.1\n");
            }
        }
        String target = "x86_64-unknown-linux-gnu";
        runner.run(List.of("rustup", "target", "add", target), directory, log);
        // Both tools find Zig through PATH; keep all environment changes inside this child process.
        String script = "import os,pathlib,subprocess,sys,ziglang; "
                + "os.environ['PATH']=str(pathlib.Path(ziglang.__file__).parent)+os.pathsep+"
                + "str(pathlib.Path(sys.executable).parent)+os.pathsep+os.environ.get('PATH',''); "
                + "sys.exit(subprocess.call(sys.argv[1:]))";
        Path cargoZig = python.getParent().resolve(windows ? "cargo-zigbuild.exe" : "cargo-zigbuild");
        runner.run(List.of(python.toString(), "-c", script, cargoZig.toString(), "zigbuild",
                "--release", "--locked", "--manifest-path", source.resolve("Cargo.toml").toString(),
                "--target-dir", directory.resolve("build").toString(), "-p", "matsim-pt-router-jni",
                "--target", target + ".2.17"), directory, log);
        Files.copy(directory.resolve("build").resolve(target).resolve("release").resolve(platform.library()),
                library, StandardCopyOption.REPLACE_EXISTING);
    }
}
