import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Enforces the localisation rules in .claude/rules/code-style.md.
 *
 * Every rule here was broken at least once while the template was being built, by someone who had
 * read the rule and meant to follow it. Prose is checked by intention and intention fails under
 * pressure; a failing build does not. Each test names the rule it enforces so the fix is obvious
 * from the failure alone.
 */
class LocalisationTest {
    @Test
    fun `every locale has every key`() {
        val problems =
            buildList {
                for (module in resourceModules()) {
                    val default = File(module, "values/strings.xml")
                    val expected = translatableKeys(default)
                    module
                        .listFiles { f ->
                            f.isDirectory && f.name.startsWith("values-")
                        }.orEmpty()
                        .forEach { locale ->
                            val actual = keys(File(locale, "strings.xml"))
                            val missing = expected - actual
                            val extra = actual - translatableKeys(default) - untranslatableKeys(default)
                            if (missing.isNotEmpty()) add("${module.parentFile.name}/${locale.name}: missing $missing")
                            if (extra.isNotEmpty()) add("${module.parentFile.name}/${locale.name}: unknown $extra")
                        }
                }
            }

        assertTrue(
            problems.isEmpty(),
            """
            Locales are out of step with the default:
            ${problems.joinToString("\n            ")}

            Every user-visible string is added to every locale in the same change (.claude/rules/code-style.md).
            A key missing from one locale is a crash on that device, not a fallback.
            """.trimIndent(),
        )
    }

    @Test
    fun `an untranslatable string is declared, not implied`() {
        val problems =
            buildList {
                for (module in resourceModules()) {
                    val untranslatable = untranslatableKeys(File(module, "values/strings.xml"))
                    module
                        .listFiles { f ->
                            f.isDirectory && f.name.startsWith("values-")
                        }.orEmpty()
                        .forEach { locale ->
                            val translated = keys(File(locale, "strings.xml")).intersect(untranslatable)
                            if (translated.isNotEmpty()) {
                                add("${module.parentFile.name}/${locale.name}: $translated")
                            }
                        }
                }
            }

        assertTrue(
            problems.isEmpty(),
            """
            These keys are marked translatable="false" but appear in a locale anyway:
            ${problems.joinToString("\n            ")}

            A string that must read the same in every language — a brand name, or a language's own
            name in the picker — lives only in values/ (.claude/rules/code-style.md).
            """.trimIndent(),
        )
    }

    @Test
    fun `no user-visible string is written in Kotlin`() {
        // Matches `Text("...")` and `UiText.Raw("...")` with a literal, which is how a hard-coded
        // string actually reaches a screen. Empty strings and single characters are not copy.
        val literal = Regex("""(Text|UiText\.Raw)\(\s*"[^"]{2,}"""")
        val offenders =
            uiSources()
                .filter { it.readText().contains(literal) }
                .map { it.relativeTo(ROOT).path }

        assertTrue(
            offenders.isEmpty(),
            """
            User-visible text is hard-coded in: $offenders

            It belongs in the owning module's composeResources/values/strings.xml, so a translator,
            a reviewer and a tool can all see it (.claude/rules/code-style.md). A literal in Kotlin is invisible to
            all three.
            """.trimIndent(),
        )
    }

    @Test
    fun `every module with resources is packaged into the Android app`() {
        val declared = File(ROOT, "androidApp/build.gradle.kts").readText()
        val missing =
            resourceModules()
                // …/<module>/src/commonMain/composeResources -> <module>
                .map { it.parentFile.parentFile.parentFile }
                .map { ":" + it.relativeTo(ROOT).path.replace(File.separatorChar, ':') }
                .filterNot { declared.contains("\"$it\"") }

        assertTrue(
            missing.isEmpty(),
            """
            These modules have composeResources but are absent from `composeResourceModules`
            in androidApp/build.gradle.kts: $missing

            Compose Multiplatform does not package a KMP library's resources under AGP 9, so that
            map is what copies them into the APK. A module missing from it crashes at runtime with
            MissingResourceException on the screen that uses it — not at build time.
            """.trimIndent(),
        )
    }

    private companion object {
        val ROOT: File =
            generateSequence(File(".").absoluteFile) { it.parentFile }
                .first { File(it, "settings.gradle.kts").isFile }

        /** Every `composeResources` directory in the repository. */
        fun resourceModules(): List<File> =
            ROOT
                .walkTopDown()
                // Not the copies in other checkouts: the desktop app keeps a session's git worktree in
                // .claude/worktrees, and its modules are not this build's.
                .onEnter { it.name !in setOf("build", ".git", "worktrees") }
                .filter { it.isDirectory && it.name == "composeResources" }
                .toList()

        fun uiSources(): List<File> =
            listOf("feature", "composeApp", "core")
                .map { File(ROOT, it) }
                .filter { it.isDirectory }
                .flatMap { dir ->
                    dir
                        .walkTopDown()
                        .onEnter { it.name != "build" }
                        .filter { it.isFile && it.extension == "kt" && !it.path.contains("Test") }
                        .toList()
                }

        fun entries(file: File): List<Pair<String, Boolean>> {
            if (!file.isFile) return emptyList()
            return Regex("""<string name="([^"]+)"([^>]*)>""")
                .findAll(file.readText())
                .map { it.groupValues[1] to it.groupValues[2].contains("translatable=\"false\"") }
                .toList()
        }

        fun keys(file: File): Set<String> = entries(file).map { it.first }.toSet()

        fun translatableKeys(file: File): Set<String> = entries(file).filterNot { it.second }.map { it.first }.toSet()

        fun untranslatableKeys(file: File): Set<String> = entries(file).filter { it.second }.map { it.first }.toSet()
    }
}
