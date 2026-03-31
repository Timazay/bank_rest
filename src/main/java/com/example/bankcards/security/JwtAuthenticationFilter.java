package com.example.bankcards.security;

import com.example.bankcards.entity.enums.TokenType;
import com.example.bankcards.util.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NotNull HttpServletResponse response, @NotNull FilterChain filterChain)
            throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            doFilter(request, response, filterChain, authHeader);
        } catch (Exception e) {
            getErrorMessage(response, e);
        }
    }

    private void doFilter(HttpServletRequest request,
                          HttpServletResponse response,
                          FilterChain filterChain,
                          String authHeader) throws ServletException, IOException {
        final String jwt = authHeader.substring(7);

        final String email = jwtUtils.extractEmail(jwt, TokenType.ACCESS);

        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = getUserDetails(email);

            if (jwtUtils.validateToken(jwt, TokenType.ACCESS)) {
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }

    private UserDetails getUserDetails(String email) {
        return userDetailsService.loadUserByUsername(email);
    }

    private void getErrorMessage(HttpServletResponse response, Exception e) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(String.format(
                "{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"%s\"}",
                e.getMessage()
        ));
        response.getWriter().flush();
    }
}
