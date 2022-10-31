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

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.parameter.ExtensionInformation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConfigurationReaderTest {

    private @NotNull ConfigurationReader configurationReader;
    private @NotNull Path configPath;

    @BeforeEach
    void setUp(final @TempDir @NotNull Path tempDir) {
        final ExtensionInformation extensionInformation = mock(ExtensionInformation.class);
        when(extensionInformation.getExtensionHomeFolder()).thenReturn(tempDir.toFile());
        configurationReader = new ConfigurationReader(extensionInformation);
        configPath = tempDir.resolve(ConfigurationReader.CONFIG_PATH);
    }

    @Test
    void test_ReadConfiguration_no_file() {
        assertThrows(FileNotFoundException.class, configurationReader::readConfiguration);
    }

    @Test
    void test_successfully_read_config() throws Exception {
        Files.writeString(configPath, "metric_path=/metrics\nip=127.0.0.1\nsenseless=nonsense\nport=1234");
        configurationReader.readConfiguration();
    }

    @Test
    void test_bad_format_port() throws Exception {
        Files.writeString(configPath, "metric_path=/metrics\nip=127.0.0.1\nsenseless=nonsense\nport=localhost");
        final InvalidConfigurationException e =
                assertThrows(InvalidConfigurationException.class, configurationReader::readConfiguration);
        assertTrue(e.getMessage().contains("port"));
    }

    @Test
    void test_bad_format_host() throws Exception {
        Files.writeString(configPath, "metric_path=/metrics\nip= \nsenseless=nonsense\nport=1234");
        final InvalidConfigurationException e =
                assertThrows(InvalidConfigurationException.class, configurationReader::readConfiguration);
        assertTrue(e.getMessage().contains("ip"));
    }

    @Test
    void test_bad_format_metric_path() throws Exception {
        Files.writeString(configPath, "metric_path=metrics\nip=127.0.0.1\nsenseless=nonsensen\nport=1234");
        final InvalidConfigurationException e =
                assertThrows(InvalidConfigurationException.class, configurationReader::readConfiguration);
        assertTrue(e.getMessage().contains("metric_path"));
    }

    @Test
    void test_typo_metric_path() throws Exception {
        Files.writeString(configPath, "metric_ph=metrics\nip=127.0.0.1\nsenseless=nonsense\nport=1234");
        final InvalidConfigurationException e =
                assertThrows(InvalidConfigurationException.class, configurationReader::readConfiguration);
        assertTrue(e.getMessage().contains("metric_path"));
    }

    @Test
    void test_typo_ip() throws Exception {
        Files.writeString(configPath, "metric_path=metrics\nIP=127.0.0.1\nsenseless=nonsense\nport=1234");
        final InvalidConfigurationException e =
                assertThrows(InvalidConfigurationException.class, configurationReader::readConfiguration);
        assertTrue(e.getMessage().contains("ip"));
    }

    @Test
    void test_typo_port() throws Exception {
        Files.writeString(configPath, "metric_path=metrics\nip=127.0.0.1\nsenseless=nonsense\npot=1234");
        final InvalidConfigurationException e =
                assertThrows(InvalidConfigurationException.class, configurationReader::readConfiguration);
        assertTrue(e.getMessage().contains("port"));
    }
}
