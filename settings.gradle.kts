enableFeaturePreview("VERSION_CATALOGS")
rootProject.name = "Droidiqa"
include("droidiqa")

pluginManagement{
    repositories{
        google()
        mavenCentral()
        gradlePluginPortal()
    }

    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "android-gradle") {
                useModule("com.android.tools.build:gradle:$requested.version")
            }
            if (requested.id.id == "kotlin-gradle") {
                useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:$requested.version")
            }
            if(requested.id.id=="dokka"){
                useModule("org.jetbrains.dokka:dokka-gradle-plugin:$requested.version")
            }
        }
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }

}

includeBuild("build-logic")