import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertTrue
import kotlin.test.Test

/**
 * Enforces the dependency rules from .claude/rules/architecture.md.
 *
 * Konsist reads the repository's Kotlin files and asserts on their imports, so these rules hold for
 * every module without each one having to police itself.
 *
 * Every rule here keys off the module layout — `/feature/<name>/`, `/core/<name>/` — and never off
 * a package name, because this file ships to other people's projects. A rule written against one
 * project's package matches nothing in anyone else's, and `imports.none { … }` over an empty set
 * is true: the test would pass while checking nothing, which is the failure it exists to prevent.
 *
 * When one of these fails, the fix is almost never to change the rule: it is to move the code to
 * the layer it belongs in.
 */
class LayerDependencyTest {
    /** Every Kotlin file in the repository, ignoring generated output. */
    private val sources = Konsist.scopeFromProject()

    /**
     * The feature names this project actually has, read from its directories.
     *
     * An import is only judged against these, so a third-party package that happens to contain
     * `.feature.` cannot be mistaken for one of ours.
     */
    private val featureNames: Set<String> =
        sources.files.mapNotNull { FEATURE_PATH.find(it.path)?.groupValues?.get(1) }.toSet()

    @Test
    fun `a feature never depends on another feature`() {
        sources.files
            .filter { it.path.contains("/feature/") }
            .assertTrue(testName = "a feature never imports another feature") { file ->
                val ownFeature = FEATURE_PATH.find(file.path)?.groupValues?.get(1)
                file.imports.none { import ->
                    val imported = IMPORTED_FEATURE.find(import.name)?.groupValues?.get(1)
                    imported != null && imported in featureNames && imported != ownFeature
                }
            }
    }

    @Test
    fun `the domain layer stays pure Kotlin`() {
        sources.files
            .filter { it.path.contains("/core/domain/") }
            .assertTrue(testName = "no Compose, Android or platform imports in :core:domain") { file ->
                file.imports.none { import ->
                    import.name.startsWith("androidx.compose.") ||
                        import.name.startsWith("android.") ||
                        import.name.startsWith("platform.")
                }
            }
    }

    @Test
    fun `nothing above the data layer reaches a data source`() {
        sources.files
            .filter { it.path.contains("/feature/") || it.path.contains("/core/domain/") }
            .assertTrue(testName = "features and use cases go through a repository") { file ->
                file.imports.none { import -> IMPORTED_DATA_SOURCE.containsMatchIn(import.name) }
            }
    }

    @Test
    fun `a ViewModel never holds an Android type`() {
        sources.files
            .filter { it.name.endsWith("ViewModel") }
            .assertTrue(testName = "no Context, Activity or Resources in a ViewModel") { file ->
                file.imports.none { import ->
                    import.name.startsWith("android.content.Context") ||
                        import.name.startsWith("android.app.") ||
                        import.name.startsWith("androidx.compose.")
                }
            }
    }

    @Test
    fun `platform code stays in core platform`() {
        sources.files
            .filter { it.text.contains("\nexpect ") || it.text.contains("\ninternal expect ") }
            .assertTrue(testName = "expect declarations live in :core:platform, or :core:database for Room") { file ->
                file.path.contains("/core/platform/") || file.path.contains("/core/database/")
            }
    }

    private companion object {
        /** `/feature/profile/` -> `profile`, from a file's path. */
        val FEATURE_PATH = Regex("/feature/([^/]+)/")

        /** `com.anything.feature.profile.ProfileScreen` -> `profile`, from an import. */
        val IMPORTED_FEATURE = Regex("""\.feature\.([^.]+)\.""")

        /**
         * An import that reaches straight into a data source, whatever the root package is.
         *
         * The module names are the ones the standards define, so matching them is matching the
         * standard rather than any one project.
         */
        val IMPORTED_DATA_SOURCE = Regex("""\.core\.(database|network|datastore)\.""")
    }
}
