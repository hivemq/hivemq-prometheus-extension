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