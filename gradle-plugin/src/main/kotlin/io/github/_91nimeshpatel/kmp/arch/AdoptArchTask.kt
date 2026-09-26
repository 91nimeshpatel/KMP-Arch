/*
 * Copyright (c) 2026 Nimesh Patel
 *
 * Licensed under the MIT License. See the LICENSE file in the project root for the full text.
 * https://github.com/91nimeshpatel/KMP-Arch
 */
package io.github._91nimeshpatel.kmp.arch

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.work.DisableCachingByDefault
import java.io.File
import java.util.zip.ZipInputStream

/**
 * Writes the bundled standards into the project.
 *
 * ### What it will and will not touch
 * The files listed in `standards/MANIFEST` belong to the standards and are replaced on every run —
 * a project that edits one loses the edit, which is why the exceptions belong in
 * `.claude/project.md` instead. Everything else in the repository is left alone, and
 * `.claude/project.md` itself is written only when it does not already exist.
 *
 * ### Why it stops first
 * By default it reports and writes nothing. A build task that silently rewrites files someone has
 * open is a bad surprise, and the difference between a useful default and an unwelcome one is
 * whether they were asked. `--apply` is the answer.
 */
@DisableCachingByDefault(
    because = "It writes into the project rather than into the build directory, so there is " +
        "no output Gradle could cache and replaying a cached run would write nothing.",
)
abstract class AdoptArchTask : DefaultTask() {

    /**
     * Where the files go — the project's root, set when the task is created.
     *
     * Resolved at configuration time rather than read from `project` while the task runs, because
     * reaching for the project during execution is what the configuration cache forbids.
     */
    @get:Internal
    abstract val targetDirectory: DirectoryProperty

    /** Without this the task only reports. */
    @get:Input
    @set:Option(option = "apply", description = "Write the files instead of only listing them.")
    var apply: Boolean = false

    /** Overwrite a file the project has edited. Off by default: an edit may have been deliberate. */
    @get:Input
    @set:Option(option = "force", description = "Replace files this project has modified.")
    var force: Boolean = false

    @TaskAction
    fun run() {
        val target = targetDirectory.get().asFile
        val bundled = readBundle()
        check(bundled.isNotEmpty()) { "The plugin carries no standards. This is a packaging bug." }

        val ignored = ignoredPatterns(target)
        val create = mutableListOf<String>()
        val update = mutableListOf<String>()
        val edited = mutableListOf<String>()
        val same = mutableListOf<String>()
        val skipped = mutableListOf<String>()

        for ((path, bytes) in bundled) {
            // The bundle's own index. It is how the packaging is checked, not something a project
            // has any use for.
            if (path == "MANIFEST") continue
            if (ignored.any { it.matches(path) }) {
                skipped += path
                continue
            }
            val existing = File(target, path)
            when {
                !existing.isFile -> create += path
                existing.readBytes().contentEquals(bytes) -> same += path
                // The project owns this one; it is written once and never again.
                path == PROJECT_OWNED -> same += path
                else -> if (force) update += path else edited += path
            }
        }

        report(create, update, edited, same, skipped)
        if (!apply) {
            logger.lifecycle("\nNothing was written. Re-run with --apply.")
            return
        }

        for (path in create + update) {
            val file = File(target, path)
            file.parentFile.mkdirs()
            file.writeBytes(bundled.getValue(path))
            if (path.endsWith(".sh") || path.endsWith(".py")) file.setExecutable(true)
        }
        logger.lifecycle("\nWrote ${create.size + update.size} files.")
        if (edited.isNotEmpty()) {
            logger.lifecycle(
                "Left ${edited.size} alone because this project has changed them. Record why in " +
                    ".claude/project.md, or re-run with --force to take the shared version.",
            )
        }
        logger.lifecycle(NEXT_STEPS)
    }

    private fun report(
        create: List<String>,
        update: List<String>,
        edited: List<String>,
        same: List<String>,
        skipped: List<String>,
    ) {
        fun list(title: String, paths: List<String>, mark: String) {
            if (paths.isEmpty()) return
            logger.lifecycle("\n$title (${paths.size})")
            paths.sorted().forEach { logger.lifecycle("  $mark $it") }
        }
        list("Would create", create, "+")
        list("Would replace", update, "!")
        list("Changed by this project, left alone", edited, "~")
        list("Skipped by .arch-ignore", skipped, "-")
        if (same.isNotEmpty()) logger.lifecycle("\nAlready up to date: ${same.size}")
    }

    /** Globs from `.arch-ignore`, one per line, `#` for comments. */
    private fun ignoredPatterns(target: File): List<Regex> {
        val file = File(target, ".arch-ignore")
        if (!file.isFile) return emptyList()
        return file.readLines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .map { glob ->
                Regex(
                    "^" + Regex.escape(glob)
                        .replace("\\*\\*", "\u0000")
                        .replace("\\*", "[^/]*")
                        .replace("\u0000", ".*") + "$",
                )
            }
    }

    /**
     * The standards, read out of this plugin's own jar.
     *
     * Read from the jar rather than from a directory beside it: once published, the plugin *is* the
     * jar, and anything not inside it does not reach the consumer.
     */
    private fun readBundle(): Map<String, ByteArray> {
        val source = javaClass.protectionDomain.codeSource?.location ?: return emptyMap()
        val jar = File(source.toURI())
        if (jar.isDirectory) {
            val root = File(jar, BUNDLE)
            if (!root.isDirectory) return emptyMap()
            return root.walkTopDown().filter { it.isFile }
                .associate { it.relativeTo(root).path to it.readBytes() }
        }
        val out = mutableMapOf<String, ByteArray>()
        ZipInputStream(jar.inputStream()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.isDirectory || !entry.name.startsWith("$BUNDLE/")) continue
                out[entry.name.removePrefix("$BUNDLE/")] = zip.readBytes()
            }
        }
        return out
    }

    private companion object {
        const val BUNDLE = "arch"
        const val PROJECT_OWNED = ".claude/project.md"
        val NEXT_STEPS = """
            Next:
              - .github/CODEOWNERS ships with a placeholder handle; reviews go nowhere until it is
                changed.
              - settings.gradle.kts must include ":architecture-tests", and qualityCheck must depend
                on ":architecture-tests:test", or the rules are installed but never run.
              - Anything specific to this project belongs in .claude/project.md, which this task
                writes once and never touches again.
              - ./gradlew qualityCheck
        """.trimIndent()
    }
}
