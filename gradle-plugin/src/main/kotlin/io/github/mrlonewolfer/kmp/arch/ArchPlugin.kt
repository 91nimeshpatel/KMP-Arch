/*
 * Copyright (c) 2026 mrlonewolfer
 *
 * Licensed under the MIT License. See the LICENSE file in the project root for the full text.
 * https://github.com/mrlonewolfer/KMP-Arch
 */
package io.github.mrlonewolfer.kmp.arch

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
 * repository; `adoptArch` does that, and only after listing what it would touch.
 */
class ArchPlugin : Plugin<Settings> {
    override fun apply(settings: Settings) {
        settings.gradle.rootProject {
            tasks.register("adoptArch", AdoptArchTask::class.java) {
                group = "kmp arch"
                description = "Installs or updates the shared engineering standards in this project."
                targetDirectory.set(layout.projectDirectory)
            }
        }
    }
}
