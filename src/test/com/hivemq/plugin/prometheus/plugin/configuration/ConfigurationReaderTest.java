/*
 * Copyright 2018 dc-square GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hivemq.plugin.prometheus.plugin.configuration;

import com.hivemq.plugin.api.parameter.PluginInformation;
import com.hivemq.plugin.prometheus.plugin.exception.InvalidConfigurationException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;

import java.io.FileNotFoundException;
import java.io.FileWriter;

import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class ConfigurationReaderTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    @Mock
    private ConfigurationReader configurationReader;
    @Mock
    private PluginInformation pluginInformation;

    @Before
    public void init() {
        initMocks(this);
    }


    @Test(expected = FileNotFoundException.class)
    public void test_ReadConfiguration_no_file() throws Exception {
        when(pluginInformation.getPluginHomeFolder()).thenReturn(temporaryFolder.getRoot());
        ConfigurationReader configurationReader = new ConfigurationReader(pluginInformation);
        configurationReader.readConfiguration();
    }

    @Test
    public void test_successfully_read_config() throws Exception {
        configurationReader = new ConfigurationReader(pluginInformation);
        when(pluginInformation.getPluginHomeFolder()).thenReturn(temporaryFolder.getRoot());

        FileWriter out = new FileWriter(temporaryFolder.newFile(ConfigurationReader.CONFIG_PATH));

        out.write("metric_path=/metrics\nip=127.0.0.1\nsenseless=nonsense\nport=1234");
        out.flush();
        out.close();
        configurationReader.readConfiguration();
    }


    @Test(expected = InvalidConfigurationException.class)
    public void test_bad_format_port() throws Exception {
        configurationReader = new ConfigurationReader(pluginInformation);
        when(pluginInformation.getPluginHomeFolder()).thenReturn(temporaryFolder.getRoot());

        try (FileWriter out = new FileWriter(temporaryFolder.newFile(ConfigurationReader.CONFIG_PATH))) {
            out.write("metric_path=/metrics\nip=127.0.0.1\nsenseless=nonsense\nport=localhost");
            out.flush();
            configurationReader.readConfiguration();
        } catch (InvalidConfigurationException e) {
            Assert.assertTrue(e.getMessage().contains("port"));
            throw e;
        }
    }


    @Test(expected = InvalidConfigurationException.class)
    public void test_bad_format_host() throws Exception {
        configurationReader = new ConfigurationReader(pluginInformation);
        when(pluginInformation.getPluginHomeFolder()).thenReturn(temporaryFolder.getRoot());

        try (FileWriter out = new FileWriter(temporaryFolder.newFile(ConfigurationReader.CONFIG_PATH))) {
            out.write("metric_path=/metrics\nip= \nsenseless=nonsense\nport=1234");
            out.flush();
            configurationReader.readConfiguration();
        } catch (InvalidConfigurationException e) {
            Assert.assertTrue(e.getMessage().contains("ip"));
            throw e;
        }
    }

    @Test(expected = InvalidConfigurationException.class)
    public void test_bad_format_metric_path() throws Exception {
        configurationReader = new ConfigurationReader(pluginInformation);
        when(pluginInformation.getPluginHomeFolder()).thenReturn(temporaryFolder.getRoot());

        try (FileWriter out = new FileWriter(temporaryFolder.newFile(ConfigurationReader.CONFIG_PATH))) {
            out.write("metric_path=metrics\nip=127.0.0.1\nsenseless=nonsensen\nport=1234");
            out.flush();

            configurationReader.readConfiguration();
        } catch (InvalidConfigurationException e) {
            Assert.assertTrue(e.getMessage().contains("metric_path"));
            throw e;
        }
    }


    @Test(expected = InvalidConfigurationException.class)
    public void test_typo_metric_path() throws Exception {
        configurationReader = new ConfigurationReader(pluginInformation);
        when(pluginInformation.getPluginHomeFolder()).thenReturn(temporaryFolder.getRoot());

        try (FileWriter out = new FileWriter(temporaryFolder.newFile(ConfigurationReader.CONFIG_PATH))) {
            out.write("metric_ph=metrics\nip=127.0.0.1\nsenseless=nonsense\nport=1234");
            out.flush();

            configurationReader.readConfiguration();
        } catch (InvalidConfigurationException e) {

            Assert.assertTrue(e.getMessage().contains("metric_path"));
            throw e;
        }
    }

    @Test(expected = InvalidConfigurationException.class)
    public void test_typo_ip() throws Exception {
        configurationReader = new ConfigurationReader(pluginInformation);
        when(pluginInformation.getPluginHomeFolder()).thenReturn(temporaryFolder.getRoot());

        try (FileWriter out = new FileWriter(temporaryFolder.newFile(ConfigurationReader.CONFIG_PATH))) {
            out.write("metric_path=metrics\nIP=127.0.0.1\nsenseless=nonsense\nport=1234");
            out.flush();
            configurationReader.readConfiguration();
        } catch (InvalidConfigurationException e) {
            Assert.assertTrue(e.getMessage().contains("ip"));
            throw e;
        }
    }

    @Test(expected = InvalidConfigurationException.class)
    public void test_typo_port() throws Exception {
        configurationReader = new ConfigurationReader(pluginInformation);
        when(pluginInformation.getPluginHomeFolder()).thenReturn(temporaryFolder.getRoot());

        try (FileWriter out = new FileWriter(temporaryFolder.newFile(ConfigurationReader.CONFIG_PATH))) {
            out.write("metric_path=metrics\nip=127.0.0.1\nsenseless=nonsense\npot=1234");
            out.flush();
            configurationReader.readConfiguration();
        } catch (InvalidConfigurationException e) {
            Assert.assertTrue(e.getMessage().contains("port"));
            throw e;
        }
    }


}