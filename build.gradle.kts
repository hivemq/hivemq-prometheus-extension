plugins {
    alias(libs.plugins.hivemq.extension)
    alias(libs.plugins.defaults)
    alias(libs.plugins.oci)
    alias(libs.plugins.license)
}

group = "com.hivemq.extensions"
description = "HiveMQ extension to utilize Prometheus for monitoring"

hivemqExtension {
    name = "Prometheus Monitoring Extension"
    author = "HiveMQ"
    priority = 1000
    startPriority = 1000
    sdkVersion = libs.versions.hivemq.extensionSdk

    resources {
        from("LICENSE")
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(11)
    }
}

dependencies {
    compileOnly(libs.jetbrains.annotations)
    implementation(libs.prometheus.simpleClient)
    implementation(libs.prometheus.simpleClient.dropwizard)
    implementation(libs.prometheus.simpleClient.servlet)
    implementation(libs.jetty.server)
    implementation(libs.jetty.servlet)
    implementation(libs.jetty.util)
    implementation(libs.owner)
}

oci {
    registries {
        dockerHub {
            optionalCredentials()
        }
    }
    imageMapping {
        mapModule("com.hivemq", "hivemq-enterprise") {
            toImage("hivemq/hivemq4")
        }
    }
    imageDefinitions.register("main") {
        allPlatforms {
            dependencies {
                runtime("com.hivemq:hivemq-enterprise:latest") { isChanging = true }
            }
            config {
                ports = setOf("9399")
            }
            layer("main") {
                contents {
                    permissions("opt/hivemq/", 0b111_111_101)
                    permissions("opt/hivemq/extensions/", 0b111_111_101)
                    into("opt/hivemq/extensions") {
                        permissions("*/", 0b111_111_101)
                        permissions("*/hivemq-extension.xml", 0b110_110_100)
                        permissions("*/prometheusConfiguration.properties", 0b110_110_100)
                        from(zipTree(tasks.hivemqExtensionZip.flatMap { it.archiveFile }))
                    }
                }
            }
        }
    }
}

@Suppress("UnstableApiUsage")
testing {
    suites {
        withType<JvmTestSuite> {
            useJUnitJupiter(libs.versions.junit.jupiter)
        }
        "test"(JvmTestSuite::class) {
            dependencies {
                compileOnly(libs.jetbrains.annotations)
                implementation(libs.assertj)
                implementation(libs.mockito)
            }
        }
        "integrationTest"(JvmTestSuite::class) {
            dependencies {
                compileOnly(libs.jetbrains.annotations)
                implementation(libs.assertj)
                implementation(libs.testcontainers)
                implementation(libs.testcontainers.hivemq)
                implementation(libs.testcontainers.junitJupiter)
                implementation(libs.gradleOci.junitJupiter)
                implementation(libs.hivemq.extensionSdk)
                runtimeOnly(libs.logback.classic)
            }
            oci.of(this) {
                imageDependencies {
                    runtime(project).tag("latest")
                }
            }
        }
    }
}

license {
    header = rootDir.resolve("HEADER")
    mapping("java", "SLASHSTAR_STYLE")
}

// configure reproducible builds
tasks.withType<AbstractArchiveTask>().configureEach {
    // normalize file permissions for reproducibility
    // files: 0644 (rw-r--r--), directories: 0755 (rwxr-xr-x)
    filePermissions {
        unix("0644")
    }
    dirPermissions {
        unix("0755")
    }
}

tasks.withType<JavaCompile>().configureEach {
    // ensure consistent compilation across different JDK versions
    options.compilerArgs.addAll(listOf(
        // include parameter names for reflection (improves consistency)
        "-parameters"
    ))
}
