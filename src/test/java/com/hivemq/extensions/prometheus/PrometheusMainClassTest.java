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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.*;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

public class PrometheusMainClassTest {


    @Mock
    ExtensionStartInput extensionStartInput;
    @Mock
    ExtensionStartOutput extensionStartOutput;
    @Mock
    ExtensionInformation extensionInformation;
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Captor
    ArgumentCaptor<String> stringCaptor;

    private final PrometheusMainClass prometheusMainClass = new PrometheusMainClass();

    @Before
    public void settp() {
        MockitoAnnotations.initMocks(this);

    }

    @Test
    public void test_start_extension_fail_no_configfile() {
        when(extensionStartInput.getExtensionInformation()).thenReturn(extensionInformation);
        when(extensionInformation.getExtensionHomeFolder()).thenReturn(temporaryFolder.getRoot());
        prometheusMainClass.extensionStart(extensionStartInput, extensionStartOutput);
        verify(extensionStartOutput, times(1)).preventExtensionStartup(stringCaptor.capture());
        assertTrue(stringCaptor.getValue().contains("could not be read"));

    }

    @Test
    public void test_start_extension_fail_corrupt_configfile() throws Exception {
        when(extensionStartInput.getExtensionInformation()).thenReturn(extensionInformation);
        when(extensionInformation.getExtensionHomeFolder()).thenReturn(temporaryFolder.getRoot());
        temporaryFolder.newFile(ConfigurationReader.CONFIG_PATH);
        prometheusMainClass.extensionStart(extensionStartInput, extensionStartOutput);
        verify(extensionStartOutput, times(1)).preventExtensionStartup(Matchers.anyString());
    }


}