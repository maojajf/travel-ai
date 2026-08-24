package com.travel.backend.service;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson2.JSONObject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.backend.config.AiProperties;
import com.travel.backend.domain.dto.RouteGenerateDTO;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.travel.backend.domain.model.KvConfig;
import com.travel.backend.domain.vo.AiConfigVO;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AiRouteStreamClient {

    private final ObjectMapper objectMapper;
    private final AiProperties aiProperties;
    private final RoutePromptBuilder routePromptBuilder;
    private final KvConfigService kvConfigService;

    public AiRouteStreamClient(
        ObjectMapper objectMapper,
        AiProperties aiProperties,
        RoutePromptBuilder routePromptBuilder,
        KvConfigService kvConfigService
    ) {
        this.objectMapper = objectMapper;
        this.aiProperties = aiProperties;
        this.routePromptBuilder = routePromptBuilder;
        this.kvConfigService = kvConfigService;
    }

    public String generate(RouteGenerateDTO request, Consumer<String> onDelta) throws IOException, InterruptedException {
        KvConfig aiApiConfig = kvConfigService.getEntityByConfigKey("ai_api_config");
        if(ObjectUtil.isNotEmpty(aiApiConfig)){
            AiConfigVO aiConfig =  JSONObject.parseObject(aiApiConfig.getConfigValue(), AiConfigVO.class);
            aiProperties.setApiKey(aiConfig.getApikey());
            aiProperties.setBaseUrl(aiConfig.getBaseurl());
            aiProperties.setModel(aiConfig.getModel());
        }

        validateConfiguration();

        HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(aiProperties.getConnectTimeoutSeconds()))
            .build();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", aiProperties.getModel());
        payload.put("stream", true);
        payload.put("temperature", aiProperties.getTemperature());
        payload.put("max_tokens", aiProperties.getMaxTokens());
        payload.put("messages", List.of(
            buildMessage("system", routePromptBuilder.buildSystemPrompt()),
            buildMessage("user", routePromptBuilder.buildFormatGuardPrompt()),
            buildMessage("user", routePromptBuilder.buildUserPrompt(request))
        ));

        HttpRequest httpRequest = HttpRequest.newBuilder()
            .uri(URI.create(normalizeEndpoint(aiProperties.getBaseUrl())))
            .header("Content-Type", "application/json")
            .header("Accept", "text/event-stream")
            .header("Authorization", "Bearer " + aiProperties.getApiKey())
            .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
            .build();

        HttpResponse<java.util.stream.Stream<String>> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofLines());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("AI service request failed with status " + response.statusCode());
        }

        StringBuilder markdown = new StringBuilder();
        try (java.util.stream.Stream<String> lines = response.body()) {
            lines.forEach(line -> consumeSseLine(line, markdown, onDelta));
        }

        if (markdown.isEmpty()) {
            throw new IllegalStateException("AI service returned empty route content");
        }
        return markdown.toString();
    }

    private void consumeSseLine(String line, StringBuilder markdown, Consumer<String> onDelta) {
        if (!StringUtils.hasText(line) || !line.startsWith("data:")) {
            return;
        }

        String payload = line.substring(5).trim();
        if ("[DONE]".equals(payload)) {
            return;
        }

        try {
            JsonNode root = objectMapper.readTree(payload);
            JsonNode contentNode = root.path("choices").path(0).path("delta").path("content");
            if (contentNode.isTextual() && StringUtils.hasText(contentNode.asText())) {
                String chunk = contentNode.asText();
                markdown.append(chunk);
                onDelta.accept(chunk);
            }
        } catch (IOException ignored) {
            // Ignore malformed intermediate chunks from upstream.
        }
    }

    private void validateConfiguration() {
        if (!StringUtils.hasText(aiProperties.getBaseUrl()) || !StringUtils.hasText(aiProperties.getApiKey())) {
            throw new IllegalStateException("AI configuration is missing, please set ai.base-url and ai.api-key");
        }
    }

    private Map<String, Object> buildMessage(String role, String content) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", role);
        message.put("content", content);
        return message;
    }

    private String normalizeEndpoint(String baseUrl) {
        String trimmed = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return trimmed + "/chat/completions";
    }
}
