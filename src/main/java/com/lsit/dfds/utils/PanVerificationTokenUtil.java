package com.lsit.dfds.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class PanVerificationTokenUtil {

	@Value("${verify-pan.token-secret}")
	private String secret;
	@Value("${verify-pan.ttl-seconds:900}")
	private long ttlSeconds;

	private final ObjectMapper mapper = new ObjectMapper();

	public String issueToken(String pan) {
		try {
			long exp = Instant.now().getEpochSecond() + ttlSeconds;
			var payload = Map.of("pan", pan.trim().toUpperCase(), "exp", exp);
			String json = mapper.writeValueAsString(payload);
			String b64 = base64Url(json.getBytes(StandardCharsets.UTF_8));
			String sig = sign(b64);
			return b64 + "." + sig;
		} catch (Exception e) {
			throw new RuntimeException("Failed to issue PAN token", e);
		}
	}

	public boolean validateToken(String token, String pan) {
		try {
			if (token == null || !token.contains("."))
				return false;
			String[] parts = token.split("\\.", 2);
			String b64 = parts[0];
			String sig = parts[1];
			if (!sign(b64).equals(sig))
				return false;

			byte[] json = Base64.getUrlDecoder().decode(b64);
			Map<?, ?> payload = mapper.readValue(json, Map.class);

			String tp = String.valueOf(payload.get("pan")).toUpperCase();
			long exp = Long.parseLong(String.valueOf(payload.get("exp")));
			long now = Instant.now().getEpochSecond();
			return tp.equals(pan.trim().toUpperCase()) && now <= exp;
		} catch (Exception e) {
			return false;
		}
	}

	private String sign(String data) throws Exception {
		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
		return base64Url(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
	}

	private static String base64Url(byte[] b) {
		return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
	}
}
