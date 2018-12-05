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

package com.hivemq.plugin.prometheus.plugin;

import com.hivemq.plugin.api.PluginMain;
import com.hivemq.plugin.api.annotations.NotNull;
import com.hivemq.plugin.api.parameter.PluginStartInput;
import com.hivemq.plugin.api.parameter.PluginStartOutput;
import com.hivemq.plugin.api.parameter.PluginStopInput;
import com.hivemq.plugin.api.parameter.PluginStopOutput;
import com.hivemq.plugin.api.services.Services;
import com.hivemq.plugin.prometheus.plugin.configuration.ConfigurationReader;
import com.hivemq.plugin.prometheus.plugin.configuration.PrometheusPluginConfiguration;
import com.hivemq.plugin.prometheus.plugin.exception.InvalidConfigurationException;
import com.hivemq.plugin.prometheus.plugin.export.PrometheusServer;

import java.io.FileNotFoundException;


/**
 * This is the main class of the  prometheus plugin, which is instantiated during the HiveMQ start up process.
 *
 * @author Daniel Krüger
 */
public class PrometheusMainClass implements PluginMain {

    private PrometheusServer prometheusServer;

    @Override
    public void pluginStart(@NotNull PluginStartInput pluginStartInput, @NotNull PluginStartOutput pluginStartOutput) {

        PrometheusPluginConfiguration configuration;
        try {
            configuration = new ConfigurationReader(pluginStartInput.getPluginInformation()).readConfiguration();
        } catch (FileNotFoundException e) {
            pluginStartOutput.preventPluginStartup("The configuration file: " + e.getMessage() + " could not be read.");
            return;
        } catch (InvalidConfigurationException e) {
            pluginStartOutput.preventPluginStartup(e.getMessage());
            return;
        } catch (Exception e) {
            pluginStartOutput.preventPluginStartup("Unknown error while reading configuration file" + ((e.getMessage() != null) ? ": " + e.getMessage() : ""));
            return;
        }
        if (configuration == null) {
            pluginStartOutput.preventPluginStartup("Unspecified error occurred while reading configuration");
            return;
        }

        prometheusServer = new PrometheusServer(configuration, Services.metricRegistry());
        prometheusServer.start();
    }

    @Override
    public void pluginStop(@NotNull PluginStopInput pluginStopInput, @NotNull PluginStopOutput pluginStopOutput) {
        prometheusServer.stop();
    }
}
