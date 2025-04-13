package com.library.management.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

@Configuration
@EnableJpaAuditing
public class AuditConfig {

    /**
     * Creates an AuditorAware bean that provides the current user for auditing.
     * In a real app, this would get the authenticated user.
     */
    @Bean
    public AuditorAware<String> auditorProvider() {
        // For now, just use "system" as the auditor
        return () -> Optional.of("system");
    }
}
