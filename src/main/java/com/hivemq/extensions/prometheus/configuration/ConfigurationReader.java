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
import org.aeonbits.owner.ConfigFactory;
import org.jetbrains.annotations.NotNull;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

/**
 * Provides the possibility to obtain the configuration of the prometheus-extension via readConfiguration()
 *
 * @author David Sondermann
 */
public class ConfigurationReader {

    public static final @NotNull String CONFIG_PATH = "conf/config.properties";
    public static final @NotNull String LEGACY_CONFIG_PATH = "prometheusConfiguration.properties";
    /**
     * The minimal possible  port
     */
    private static final int MIN_PORT = 1;
    /**
     * The maximum possible port
     */
    private static final int MAX_PORT = 65535;

    private final @NotNull ConfigResolver configResolver;

    public ConfigurationReader(final @NotNull ExtensionInformation extensionInformation) {
        this.configResolver = new ConfigResolver(extensionInformation.getExtensionHomeFolder().toPath(),
                "Prometheus Monitoring Extension",
                CONFIG_PATH,
                LEGACY_CONFIG_PATH);
    }

    public @NotNull PrometheusExtensionConfiguration readConfiguration()
            throws FileNotFoundException, InvalidConfigurationException {
        final var file = configResolver.get().toFile();
        if (!file.canRead()) {
            throw new FileNotFoundException(file.getAbsolutePath());
        }
        try (final var in = new FileInputStream(file)) {
            final var properties = new Properties();
            properties.load(in);
            testAllPropertiesDeclared(properties);
            final var prometheusExtensionConfiguration =
                    ConfigFactory.create(PrometheusExtensionConfiguration.class, properties);
            testConfiguration(prometheusExtensionConfiguration);
            return prometheusExtensionConfiguration;
        } catch (final IOException e) {
            throw new InvalidConfigurationException("Error while reading configuration file.");
        }
    }

    /**
     * In the ConfigFactory.create() it is not tested whether the entries make sense (here e.g. port holds an int)
     *
     * @param config config to be tested
     * @throws InvalidConfigurationException thrown when an entry makes no sense or does not meet the requirements
     */
    private void testConfiguration(final @NotNull PrometheusExtensionConfiguration config)
            throws InvalidConfigurationException {
        var error = false;
        final var sb = new StringBuilder();
        // test port
        try {
            testPortSense(config);
        } catch (final InvalidConfigurationException e) {
            error = true;
            sb.append(e.getMessage());
        }
        // test MetricPath
        try {
            testMetricsPathSense(config);
        } catch (final InvalidConfigurationException e) {
            error = true;
            sb.append(e.getMessage());
        }
        // test IP
        try {
            testIpSense(config);
        } catch (final InvalidConfigurationException e) {
            error = true;
            sb.append(e.getMessage());
        }
        if (error) {
            throw new InvalidConfigurationException("Error while parsing and testing the configuration: " + sb);
        }
    }

    private void testPortSense(final @NotNull PrometheusExtensionConfiguration config)
            throws InvalidConfigurationException {
        try {
            config.port();
        } catch (final Exception e) {
            throw new InvalidConfigurationException("Invalid port configuration");
        }
        final var port = config.port();
        if (port < MIN_PORT) {
            throw new InvalidConfigurationException(String.format("The port must not be smaller than %d, but was %d",
                    MIN_PORT,
                    port));
        }
        if (port > MAX_PORT) {
            throw new InvalidConfigurationException(String.format("The port must not be greater than %d, but was %d",
                    MAX_PORT,
                    port));
        }
    }

    private void testIpSense(final @NotNull PrometheusExtensionConfiguration config)
            throws InvalidConfigurationException {
        try {
            config.hostIp();
        } catch (final Exception e) {
            throw new InvalidConfigurationException("Invalid host ip configuration");
        }
        final var ip = config.hostIp();
        if (ip == null || ip.isBlank()) {
            throw new InvalidConfigurationException("The ip must not be blank");
        }
    }

    private void testMetricsPathSense(final @NotNull PrometheusExtensionConfiguration config)
            throws InvalidConfigurationException {
        try {
            config.metricPath();
        } catch (final Exception e) {
            throw new InvalidConfigurationException("Invalid metric_path configuration");
        }
        final var path = config.metricPath();
        if (path == null || !path.startsWith("/")) {
            throw new InvalidConfigurationException("The metric_path must begin with a slash, e.g. \"/metrics\"");
        }
    }

    private void testAllPropertiesDeclared(final @NotNull Properties properties) throws InvalidConfigurationException {
        boolean error = false;
        final var sb = new StringBuilder();
        if (!properties.containsKey(PrometheusExtensionConfiguration.METRIC_PATH_KEY)) {
            sb.append(" " + PrometheusExtensionConfiguration.METRIC_PATH_KEY);
            error = true;
        }
        if (!properties.containsKey(PrometheusExtensionConfiguration.IP_KEY)) {
            sb.append(" " + PrometheusExtensionConfiguration.IP_KEY);
            error = true;
        }
        if (!properties.containsKey(PrometheusExtensionConfiguration.PORT_KEY)) {
            sb.append(" " + PrometheusExtensionConfiguration.PORT_KEY);
            error = true;
        }
        if (error) {
            throw new InvalidConfigurationException(String.format("Missing required configuration of:%s", sb));
        }
    }
}
