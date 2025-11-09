package com.prismnetai.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RoutingConfig {

    @Value("${prismnet.routing.metrics.lookback-period:365d}")
    private String metricsLookbackPeriod;

    @Bean
    public Duration metricsLookbackDuration() {
        return parseDuration(metricsLookbackPeriod);
    }

    private Duration parseDuration(String durationStr) {
        if (durationStr == null || durationStr.trim().isEmpty()) {
            return Duration.ofDays(365); // Default to 1 year
        }

        durationStr = durationStr.trim().toLowerCase();

        if (durationStr.endsWith("d")) {
            long days = Long.parseLong(durationStr.substring(0, durationStr.length() - 1));
            return Duration.ofDays(days);
        } else if (durationStr.endsWith("h")) {
            long hours = Long.parseLong(durationStr.substring(0, durationStr.length() - 1));
            return Duration.ofHours(hours);
        } else if (durationStr.endsWith("m")) {
            long minutes = Long.parseLong(durationStr.substring(0, durationStr.length() - 1));
            return Duration.ofMinutes(minutes);
        } else {
            // Assume days if no suffix
            long days = Long.parseLong(durationStr);
            return Duration.ofDays(days);
        }
    }
}