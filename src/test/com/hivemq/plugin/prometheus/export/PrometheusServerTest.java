package com.hivemq.plugin.prometheus.export;

import com.codahale.metrics.MetricRegistry;
import com.hivemq.plugin.api.parameter.PluginInformation;
import com.hivemq.plugin.prometheus.configuration.PrometheusPluginConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

public class PrometheusServerTest {


    private PrometheusServer prometheusServer;

    @Mock
    PluginInformation pluginInformation;

    @Mock
    PrometheusPluginConfiguration prometheusPluginConfiguration;

    final String host = "localhost";
    int port;
    final String path = "/metrics";

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        port = createRandomPort();
    }

    @Test
    public void test_start_successful() throws Exception {
        when(prometheusPluginConfiguration.hostIp()).thenReturn(host);
        when(prometheusPluginConfiguration.port()).thenReturn(port);
        when(prometheusPluginConfiguration.metricPath()).thenReturn(path);
        prometheusServer = new PrometheusServer(prometheusPluginConfiguration, new MetricRegistry());
        prometheusServer.start();
        URL url = new URL("http://" + host + ":" + port + path);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        assertEquals(200, con.getResponseCode());
    }

    @Test
    public void test_stop_running_server() throws Exception {
        when(prometheusPluginConfiguration.hostIp()).thenReturn(host);
        when(prometheusPluginConfiguration.port()).thenReturn(port);
        when(prometheusPluginConfiguration.metricPath()).thenReturn(path);
        prometheusServer = new PrometheusServer(prometheusPluginConfiguration, new MetricRegistry());
        prometheusServer.start();
        URL url = new URL("http://" + host + ":" + port + path);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        assertEquals(200, con.getResponseCode());
        prometheusServer.stop();
    }


    private int createRandomPort() {
        try {
            final ServerSocket serverSocket = new ServerSocket(0);
            int port = serverSocket.getLocalPort();
            serverSocket.close();
            return port;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}