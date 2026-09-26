package io.github.mrlonewolfer.kmp.standards

import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings

/**
 * Gives a project the shared engineering standards.
 *
 * Applied in `settings.gradle.kts` rather than in a module's build file, because the standards are
 * a property of the repository — its rules, its CI, its architecture tests — and not of any one
 * module. A settings plugin is also the only kind that runs before modules are configured, which is
 * where per-module conventions would go if they are added later.
 *
 * It registers one task and changes nothing else. Applying a plugin should not rewrite someone's
 * repository; `adoptStandards` does that, and only after listing what it would touch.
 */
class StandardsPlugin : Plugin<Settings> {
    override fun apply(settings: Settings) {
        settings.gradle.rootProject {
            tasks.register("adoptStandards", AdoptStandardsTask::class.java) {
                group = "kmp standards"
                description = "Installs or updates the shared engineering standards in this project."
            }
        }
    }
}
