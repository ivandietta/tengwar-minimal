import com.android.build.api.artifact.SingleArtifact
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.tengwarminimal"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.tengwarminimal"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

// The app must never have network access (brief, principle 1). These checks read each
// variant's merged manifest, so they also catch a permission that a library adds.
val checkNoInternetPermission = tasks.register("checkNoInternetPermission") {
    group = "verification"
    description = "Fails if the merged manifest of any variant requests android.permission.INTERNET."
}

tasks.named("check") {
    dependsOn(checkNoInternetPermission)
}

androidComponents {
    onVariants { variant ->
        val variantCheck = tasks.register<NoInternetPermissionCheck>(
            "check${variant.name.replaceFirstChar { it.uppercase() }}NoInternetPermission",
        ) {
            mergedManifest.set(variant.artifacts.get(SingleArtifact.MERGED_MANIFEST))
        }
        checkNoInternetPermission.configure { dependsOn(variantCheck) }
    }
}

abstract class NoInternetPermissionCheck : DefaultTask() {
    @get:InputFile
    abstract val mergedManifest: RegularFileProperty

    @TaskAction
    fun check() {
        val manifest = mergedManifest.get().asFile
        val document = DocumentBuilderFactory.newInstance()
            .apply { isNamespaceAware = true }
            .newDocumentBuilder()
            .parse(manifest)

        val offendingTags = PERMISSION_REQUEST_TAGS.filter { tag ->
            val elements = document.getElementsByTagName(tag)
            (0 until elements.length).any { index ->
                val element = elements.item(index) as Element
                element.getAttributeNS(ANDROID_NAMESPACE, "name") == INTERNET_PERMISSION
            }
        }

        if (offendingTags.isNotEmpty()) {
            throw GradleException(
                "The merged manifest $manifest requests $INTERNET_PERMISSION " +
                    "in <${offendingTags.joinToString("> and <")}>. Tengwar Minimal must not " +
                    "have network access. The manifest merger report in " +
                    "app/build/outputs/logs/ shows which manifest added it.",
            )
        }
    }

    private companion object {
        const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
        const val INTERNET_PERMISSION = "android.permission.INTERNET"
        val PERMISSION_REQUEST_TAGS = listOf("uses-permission", "uses-permission-sdk-23")
    }
}