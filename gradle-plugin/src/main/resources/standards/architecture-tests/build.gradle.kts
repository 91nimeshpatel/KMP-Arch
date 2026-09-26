// The architecture rules from .claude/rules/architecture.md, as tests.
//
// A rule nobody checks is a rule that erodes. These run on every pull request, so a forbidden
// dependency fails the build with the rule's name instead of being caught (or missed) in review.
//
// A plain JVM module: Konsist reads the Kotlin source of every other module as text, so it needs
// none of their dependencies and adds nothing to the app.
plugins {
    alias(libs.plugins.kotlinJvm)
}

// Java and Kotlin must agree on the bytecode level, and `jvmToolchain()` is banned here because
// Android Studio's Gradle client cannot provision toolchains on macOS (.claude/rules/dependencies.md).
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    testImplementation(libs.konsist)
    testImplementation(kotlin("test"))
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()

    // These tests read the repository as data — Kotlin sources, the version catalog, the Markdown
    // in docs/ — none of which Gradle can see as task inputs. Without this, editing only a document
    // or only a version leaves the task UP-TO-DATE and the rule is silently skipped, which is
    // exactly the moment it was written to catch. The whole suite runs in about two seconds, so
    // always re-running costs nothing worth having.
    outputs.upToDateWhen { false }
}
