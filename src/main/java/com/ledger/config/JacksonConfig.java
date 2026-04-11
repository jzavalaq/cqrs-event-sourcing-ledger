package com.ledger.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Jackson JSON configuration.
 * <p>
 * Configures the ObjectMapper for proper handling of Java 8+ time types
 * and lenient deserialization.
 * </p>
 */
@Configuration
public class JacksonConfig {

    /**
     * Creates the primary ObjectMapper bean.
     * <p>
     * Configuration:
     * <ul>
     *   <li>JavaTimeModule for Instant, LocalDateTime, etc.</li>
     *   <li>Dates serialized as ISO-8601 strings, not timestamps</li>
     *   <li>Unknown properties ignored during deserialization</li>
     * </ul>
     * </p>
     *
     * @return configured ObjectMapper
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return mapper;
    }
}
