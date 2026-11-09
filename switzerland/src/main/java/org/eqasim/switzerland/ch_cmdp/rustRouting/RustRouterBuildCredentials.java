// Reuses existing noninteractive Git credentials for private release downloads during Maven builds
// only.
package org.eqasim.switzerland.ch_cmdp.rustRouting;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

final class RustRouterBuildCredentials {
    private RustRouterBuildCredentials() {}

    static String resolve() throws InterruptedException {
        String token = System.getenv("GITHUB_TOKEN");
        if (token == null || token.isBlank()) token = System.getenv("GH_TOKEN");
        if (token != null && !token.isBlank()) return token;
        if (!Boolean.getBoolean("eqasim.rust.gitCredentials")) return null;
        Process process = null;
        try {
            var builder =
                    new ProcessBuilder("git", "credential", "fill")
                            .redirectError(ProcessBuilder.Redirect.DISCARD);
            builder.environment().put("GIT_TERMINAL_PROMPT", "0");
            builder.environment().put("GCM_INTERACTIVE", "Never");
            process = builder.start();
            try (var input = process.getOutputStream()) {
                input.write("protocol=https\nhost=github.com\n\n".getBytes(StandardCharsets.UTF_8));
            }
            if (!process.waitFor(10, TimeUnit.SECONDS) || process.exitValue() != 0) return null;
            return password(
                    new String(process.getInputStream().readNBytes(16384), StandardCharsets.UTF_8));
        } catch (java.io.IOException ignored) {
            // No Git/helper on this build machine: anonymous or explicit-token downloads can still
            // work.
            return null;
        } finally {
            if (process != null && process.isAlive()) {
                process.descendants().forEach(ProcessHandle::destroyForcibly);
                process.destroyForcibly();
            }
        }
    }

    static String password(String response) {
        return response.lines()
                .filter(line -> line.startsWith("password="))
                .map(line -> line.substring(9))
                .filter(value -> !value.isBlank())
                .findFirst()
                .orElse(null);
    }
}
