rootProject.name = "hivemq-prometheus-extension"

pluginManagement {
    plugins {
        id("com.hivemq.extension") version "${extra["plugin.hivemq-extension.version"]}"
        id("com.github.hierynomus.license") version "${extra["plugin.license.version"]}"
        id("org.asciidoctor.jvm.convert") version "${extra["plugin.asciidoctor.version"]}"
    }
}