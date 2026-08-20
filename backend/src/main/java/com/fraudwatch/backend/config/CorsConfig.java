package com.fraudwatch.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Allows Daniel's dashboard to call this API from the browser.
 * <p>
 * Browsers enforce the "same-origin policy": JavaScript running on one
 * origin (scheme + host + port — e.g. {@code http://localhost:5173} for
 * the dashboard's dev server) is blocked by the browser itself from
 * reading responses from a different origin (e.g. this API on
 * {@code http://localhost:8080}), unless the server explicitly says
 * it's OK via CORS ("Cross-Origin Resource Sharing") response headers.
 * Without this config, the backend still runs the request server-side,
 * but the browser throws the response away before the dashboard's code
 * ever sees it — that's the "blocked" behavior we were seeing.
 * <p>
 * Everyone on the team runs the dashboard locally on their own machine
 * (Vite can pick a different port each time), so rather than hardcode
 * one exact origin, we allow any localhost/127.0.0.1 origin regardless
 * of port. {@code allowedOriginPatterns} (not {@code allowedOrigins}) is
 * what lets us use a wildcard like {@code :*} for the port here.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*")
                .allowedMethods("GET", "POST")
                .allowedHeaders("*");
    }
}
