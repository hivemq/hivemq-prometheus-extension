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
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extensions.prometheus.configuration.PrometheusExtensionConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PrometheusServerTest {

    private @NotNull PrometheusExtensionConfiguration config;

    @BeforeEach
    void setUp() {
        final int port = createRandomPort();
        config = mock(PrometheusExtensionConfiguration.class);
        when(config.hostIp()).thenReturn("localhost");
        when(config.port()).thenReturn(port);
        when(config.metricPath()).thenReturn("/metrics");
    }

    @Test
    void test_start_stop_successful() throws Exception {
        final PrometheusServer prometheusServer = new PrometheusServer(config, new MetricRegistry());
        prometheusServer.start();
        final URL url = new URL("http://" + config.hostIp() + ":" + config.port() + config.metricPath());
        final HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        assertEquals(200, con.getResponseCode());
        prometheusServer.stop();
    }

    private int createRandomPort() {
        try {
            final ServerSocket serverSocket = new ServerSocket(0);
            final int port = serverSocket.getLocalPort();
            serverSocket.close();
            return port;
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }
}
