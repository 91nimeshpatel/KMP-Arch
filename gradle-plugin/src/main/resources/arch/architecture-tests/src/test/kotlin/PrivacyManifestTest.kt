import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The iOS app ships a privacy manifest that App Store Connect accepts.
 *
 * Apple requires one in every app and rejects an upload whose manifest is missing a required-reason
 * API (ITMS-91053) or declares tracking without a tracking domain (ITMS-91064, which rejected a build
 * of an app made from this template). A manifest that exists on disk but is not in the target's
 * Resources phase is not in the app at all.
 */
class PrivacyManifestTest {
    @Test
    fun `the privacy manifest is complete and is in the app`() {
        val manifest = File(ROOT, "iosApp/iosApp/PrivacyInfo.xcprivacy").readText()
        listOf(
            "NSPrivacyTracking",
            "NSPrivacyCollectedDataTypes",
            "NSPrivacyAccessedAPITypes",
            "NSPrivacyAccessedAPICategoryUserDefaults",
        ).forEach { key -> assertTrue(key in manifest, "PrivacyInfo.xcprivacy must declare $key") }

        val tracking = Regex("<key>NSPrivacyTracking</key>\\s*<true/>").containsMatchIn(manifest)
        val domains =
            Regex("<key>NSPrivacyTrackingDomains</key>\\s*<array>\\s*<string>").containsMatchIn(manifest)
        assertTrue(!tracking || domains, "NSPrivacyTracking true needs at least one NSPrivacyTrackingDomains entry")

        val project = File(ROOT, "iosApp/iosApp.xcodeproj/project.pbxproj").readText()
        assertTrue(
            "PrivacyInfo.xcprivacy in Resources */," in project,
            "PrivacyInfo.xcprivacy must be in the app target's Resources build phase",
        )
    }

    private companion object {
        val ROOT: File =
            generateSequence(File(".").absoluteFile) { it.parentFile }
                .first { File(it, "settings.gradle.kts").isFile }
    }
}
