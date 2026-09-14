package com.romi.mogumogu.config;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

        /** 建立 Overpass API 用的 RestClient */
        @Bean
        public RestClient overpassRestClient() {

                // 設定連線逾時
                HttpClient httpClient = HttpClient.newBuilder()
                                .connectTimeout(Duration.ofSeconds(8))
                                .build();

                // 設定讀取逾時
                JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);

                requestFactory.setReadTimeout(Duration.ofSeconds(35));

                return RestClient.builder()
                                .requestFactory(requestFactory)
                                .defaultHeader(
                                                "User-Agent",
                                                "MoguMogu/1.0")
                                .build();
        }
}