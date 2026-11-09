package com.library.loan_service.config;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    // Builder padrão (sem load balancer). Usado por quem injeta RestClient.Builder
    // sem qualificador — incluindo o próprio cliente do Eureka, que precisa chamar
    // http://localhost:8761 diretamente, e não como nome de serviço.
    @Bean
    @Primary
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    // Builder com load balancer: resolve nomes de serviço do Eureka (ex.: http://CATALOG-SERVICE).
    @Bean
    @LoadBalanced
    public RestClient.Builder loadBalancedRestClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    public RestClient restClient(@LoadBalanced RestClient.Builder builder) {
        return builder.build();
    }
}
