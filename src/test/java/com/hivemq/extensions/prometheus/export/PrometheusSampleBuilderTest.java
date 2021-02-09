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
import io.prometheus.client.Collector;
import io.prometheus.client.dropwizard.DropwizardExports;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

public class PrometheusSampleBuilderTest {

    private PrometheusExtensionConfiguration config;

    @Before
    public void setup() {
        config = Mockito.mock(PrometheusExtensionConfiguration.class);
    }

    @Test
    public void test_add_one_label() {
        Mockito.when(config.labels()).thenReturn("host_ip=1.2.3.4");
        final Collector.MetricFamilySamples.Sample sample = getSampleForName("test");
        assertEquals("name incorrect", Collections.singletonList("host_ip"), sample.labelNames);
        assertEquals("value incorrect", Collections.singletonList("1.2.3.4"), sample.labelValues);
    }

    @Test
    public void test_add_two_labels() {
        Mockito.when(config.labels()).thenReturn("host_ip=1.2.3.4;scp=some.longer.name");
        final Collector.MetricFamilySamples.Sample sample = getSampleForName("test");
        assertEquals("names incorrect", Arrays.asList("host_ip", "scp"), sample.labelNames);
        assertEquals("values incorrect", Arrays.asList("1.2.3.4", "some.longer.name"), sample.labelValues);
    }

    @Test
    public void test_labels_with_trailing_semicolon() {
        Mockito.when(config.labels()).thenReturn("host_ip=1.2.3.4;");
        final Collector.MetricFamilySamples.Sample sample = getSampleForName("test");
        assertEquals("name incorrect", Collections.singletonList("host_ip"), sample.labelNames);
        assertEquals("value incorrect", Collections.singletonList("1.2.3.4"), sample.labelValues);
    }

    @Test
    public void test_invalid_labels() {
        Mockito.when(config.labels()).thenReturn("host_ip=;");
        final Collector.MetricFamilySamples.Sample sample = getSampleForName("test");
        assertEquals("name should be empty", Collections.EMPTY_LIST, sample.labelNames);
        assertEquals("value should be empty", Collections.EMPTY_LIST, sample.labelValues);
    }

    @Test
    public void test_empty_labels() {
        Mockito.when(config.labels()).thenReturn("");
        final Collector.MetricFamilySamples.Sample sample = getSampleForName("test");
        assertEquals("name should be empty", Collections.EMPTY_LIST, sample.labelNames);
        assertEquals("value should be empty", Collections.EMPTY_LIST, sample.labelValues);
    }

    @Test
    public void test_chinese_labels() {
        Mockito.when(config.labels()).thenReturn("的=不");
        final Collector.MetricFamilySamples.Sample sample = getSampleForName("test");
        assertEquals("name incorrect", Collections.singletonList("的"), sample.labelNames);
        assertEquals("value incorrect", Collections.singletonList("不"), sample.labelValues);
    }

    @Test
    public void test_labels_with_leading_whitespace() {
        Mockito.when(config.labels()).thenReturn("            host_ip=1.2.3.4");
        final Collector.MetricFamilySamples.Sample sample = getSampleForName("test");
        assertEquals("name incorrect", Collections.singletonList("host_ip"), sample.labelNames);
        assertEquals("value incorrect", Collections.singletonList("1.2.3.4"), sample.labelValues);
    }

    @Test
    public void test_suffix_for_name() {
        final String metricName = "test.name";
        final String suffix = "suffix";
        Mockito.when(config.suffix()).thenReturn(suffix);
        assertEquals("Metric name incorrect", "test_name_suffix", getSampleForName(metricName).name);
    }

    @Test
    public void test_random_suffix_for_name() {
        final String metricName = "test.name";
        final String suffix = RandomStringUtils.random(55, true, true);
        Mockito.when(config.suffix()).thenReturn(suffix);
        assertEquals("Metric name incorrect", "test_name_" + suffix, getSampleForName(metricName).name);
    }

    @Test
    public void test_empty_suffix() {
        final String metricName = "test.name";
        Mockito.when(config.suffix()).thenReturn("");
        assertEquals("Metric name incorrect", "test_name", getSampleForName(metricName).name);
    }

    @Test
    public void test_suffix_with_period() {
        final String metricName = "test.name";
        final String suffix = ".test";
        Mockito.when(config.suffix()).thenReturn(suffix);
        assertEquals("Metric name incorrect", "test_name__test", getSampleForName(metricName).name);
    }

    @Test
    public void test_adding_suffix_to_meter() {
        final String suffix = "cluster1";
        when(config.suffix()).thenReturn(suffix);

        final MetricRegistry registry = new MetricRegistry();
        registry.meter("testing.meter");

        final String name = new DropwizardExports(registry, new PrometheusSampleBuilder(config)).collect().get(0).name;
        assertEquals("metric name incorrect", "testing_meter_total_" + suffix, name);
    }

    @Test
    public void test_metrics_compare_histogram() {
        final MetricRegistry registry = new MetricRegistry();
        registry.histogram("testing.histogram");
        compareQuantileSamples(registry);
    }

    @Test
    public void test_metrics_compare_timer() {
        final MetricRegistry registry = new MetricRegistry();
        registry.timer("testing.timer");
        compareQuantileSamples(registry);
    }

    @Test
    public void test_quantile_metrics_with_labels() {
        when(config.labels()).thenReturn("hostname=test.invalid.domain");

        final MetricRegistry registry = new MetricRegistry();
        registry.histogram("testing.histogram");

        final List<Collector.MetricFamilySamples.Sample> labeledSamples =
                new DropwizardExports(registry, new PrometheusSampleBuilder(config)).collect().get(0).samples;

        assertEquals("Number of labels incorrect", 2, labeledSamples.get(0).labelNames.size());

        for (final Collector.MetricFamilySamples.Sample sample :  labeledSamples) {
            if (sample.name.endsWith("_count")) {
                //quantile_count sample
                assertEquals("names not matching", List.of("hostname"), sample.labelNames);
            } else {
                assertEquals("sample not matching", List.of("quantile", "hostname"), sample.labelNames);
            }
        }
    }

    private Collector.MetricFamilySamples.Sample getSampleForName(final String metricName) {
        final PrometheusSampleBuilder builder = new PrometheusSampleBuilder(config);
        return builder.createSample(metricName, null,
                null, null, 1.0);
    }

    private void compareQuantileSamples(final MetricRegistry registry) {
        when(config.labels()).thenReturn("");

        final List<Collector.MetricFamilySamples.Sample> defaultSamples =
                new DropwizardExports(registry).collect().get(0).samples;
        final List<Collector.MetricFamilySamples.Sample> labeledSamples =
                new DropwizardExports(registry, new PrometheusSampleBuilder(config)).collect().get(0).samples;

        assertEquals("Number of samples incorrect", defaultSamples.size(), labeledSamples.size());
        assertEquals("Number of labels incorrect", defaultSamples.get(0).labelNames.size(), labeledSamples.get(0).labelNames.size());

        for (int i = 0; i < defaultSamples.size(); i++) {
            assertEquals("sample not matching", defaultSamples.get(i), labeledSamples.get(i));
        }
    }
}