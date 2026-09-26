import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Enforces .claude/rules/code-style.md: a platform seam is not done until it is written down.
 *
 * `LayerDependencyTest` already stops an `expect` appearing outside `:core:platform`. This one
 * stops a *legitimate* seam being added and left out of the architecture document — which is how
 * the table in `docs/ARCHITECTURE.md` quietly stops describing the app. Adding a seam is the moment
 * someone decides the two platforms genuinely differ, and that decision is worth a row in a table.
 */
class SeamDocumentationTest {
    @Test
    fun `every platform seam appears in the architecture document`() {
        val architecture = File(ROOT, "docs/ARCHITECTURE.md").readText()
        val undocumented = seams().filterNot { architecture.contains(it) }

        assertTrue(
            undocumented.isEmpty(),
            """
            These `expect` declarations are not mentioned in docs/ARCHITECTURE.md: $undocumented

            Add a row to the platform-seam table saying what each platform does and why the two
            cannot share one implementation (.claude/rules/code-style.md). A seam nobody wrote down is a
            difference between the platforms that nobody can find.
            """.trimIndent(),
        )
    }

    private companion object {
        val ROOT: File =
            generateSequence(File(".").absoluteFile) { it.parentFile }
                .first { File(it, "settings.gradle.kts").isFile }

        /**
         * The name of every `expect` declaration in the repository.
         *
         * Matched on the declaration line rather than by parsing Kotlin: this test must keep
         * working when the code it reads does not compile, because that is exactly when someone is
         * mid-change and about to forget the document.
         */
        fun seams(): List<String> {
            val declaration =
                Regex(
                    // The optional group before the name skips an extension receiver, so
                    // `expect fun Modifier.exposeTestTags()` gives the function, not `Modifier`.
                    "^\\s*(?:internal\\s+|private\\s+)?expect\\s+" +
                        "(?:fun|val|var|class|object|interface)\\s+" +
                        "(?:[A-Za-z0-9_.]+\\.)?([A-Za-z_][A-Za-z0-9_]*)",
                    RegexOption.MULTILINE,
                )
            return File(ROOT, "core")
                .walkTopDown()
                .onEnter { it.name != "build" }
                .filter { it.isFile && it.extension == "kt" }
                .flatMap { file -> declaration.findAll(file.readText()).map { it.groupValues[1] } }
                .distinct()
                .sorted()
                .toList()
        }
    }
}
