package com.nervatech.nerva_build.security;

import com.nervatech.nerva_build.service.AppUserAuthService;
import com.nervatech.nerva_build.service.CustomerAccountAuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AppUserAuthService appUserAuthService;
    private final CustomerAccountAuthService customerAccountAuthService;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            AppUserAuthService appUserAuthService,
            CustomerAccountAuthService customerAccountAuthService
    ) {
        this.jwtService = jwtService;
        this.appUserAuthService = appUserAuthService;
        this.customerAccountAuthService = customerAccountAuthService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            if (jwtService.isTokenValid(token) && SecurityContextHolder.getContext().getAuthentication() == null) {
                Long userId = jwtService.extractUserId(token);
                String principalType = jwtService.extractPrincipalType(token);
                UserDetails userDetails = JwtService.PRINCIPAL_TYPE_CUSTOMER.equals(principalType)
                        ? customerAccountAuthService.loadById(userId)
                        : appUserAuthService.loadById(userId);

                UsernamePasswordAuthenticationToken authenticationToken =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }
        } catch (Exception ignored) {
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
