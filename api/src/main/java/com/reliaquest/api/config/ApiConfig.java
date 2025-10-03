package com.reliaquest.api.config;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class ApiConfig {

    @Bean
    public RestTemplate restTemplate() {
        // Connection pooling for better scalability
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(100);
        connectionManager.setDefaultMaxPerRoute(20);
        
        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(
                        org.apache.hc.client5.http.config.RequestConfig.custom()
                                .setConnectionRequestTimeout(Timeout.ofSeconds(2))
                                .setConnectTimeout(Timeout.ofSeconds(5))
                                .setResponseTimeout(Timeout.ofSeconds(10))
                                .build()
                )
                .build();
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
        return new RestTemplate(factory);
    }

    @Bean
    public String serverBaseUrl(@Value("${server.base-url}") String baseUrl) {
        return baseUrl;
    }
}



