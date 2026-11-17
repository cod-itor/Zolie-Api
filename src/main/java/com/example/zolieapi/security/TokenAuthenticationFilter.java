package com.example.zolieapi.security;

import com.example.zolieapi.exception.ApiException;
import com.example.zolieapi.exception.ErrorResponse;
import com.example.zolieapi.model.User;
import com.example.zolieapi.model.enums.AccountStatus;
import com.example.zolieapi.model.enums.UserRole;
import com.example.zolieapi.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER = "Bearer ";

    private final TokenVerifier tokenVerifier;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER)) {
            chain.doFilter(request, response);
            return;
        }
        try {
            AuthUser authUser = authenticate(header.substring(BEARER.length()).trim());
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(authUser, null, authorities(authUser)));
        } catch (ApiException e) {
            SecurityContextHolder.clearContext();
            writeError(response, e);
            return;
        }
        chain.doFilter(request, response);
    }

    public AuthUser authenticate(String token) {
        VerifiedToken verified = tokenVerifier.verify(token);
        User user = userRepository.findByFirebaseUid(verified.uid()).orElse(null);
        if (user == null) {
            return new AuthUser(verified.uid(), verified.email(), verified.admin(), null, false);
        }
        boolean admin = verified.admin() || user.getRole() == UserRole.ADMIN;
        boolean suspended = user.getStatus() == AccountStatus.SUSPENDED;
        return new AuthUser(verified.uid(), verified.email(), admin, user.getId(), suspended);
    }

    private List<GrantedAuthority> authorities(AuthUser authUser) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        if (authUser.admin()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }
        return authorities;
    }

    private void writeError(HttpServletResponse response, ApiException e) throws IOException {
        response.setStatus(e.getCode().getStatus().value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(ErrorResponse.of(e.getCode(), e.getMessage()).toJson());
    }
}
