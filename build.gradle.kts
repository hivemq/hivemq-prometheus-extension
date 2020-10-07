plugins {
    id("com.hivemq.extension")
    id("org.asciidoctor.jvm.convert")
}

group = "com.hivemq.extensions"
description = "HiveMQ extension to utilize Prometheus for monitoring"

hivemqExtension {
    name = "Prometheus Monitoring Extension"
    author = "HiveMQ"
    priority = 1000
    startPriority = 0
    sdkVersion = "${property("hivemq-extension-sdk.version")}"
}

tasks.hivemqExtensionResources {
    from("LICENSE.txt")
    from("README.adoc") { rename { "README.txt" } }
    from(tasks.asciidoctor)
}

val prepareAsciidocTask = tasks.register<Sync>("prepareAsciidoc") {
    from("README.adoc").into(buildDir.resolve("tmp/asciidoc"))
}
tasks.asciidoctor {
    dependsOn(prepareAsciidocTask)
    sourceDir(prepareAsciidocTask.get().outputs.files.asPath)
}

dependencies {
    implementation("io.prometheus:simpleclient:${property("prometheus-simpleclient.version")}")
    implementation("io.prometheus:simpleclient_dropwizard:${property("prometheus-simpleclient.version")}")
    implementation("io.prometheus:simpleclient_servlet:${property("prometheus-simpleclient.version")}")
    implementation("org.eclipse.jetty:jetty-server:${property("jetty.version")}")
    implementation("org.eclipse.jetty:jetty-servlet:${property("jetty.version")}")
    implementation("org.eclipse.jetty:jetty-util:${property("jetty.version")}")
    implementation("org.aeonbits.owner:owner:${property("owner.version")}")

    testImplementation("junit:junit:${property("junit.version")}")
    testImplementation("org.mockito:mockito-all:${property("mockito.version")}")
}