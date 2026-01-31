/*
 * Copyright 2018-present HiveMQ GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hivemq.extensions.prometheus.configuration;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigResolverTest {

    private static final @NotNull String EXTENSION_NAME = "Test Extension";
    private static final @NotNull String CONFIG_LOCATION = "conf/prometheusConfiguration.properties";
    private static final @NotNull String LEGACY_CONFIG_LOCATION = "prometheusConfiguration.properties";

    @TempDir
    private @NotNull Path tempDir;

    @Test
    void get_whenNoConfigExists_thenConfLocation() {
        final var resolver = new ConfigResolver(tempDir, EXTENSION_NAME, CONFIG_LOCATION, LEGACY_CONFIG_LOCATION);

        final var result = resolver.get();
        assertThat(result).isEqualTo(tempDir.resolve(CONFIG_LOCATION));
    }

    @Test
    void get_whenNoLegacyLocationExists_thenConfLocation() throws IOException {
        final var confDir = tempDir.resolve("conf");
        Files.createDirectories(confDir);
        Files.writeString(confDir.resolve("prometheusConfiguration.properties"), "ip=0.0.0.0");

        final var resolver = new ConfigResolver(tempDir, EXTENSION_NAME, CONFIG_LOCATION, LEGACY_CONFIG_LOCATION);

        final var result = resolver.get();
        assertThat(result).isEqualTo(tempDir.resolve(CONFIG_LOCATION));
    }

    @Test
    void get_whenOnlyLegacyLocationExists_thenLegacyLocation() throws IOException {
        Files.writeString(tempDir.resolve("prometheusConfiguration.properties"), "ip=0.0.0.0");

        final var logger = (Logger) LoggerFactory.getLogger(ConfigResolver.class);
        final var listAppender = new ListAppender<ILoggingEvent>();
        listAppender.start();
        logger.addAppender(listAppender);

        try {
            final var resolver = new ConfigResolver(tempDir, EXTENSION_NAME, CONFIG_LOCATION, LEGACY_CONFIG_LOCATION);

            final var result = resolver.get();
            assertThat(result).isEqualTo(tempDir.resolve(LEGACY_CONFIG_LOCATION));

            final var warnLogs =
                    listAppender.list.stream().filter(event -> event.getLevel().toString().equals("WARN")).toList();
            assertThat(warnLogs).hasSize(1);
            assertThat(warnLogs.getFirst().getFormattedMessage()).contains(EXTENSION_NAME);
            assertThat(warnLogs.getFirst().getFormattedMessage()).contains("legacy location");
        } finally {
            logger.detachAppender(listAppender);
        }
    }

    @Test
    void get_whenBothLocationsExist_thenLegacyLocation() throws IOException {
        // create config at legacy location
        Files.writeString(tempDir.resolve("prometheusConfiguration.properties"), "ip=legacy");

        // create config at new location
        final var confDir = tempDir.resolve("conf");
        Files.createDirectories(confDir);
        Files.writeString(confDir.resolve("prometheusConfiguration.properties"), "ip=new");

        final var logger = (Logger) LoggerFactory.getLogger(ConfigResolver.class);
        final var listAppender = new ListAppender<ILoggingEvent>();
        listAppender.start();
        logger.addAppender(listAppender);

        try {
            final var resolver = new ConfigResolver(tempDir, EXTENSION_NAME, CONFIG_LOCATION, LEGACY_CONFIG_LOCATION);

            final var result = resolver.get();
            assertThat(result).isEqualTo(tempDir.resolve(LEGACY_CONFIG_LOCATION));

            final var warnLogs =
                    listAppender.list.stream().filter(event -> event.getLevel().toString().equals("WARN")).toList();
            assertThat(warnLogs).hasSize(1);
            assertThat(warnLogs.getFirst().getFormattedMessage()).contains(EXTENSION_NAME);
            assertThat(warnLogs.getFirst().getFormattedMessage()).contains("legacy location");
        } finally {
            logger.detachAppender(listAppender);
        }
    }

    @Test
    void get_whenCalledMultipleTimes_withLegacyLocation_warningLoggedOnlyOnce() throws IOException {
        Files.writeString(tempDir.resolve("prometheusConfiguration.properties"), "ip=0.0.0.0");

        final var logger = (Logger) LoggerFactory.getLogger(ConfigResolver.class);
        final var listAppender = new ListAppender<ILoggingEvent>();
        listAppender.start();
        logger.addAppender(listAppender);

        try {
            final var resolver = new ConfigResolver(tempDir, EXTENSION_NAME, CONFIG_LOCATION, LEGACY_CONFIG_LOCATION);

            // call get() multiple times
            resolver.get();
            resolver.get();
            resolver.get();
            resolver.get();

            // all calls should return the legacy location
            assertThat(resolver.get()).isEqualTo(tempDir.resolve(LEGACY_CONFIG_LOCATION));

            // verify warning was logged only once
            final var warnLogs =
                    listAppender.list.stream().filter(event -> event.getLevel().toString().equals("WARN")).toList();
            assertThat(warnLogs).hasSize(1);
            assertThat(warnLogs.getFirst().getFormattedMessage()).contains(EXTENSION_NAME);
            assertThat(warnLogs.getFirst().getFormattedMessage()).contains("legacy location");
        } finally {
            logger.detachAppender(listAppender);
        }
    }
}
