plugins {
    id("com.hivemq.extension")
    id("org.asciidoctor.jvm.convert")
}

group = "com.hivemq"
description = "Prometheus Monitoring Extension"

hivemqExtension {
    name = "Prometheus Monitoring Extension"
    author = "HiveMQ"
    priority = 0
    startPriority = 0
    sdkVersion = "${property("hivemq-extension-sdk.version")}"
}

tasks.hivemqExtensionResources {
    from("LICENSE")
    from("README.adoc") { rename { "README.txt" } }
    from(tasks.asciidoctor)
    from("src/main/resources/prometheusConfiguration.properties")
}

val prepareAsciidocTask = tasks.register<Sync>("prepareAsciidoc") {
    from("README.adoc").into(buildDir.resolve("tmp/asciidoc"))
}
tasks.asciidoctor {
    dependsOn(prepareAsciidocTask)
    sourceDir(prepareAsciidocTask.get().outputs.files.asPath)
}

dependencies {
    implementation("io.prometheus:simpleclient_dropwizard:${property("simpleclient-dropwizard.version")}")
    implementation("io.prometheus:simpleclient_servlet:${property("simpleclient-servlet.version")}")
    implementation("io.prometheus:simpleclient_common:${property("simpleclient-common.version")}")
    implementation("org.eclipse.jetty:jetty-server:${property("jetty-server.version")}")
    implementation("org.eclipse.jetty:jetty-servlet:${property("jetty-servlet.version")}")
    implementation("org.aeonbits.owner:owner:${property("owner.version")}")

    testImplementation("junit:junit:${property("junit.version")}")
    testImplementation("org.mockito:mockito-all:${property("mockito.version")}")
    testImplementation("commons-io:commons-io:${property("commons-io.version")}")
}