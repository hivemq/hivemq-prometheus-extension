plugins {
    alias(libs.plugins.hivemq.extension)
    alias(libs.plugins.defaults)
    alias(libs.plugins.license)
}

group = "com.hivemq.extensions"
description = "HiveMQ extension to utilize Prometheus for monitoring"

hivemqExtension {
    name.set("Prometheus Monitoring Extension")
    author.set("HiveMQ")
    priority.set(1000)
    startPriority.set(1000)
    sdkVersion.set(libs.versions.hivemq.extensionSdk)

    resources {
        from("LICENSE")
    }
}

dependencies {
    implementation(libs.prometheus.simpleClient)
    implementation(libs.prometheus.simpleClient.dropwizard)
    implementation(libs.prometheus.simpleClient.servlet)
    implementation(libs.jetty.server)
    implementation(libs.jetty.servlet)
    implementation(libs.jetty.util)
    implementation(libs.owner)
}

@Suppress("UnstableApiUsage")
testing {
    suites {
        withType<JvmTestSuite> {
            useJUnitJupiter(libs.versions.junit.jupiter)
        }
        "test"(JvmTestSuite::class) {
            dependencies {
                implementation(libs.mockito)
            }
        }
        "integrationTest"(JvmTestSuite::class) {
            dependencies {
                compileOnly(libs.jetbrains.annotations)
                implementation(libs.assertj)
                implementation(libs.testcontainers.junitJupiter)
                implementation(libs.testcontainers.hivemq)
                implementation(libs.hivemq.extensionSdk)
                implementation(libs.okhttp)
                runtimeOnly(libs.logback.classic)
            }
        }
    }
}

license {
    header = rootDir.resolve("HEADER")
    mapping("java", "SLASHSTAR_STYLE")
}
