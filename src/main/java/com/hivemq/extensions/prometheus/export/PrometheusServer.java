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
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.dropwizard.DropwizardExports;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Class that handles start and stop of a server, to enable requests the Metrics via the
 * {@link MonitoredMetricServlet}.
 *
 * @author Daniel Krüger
 */
public class PrometheusServer {

    /* Minimum thread count for Jetty's thread pool */
    private static final int MIN_THREADS = 3;
    /* Maximum thread count for Jetty's thread pool */
    private static final int MAX_THREADS = 8;

    private static final @NotNull Logger LOG = LoggerFactory.getLogger(PrometheusServer.class);

    private final @NotNull AtomicReference<DropwizardExports> dropwizardExportsRef = new AtomicReference<>();

    private final @NotNull PrometheusExtensionConfiguration configuration;
    private final @NotNull MetricRegistry metricRegistry;
    private final @NotNull Server server;

    public PrometheusServer(
            final @NotNull PrometheusExtensionConfiguration configuration,
            final @NotNull MetricRegistry metricRegistry) {
        this.configuration = configuration;
        this.metricRegistry = metricRegistry;
        // Set sane thread pool limits (this being a metrics extension)
        final QueuedThreadPool queuedThreadPool = new QueuedThreadPool();
        queuedThreadPool.setMinThreads(MIN_THREADS);
        queuedThreadPool.setMaxThreads(MAX_THREADS);
        this.server = new Server(queuedThreadPool);
        final ServerConnector connector = new ServerConnector(server);
        connector.setHost(configuration.hostIp());
        connector.setPort(configuration.port());
        server.setConnectors(new Connector[]{connector});
    }

    public void start() throws Exception {
        final DropwizardExports dropwizardExports = new DropwizardExports(metricRegistry);
        CollectorRegistry.defaultRegistry.register(dropwizardExports);
        dropwizardExportsRef.set(dropwizardExports);

        final ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");
        server.setHandler(context);
        context.addServlet(new ServletHolder(new MonitoredMetricServlet(metricRegistry)), configuration.metricPath());
        server.start();
        LOG.info("Started Jetty Server exposing Prometheus Servlet on URI {}",
                trimTrailingSlash(server.getURI()) + configuration.metricPath());
    }

    public void stop() {
        try {
            final DropwizardExports dropwizardExports = dropwizardExportsRef.getAndSet(null);
            if (dropwizardExports != null) {
                CollectorRegistry.defaultRegistry.unregister(dropwizardExports);
            }
            server.stop();
        } catch (final Exception e) {
            LOG.error("Exception occurred while stopping the Prometheus Extension");
            LOG.debug("Original exception was: ", e);
        }
    }

    private @NotNull String trimTrailingSlash(final @Nullable URI serverUri) {
        if (serverUri == null) {
            return "";
        }
        final String serverUriString = serverUri.toString();
        if (serverUriString.endsWith("/")) {
            return serverUriString.substring(0, serverUriString.length() - 1);
        }
        return serverUriString;
    }
}
