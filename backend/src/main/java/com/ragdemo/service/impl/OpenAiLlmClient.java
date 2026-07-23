package com.ragdemo.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ragdemo.service.LlmClient;
import com.ragdemo.service.LlmMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * OpenAI 兼容的大模型客户端（M2）。
 * 使用 JDK 自带的 java.net.http.HttpClient 调用 /chat/completions（stream=true），
 * 逐行读取 SSE 流（data: {...}），解析 choices[0].delta.content 增量并回调 onDelta。
 * 选用 JDK HttpClient 而非 RestTemplate，是因为 RestTemplate 不便于按块读取流式响应体。
 *
 * 配置项（application.yml 的 llm.api.*）：base-url 须以 /v1 结尾（代码会拼 /chat/completions），
 * key 走环境变量 LLM_API_KEY（兜底 EMBEDDING_API_KEY），不硬编码。
 */
@Service
public class OpenAiLlmClient implements LlmClient {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Value("${llm.api.base-url}")
    private String baseUrl;

    @Value("${llm.api.key}")
    private String apiKey;

    @Value("${llm.api.model}")
    private String model;

    @Override
    public boolean available() {
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public void streamGenerate(List<LlmMessage> messages, Consumer<String> onDelta) throws Exception {
        if (!available()) {
            throw new IllegalStateException("LLM API key 未配置，无法调用远程大模型");
        }
        // base-url 形如 https://api.deepseek.com/v1，拼接为 /chat/completions
        String url = baseUrl.endsWith("/") ? baseUrl + "chat/completions" : baseUrl + "/chat/completions";

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", messages,
                "stream", true,
                "temperature", 0.3
        );
        String json = objectMapper.writeValueAsString(body);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMinutes(2))
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpResponse<java.io.InputStream> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (resp.statusCode() != 200) {
            // 非 200：读错误体便于排查（如 key 无效、模型不存在、余额不足）
            String errBody = new String(resp.body().readAllBytes(), StandardCharsets.UTF_8);
            throw new IllegalStateException("LLM 接口返回 " + resp.statusCode() + "：" + errBody);
        }

        // 逐行读取 SSE：每行形如 "data: {...}"，以 "[DONE]" 结束
        try (java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(resp.body(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("data:")) {
                    continue;
                }
                String data = line.substring(5).trim();
                if (data.isEmpty() || "[DONE]".equals(data)) {
                    if ("[DONE]".equals(data)) {
                        break;
                    }
                    continue;
                }
                try {
                    JsonNode node = objectMapper.readTree(data);
                    JsonNode delta = node.path("choices").path(0).path("delta").path("content");
                    if (delta.isTextual() && !delta.asText().isEmpty()) {
                        onDelta.accept(delta.asText());
                    }
                } catch (Exception e) {
                    // 个别控制行（如注释、心跳）解析失败则跳过，不影响主流程
                }
            }
        }
    }
}
