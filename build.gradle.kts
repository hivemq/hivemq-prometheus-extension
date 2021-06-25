plugins {
    id("com.hivemq.extension")
    id("com.github.hierynomus.license")
    id("com.github.sgtsilvio.gradle.utf8")
    id("org.asciidoctor.jvm.convert")
}

group = "com.hivemq.extensions"
description = "HiveMQ extension to utilize Prometheus for monitoring"

hivemqExtension {
    name.set("Prometheus Monitoring Extension")
    author.set("HiveMQ")
    priority.set(1000)
    startPriority.set(1000)
    sdkVersion.set("${property("hivemq-extension-sdk.version")}")
}

dependencies {
    implementation("io.prometheus:simpleclient:${property("prometheus-simpleclient.version")}")
    implementation("io.prometheus:simpleclient_dropwizard:${property("prometheus-simpleclient.version")}")
    implementation("io.prometheus:simpleclient_servlet:${property("prometheus-simpleclient.version")}")
    implementation("org.eclipse.jetty:jetty-server:${property("jetty.version")}")
    implementation("org.eclipse.jetty:jetty-servlet:${property("jetty.version")}")
    implementation("org.eclipse.jetty:jetty-util:${property("jetty.version")}")
    implementation("org.aeonbits.owner:owner:${property("owner.version")}")
}

/* ******************** resources ******************** */

val prepareAsciidoc by tasks.registering(Sync::class) {
    from("README.adoc").into({ temporaryDir })
}

tasks.asciidoctor {
    dependsOn(prepareAsciidoc)
    sourceDir(prepareAsciidoc.map { it.destinationDir })
}

hivemqExtension.resources {
    from("LICENSE")
    from("README.adoc") { rename { "README.txt" } }
    from(tasks.asciidoctor)
}

/* ******************** test ******************** */

dependencies {
    testImplementation("junit:junit:${property("junit.version")}")
    testImplementation("org.mockito:mockito-all:${property("mockito.version")}")
}

/* ******************** checks ******************** */

license {
    header = rootDir.resolve("HEADER")
    mapping("java", "SLASHSTAR_STYLE")
}