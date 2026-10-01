import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Every skill, hook and document is wired in, so it is actually followed.
 *
 * An app built from this template shipped 1.0.0 by hand from main although the release rule existed:
 * the rule only loaded when docs or CI were edited, no skill described releasing, the release plan was
 * not among the documents CLAUDE.md says to read, and nothing blocked a store build. A skill, hook or
 * document that exists but is not listed in CLAUDE.md, or a hook that settings.json does not run, is
 * the same failure waiting to repeat. This makes adding one without wiring it a build error. (Rules
 * are covered by [RuleReferenceTest].)
 *
 * Where CLAUDE.md is installed by the KMP Arch plugin and replaced on update, a project lists its own
 * skills, hooks and documents in `.claude/project.md`, which counts as well.
 */
class AgentWiringTest {
    private val claude =
        File(ROOT, "CLAUDE.md").readText() +
            File(ROOT, ".claude/project.md").takeIf { it.isFile }?.readText().orEmpty()

    @Test
    fun `every skill is listed in CLAUDE and says when to use it`() {
        val skills = File(ROOT, ".claude/skills").listFiles { f -> File(f, "SKILL.md").isFile }.orEmpty()
        val unlisted = skills.map { ".claude/skills/${it.name}" }.filterNot { "`$it`" in claude }
        assertTrue(unlisted.isEmpty(), "Skills missing from CLAUDE.md's skill table (or .claude/project.md): $unlisted")
        val undescribed =
            skills.filterNot { skill ->
                val head = File(skill, "SKILL.md").readText().substringAfter("---").substringBefore("---")
                "name: ${skill.name}" in head && "description:" in head
            }
        assertTrue(
            undescribed.isEmpty(),
            "Skills without name/description front matter: ${undescribed.map { it.name }}",
        )
    }

    @Test
    fun `every hook runs and is listed in CLAUDE`() {
        val settings = File(ROOT, ".claude/settings.json").readText()
        val hooks = File(ROOT, ".claude/hooks").listFiles { f -> f.extension == "py" }.orEmpty().map { it.name }
        val notRun = hooks.filterNot { ".claude/hooks/$it" in settings }
        assertTrue(notRun.isEmpty(), "Hooks not registered in .claude/settings.json: $notRun")
        val unlisted = hooks.filterNot { "`$it`" in claude }
        assertTrue(unlisted.isEmpty(), "Hooks missing from CLAUDE.md's hook table (or .claude/project.md): $unlisted")
    }

    @Test
    fun `every document in docs is one CLAUDE tells agents to read`() {
        val unlisted =
            File(ROOT, "docs")
                .listFiles { f -> f.extension == "md" }
                .orEmpty()
                .map { "docs/${it.name}" }
                .filterNot { "($it)" in claude }
        assertTrue(
            unlisted.isEmpty(),
            "Documents missing from CLAUDE.md's document table (or .claude/project.md, as a link): $unlisted",
        )
    }

    @Test
    fun `store builds go through the release script`() {
        val script = File(ROOT, "tools/release/release.sh")
        assertTrue(script.isFile && script.canExecute(), "tools/release/release.sh must exist and be executable")
        val text = script.readText()
        listOf(
            "release/\$MAJOR.\$MINOR" to "it must run only from release/X.Y",
            "MAJOR * 1000000 + MINOR * 10000 + PATCH * 100 + 99" to "versionCode must derive from the tag",
            "--e2e-log" to "it must require a passing E2E run of the commit",
            "gh release create" to "it must publish the GitHub Release with the artifacts",
        ).forEach { (needle, why) -> assertTrue(needle in text, "release.sh: $why") }
        val settings = File(ROOT, ".claude/settings.json").readText()
        assertTrue("release-guard.py" in settings, "release-guard.py must block store builds outside the script")
        val plan = File(ROOT, "docs/RELEASE_PLAN.md").readText()
        assertTrue("tools/release/release.sh" in plan, "RELEASE_PLAN must describe building with the script")
        val cut = File(ROOT, ".github/workflows/release-cut.yml").readText()
        assertTrue("BRANCH=\"release/\$VERSION\"" in cut, "release-cut must create release/X.Y")
    }

    private companion object {
        val ROOT: File =
            generateSequence(File(".").absoluteFile) { it.parentFile }
                .first { File(it, "settings.gradle.kts").isFile }
    }
}
