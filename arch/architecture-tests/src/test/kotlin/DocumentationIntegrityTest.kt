import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The claims a document makes about the build must be true, and its links must go somewhere.
 *
 * These are the statements that rot silently. A version quoted in prose does not move when the
 * catalog moves; a link keeps pointing at a file that was renamed. Neither breaks a build, so
 * neither gets noticed — until someone acts on a version that has not been true for six months.
 */
class DocumentationIntegrityTest {
    @Test
    fun `every toolchain version quoted in prose matches the build`() {
        val actual =
            buildMap {
                Regex("""^(agp|kotlin|ksp)\s*=\s*"([^"]+)"""", RegexOption.MULTILINE)
                    .findAll(File(ROOT, "gradle/libs.versions.toml").readText())
                    .forEach { put(it.groupValues[1], it.groupValues[2]) }
                Regex("""gradle-([0-9.]+)-bin""")
                    .find(File(ROOT, "gradle/wrapper/gradle-wrapper.properties").readText())
                    ?.let { put("gradle", it.groupValues[1]) }
            }
        val label = mapOf("agp" to "AGP", "kotlin" to "Kotlin", "ksp" to "KSP", "gradle" to "Gradle")

        val wrong =
            buildList {
                for (doc in DOCS) {
                    val text = doc.readText()
                    for ((key, version) in actual) {
                        // Any "Kotlin 1.2.3" in prose must be the version the build actually uses.
                        // Digits and dots only, so a sentence-ending period is not read as part of
                        // the version — "AGP 9." is prose about the major line, not a claim about 9.0.
                        Regex("""\b${label[key]} (\d+(?:\.\d+)+)""")
                            .findAll(text)
                            .map { it.groupValues[1] }
                            .filter { it != version }
                            .forEach { add("${doc.relativeTo(ROOT)}: says ${label[key]} $it, build uses $version") }
                    }
                }
            }

        assertTrue(
            wrong.isEmpty(),
            """
            Documents quote toolchain versions the build does not use:
            ${wrong.joinToString("\n            ")}

            A version in prose is a claim. Update it with the build, or write the major line only
            ("AGP 9") where the exact number does not matter.
            """.trimIndent(),
        )
    }

    @Test
    fun `every relative link resolves`() {
        val link = Regex("""\[[^]]*]\((?!https?:|#)([^)\s]+)\)""")
        val broken =
            buildList {
                for (doc in DOCS) {
                    link
                        .findAll(doc.readText())
                        .map { it.groupValues[1].substringBefore('#') }
                        .filter { it.isNotBlank() }
                        .filterNot { File(doc.parentFile, it).exists() }
                        .forEach { add("${doc.relativeTo(ROOT)} -> $it") }
                }
            }

        assertTrue(
            broken.isEmpty(),
            """
            These links point at files that do not exist:
            ${broken.joinToString("\n            ")}
            """.trimIndent(),
        )
    }

    @Test
    fun `every recorded decision is about something still in use`() {
        val decisions =
            File(ROOT, "docs/ARCHITECTURE.md")
                .readText()
                .substringAfter("## 9. Decisions, recorded")
                .lines()
                .filter { it.startsWith("| ") && !it.startsWith("| Decision") && !it.startsWith("|---") }
                .map {
                    it
                        .removePrefix("| ")
                        .substringBefore(" |")
                        .trim()
                        .trim('`')
                }

        // A decision naming a library must name one the project actually has. A row left behind
        // after the library was swapped out is worse than no row: it describes a different app.
        val catalog = File(ROOT, "gradle/libs.versions.toml").readText().lowercase()
        val named = decisions.filter { it.first().isUpperCase() && !it.contains(' ') }
        val stale = named.filterNot { catalog.contains(it.lowercase()) }

        assertTrue(
            stale.isEmpty(),
            """
            These decisions name a library that is no longer in the version catalog: $stale

            When a decision is reversed, rewrite the row — do not leave it. It is the first thing
            someone reads when asking why the project is built this way.
            """.trimIndent(),
        )
    }

    private companion object {
        val ROOT: File =
            generateSequence(File(".").absoluteFile) { it.parentFile }
                .first { File(it, "settings.gradle.kts").isFile }

        val DOCS: List<File> =
            (
                listOf(File(ROOT, "CLAUDE.md"), File(ROOT, "README.md"), File(ROOT, "REVIEW.md")) +
                    File(ROOT, "docs").walkTopDown().filter { it.extension == "md" } +
                    File(ROOT, ".claude").walkTopDown().filter { it.extension == "md" }
            ).filter { it.isFile }
    }
}
