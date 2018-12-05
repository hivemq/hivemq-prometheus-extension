package com.hivemq.plugin.prometheus.plugin;

import com.hivemq.plugin.api.parameter.PluginInformation;
import com.hivemq.plugin.api.parameter.PluginStartInput;
import com.hivemq.plugin.api.parameter.PluginStartOutput;
import com.hivemq.plugin.prometheus.plugin.configuration.ConfigurationReader;
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
    PluginStartInput pluginStartInput;
    @Mock
    PluginStartOutput pluginStartOutput;
    @Mock
    PluginInformation pluginInformation;
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
    public void test_start_plugin_fail_no_configfile() {
        when(pluginStartInput.getPluginInformation()).thenReturn(pluginInformation);
        when(pluginInformation.getPluginHomeFolder()).thenReturn(temporaryFolder.getRoot());
        prometheusMainClass.pluginStart(pluginStartInput, pluginStartOutput);
        verify(pluginStartOutput, times(1)).preventPluginStartup(stringCaptor.capture());
        assertTrue(stringCaptor.getValue().contains("could not be read"));

    }

    @Test
    public void test_start_plugin_fail_corrupt_configfile() throws Exception {
        when(pluginStartInput.getPluginInformation()).thenReturn(pluginInformation);
        when(pluginInformation.getPluginHomeFolder()).thenReturn(temporaryFolder.getRoot());
        temporaryFolder.newFile(ConfigurationReader.CONFIG_PATH);
        prometheusMainClass.pluginStart(pluginStartInput, pluginStartOutput);
        verify(pluginStartOutput, times(1)).preventPluginStartup(Matchers.anyString());
    }


}