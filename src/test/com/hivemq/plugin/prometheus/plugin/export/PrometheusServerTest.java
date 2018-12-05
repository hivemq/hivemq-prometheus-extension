package com.hivemq.plugin.prometheus.plugin.export;

import com.codahale.metrics.MetricRegistry;
import com.hivemq.plugin.api.parameter.PluginInformation;
import com.hivemq.plugin.prometheus.plugin.configuration.PrometheusPluginConfiguration;
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
        prometheusServer = new PrometheusServer(prometheusPluginConfiguration, new MetricRegistry());
        when(prometheusPluginConfiguration.hostIp()).thenReturn(host);
        when(prometheusPluginConfiguration.port()).thenReturn(port);
        when(prometheusPluginConfiguration.metricPath()).thenReturn(path);
        prometheusServer.start();
        URL url = new URL("http://" + host + ":" + port + path);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        assertEquals(200, con.getResponseCode());
    }

    @Test(expected = ConnectException.class)
    public void test_stop_running_server() throws Exception {
        prometheusServer = new PrometheusServer(prometheusPluginConfiguration, new MetricRegistry());
        when(prometheusPluginConfiguration.hostIp()).thenReturn(host);
        when(prometheusPluginConfiguration.port()).thenReturn(port);
        when(prometheusPluginConfiguration.metricPath()).thenReturn(path);
        prometheusServer.start();
        URL url = new URL("http://" + host + ":" + port + path);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        assertEquals(200, con.getResponseCode());
        prometheusServer.stop();
        HttpURLConnection con2 = (HttpURLConnection) url.openConnection();
        con2.setRequestMethod("GET");
        con2.getResponseCode(); //triggers the exception
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