// A build of its own, not part of any app. It produces the plugin that apps then apply.
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
    }
}

rootProject.name = "kmp-arch-gradle-plugin"
