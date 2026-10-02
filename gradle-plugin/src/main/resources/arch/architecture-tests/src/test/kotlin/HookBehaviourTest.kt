import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The hooks do what CLAUDE.md says they do, fed the payloads Claude Code sends them.
 *
 * A hook that silently allows is indistinguishable from one that works: nothing fails. A backtest of
 * every hook in an app built from this template found three that did: release-guard.py let
 * `xcodebuild -exportArchive` (the signing and upload step) through, because its pattern put a word
 * boundary before a hyphen; docs-gate.py let a turn end with an undocumented seam when the
 * session's directory was a subfolder; and catalog-changed.py crashed there.
 */
class HookBehaviourTest {
    @Test
    fun `release-guard blocks every store build and nothing else`() {
        val blocked =
            listOf(
                "./gradlew :androidApp:bundleRelease",
                "./gradlew -p androidApp bundleRelease",
                "KEYSTORE_PASSWORD=x ./gradlew :androidApp:assembleRelease",
                "xcodebuild archive -project iosApp/iosApp.xcodeproj -scheme iosApp",
                "xcodebuild -exportArchive -archivePath a.xcarchive",
                "xcrun altool --upload-app -f a.ipa",
                "xcrun notarytool submit a.zip",
                "tools/release/release.sh --help; xcodebuild archive -scheme iosApp",
            )
        val allowed =
            listOf(
                "tools/release/release.sh v1.0.1 --e2e-log e2e.log",
                "./gradlew :androidApp:assembleRelease",
                "./gradlew :androidApp:installDebug",
                "./gradlew qualityCheck allTests",
                "xcodebuild build -scheme iosApp -configuration Debug",
            )
        blocked.forEach { assertEquals(2, run("release-guard.py", bash(it)), "release-guard.py must block: $it") }
        allowed.forEach { assertEquals(0, run("release-guard.py", bash(it)), "release-guard.py must allow: $it") }
    }

    @Test
    fun `docs-gate and catalog-changed check this repository from any directory`() {
        // From a subfolder or a parent folder the session's directory is not the repository root.
        listOf(ROOT, File(ROOT, "docs"), ROOT.parentFile).forEach { cwd ->
            val payload = """{"cwd": ${json(cwd.path)}, "scratchpad_dir": ${json(scratch().path)}}"""
            assertEquals(0, run("docs-gate.py", payload), "docs-gate.py from ${cwd.path}: the documents match")
            val catalog =
                """{"tool_input": {"file_path": ${json(File(ROOT, "gradle/libs.versions.toml").path)}}, """ +
                    """"cwd": ${json(cwd.path)}}"""
            assertEquals(0, run("catalog-changed.py", catalog), "catalog-changed.py from ${cwd.path} must not crash")
        }
    }

    @Test
    fun `docs-gate stops a turn with an undocumented seam, from a subfolder too`() {
        // A throwaway repository holding a copy of the hook and one undocumented seam, so the real
        // one is never touched. Run from a subfolder, as a session that has cd'ed into one would.
        val repo = File.createTempFile("docs-gate", "").also { it.delete() }
        try {
            File(repo, ".claude/hooks").mkdirs()
            File(ROOT, ".claude/hooks/docs-gate.py").copyTo(File(repo, ".claude/hooks/docs-gate.py"))
            File(repo, "docs").mkdirs()
            File(repo, "docs/ARCHITECTURE.md").writeText("# Architecture\n")
            File(repo, "core").mkdirs()
            File(File(repo, "core"), "Seam.kt").writeText("expect fun undocumentedSeam(): Int\n")
            val payload = """{"cwd": ${json(File(repo, "docs").path)}, "scratchpad_dir": ${json(scratch().path)}}"""
            assertEquals(2, run("docs-gate.py", payload, repo), "docs-gate.py must stop the turn from a subfolder")
        } finally {
            repo.deleteRecursively()
        }
    }

    private fun bash(command: String) = """{"tool_name": "Bash", "tool_input": {"command": ${json(command)}}}"""

    private fun json(text: String) = "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

    private fun scratch(): File = File.createTempFile("hook-test", "").also { it.delete() }

    private fun run(
        hook: String,
        payload: String,
        repo: File = ROOT,
    ): Int {
        val process =
            ProcessBuilder("python3", File(repo, ".claude/hooks/$hook").path)
                .directory(repo)
                .redirectErrorStream(true)
                .start()
        process.outputStream.use { it.write(payload.toByteArray()) }
        process.inputStream.readBytes()
        return process.waitFor()
    }

    private companion object {
        val ROOT: File =
            generateSequence(File(".").absoluteFile) { it.parentFile }
                .first { File(it, "settings.gradle.kts").isFile }
    }
}
