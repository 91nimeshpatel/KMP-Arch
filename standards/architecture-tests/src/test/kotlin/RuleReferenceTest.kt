import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Every reference to a rule points at something that exists.
 *
 * Rules were renumbered once and left sixty-one files quoting sections that had moved. A citation
 * that resolves to the wrong rule is worse than none: it sends the reader somewhere confident and
 * wrong. Rule *files* are referenced by name so they survive reorganisation; the few remaining
 * section numbers are checked here so they cannot rot silently.
 */
class RuleReferenceTest {
    @Test
    fun `every referenced rule file exists`() {
        val referenced = scan(Regex("""\.claude/rules/[a-z-]+\.md"""))
        val missing = referenced.filterNot { File(ROOT, it).isFile }

        assertTrue(missing.isEmpty(), "Referenced rule files that do not exist: $missing")
    }

    @Test
    fun `every referenced CLAUDE section exists`() {
        val headings =
            Regex("""^## (\d+)\.""", RegexOption.MULTILINE)
                .findAll(File(ROOT, "CLAUDE.md").readText())
                .map { it.groupValues[1] }
                .toSet()

        val referenced = scan(Regex("""CLAUDE\.md §(\d+)""")).map { it.substringAfter("§") }
        val dangling = referenced.filterNot { it in headings }.distinct()

        assertTrue(
            dangling.isEmpty(),
            """
            These CLAUDE.md sections are cited but do not exist: ${dangling.map { "§$it" }}
            CLAUDE.md has sections: ${headings.sorted().map { "§$it" }}

            Prefer citing a rule file by name — `.claude/rules/architecture.md` — which stays valid
            when sections are reordered.
            """.trimIndent(),
        )
    }

    @Test
    fun `every rule file is reachable from CLAUDE`() {
        val index = File(ROOT, "CLAUDE.md").readText()
        val orphans =
            File(ROOT, ".claude/rules")
                .listFiles { f -> f.extension == "md" }
                .orEmpty()
                .map { ".claude/rules/${it.name}" }
                .filterNot { index.contains(it) }

        assertTrue(
            orphans.isEmpty(),
            """
            These rule files are not listed in CLAUDE.md: $orphans

            A path-scoped rule loads only when someone opens a file it covers, so an unlisted rule is
            invisible to anyone reading the contract top to bottom. List it in the table.
            """.trimIndent(),
        )
    }

    private companion object {
        val ROOT: File =
            generateSequence(File(".").absoluteFile) { it.parentFile }
                .first { File(it, "settings.gradle.kts").isFile }

        fun scan(pattern: Regex): List<String> =
            ROOT
                .walkTopDown()
                .onEnter { it.name !in setOf("build", ".gradle", ".git", ".kotlin", ".venv") }
                .filter { it.isFile && it.extension in setOf("kt", "kts", "md", "toml", "xml", "yml") }
                .flatMap { f -> pattern.findAll(f.readText()).map { it.value } }
                .distinct()
                .toList()
    }
}
