package com.example.zolieapi.config;

import com.example.zolieapi.exception.ErrorCode;
import com.example.zolieapi.exception.ErrorResponse;
import com.example.zolieapi.security.TokenAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String API = WebConfig.API_PREFIX;

    private static final String[] PUBLIC_PATHS = {
            "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/actuator/health", "/ws/**", "/error"
    };

    private static final String[] GUEST_READ_PATHS = {
            API + "/feed/public",
            API + "/reels/feed",
            API + "/reels/*",
            API + "/reels/*/comments",
            API + "/posts/*",
            API + "/posts/*/comments",
            API + "/comments/*/replies",
            API + "/users/*",
            API + "/users/*/posts",
            API + "/users/*/reels",
            API + "/interests",
            API + "/usernames/*/availability"
    };

    private final TokenAuthenticationFilter tokenAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .requestMatchers(HttpMethod.GET, GUEST_READ_PATHS).permitAll()
                        .requestMatchers(HttpMethod.POST, API + "/auth/resolve-login").permitAll()
                        .requestMatchers(API + "/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint((request, response, e) -> write(response, ErrorCode.AUTH_REQUIRED))
                        .accessDeniedHandler((request, response, e) -> write(response, ErrorCode.FORBIDDEN)))
                .addFilterBefore(tokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            throw new UsernameNotFoundException(username);
        };
    }

    private void write(HttpServletResponse response, ErrorCode code) throws IOException {
        response.setStatus(code.getStatus().value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(ErrorResponse.of(code, code.getDefaultMessage()).toJson());
    }
}
