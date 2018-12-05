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
package com.hivemq.plugin.prometheus.plugin.export;

import com.codahale.metrics.MetricRegistry;
import com.hivemq.plugin.api.annotations.NotNull;
import com.hivemq.plugin.api.annotations.Nullable;
import com.hivemq.plugin.prometheus.plugin.configuration.PrometheusPluginConfiguration;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.dropwizard.DropwizardExports;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

/**
 * class that handles start and stop of a server, to enable requests the Metrics via the MonitoredMetricsServlet.
 *
 * @author Daniel Krüger
 */
public class PrometheusServer {


    @NotNull
    private static final Logger log = LoggerFactory.getLogger(PrometheusServer.class);
    @Nullable
    private PrometheusPluginConfiguration configuration;
    @NotNull
    private final MetricRegistry metricRegistry;

    @Nullable
    private Server server;

    public PrometheusServer(@NotNull final PrometheusPluginConfiguration configuration, @NotNull final MetricRegistry metricRegistry) {
        this.configuration = configuration;
        this.metricRegistry = metricRegistry;
    }


    public void start() {
        CollectorRegistry.defaultRegistry.register(new DropwizardExports(metricRegistry));
        server = new Server(new InetSocketAddress(configuration.hostIp(), configuration.port()));
        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");
        server.setHandler(context);
        context.addServlet(new ServletHolder(new MonitoredMetricServlet(metricRegistry)), configuration.metricPath());
        try {
            server.start();
        } catch (Exception e) {
            log.error("Error starting the Jetty Server for Prometheus Plugin");
            log.debug("Original exception was:", e);
        }
        log.info("Started Jetty Server exposing Prometheus Servlet on URI {}", server.getURI()+configuration.metricPath());
    }

    public void stop() {
        try {
            CollectorRegistry.defaultRegistry.unregister(new DropwizardExports(metricRegistry));
            server.stop();
        } catch (Exception e) {
            log.error("Exception occurred while stopping the Prometheus Plugin");
            log.debug("Original exception was: ", e);
        }
    }
}
