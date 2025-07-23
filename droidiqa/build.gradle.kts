plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.dokka)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.kotlinx.serialization)
    id("maven-publish")
}

val droidiqaVersion = "0.8.2"

val gitHubCredentials = rootProject.file("github.properties")
if (gitHubCredentials.exists()) {
    apply(from = gitHubCredentials)
} else {
    ext["githubUser"] = "unknown"
    ext["githubKey"] = "unknown"
}

android {
    namespace = "ch.newturicum.droidiqa"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        jvmToolchain(21)
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
        singleVariant("debug") {
            withSourcesJar()
            withJavadocJar()
        }
    }

    testOptions {
        targetSdk = 35
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

// Configure Dokka V2 using tasks
tasks.withType<org.jetbrains.dokka.gradle.DokkaTask>().configureEach {
    dokkaSourceSets {
        named("main") {
            // Include your Module.md
            includes.from("Module.md")

            // Configure links - use new property names
            externalDocumentationLink {
                url.set(uri("https://developer.android.com/reference/").toURL())
            }

            // Show only public and protected members
            documentedVisibilities.set(
                setOf(
                    org.jetbrains.dokka.DokkaConfiguration.Visibility.PUBLIC,
                    org.jetbrains.dokka.DokkaConfiguration.Visibility.PROTECTED
                )
            )

            skipEmptyPackages.set(true)
            reportUndocumented.set(false)
        }
    }
}

// Configure specific Dokka tasks
tasks.named<org.jetbrains.dokka.gradle.DokkaTask>("dokkaHtml").configure {
    outputDirectory.set(layout.buildDirectory.dir("dokka/html"))
}

tasks.named<org.jetbrains.dokka.gradle.DokkaTask>("dokkaJavadoc").configure {
    outputDirectory.set(layout.buildDirectory.dir("dokka/javadoc"))
}

// Override the Android-generated javadoc jar to use Dokka output
tasks.whenTaskAdded {
    if (name.contains("javadocJar", ignoreCase = true)) {
        val javadocJar = this as Jar
        javadocJar.apply {
            dependsOn("dokkaJavadoc") // Use dokkaJavadoc for javadoc jar
            from(layout.buildDirectory.dir("dokka/javadoc"))
            archiveClassifier.set("javadoc")
        }
    }
}

afterEvaluate {
    publishing {
        publications {
            register<MavenPublication>("release") {
                groupId = "ch.newturicum.libraries"
                artifactId = "droidiqa"
                version = droidiqaVersion

                from(components["release"])

                pom {
                    name.set("Droidiqa")
                    description.set("Android SDK for Zilliqa blockchain interaction")
                    url.set("https://github.com/NewTuricum/Droidiqa")

                    licenses {
                        license {
                            name.set("MIT License")
                            url.set("https://opensource.org/licenses/MIT")
                        }
                    }

                    developers {
                        developer {
                            id.set("newturicum")
                            name.set("NewTuricum")
                        }
                    }

                    scm {
                        connection.set("scm:git:git://github.com/NewTuricum/Droidiqa.git")
                        developerConnection.set("scm:git:ssh://github.com/NewTuricum/Droidiqa.git")
                        url.set("https://github.com/NewTuricum/Droidiqa")
                    }
                }
            }

            register<MavenPublication>("debug") {
                groupId = "ch.newturicum.libraries"
                artifactId = "droidiqa"
                version = "$droidiqaVersion-debug"

                from(components["debug"])
            }
        }

        repositories {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/NewTuricum/Droidiqa")
                credentials {
                    username = project.findProperty("githubUser") as String? ?: "unknown"
                    password = project.findProperty("githubKey") as String? ?: "unknown"
                }
            }
        }
    }
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.androidx.core)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.lifecycle)
    implementation(libs.androidx.lifecycle.livedata)

    ksp(libs.androidx.room.compiler)
    implementation(libs.volley)
    implementation(libs.laksaj)

    // Kotlin serialization
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext)
    testImplementation(kotlin("test"))
}