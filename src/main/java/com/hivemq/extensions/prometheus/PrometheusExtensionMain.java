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

import com.hivemq.extension.sdk.api.ExtensionMain;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartOutput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStopInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStopOutput;
import com.hivemq.extension.sdk.api.services.Services;
import com.hivemq.extensions.prometheus.configuration.ConfigurationReader;
import com.hivemq.extensions.prometheus.configuration.InvalidConfigurationException;
import com.hivemq.extensions.prometheus.export.PrometheusServer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This is the main class of the prometheus extension, which is instantiated during the HiveMQ start up process.
 *
 * @author David Sondermann
 */
public class PrometheusExtensionMain implements ExtensionMain {

    private static final @NotNull Logger LOG = LoggerFactory.getLogger(PrometheusExtensionMain.class);

    private final @NotNull AtomicReference<PrometheusServer> prometheusServerRef = new AtomicReference<>();

    @Override
    public void extensionStart(
            final @NotNull ExtensionStartInput extensionStartInput,
            final @NotNull ExtensionStartOutput extensionStartOutput) {
        try {
            final var configuration =
                    new ConfigurationReader(extensionStartInput.getExtensionInformation()).readConfiguration();
            try {
                final var prometheusServer = new PrometheusServer(configuration, Services.metricRegistry());
                prometheusServerRef.set(prometheusServer);
                prometheusServer.start();
            } catch (final Exception e) {
                LOG.error("Error starting the HTTP Server for Prometheus Extension", e);
                extensionStartOutput.preventExtensionStartup(String.format(
                        "Error starting the HTTP Server for Prometheus Extension%s",
                        (e.getMessage() != null) ? ": " + e.getMessage() : ""));
            }
        } catch (final FileNotFoundException e) {
            extensionStartOutput.preventExtensionStartup(String.format("The configuration file: %s could not be read",
                    e.getMessage()));
        } catch (final InvalidConfigurationException e) {
            extensionStartOutput.preventExtensionStartup(e.getMessage());
        } catch (final Exception e) {
            extensionStartOutput.preventExtensionStartup(String.format(
                    "Unknown error while reading configuration file%s",
                    (e.getMessage() != null) ? ": " + e.getMessage() : ""));
        }
    }

    @Override
    public void extensionStop(
            final @NotNull ExtensionStopInput extensionStopInput,
            final @NotNull ExtensionStopOutput extensionStopOutput) {
        final var prometheusServer = prometheusServerRef.getAndSet(null);
        if (prometheusServer != null) {
            prometheusServer.stop();
        }
    }
}
