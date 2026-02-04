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

import com.hivemq.extension.sdk.api.parameter.ExtensionInformation;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConfigurationReaderTest {

    private @NotNull ConfigurationReader configurationReader;
    private @NotNull Path configPath;

    @TempDir
    private @NotNull Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        final var extensionInformation = mock(ExtensionInformation.class);
        when(extensionInformation.getExtensionHomeFolder()).thenReturn(tempDir.toFile());
        configurationReader = new ConfigurationReader(extensionInformation);
        configPath = tempDir.resolve(ConfigurationReader.CONFIG_PATH);
        Files.createDirectories(configPath.getParent());
    }

    @Test
    void readConfiguration() throws Exception {
        Files.writeString(configPath, """
                metric_path=/metrics
                ip=127.0.0.1
                senseless=nonsense
                port=1234
                """);
        configurationReader.readConfiguration();
    }

    @Test
    void readConfiguration_withNoConfigFile() {
        assertThatThrownBy(configurationReader::readConfiguration).isInstanceOf(FileNotFoundException.class);
    }

    @Test
    void readConfiguration_withInvalidPort() throws Exception {
        Files.writeString(configPath, """
                metric_path=/metrics
                ip=127.0.0.1
                senseless=nonsense
                port=localhost
                """);
        final var e = assertThrows(InvalidConfigurationException.class, configurationReader::readConfiguration);
        assertThat(e.getMessage()).contains("port");
    }

    @Test
    void readConfiguration_withInvalidHost() throws Exception {
        Files.writeString(configPath, """
                metric_path=/metrics
                ip=\s
                senseless=nonsense
                port=1234
                """);
        final var e = assertThrows(InvalidConfigurationException.class, configurationReader::readConfiguration);
        assertThat(e.getMessage()).contains("ip");
    }

    @Test
    void readConfiguration_withInvalidMetricPath() throws Exception {
        Files.writeString(configPath, """
                metric_path=metrics
                ip=127.0.0.1
                senseless=nonsense
                port=1234
                """);
        final var e = assertThrows(InvalidConfigurationException.class, configurationReader::readConfiguration);
        assertThat(e.getMessage()).contains("metric_path");
    }

    @Test
    void readConfiguration_withInvalidMetricPathName() throws Exception {
        Files.writeString(configPath, """
                metric_ph=metrics
                ip=127.0.0.1
                senseless=nonsense
                port=1234
                """);
        final var e = assertThrows(InvalidConfigurationException.class, configurationReader::readConfiguration);
        assertThat(e.getMessage()).contains("metric_path");
    }

    @Test
    void readConfiguration_withInvalidIpName() throws Exception {
        Files.writeString(configPath, """
                metric_path=metrics
                IP=127.0.0.1
                senseless=nonsense
                port=1234
                """);
        final var e = assertThrows(InvalidConfigurationException.class, configurationReader::readConfiguration);
        assertThat(e.getMessage()).contains("ip");
    }

    @Test
    void readConfiguration_withInvalidPortName() throws Exception {
        Files.writeString(configPath, """
                metric_path=metrics
                ip=127.0.0.1
                senseless=nonsense
                pot=1234
                """);
        final var e = assertThrows(InvalidConfigurationException.class, configurationReader::readConfiguration);
        assertThat(e.getMessage()).contains("port");
    }

    @Test
    void readConfiguration_withLegacyLocation() throws Exception {
        Files.writeString(tempDir.resolve(ConfigurationReader.LEGACY_CONFIG_PATH), """
                metric_path=/metrics
                ip=127.0.0.1
                port=1234
                """);
        configurationReader.readConfiguration();
    }
}
