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

package com.hivemq.extensions.prometheus.export;

import com.codahale.metrics.MetricRegistry;
import com.hivemq.extensions.prometheus.configuration.PrometheusExtensionConfiguration;
import io.prometheus.client.dropwizard.DropwizardExports;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Class that handles start and stop of a server, to enable requests the Metrics via HTTP.
 *
 * @author David Sondermann
 */
public class PrometheusServer {

    private static final @NotNull Logger LOG = LoggerFactory.getLogger(PrometheusServer.class);

    private final @NotNull AtomicReference<PrometheusHttpServer> httpServerRef = new AtomicReference<>();

    private final @NotNull PrometheusExtensionConfiguration configuration;
    private final @NotNull MetricRegistry metricRegistry;

    public PrometheusServer(
            final @NotNull PrometheusExtensionConfiguration configuration,
            final @NotNull MetricRegistry metricRegistry) {
        this.configuration = configuration;
        this.metricRegistry = metricRegistry;
    }

    public void start() throws IOException {
        final var server = PrometheusHttpServer.builder()
                .port(configuration.port())
                .hostname(configuration.hostIp())
                .metricsHandlerPath(configuration.metricPath())
                .collector(new DropwizardExports(metricRegistry))
                .buildAndStart();
        httpServerRef.set(server);

        //noinspection HttpUrlsUsage
        LOG.info("Started HTTPServer exposing Prometheus metrics on http://{}:{}{}",
                configuration.hostIp(),
                configuration.port(),
                configuration.metricPath());
    }

    public void stop() {
        try {
            final var server = httpServerRef.getAndSet(null);
            if (server != null) {
                server.stop();
            }
        } catch (final Exception e) {
            LOG.error("Exception occurred while stopping the Prometheus Extension");
            LOG.debug("Original exception was", e);
        }
    }
}
