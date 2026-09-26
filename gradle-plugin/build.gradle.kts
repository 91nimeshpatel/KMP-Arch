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
            implementationClass = "io.github.mrlonewolfer.kmp.arch.StandardsPlugin"
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
