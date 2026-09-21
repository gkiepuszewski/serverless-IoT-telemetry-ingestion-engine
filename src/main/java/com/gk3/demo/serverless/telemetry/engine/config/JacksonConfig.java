package com.gk3.demo.serverless.telemetry.engine.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers an explicit {@link ObjectMapper} bean with the {@link JavaTimeModule}.
 * <p>
 * Even though {@code spring-boot-starter-web} is on the classpath (added for the registration
 * REST API) and would auto-configure its own default {@code ObjectMapper}, this explicit
 * {@code @Bean} always wins over the auto-configured one (Spring Boot's auto-configuration is
 * {@code @ConditionalOnMissingBean}), guaranteeing {@code JavaTimeModule} is always registered.
 * This bean is also what {@code SqsListenerConfig} picks up for the SQS message converter -
 * without it, {@code SqsMessagingMessageConverter} would fall back to its own bare
 * {@code ObjectMapper} with no support for {@code java.time.Instant}, which was a real
 * deserialization bug caught in {@code @SqsListener} while hardening this PoC.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }
}
