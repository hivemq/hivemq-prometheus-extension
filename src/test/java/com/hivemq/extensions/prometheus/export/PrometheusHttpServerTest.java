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

import io.prometheus.client.Collector;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PrometheusHttpServerTest {

    private final @NotNull Collector collector = mock();

    @Test
    void defaultHandler() throws Exception {
        run(PrometheusHttpServer.builder().collector(collector).buildAndStart(), "/metrics");
    }

    @Test
    void metricsNullPath() throws Exception {
        run(PrometheusHttpServer.builder().collector(collector).metricsHandlerPath(null).buildAndStart(), "/metrics");
    }

    @Test
    void metricsCustomPath() throws Exception {
        run(PrometheusHttpServer.builder()
                .port(0)
                .metricsHandlerPath("/my-metrics")
                .collector(collector)
                .buildAndStart(), "/my-metrics");
    }

    @Test
    void invalidPort() {
        assertThatThrownBy(() -> PrometheusHttpServer.builder()
                .port(-1)
                .collector(collector)
                .buildAndStart()).isInstanceOf(IllegalArgumentException.class).hasMessage("port out of range:-1");
    }

    @Test
    void metricsCustomPath_withRootPath() throws Exception {
        run(PrometheusHttpServer.builder()
                .port(0)
                .metricsHandlerPath("/")
                .collector(collector)
                .buildAndStart(), "/");
    }

    private void run(final @NotNull PrometheusHttpServer server, final @NotNull String path) throws Exception {
        try (final var socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", server.getPort()));
            final var httpRequest = """
                    GET %s HTTP/1.1\r
                    HOST: localhost\r
                    \r
                    """.formatted(path);
            socket.getOutputStream().write(httpRequest.getBytes(StandardCharsets.UTF_8));
            socket.getOutputStream().flush();
            var actualResponse = "";
            final var resp = new byte[500];
            final var read = socket.getInputStream().read(resp, 0, resp.length);
            if (read > 0) {
                actualResponse = new String(resp, 0, read, StandardCharsets.UTF_8);
            }
            assertThat(actualResponse).contains("200");
            verify(collector).collect();
        } finally {
            server.stop();
        }
    }
}
