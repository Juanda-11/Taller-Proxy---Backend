package com.taller.proxy.config;

import com.taller.proxy.proxy.LoggingProxy;
import com.taller.proxy.proxy.MicroserviceProxy;
import com.taller.proxy.proxy.ProxyRegistry;
import com.taller.proxy.service.InventoryService;
import com.taller.proxy.service.OrderService;
import com.taller.proxy.service.PaymentService;
import com.taller.proxy.store.LogStore;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class ProxyConfig implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origins:*}")
    private String allowedOrigins;

    /** Cada servicio real queda detras de su propio LoggingProxy. */
    @Bean
    public ProxyRegistry proxies(InventoryService inventory, OrderService orders,
                                                          PaymentService payments, LogStore store) {
        Map<String, MicroserviceProxy<Object>> map = new LinkedHashMap<>();
        map.put("inventory", new LoggingProxy<>("inventory", inventory, store));
        map.put("orders", new LoggingProxy<>("orders", orders, store));
        map.put("payments", new LoggingProxy<>("payments", payments, store));
        return new ProxyRegistry(map);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(allowedOrigins.split("\\s*,\\s*"))
                .allowedMethods("GET", "POST", "OPTIONS");
    }
}
