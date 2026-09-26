import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertTrue
import kotlin.test.Test

/**
 * Enforces the dependency rules from .claude/rules/architecture.md.
 *
 * Konsist reads the repository's Kotlin files and asserts on their imports, so these rules hold for
 * every module without each one having to police itself.
 *
 * When one of these fails, the fix is almost never to change the rule: it is to move the code to
 * the layer it belongs in.
 */
class LayerDependencyTest {
    /** Every Kotlin file in the repository, ignoring generated output. */
    private val sources = Konsist.scopeFromProject()

    @Test
    fun `a feature never depends on another feature`() {
        sources.files
            .filter { it.path.contains("/feature/") }
            .assertTrue(testName = "a feature never imports another feature") { file ->
                val ownFeature = FEATURE_PATH.find(file.path)?.groupValues?.get(1)
                file.imports.none { import ->
                    import.name.startsWith(FEATURE_PACKAGE) &&
                        !import.name.startsWith("$FEATURE_PACKAGE.$ownFeature.")
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
                file.imports.none { import ->
                    import.name.startsWith("com.vidmira.kmptemplate.core.database.") ||
                        import.name.startsWith("com.vidmira.kmptemplate.core.network.") ||
                        import.name.startsWith("com.vidmira.kmptemplate.core.datastore.")
                }
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
            .assertTrue(testName = "expect declarations live in :core:platform, or a documented exception") { file ->
                file.path.contains("/core/platform/") ||
                    EXPECT_EXCEPTIONS.any { allowed -> file.path.endsWith(allowed) }
            }
    }

    private companion object {
        val FEATURE_PATH = Regex("/feature/([^/]+)/")
        const val FEATURE_PACKAGE = "com.vidmira.kmptemplate.feature"

        /**
         * The two places outside `:core:platform` that declare an `expect`.
         *
         * Room's builder signature differs per platform (Android's overload needs a `Context`), so
         * the seam has to sit next to the database; and Room's KSP compiler generates the `actual`s
         * for `@ConstructedBy`, so there is no hand-written code to move. Documented in
         * .claude/rules/architecture.md; any addition here needs the same written justification.
         */
        val EXPECT_EXCEPTIONS =
            setOf(
                "core/database/src/commonMain/kotlin/com/vidmira/kmptemplate/core/database/DatabaseFactory.kt",
                "core/database/src/commonMain/kotlin/com/vidmira/kmptemplate/core/database/TemplateDatabase.kt",
            )
    }
}
