package com.lsit.dfds.utils;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.lsit.dfds.dto.KycEnvelope;
import com.lsit.dfds.dto.PanVerifyResult;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PanKycClient {

	@Value("${kyc.base-url}")
	private String baseUrl;
	@Value("${kyc.client-id}")
	private String clientId;
	@Value("${kyc.client-secret}")
	private String clientSecret;
	private final WebClient.Builder webClientBuilder;

	private WebClient client() {
		return webClientBuilder.build();
	}

	public PanVerifyResult verifyPan(String pan) {
		var env = client().post().uri(baseUrl + "kyc/external/panDataFetch").header("clientId", clientId)
				.header("clientSecret", clientSecret).bodyValue(Map.of("pan", pan)).retrieve()
				.onStatus(HttpStatusCode::isError,
						r -> r.bodyToMono(String.class)
								.map(s -> new RuntimeException("PAN API HTTP " + r.statusCode() + ": " + s)))
				.bodyToMono(KycEnvelope.class).block();

		if (env == null)
			throw new RuntimeException("PAN API returned empty response");
		return new PanVerifyResult(env.isOk(), env.getResponseCode(), env.getResponseMessage(), env.getData());
	}
}