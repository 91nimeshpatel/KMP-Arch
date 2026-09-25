import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The published module graph is generated, and this proves it is current.
 *
 * The graph used to be written by hand in three documents. Three copies of one fact drift, and a
 * wrong diagram is worse than none because the next reader reasons from it. So it is derived from
 * the `project(":…")` declarations Gradle actually resolves, and lives in exactly one place.
 *
 * This test is the other half of that: generation only helps if someone regenerates. Change a module
 * dependency without running the generator and the build fails here, naming the command.
 */
class ModuleGraphTest {
    @Test
    fun `the generated module graph is up to date`() {
        val process =
            ProcessBuilder("python3", "tools/module-graph.py", "--check")
                .directory(ROOT)
                .redirectErrorStream(true)
                .start()
        val output =
            process.inputStream
                .bufferedReader()
                .readText()
                .trim()
        val exit = process.waitFor()

        assertTrue(
            exit == 0,
            """
            The module graph in the documents no longer matches the build files.

            $output

            Do not edit the diagram by hand — it is generated. Run:
                python3 tools/module-graph.py
            """.trimIndent(),
        )
    }

    private companion object {
        val ROOT: File =
            generateSequence(File(".").absoluteFile) { it.parentFile }
                .first { File(it, "settings.gradle.kts").isFile }
    }
}
