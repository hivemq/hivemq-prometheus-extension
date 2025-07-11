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
import com.codahale.metrics.Timer;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.MetricsServlet;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * This class extends the {@link MetricsServlet} by measuring the duration of the get-method and adds it to the
 * {@link MetricRegistry}.
 *
 * @author Daniel Krüger
 */
class MonitoredMetricServlet extends MetricsServlet {

    private static final long serialVersionUID = 3841226821748298393L;
    private static final @NotNull String METRIC_TOPIC = "get.time";

    private static final @NotNull Logger LOG = LoggerFactory.getLogger(MonitoredMetricServlet.class);

    private final @NotNull Timer responses;

    MonitoredMetricServlet(final @NotNull MetricRegistry metricRegistry) {
        super(CollectorRegistry.defaultRegistry);
        this.responses = metricRegistry.timer(name(MonitoredMetricServlet.class, METRIC_TOPIC));
    }

    @Override
    protected void doGet(final @NotNull HttpServletRequest req, final @NotNull HttpServletResponse resp) {
        LOG.debug("Received HTTP-Get-Request from Prometheus to scrape metrics from HiveMQ: {}", req);
        try (final var ignored = responses.time()) {
            super.doGet(req, resp);
        } catch (final Exception e) {
            LOG.warn("Exception occurred while collecting metrics and creating of Prometheus response: {}.",
                    e.getClass().getSimpleName());
            LOG.debug("Original exception was: ", e);
        }
    }
}
