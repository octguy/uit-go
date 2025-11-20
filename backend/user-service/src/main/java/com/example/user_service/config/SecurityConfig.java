package com.example.user_service.config;

import com.example.user_service.jwt.JwtAuthenticationEntryPoint;
import com.example.user_service.jwt.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationEntryPoint authenticationEntryPoint,
                          JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

//    @Bean
//    public CorsConfigurationSource corsConfigurationSource() {
//        CorsConfiguration configuration = new CorsConfiguration();
//        configuration.setAllowedOrigins(List.of(frontendURL));
//        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
//        configuration.setAllowedHeaders(List.of("*"));
//        configuration.setAllowCredentials(true);
//        configuration.setMaxAge(3600L);
//
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//        source.registerCorsConfiguration("/**", configuration);
//        return source;
//    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .cors(AbstractHttpConfigurer::disable)  // tắt CORS
                .csrf(AbstractHttpConfigurer::disable)  // tắt CSRF
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll() // cho phép tất cả API
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                );

        // vẫn để filter JWT (nếu muốn giữ)
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }


    /**
     * Key changes:
     *
     *
     * Added corsConfigurationSource() bean that explicitly configures CORS
     * Added .cors(cors -> cors.configurationSource(corsConfigurationSource())) to the security filter chain before .csrf()
     * This ensures Spring Security applies CORS settings before checking authentication
     * Why /api/auth worked but /api/tasks didn't:
     *
     *
     * /api/auth/** is permitted for all requests (no authentication required)
     * /api/tasks/** requires authentication, so Spring Security intercepts the request
     * Without explicit CORS configuration in Security, the preflight OPTIONS request is blocked
     * The WebConfig CORS settings are applied at the MVC layer, but Spring Security filters run before that, which is why you need CORS configuration in both places or just in SecurityConfig.
     */
}
