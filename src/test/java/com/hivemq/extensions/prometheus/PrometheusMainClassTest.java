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

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.parameter.ExtensionInformation;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartOutput;
import com.hivemq.extensions.prometheus.configuration.ConfigurationReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import static org.mockito.internal.verification.VerificationModeFactory.times;

class PrometheusMainClassTest {

    private final @NotNull PrometheusMainClass prometheusMainClass = new PrometheusMainClass();
    private @NotNull ExtensionStartInput extensionStartInput;
    private @NotNull ExtensionStartOutput extensionStartOutput;
    private @NotNull Path configPath;

    @BeforeEach
    void setUp(@TempDir final @NotNull Path tempDir) {
        extensionStartInput = mock(ExtensionStartInput.class);
        extensionStartOutput = mock(ExtensionStartOutput.class);
        final ExtensionInformation extensionInformation = mock(ExtensionInformation.class);
        when(extensionStartInput.getExtensionInformation()).thenReturn(extensionInformation);
        when(extensionInformation.getExtensionHomeFolder()).thenReturn(tempDir.toFile());
        configPath = tempDir.resolve(ConfigurationReader.CONFIG_PATH);
    }

    @Test
    void test_start_extension_fail_no_configfile() {
        prometheusMainClass.extensionStart(extensionStartInput, extensionStartOutput);
        final ArgumentCaptor<String> stringCaptor = ArgumentCaptor.forClass(String.class);
        verify(extensionStartOutput, times(1)).preventExtensionStartup(stringCaptor.capture());
        assertTrue(stringCaptor.getValue().contains("could not be read"));
    }

    @Test
    void test_start_extension_fail_corrupt_configfile() throws Exception {
        Files.createFile(configPath);
        prometheusMainClass.extensionStart(extensionStartInput, extensionStartOutput);
        verify(extensionStartOutput, times(1)).preventExtensionStartup(anyString());
    }
}