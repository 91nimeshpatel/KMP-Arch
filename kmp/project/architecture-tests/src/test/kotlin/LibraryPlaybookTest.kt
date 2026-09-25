import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Enforces .claude/rules/dependencies.md: every dependency in the version catalog has an entry in
 * `docs/LIBRARIES.md`, and the playbook mentions no dependency that has been removed.
 *
 * A convention that is only written down decays: someone adds a library in a hurry, the entry is
 * "done next time", and six months later nobody remembers why a version is pinned. This turns the
 * convention into a failing build, which is the only kind of rule that survives.
 *
 * It deliberately checks *presence*, not content — no test can tell whether a precaution is true.
 * Reviewers do that; this only guarantees there is something for them to review.
 */
class LibraryPlaybookTest {
    @Test
    fun `every library family in the version catalog is documented`() {
        // Case-insensitive: a heading reads "### Konsist", the catalog alias is `konsist`.
        val playbook = playbookText().lowercase()
        val undocumented = catalogFamilies().filterNot { family -> playbook.contains(family) }

        assertTrue(
            undocumented.isEmpty(),
            """
            These dependencies have no entry in docs/LIBRARIES.md: $undocumented

            Adding a library adds its entry in the same commit (.claude/rules/dependencies.md): why it is here, how
            this project uses it, the precautions, and what to re-verify on upgrade. Mention the
            catalog alias or its family name somewhere in the entry.
            """.trimIndent(),
        )
    }

    @Test
    fun `the playbook documents no dependency the project has dropped`() {
        val catalog = catalogText().lowercase()
        val playbook = playbookText().lowercase()
        val stale = KNOWN_REMOVED.filter { removed -> playbook.contains(removed) }

        assertTrue(
            stale.isEmpty() || stale.any { catalog.contains(it) },
            """
            docs/LIBRARIES.md still describes dependencies that are no longer in the catalog: $stale

            Removing a dependency removes its entry, and any workaround the entry justified
            (.claude/rules/dependencies.md). A stale warning sends the next person the wrong way.
            """.trimIndent(),
        )
    }

    @Test
    fun `every documented version matches the catalog`() {
        val playbook = playbookText()
        val stale = catalogVersions().filterNot { (_, version) -> playbook.contains(version) }

        assertTrue(
            stale.isEmpty(),
            """
            These versions are in gradle/libs.versions.toml but nowhere in docs/LIBRARIES.md:
            ${stale.joinToString("\n            ") { (alias, version) -> "$alias = $version" }}

            Upgrading a dependency updates its entry in the same commit (.claude/rules/dependencies.md): the version,
            and every precaution and "On upgrade" note that the new release changes. Without this
            check a bump passes silently, the entry keeps quoting the old version, and the next
            person trusts a warning that no longer applies.
            """.trimIndent(),
        )
    }

    private companion object {
        /** Repository root, found by walking up from wherever Gradle started this module. */
        val ROOT: File =
            generateSequence(File(".").absoluteFile) { it.parentFile }
                .first { File(it, "settings.gradle.kts").isFile && File(it, "gradle/libs.versions.toml").isFile }

        /**
         * Names that must never reappear in the playbook without also being in the catalog.
         *
         * Libraries this project deliberately dropped. Listing them by hand is the point: it is a
         * short, reviewed list of decisions, not an attempt to diff history automatically.
         */
        val KNOWN_REMOVED = listOf("app.cash.sqldelight", "org.robolectric", "io.realm")

        fun catalogText(): String = File(ROOT, "gradle/libs.versions.toml").readText()

        fun playbookText(): String = File(ROOT, "docs/LIBRARIES.md").readText()

        /**
         * Every version the catalog actually uses, as a pair of alias and version number.
         *
         * Plugin versions count too. A plugin can break the build just as thoroughly as a library —
         * KSP's version has to match Kotlin's — so it belongs in the playbook like anything else.
         * Numbers nothing refers to, such as the SDK levels, never appear here.
         */
        fun catalogVersions(): List<Pair<String, String>> {
            val toml = catalogText()
            val declared =
                Regex("""^([A-Za-z0-9_.-]+)\s*=\s*"([^"]+)"""", RegexOption.MULTILINE)
                    .findAll(toml.substringAfter("[versions]").substringBefore("[libraries]"))
                    .associate { it.groupValues[1] to it.groupValues[2] }

            // Every `version.ref`, from `[libraries]` *and* `[plugins]`. A plugin is a dependency of
            // the build and breaks it just as thoroughly — KSP's version is pinned to Kotlin's, and
            // that is exactly the kind of fact the playbook exists to hold.
            val used =
                Regex("""version\.ref\s*=\s*"([^"]+)"""")
                    .findAll(toml.substringAfter("[libraries]"))
                    .map { it.groupValues[1] }
                    .toSet()

            return declared
                .filterKeys { it in used }
                .map { (alias, version) -> alias to version }
                .sortedBy { it.first }
        }

        /**
         * The distinct families in `[libraries]`, taken from the first word of each alias.
         *
         * Families rather than every single alias: `ktor-client-core` and `ktor-client-darwin` are
         * one decision and belong in one entry. Asking for a heading per artifact would produce
         * paperwork, not knowledge.
         */
        fun catalogFamilies(): List<String> {
            val libraries =
                catalogText()
                    .substringAfter("[libraries]")
                    .substringBefore("[bundles]")
                    .substringBefore("[plugins]")

            return libraries
                .lineSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") && it.contains("=") }
                .map { it.substringBefore("=").trim() }
                .map { alias -> alias.substringBefore("-").lowercase() }
                // Declared for the build itself, not used by the app; they are covered by the
                // toolchain entries rather than by one of their own.
                .filterNot { it == "plugin" }
                .distinct()
                .sorted()
                .toList()
        }
    }
}
