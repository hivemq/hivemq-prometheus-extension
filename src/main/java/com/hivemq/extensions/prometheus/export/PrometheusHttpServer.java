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

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.HTTPServer.HTTPMetricHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * An HTTP server for exposing Prometheus metrics.
 * <p>
 * This class uses {@link com.sun.net.httpserver.HttpServer} to provide a lightweight HTTP endpoint for Prometheus
 * to scrape metrics. It registers a metrics handler at the root path ("/") and at a configurable endpoint.
 * <p>
 * Usage:
 * <pre>
 *     final var server = PrometheusHttpServer.builder()
 *         .collector(myCollector)
 *         .hostname("0.0.0.0")
 *         .port(8080)
 *         .metricsHandlerEndpoint("/metrics")
 *         .buildAndStart();
 *     // ...
 *     server.stop();
 * </pre>
 *
 * @author David Sondermann
 */
public class PrometheusHttpServer {

    static {
        if (!System.getProperties().containsKey("sun.net.httpserver.maxReqTime")) {
            System.setProperty("sun.net.httpserver.maxReqTime", "60");
        }
        if (!System.getProperties().containsKey("sun.net.httpserver.maxRspTime")) {
            System.setProperty("sun.net.httpserver.maxRspTime", "600");
        }
    }

    protected final @NotNull HttpServer server;
    protected final @NotNull ExecutorService executorService;

    private PrometheusHttpServer(
            final @NotNull Collector collector,
            final @NotNull ExecutorService executorService,
            final @NotNull HttpServer httpServer,
            final @NotNull String metricsHandlerEndpoint) {
        if (httpServer.getAddress() == null) {
            throw new IllegalArgumentException("HttpServer hasn't been bound to an address");
        }
        this.server = httpServer;
        this.executorService = executorService;
        final var collectorRegistry = new CollectorRegistry(true);
        collectorRegistry.register(collector);
        final var metricHandler = new HTTPMetricHandler(collectorRegistry, null);
        registerHandler("/", metricHandler);
        registerHandler(metricsHandlerEndpoint, metricHandler);
        try {
            // HttpServer.start() starts the HttpServer in a new background thread.
            // If we call HttpServer.start() from a thread of the executorService,
            // the background thread will inherit the "daemon" property,
            // i.e. the server will run as a Daemon thread.
            // See https://github.com/prometheus/client_java/pull/955
            executorService.submit(server::start).get();
            // calling .get() on the Future here to avoid silently discarding errors
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (final ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private void registerHandler(final @NotNull String path, final @NotNull HttpHandler handler) {
        server.createContext(path, handler);
    }

    /**
     * Stop the HTTP server.
     */
    public void stop() {
        server.stop(0);
        // free any (parked/idle) threads in pool
        executorService.shutdown();
    }

    /**
     * Gets the port number. This is useful if you did not specify a port and the server picked a free
     * port automatically.
     */
    public int getPort() {
        return server.getAddress().getPort();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private @Nullable Collector collector;
        private @Nullable String hostname = null;

        private int port = 0;
        private @NotNull String metricsHandlerEndpoint = "/metrics";

        private Builder() {
        }

        public Builder collector(final @NotNull Collector collector) {
            this.collector = collector;
            return this;
        }

        /**
         * Use this hostname to resolve the IP address to bind to. Default is empty, indicating that the
         * PrometheusHttpServer binds to the wildcard address.
         */
        public Builder hostname(final @Nullable String hostname) {
            this.hostname = hostname;
            return this;
        }

        /**
         * Port to bind to. Default is 0, indicating that a random port will be selected. You can learn
         * the randomly selected port by calling {@link PrometheusHttpServer#getPort()}.
         */
        public Builder port(final int port) {
            this.port = port;
            return this;
        }

        /**
         * Optional: Override default metrics endpoint. Default is {@code /metrics}.
         */
        public Builder metricsHandlerPath(final @Nullable String metricsHandlerEndpoint) {
            if (metricsHandlerEndpoint != null) {
                this.metricsHandlerEndpoint = metricsHandlerEndpoint;
            }
            return this;
        }

        /**
         * Build and start the PrometheusHttpServer.
         */
        public PrometheusHttpServer buildAndStart() throws IOException {
            Objects.requireNonNull(collector);
            final var executorService = new ThreadPoolExecutor(1,
                    10,
                    120,
                    TimeUnit.SECONDS,
                    new SynchronousQueue<>(true),
                    NamedDaemonThreadFactory.defaultThreadFactory(),
                    new BlockingRejectedExecutionHandler());
            final var httpServer = HttpServer.create(makeInetSocketAddress(), 3);
            httpServer.setExecutor(executorService);
            return new PrometheusHttpServer(collector, executorService, httpServer, metricsHandlerEndpoint);
        }

        private @NotNull InetSocketAddress makeInetSocketAddress() {
            if (hostname != null) {
                return new InetSocketAddress(hostname, port);
            } else {
                return new InetSocketAddress(port);
            }
        }
    }
}
