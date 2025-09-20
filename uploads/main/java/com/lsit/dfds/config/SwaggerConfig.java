package com.lsit.dfds.config;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class SwaggerConfig {

	@Bean
	public OpenAPI customOpenAPI() {
		return new OpenAPI().info(new Info().title("DFDS API").description("API documentation for DFDS service") // counts
																													// will
																													// be
																													// appended
																													// below
				.version("1.0.0"));
	}

	/**
	 * Appends API statistics (paths, operations, per-method) into the
	 * Info.description so it's visible on the Swagger UI page.
	 */
	@Bean
	public OpenApiCustomizer apiCountCustomizer() {
		return openApi -> {
			var paths = Optional.ofNullable(openApi.getPaths()).orElseGet(io.swagger.v3.oas.models.Paths::new);

			int pathCount = paths.size();
			int opCount = paths.values().stream().mapToInt(p -> p.readOperations().size()).sum();

			// Per-HTTP method breakdown
			Map<String, Long> byMethod = paths.values().stream().flatMap(p -> p.readOperationsMap().entrySet().stream())
					.collect(Collectors.groupingBy(e -> e.getKey().name(), // GET, POST, etc.
							Collectors.counting()));

			// Build a Markdown block (Swagger UI renders it)
			StringBuilder stats = new StringBuilder();
			stats.append("\n\n---\n").append("### API Statistics\n").append("- **Paths**: ").append(pathCount)
					.append("\n").append("- **Operations**: ").append(opCount).append("\n");

			if (!byMethod.isEmpty()) {
				stats.append("\n**By Method:**\n");
				byMethod.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(
						e -> stats.append("- **").append(e.getKey()).append("**: ").append(e.getValue()).append("\n"));
			}
			stats.append("\n_Last updated at startup; reflects current OpenAPI._\n");

			// Append to existing description
			Info info = Optional.ofNullable(openApi.getInfo()).orElseGet(Info::new);
			String baseDesc = Optional.ofNullable(info.getDescription()).orElse("");
			info.setDescription(baseDesc + stats.toString());
			openApi.setInfo(info);
		};
	}
}
