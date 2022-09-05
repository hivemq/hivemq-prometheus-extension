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

package com.hivemq.extensions.helloworld;

import com.codahale.metrics.MetricRegistry;
import com.hivemq.extension.sdk.api.ExtensionMain;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartOutput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStopInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStopOutput;
import com.hivemq.extension.sdk.api.services.Services;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.testcontainers.hivemq.HiveMQContainer;
import org.testcontainers.hivemq.HiveMQExtension;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Yannick Weber
 * @since 4.0.6
 */
@Testcontainers
class PrometheusExtensionIT {

    public static final @NotNull DockerImageName DOCKER_IMAGE_NAME =
            DockerImageName.parse("hivemq/hivemq4").withTag("latest");

    @Container
    final @NotNull HiveMQContainer hivemq = new HiveMQContainer(DOCKER_IMAGE_NAME) //
            .withExtension(MountableFile.forClasspathResource("hivemq-prometheus-extension"))
            .waitForExtension("Prometheus Monitoring Extension")
            .withExposedPorts(9399)
            .withExtension(HiveMQExtension.builder()
                    .mainClass(MyMetricsExtension.class)
                    .name("metrics-extension")
                    .id("metrics-extension")
                    .version("1.0.0")
                    .build())
            .waitForExtension("metrics-extension");

    @Test
    @Timeout(value = 5, unit = TimeUnit.MINUTES)
    void test_payload_modified() throws Exception {
        assertMetrics(Map.of(
                "myCounter",
                1,
                "myGauge",
                1,
                "myHistogram{quantile=\"0.5\",}",
                1,
                "myHistogram{quantile=\"0.75\",}",
                1,
                "myHistogram{quantile=\"0.95\",}",
                1,
                "myHistogram{quantile=\"0.99\",}",
                1,
                "myHistogram{quantile=\"0.999\",}",
                1,
                "myHistogram_count",
                1));

        System.out.println(getPrometheusMetrics());
    }

    private void assertMetrics(final @NotNull Map<String, Number> metrics) throws IOException {
        final Map<String, Float> prometheusMetrics = metrics.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().floatValue()));
        assertThat(getPrometheusMetrics()).containsAllEntriesOf(prometheusMetrics);
    }

    private @NotNull Map<String, Float> getPrometheusMetrics() throws IOException {
        //noinspection HttpUrlsUsage
        final Request request =
                new Request.Builder().url("http://" + hivemq.getHost() + ":" + hivemq.getMappedPort(9399) + "/metrics")
                        .build();

        final String responseString;
        try (final Response response = new OkHttpClient().newCall(request).execute()) {
            //noinspection ConstantConditions
            responseString = response.body().string();
        }

        return responseString.lines()
                .filter(s -> !s.startsWith("#"))
                .map(s -> s.split(" "))
                .map(splits -> Map.entry(splits[0], Float.parseFloat(splits[1])))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, Float::max));
    }

    public static class MyMetricsExtension implements ExtensionMain {

        @Override
        public void extensionStart(
                final @NotNull ExtensionStartInput extensionStartInput,
                final @NotNull ExtensionStartOutput extensionStartOutput) {

            final MetricRegistry metricRegistry = Services.metricRegistry();

            metricRegistry.counter("myCounter").inc();
            metricRegistry.gauge("myGauge", () -> () -> 1.0f);
            metricRegistry.histogram("myHistogram").update(1);

        }

        @Override
        public void extensionStop(
                final @NotNull ExtensionStopInput extensionStopInput,
                final @NotNull ExtensionStopOutput extensionStopOutput) {

        }
    }
}