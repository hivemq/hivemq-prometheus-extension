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

package com.hivemq.extensions.prometheus;

import com.hivemq.extension.sdk.api.parameter.ExtensionInformation;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartOutput;
import com.hivemq.extensions.prometheus.configuration.ConfigurationReader;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

class PrometheusExtensionMainTest {

    private final @NotNull PrometheusExtensionMain prometheusExtensionMain = new PrometheusExtensionMain();
    private final @NotNull ExtensionStartInput extensionStartInput = mock(ExtensionStartInput.class);
    private final @NotNull ExtensionStartOutput extensionStartOutput = mock(ExtensionStartOutput.class);

    private @NotNull Path configPath;

    @BeforeEach
    void setUp(@TempDir final @NotNull Path tempDir) {
        final var extensionInformation = mock(ExtensionInformation.class);
        when(extensionStartInput.getExtensionInformation()).thenReturn(extensionInformation);
        when(extensionInformation.getExtensionHomeFolder()).thenReturn(tempDir.toFile());
        configPath = tempDir.resolve(ConfigurationReader.CONFIG_PATH);
    }

    @Test
    void test_start_extension_fail_no_configFile() {
        prometheusExtensionMain.extensionStart(extensionStartInput, extensionStartOutput);
        final var stringCaptor = ArgumentCaptor.forClass(String.class);
        verify(extensionStartOutput, times(1)).preventExtensionStartup(stringCaptor.capture());
        assertThat(stringCaptor.getValue()).contains("could not be read");
    }

    @Test
    void test_start_extension_fail_corrupt_configFile() throws Exception {
        Files.createFile(configPath);
        prometheusExtensionMain.extensionStart(extensionStartInput, extensionStartOutput);
        verify(extensionStartOutput, times(1)).preventExtensionStartup(anyString());
    }
}
