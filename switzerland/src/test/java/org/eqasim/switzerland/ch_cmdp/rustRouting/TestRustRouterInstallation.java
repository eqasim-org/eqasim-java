// Verifies optional activation, safe release extraction, pinned source commands and offline cache
// reuse.
package org.eqasim.switzerland.ch_cmdp.rustRouting;

import static org.junit.Assert.*;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.matsim.core.config.*;
import org.matsim.core.scenario.ScenarioUtils;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.*;

public class TestRustRouterInstallation {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void latestRevisionRefreshesAndNeverFallsBackToReleaseCache() throws Exception {
        Path root = temporary.newFolder().toPath();
        String[] revision = {"a".repeat(40)};
        List<List<String>> commands = new ArrayList<>();
        RustRouterInstallation.ProcessRunner runner = (command, cwd, log) -> {
            commands.add(command);
            if (command.contains("ls-remote"))
                Files.writeString(log, revision[0] + "\trefs/heads/main\n");
            if (command.contains("checkout")) Files.writeString(cwd.resolve("Cargo.toml"), "fixture");
        };
        var first = LatestRustRouterBuild.resolve(root, "", runner);
        assertEquals(revision[0], first.revision());
        var again = LatestRustRouterBuild.resolve(root, "", runner);
        assertEquals(first.directory(), again.directory());
        assertEquals(1, commands.stream().filter(c -> c.contains("fetch")).count());
        revision[0] = "b".repeat(40);
        var updated = LatestRustRouterBuild.resolve(root, "", runner);
        assertNotEquals(first.directory(), updated.directory());
        assertTrue(commands.stream().anyMatch(c -> c.contains("fetch") && c.contains(revision[0])));
        assertThrows(java.io.IOException.class, () -> LatestRustRouterBuild.resolve(root, "",
                (command, cwd, log) -> { throw new java.io.IOException("offline"); }));
        assertThrows(java.io.IOException.class, () -> LatestRustRouterBuild.parseRevision("v0.2.0"));
    }

    @Test
    public void latestCacheIsValidatedAndLocalSourcesAlwaysRebuild() throws Exception {
        Path root = temporary.newFolder().toPath();
        Files.writeString(root.resolve("Cargo.toml"), "fixture");
        var platform = RustRouterInstallation.Platform.named("linux-x86_64");
        var source = new LatestRustRouterBuild.Source(root, root.resolve("cache"), "a".repeat(40), true);
        int[] builds = {0};
        RustRouterInstallation.ProcessRunner runner = (command, cwd, log) -> {
            builds[0]++;
            Path built = cwd.resolve("build/release").resolve(platform.library());
            Files.createDirectories(built.getParent());
            Files.writeString(built, "compiled");
        };
        Path library = LatestRustRouterBuild.build(source, platform, platform, runner);
        LatestRustRouterBuild.build(source, platform, platform, runner);
        assertEquals(1, builds[0]);
        Files.writeString(library, "corrupt");
        LatestRustRouterBuild.build(source, platform, platform, runner);
        assertEquals(2, builds[0]);
        var local = new LatestRustRouterBuild.Source(root, source.cache(), "local-working-tree", false);
        LatestRustRouterBuild.build(local, platform, platform, runner);
        LatestRustRouterBuild.build(local, platform, platform, runner);
        assertEquals(4, builds[0]);
    }

    @Test
    public void windowsCrossBuildUsesSameSourceAndPortableLinuxTarget() throws Exception {
        Path root = temporary.newFolder().toPath();
        var source = new LatestRustRouterBuild.Source(root, root.resolve("cache/revision"), "a".repeat(40), true);
        List<List<String>> commands = new ArrayList<>();
        Path library = LatestRustRouterBuild.build(source,
                RustRouterInstallation.Platform.named("linux-x86_64"),
                RustRouterInstallation.Platform.named("windows-x86_64"),
                (command, cwd, log) -> {
                    commands.add(command);
                    if (command.contains("zigbuild")) {
                        assertTrue(command.contains(root.resolve("Cargo.toml").toString()));
                        assertTrue(command.contains("x86_64-unknown-linux-gnu.2.17"));
                        assertTrue(command.contains("--locked"));
                        Path built = cwd.resolve("build/x86_64-unknown-linux-gnu/release/libmatsim_pt_router.so");
                        Files.createDirectories(built.getParent());
                        Files.writeString(built, "linux");
                    }
                });
        assertEquals("linux", Files.readString(library));
        assertTrue(commands.stream().anyMatch(c -> c.contains("venv")));
        assertTrue(commands.stream().anyMatch(c -> c.contains("ziglang==0.17.1")));
        assertThrows(IllegalArgumentException.class, () -> RustRouterBundler.supplied(
                Map.of("native-directory", root.resolve("missing").toString()),
                RustRouterInstallation.Platform.named("linux-x86_64"),
                RustRouterInstallation.Platform.named("windows-x86_64")));
    }

    @Test
    public void disabledMeansNoInstallationOrTransitRequirement() {
        var settings = new RustRoutingConfigGroup();
        settings.installationDirectory = "unusable:directory";
        var scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig(settings));
        assertFalse(settings.enabled);
        assertNull(RustRoutingModule.prepare(scenario, settings));
    }

    @Test
    public void commandOverridesConfigAndRejectsInvalidBoolean() throws Exception {
        var settings = new RustRoutingConfigGroup();
        settings.enabled = true;
        var config = ConfigUtils.createConfig(settings);
        var command =
                new CommandLine.Builder(new String[] {"--useRustRouter", "false"})
                        .allowOptions(RustRoutingModule.OPTION)
                        .build();
        assertFalse(RustRoutingModule.applyCommand(command, config).enabled);
        command =
                new CommandLine.Builder(new String[] {"--useRustRouter", "yes"})
                        .allowOptions(RustRoutingModule.OPTION)
                        .build();
        var invalid = command;
        assertThrows(
                CommandLine.ConfigurationException.class,
                () -> RustRoutingModule.applyCommand(invalid, config));
    }

    @Test
    public void installationSettingsRoundTripAndPathsUseConfigDirectory() throws Exception {
        var settings = new RustRoutingConfigGroup();
        settings.autoInstall = false;
        settings.allowSourceBuild = false;
        settings.installationDirectory = "cache";
        settings.sourceDirectory = "checkout";
        var file = temporary.newFile("config.xml").toPath();
        ConfigUtils.writeConfig(ConfigUtils.createConfig(settings), file.toString());
        var config = ConfigUtils.loadConfig(file.toString(), new RustRoutingConfigGroup());
        var restored = ConfigUtils.addOrGetModule(config, RustRoutingConfigGroup.class);
        assertFalse(restored.autoInstall);
        assertFalse(restored.allowSourceBuild);
        assertEquals("v0.2.0", restored.version);
        assertEquals(
                file.getParent().resolve("cache"),
                RustRouterInstallation.inputPath(config, restored.installationDirectory));
        assertEquals(
                file.getParent().resolve("checkout"),
                RustRouterInstallation.inputPath(config, restored.sourceDirectory));
    }

    @Test
    public void platformUsesJvmArchitectureAndSupportsAllReleasePlatforms() {
        assertEquals(
                "windows-x86_64", RustRouterInstallation.Platform.of("Windows 11", "amd64").id());
        assertEquals("linux-x86_64", RustRouterInstallation.Platform.of("Linux", "x86_64").id());
        assertEquals(
                "macos-aarch64", RustRouterInstallation.Platform.of("Mac OS X", "aarch64").id());
        assertEquals("macos-x86_64", RustRouterInstallation.Platform.of("Darwin", "x86_64").id());
        assertThrows(
                IllegalArgumentException.class,
                () -> RustRouterInstallation.Platform.of("Linux", "x86"));
    }

    @Test
    public void portableBundleIncludesLinuxAndDeduplicatesLinuxHost() {
        var windows = RustRouterInstallation.Platform.of("Windows", "amd64");
        var linux = RustRouterInstallation.Platform.of("Linux", "amd64");
        assertEquals(
                List.of(windows, linux), RustRouterBundler.platforms("host,linux-x86_64", windows));
        assertEquals(List.of(linux), RustRouterBundler.platforms("host,linux-x86_64", linux));
        assertEquals(List.of(windows), RustRouterBundler.platforms("host", windows));
        assertThrows(
                IllegalArgumentException.class,
                () -> RustRouterBundler.platforms("host,../../invalid", windows));
        assertThrows(
                IllegalArgumentException.class, () -> RustRouterBundler.platforms("", windows));
    }

    @Test
    public void bundlePreservesBothWindowsAndLinuxAndTheirChecksums() throws Exception {
        Path output = temporary.newFolder().toPath();
        Path windows = temporary.newFile().toPath(), linux = temporary.newFile().toPath();
        Files.write(windows, new byte[] {1, 2});
        Files.write(linux, new byte[] {3, 4});
        Path dll =
                RustRouterBundler.bundle(
                        output,
                        windows,
                        RustRouterInstallation.Platform.named("windows-x86_64"),
                        "v0.2.0");
        Path so =
                RustRouterBundler.bundle(
                        output,
                        linux,
                        RustRouterInstallation.Platform.named("linux-x86_64"),
                        "v0.2.0");
        assertArrayEquals(new byte[] {1, 2}, Files.readAllBytes(dll));
        assertArrayEquals(new byte[] {3, 4}, Files.readAllBytes(so));
        assertTrue(
                Files.readString(so.getParent().resolve("build.properties"))
                        .contains(RustRouterInstallation.sha256(linux)));
    }

    @Test
    public void cachedGitCredentialParsingDoesNotRequireOrStoreUsername() {
        assertEquals(
                "example-build-password",
                RustRouterBuildCredentials.password(
                        "protocol=https\n"
                            + "host=github.com\n"
                            + "username=builder\n"
                            + "password=example-build-password\n"));
        assertNull(RustRouterBuildCredentials.password("username=builder\n"));
        assertNull(RustRouterBuildCredentials.password("password=\n"));
    }

    @Test
    public void buildBundlesNamespacedBinaryAndRemovesOnlyLegacyGeneratedLibraries()
            throws Exception {
        Path output = temporary.newFolder().toPath();
        Path library = temporary.newFile().toPath();
        Files.write(library, new byte[] {1, 2, 3});
        Files.createDirectories(output.resolve("native/other-platform"));
        Files.writeString(output.resolve("native/other-platform/keep"), "keep");
        Files.writeString(output.resolve("native/matsim_pt_router.dll"), "old");
        var platform = RustRouterInstallation.Platform.of("Windows 11", "amd64");
        Path bundled = RustRouterBundler.bundle(output, library, platform, "v0.2.0");
        assertEquals(output.resolve("native/windows-x86_64/matsim_pt_router.dll"), bundled);
        assertArrayEquals(Files.readAllBytes(library), Files.readAllBytes(bundled));
        assertTrue(
                Files.readString(bundled.getParent().resolve("build.properties"))
                        .contains(RustRouterInstallation.sha256(library)));
        assertFalse(Files.exists(output.resolve("native/matsim_pt_router.dll")));
        assertEquals("keep", Files.readString(output.resolve("native/other-platform/keep")));
        assertThrows(
                java.io.IOException.class,
                () ->
                        RustRouterBundler.bundle(
                                output, output.resolve("missing"), platform, "v0.2.0"));
    }

    private Path archive(String member) throws Exception {
        Path archive = temporary.newFile().toPath();
        try (var zip = new ZipOutputStream(Files.newOutputStream(archive))) {
            zip.putNextEntry(new ZipEntry("../../escape"));
            zip.write(new byte[] {9});
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry(member));
            zip.write(new byte[] {1, 2, 3});
            zip.closeEntry();
        }
        return archive;
    }

    @Test
    public void releaseDigestIsRequiredAndOnlyKnownLibraryIsExtracted() throws Exception {
        var platform = RustRouterInstallation.Platform.of("Linux", "amd64");
        Path zip = archive("release/native/" + platform.library());
        Path library = temporary.getRoot().toPath().resolve("library.so");
        assertThrows(
                java.io.IOException.class,
                () ->
                        RustRouterInstallation.extractLibrary(
                                zip, library, "release", platform, "0".repeat(64)));
        assertFalse(Files.exists(library));
        RustRouterInstallation.extractLibrary(
                zip, library, "release", platform, RustRouterInstallation.sha256(zip));
        assertArrayEquals(new byte[] {1, 2, 3}, Files.readAllBytes(library));
        assertFalse(Files.exists(temporary.getRoot().toPath().getParent().resolve("escape")));
        Path wrong = archive("wrong/native/" + platform.library());
        assertThrows(
                java.io.IOException.class,
                () ->
                        RustRouterInstallation.extractLibrary(
                                wrong,
                                library,
                                "release",
                                platform,
                                RustRouterInstallation.sha256(wrong)));
    }

    @Test
    public void concurrentSimulationsReuseVerifiedCacheWithoutNetworkOrTools() throws Exception {
        Path root = temporary.newFolder().toPath();
        var settings = new RustRoutingConfigGroup();
        settings.allowSourceBuild = false;
        var platform = RustRouterInstallation.Platform.of("Linux", "amd64");
        Path directory = root.resolve(settings.version).resolve(platform.id());
        Files.createDirectories(directory);
        Path content = temporary.newFile().toPath();
        Files.write(content, new byte[] {1, 2, 3});
        String digest = RustRouterInstallation.sha256(content);
        Path library = directory.resolve(digest).resolve(platform.library());
        Files.createDirectories(library.getParent());
        Files.copy(content, library);
        Files.writeString(directory.resolve("installed.sha256"), digest);
        var config = ConfigUtils.createConfig();
        try (var workers = Executors.newFixedThreadPool(12)) {
            List<Callable<Path>> tasks = new ArrayList<>();
            for (int i = 0; i < 24; i++)
                tasks.add(() -> RustRouterInstallation.install(config, settings, platform, root));
            for (var result : workers.invokeAll(tasks)) assertEquals(library, result.get());
        }
        // A configured development checkout must not be confused with the official cached release.
        settings.sourceDirectory = root.resolve("nonexistent-checkout").toString();
        var missingSource =
                assertThrows(
                        java.io.IOException.class,
                        () -> RustRouterInstallation.install(config, settings, platform, root));
        assertTrue(missingSource.getMessage().contains("Cargo.toml"));
        assertEquals(digest, Files.readString(directory.resolve("installed.sha256")));
        settings.version = "../../escape";
        assertThrows(
                IllegalArgumentException.class,
                () -> RustRouterInstallation.install(config, settings, platform, root));
    }

    @Test
    public void sourceCheckoutIsPinnedAndExistingCheckoutIsNeverReset() throws Exception {
        Path directory = temporary.newFolder().toPath();
        List<List<String>> commands = new ArrayList<>();
        RustRouterInstallation.ProcessRunner runner =
                (command, cwd, log) -> {
                    commands.add(command);
                    assertEquals(directory, cwd);
                    Path clone = Path.of(command.getLast());
                    Files.createDirectories(clone);
                    Files.writeString(clone.resolve("Cargo.toml"), "fixture");
                };
        Path source = RustRouterInstallation.checkout("v0.2.0", directory, runner);
        assertEquals(source, RustRouterInstallation.checkout("v0.2.0", directory, runner));
        assertEquals(1, commands.size());
        assertEquals(
                List.of("git", "clone", "--depth", "1", "--single-branch", "--branch", "v0.2.0"),
                commands.getFirst().subList(0, 7));
        assertEquals("fixture", Files.readString(source.resolve("Cargo.toml")));
        try (var files = Files.list(directory)) {
            assertEquals(List.of(source), files.toList());
        }
    }

    @Test
    public void cargoBuildIsLockedAndUsesCacheInsteadOfUserCheckoutForOutput() throws Exception {
        Path source = temporary.newFolder().toPath();
        Files.writeString(source.resolve("Cargo.toml"), "fixture");
        Path directory = temporary.newFolder().toPath();
        Path destination = temporary.newFile().toPath();
        var platform = RustRouterInstallation.Platform.of("Linux", "amd64");
        RustRouterInstallation.buildSource(
                source,
                directory,
                destination,
                platform,
                (command, cwd, log) -> {
                    assertTrue(command.contains("--locked"));
                    assertEquals(
                            source.resolve("Cargo.toml").toString(),
                            command.get(command.indexOf("--manifest-path") + 1));
                    assertEquals(
                            directory.resolve("build").toString(),
                            command.get(command.indexOf("--target-dir") + 1));
                    assertEquals("matsim-pt-router-jni", command.getLast());
                    Path built = directory.resolve("build/release").resolve(platform.library());
                    Files.createDirectories(built.getParent());
                    Files.write(built, new byte[] {4, 5});
                });
        assertArrayEquals(new byte[] {4, 5}, Files.readAllBytes(destination));
        assertFalse(Files.exists(source.resolve("target")));
    }
}
