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

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extensions.prometheus.configuration.PrometheusExtensionConfiguration;
import io.prometheus.client.Collector;
import io.prometheus.client.dropwizard.samplebuilder.SampleBuilder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class PrometheusSampleBuilder implements SampleBuilder {

    private static final Logger log = LoggerFactory.getLogger(PrometheusSampleBuilder.class);

    private String suffix = "";
    private final List<String> labelNames = new ArrayList<>();
    private final List<String> labelValues= new ArrayList<>();

    public PrometheusSampleBuilder(@NotNull final PrometheusExtensionConfiguration extensionConfiguration) {
        setSuffix(extensionConfiguration.suffix());
        setLabels(extensionConfiguration.labels());
    }

    @Override
    public Collector.MetricFamilySamples.Sample createSample(
            final String dropwizardName,
            final String nameSuffix,
            final List<String> additionalLabelNames,
            final List<String> additionalLabelValues,
            final double value) {
        return new Collector.MetricFamilySamples.Sample(
                Collector.sanitizeMetricName(dropwizardName + suffix), labelNames, labelValues, value
        );
    }

    private void setSuffix(final String confSuffix) {
        if (confSuffix != null && confSuffix.length() > 0) {
            this.suffix = "." + confSuffix;
        }
    }

    private void setLabels(final String labels) {
        if (labels != null && labels.length() > 2) {

            final String[] splitLabels = StringUtils.splitPreserveAllTokens(labels, ";");

            for (final String label : splitLabels) {
                final String[] labelPair = StringUtils.split(label, "=");
                if (labelPair.length != 2 || labelPair[0].length() < 1 || labelPair[1].length() < 1) {
                    log.info("Skipping invalid label '{}' for Prometheus in labels '{}'.", label, labels);
                } else {
                    labelNames.add(StringUtils.trim(labelPair[0]));
                    labelValues.add(StringUtils.trim(labelPair[1]));
                }
            }
        }
    }
}