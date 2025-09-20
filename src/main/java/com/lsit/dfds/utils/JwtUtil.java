package com.lsit.dfds.utils;

import java.security.Key;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;

@Component
public class JwtUtil {

	@Value("${jwt.secret}")
	private String secret;

	private Key key;
	private final Map<String, Long> blacklist = new ConcurrentHashMap<>();

	private final long EXPIRATION_TIME = 1800000L; // 30 minutes

	@PostConstruct
	public void init() {
		if (secret == null || secret.length() < 32) {
			throw new IllegalArgumentException("JWT secret must be at least 32 characters long");
		}
		this.key = Keys.hmacShaKeyFor(secret.getBytes());
	}

	public String generateToken(String username) {
		return Jwts.builder().setSubject(username).setIssuedAt(new Date())
				.setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
				.signWith(key, SignatureAlgorithm.HS256).compact();
	}

	public String extractUsername(String token) {
		return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody().getSubject();
	}

	public void blacklistToken(String token) {
		Date expiration = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody().getExpiration();
		blacklist.put(token, expiration.getTime());
	}

	public boolean isBlacklisted(String token) {
		Long expiry = blacklist.get(token);
		if (expiry == null)
			return false;
		if (expiry <= System.currentTimeMillis()) {
			blacklist.remove(token); // auto-clean
			return false;
		}
		return true;
	}

	public String extractUsernameFromRequest(HttpServletRequest request) {
		final String authHeader = request.getHeader("Authorization");
		if (authHeader != null && authHeader.startsWith("Bearer ")) {
			return extractUsername(authHeader.substring(7));
		}
		return null;
	}

	public boolean validateToken(String token, UserDetails userDetails) {
		final String username = extractUsername(token);
		return (username.equals(userDetails.getUsername()) && !isTokenExpired(token) && !isBlacklisted(token));
	}

	private boolean isTokenExpired(String token) {
		final Date expiration = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody()
				.getExpiration();
		return expiration.before(new Date());
	}
}