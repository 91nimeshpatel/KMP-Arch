plugins {
    `kotlin-dsl`
    id("com.gradle.plugin-publish") version "2.2.1"
}

group = "io.github.mrlonewolfer"

// `./gradlew publishToMavenLocal` puts it in ~/.m2, which is how it gets tried against a real
// project before it is published anywhere anyone else can reach.
publishing {
    repositories {
        mavenLocal()
    }
}

// One version for the whole repository: the npm package and this plugin are released together, so
// a consumer never has to work out which pair of versions belong to each other.
version = providers.fileContents(
    rootProject.layout.projectDirectory.file("../VERSION"),
).asText.get().trim()

kotlin {
    jvmToolchain(17)
}

gradlePlugin {
    website = "https://github.com/mrlonewolfer/KMP-Arch"
    vcsUrl = "https://github.com/mrlonewolfer/KMP-Arch.git"

    plugins {
        create("standards") {
            id = "io.github.mrlonewolfer.kmp.arch"
            implementationClass = "io.github.mrlonewolfer.kmp.arch.ArchPlugin"
            displayName = "KMP engineering standards"
            description =
                "Installs a Kotlin Multiplatform project's shared engineering standards: the " +
                "architecture rules that run as tests, the CI and release workflows, CLAUDE.md " +
                "and the path-scoped rules an agent reads, and the tooling that scaffolds a " +
                "feature module. Apply it in settings.gradle.kts and run `./gradlew " +
                "adoptArch`; it reports every file it would write before writing anything."
            tags = listOf("kotlin-multiplatform", "kmp", "android", "conventions", "architecture")
        }
    }
}

// Ownership and licence, written into the published artifact rather than only into a file in the
// repository. A jar on a public registry travels without its repository: whoever ends up holding it
// should be able to read who wrote it and on what terms, from the artifact itself.
afterEvaluate {
    publishing.publications.withType<MavenPublication>().configureEach {
        pom {
            name = "KMP Arch"
            description =
                "Engineering standards for Kotlin Multiplatform projects, installed by one Gradle " +
                "task: architecture rules that run as tests, CI and release workflows, agent rules " +
                "and skills, and feature-module scaffolding."
            url = "https://github.com/mrlonewolfer/KMP-Arch"
            inceptionYear = "2026"

            licenses {
                license {
                    name = "MIT License"
                    url = "https://github.com/mrlonewolfer/KMP-Arch/blob/main/LICENSE"
                    distribution = "repo"
                }
            }

            developers {
                developer {
                    id = "mrlonewolfer"
                    name = "mrlonewolfer"
                    url = "https://github.com/mrlonewolfer"
                }
            }

            scm {
                url = "https://github.com/mrlonewolfer/KMP-Arch"
                connection = "scm:git:https://github.com/mrlonewolfer/KMP-Arch.git"
                developerConnection = "scm:git:ssh://git@github.com/mrlonewolfer/KMP-Arch.git"
            }
        }
    }
}

// Declared because it is true and was checked, not because the Portal asks: the task reaches for
// the project directory when it is created rather than while it runs, and two identical runs
// report "Configuration cache entry reused".
tasks.withType<com.gradle.publish.PublishTask>().configureEach {
    notCompatibleWithConfigurationCache("The publish task talks to the Gradle Plugin Portal.")
}
