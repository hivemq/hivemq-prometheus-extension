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
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PrometheusServerTest {

    private final @NotNull PrometheusExtensionConfiguration config = mock(PrometheusExtensionConfiguration.class);
    private final @NotNull MetricRegistry metricRegistry = new MetricRegistry();

    @BeforeEach
    void setUp() {
        final var port = createRandomPort();
        when(config.hostIp()).thenReturn("localhost");
        when(config.port()).thenReturn(port);
        when(config.metricPath()).thenReturn("/metrics");
    }

    @Test
    void test_start_stop_successful() throws Exception {
        metricRegistry.counter("my-counter-1").inc();

        final var prometheusServer = new PrometheusServer(config, metricRegistry);
        prometheusServer.start();

        metricRegistry.counter("my-counter-1").inc();
        metricRegistry.counter("my-counter-2").inc();

        final var httpClient = HttpClient.newHttpClient();
        //noinspection HttpUrlsUsage
        final var httpRequest = HttpRequest.newBuilder(URI.create("http://" +
                config.hostIp() +
                ":" +
                config.port() +
                config.metricPath())).build();
        final var response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()) //
                .contains("my_counter_1 2.0") //
                .contains("my_counter_2 1.0");

        prometheusServer.stop();
    }

    private int createRandomPort() {
        try {
            final var serverSocket = new ServerSocket(0);
            final var port = serverSocket.getLocalPort();
            serverSocket.close();
            return port;
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }
}
