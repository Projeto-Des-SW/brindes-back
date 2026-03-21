package br.ed.ufape.bahiabrindes.config;

import br.ed.ufape.bahiabrindes.security.JwtAuthenticationFilter;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:5173",
            "http://localhost:3000",
            "http://localhost:8080",
            "http://brindes-front-deploy.vercel.app",
            "https://brindes-front-deploy.vercel.app",
            "https://brindes-front.onrender.com"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "Accept",
            "Origin",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers"
        ));
        configuration.setExposedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "Content-Disposition"
        ));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .securityContext(context -> context.requireExplicitSave(false))
            .authorizeHttpRequests(auth -> auth

                // SWAGGER
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()

                // PUBLICO
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/clientes").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/produtos", "/api/produtos/**").permitAll()
                .requestMatchers("/uploads/**").permitAll()

                // CLIENTE
                .requestMatchers("/api/clientes/me/**").hasRole("CLIENTE")

                // FUNCIONARIO + ADMIN
                .requestMatchers("/api/meu-perfil").hasAnyRole("FUNCIONARIO", "ADMIN")
                .requestMatchers("/api/clientes/**").hasAnyRole("FUNCIONARIO", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/funcionarios").hasRole("ADMIN")
                .requestMatchers("/api/orcamentos/admin/**").hasAnyRole("FUNCIONARIO", "ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/orcamentos/*/status").hasAnyRole("FUNCIONARIO", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/orcamentos/*/artes").hasAnyRole("FUNCIONARIO", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/orcamentos/*/notificar/**").hasAnyRole("FUNCIONARIO", "ADMIN")
                .requestMatchers("/api/produtos/**").hasAnyRole("FUNCIONARIO", "ADMIN")
                .requestMatchers("/api/estoque/**").hasAnyRole("FUNCIONARIO", "ADMIN")

                .anyRequest().authenticated()
            )
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(customAuthenticationEntryPoint)
            );

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}