package br.ed.ufape.bahiabrindes.security;

import br.ed.ufape.bahiabrindes.model.entity.Cliente;
import br.ed.ufape.bahiabrindes.model.entity.Funcionario;
import br.ed.ufape.bahiabrindes.model.enums.TipoUsuario;
import br.ed.ufape.bahiabrindes.repository.ClienteRepository;
import br.ed.ufape.bahiabrindes.repository.FuncionarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@ConditionalOnBean(JwtUtil.class)
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final ObjectProvider<ClienteRepository> clienteRepositoryProvider;
    private final ObjectProvider<FuncionarioRepository> funcionarioRepositoryProvider;

    @Autowired
    public JwtAuthenticationFilter(JwtUtil jwtUtil,
                                   ObjectProvider<ClienteRepository> clienteRepositoryProvider,
                                   ObjectProvider<FuncionarioRepository> funcionarioRepositoryProvider) {
        this.jwtUtil = jwtUtil;
        this.clienteRepositoryProvider = clienteRepositoryProvider;
        this.funcionarioRepositoryProvider = funcionarioRepositoryProvider;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                String email = jwtUtil.extractEmail(token);
                if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    TipoUsuario tipoUsuario = jwtUtil.extractTipoUsuario(token);
                    UsernamePasswordAuthenticationToken authentication = buildAuthentication(token, email, tipoUsuario);
                    if (authentication != null) {
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            } catch (Exception ignored) {
                // Token inválido/expirado: deixa cair para o AuthenticationEntryPoint (401) quando necessário.
            }
        }

        filterChain.doFilter(request, response);
    }

    private UsernamePasswordAuthenticationToken buildAuthentication(String token, String email, TipoUsuario tipoUsuario) {
        if (!Boolean.TRUE.equals(jwtUtil.validateToken(token, email))) {
            return null;
        }

        List<SimpleGrantedAuthority> authorities;

        if (tipoUsuario == TipoUsuario.CLIENTE) {
            ClienteRepository clienteRepository = clienteRepositoryProvider.getIfAvailable();
            if (clienteRepository == null) {
                return null;
            }

            Cliente cliente = clienteRepository.findByEmailAndAtivoTrue(email).orElse(null);
            if (cliente == null) {
                return null;
            }

            authorities = List.of(new SimpleGrantedAuthority("ROLE_CLIENTE"));
        } else {
            FuncionarioRepository funcionarioRepository = funcionarioRepositoryProvider.getIfAvailable();
            if (funcionarioRepository == null) {
                return null;
            }

            Funcionario funcionario = funcionarioRepository.findByEmailAndAtivoTrue(email).orElse(null);
            if (funcionario == null) {
                return null;
            }

            authorities = funcionario.getPerfis().stream()
                    .map(perfil -> new SimpleGrantedAuthority("ROLE_" + perfil.getNome()))
                    .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
            authorities.add(new SimpleGrantedAuthority("ROLE_FUNCIONARIO"));
        }

        return new UsernamePasswordAuthenticationToken(email, null, authorities);
    }
}

