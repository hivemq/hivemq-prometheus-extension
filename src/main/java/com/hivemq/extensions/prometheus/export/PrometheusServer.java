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
package com.hivemq.extensions.prometheus.export;

import com.codahale.metrics.MetricRegistry;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extensions.prometheus.configuration.PrometheusExtensionConfiguration;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.dropwizard.DropwizardExports;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

/**
 * Class that handles start and stop of a server, to enable requests the Metrics via the {@link MonitoredMetricServlet}.
 *
 * @author Daniel Krüger
 */
public class PrometheusServer {

    /* Minimum thread count for Jetty's thread pool */
    public static final int MIN_THREADS = 3;
    /* Maximum thread count for Jetty's thread pool */
    public static final int MAX_THREADS = 8;

    @NotNull
    private static final Logger log = LoggerFactory.getLogger(PrometheusServer.class);
    @Nullable
    private PrometheusExtensionConfiguration configuration;
    @NotNull
    private final MetricRegistry metricRegistry;

    @Nullable
    private DropwizardExports dropwizardExports;

    @NotNull
    private final Server server;

    public PrometheusServer(@NotNull final PrometheusExtensionConfiguration configuration, @NotNull final MetricRegistry metricRegistry) {
        this.configuration = configuration;
        this.metricRegistry = metricRegistry;
        // Set sane thread pool limits (this being a metrics extension)
        QueuedThreadPool queuedThreadPool = new QueuedThreadPool();
        queuedThreadPool.setMinThreads(MIN_THREADS);
        queuedThreadPool.setMaxThreads(MAX_THREADS);
        this.server = new Server(queuedThreadPool);
        ServerConnector connector = new ServerConnector(server);
        connector.setHost(configuration.hostIp());
        connector.setPort(configuration.port());
        this.server.setConnectors(new Connector[]{connector});
    }


    public void start() {

        dropwizardExports = new DropwizardExports(metricRegistry);

        CollectorRegistry.defaultRegistry.register(dropwizardExports);
        final ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");
        server.setHandler(context);
        context.addServlet(new ServletHolder(new MonitoredMetricServlet(metricRegistry)), configuration.metricPath());
        try {
            server.start();
        } catch (final Exception e) {
            log.error("Error starting the Jetty Server for Prometheus Extension");
            log.debug("Original exception was:", e);
        }
        log.info("Started Jetty Server exposing Prometheus Servlet on URI {}", server.getURI() + configuration.metricPath());
    }

    public void stop() {
        try {
            CollectorRegistry.defaultRegistry.unregister(dropwizardExports);
            server.stop();
        } catch (final Exception e) {
            log.error("Exception occurred while stopping the Prometheus Extension");
            log.debug("Original exception was: ", e);
        }
    }
}
