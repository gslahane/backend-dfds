package com.lsit.dfds.config;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import com.lsit.dfds.utils.JwtUtil;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

	// Endpoints that should bypass JWT auth entirely
	private static final String[] WHITELIST = { "/v3/api-docs/**", "/v3/api-docs.yaml", "/swagger-ui/**",
			"/swagger-ui.html", "/swagger-resources/**", "/webjars/**", "/api/auth/login", "/error", "/actuator/**" // optional
	};

	private final AntPathMatcher pathMatcher = new AntPathMatcher();

	@Autowired
	private JwtUtil jwtUtil;

	@Autowired
	private CustomUserDetailsService userDetailsService;

	private boolean isWhitelisted(HttpServletRequest request) {
		final String uri = request.getRequestURI();
		for (String pattern : WHITELIST) {
			if (pathMatcher.match(pattern, uri)) {
				return true;
			}
		}
		return false;
	}

	// Skip filter for CORS preflight and whitelisted endpoints
	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
		if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
			return true;
		}
		return isWhitelisted(request);
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {

		final String authHeader = request.getHeader("Authorization");

		// No token -> proceed without authentication; protected endpoints will be
		// blocked later by Security
		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			chain.doFilter(request, response);
			return;
		}

		final String jwt = authHeader.substring(7);
		try {
			String username = jwtUtil.extractUsername(jwt);

			if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
				UserDetails userDetails = userDetailsService.loadUserByUsername(username);
				if (jwtUtil.validateToken(jwt, userDetails)) {
					UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails,
							null, userDetails.getAuthorities());
					authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
					SecurityContextHolder.getContext().setAuthentication(authToken);
				}
			}
		} catch (Exception ex) {
			// Don't break the request on token parsing/validation errors;
			// Security config will still enforce auth for protected endpoints.
		}

		chain.doFilter(request, response);
	}
}
