package com.ragdemo.controller;

import com.ragdemo.common.Result;
import com.ragdemo.dto.request.ChatRequest;
import com.ragdemo.dto.response.ChatSubmitResponse;
import com.ragdemo.dto.response.ChatTaskResult;
import com.ragdemo.service.ChatTaskService;
import com.ragdemo.service.LlmClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "问答")
public class ChatController {

    private final ChatTaskService chatTaskService;
    private final LlmClient llmClient;

    public ChatController(ChatTaskService chatTaskService, LlmClient llmClient) {
        this.chatTaskService = chatTaskService;
        this.llmClient = llmClient;
    }

    @PostMapping("/chat")
    @Operation(summary = "提交问答（异步任务，M1 抽取式）")
    public Result<ChatSubmitResponse> chat(@Valid @RequestBody ChatRequest req) {
        int topK = req.getTopK() == null ? 5 : req.getTopK();
        if (topK < 1) {
            topK = 1;
        }
        if (topK > 10) {
            topK = 10;
        }
        // 提交即返回任务 ID，答案在后台生成，前端轮询 /chat/result 获取
        String taskId = chatTaskService.submit(req.getQuestion(), topK);
        ChatSubmitResponse resp = new ChatSubmitResponse();
        resp.setTaskId(taskId);
        return Result.success(resp);
    }

    @GetMapping("/chat/result")
    @Operation(summary = "轮询问答结果（SSE 不可用时的降级路径）")
    public Result<ChatTaskResult> result(@RequestParam String taskId) {
        ChatTaskResult task = chatTaskService.get(taskId);
        if (task == null) {
            return Result.error(404, "任务不存在或已过期");
        }
        return Result.success(task);
    }

    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "SSE 流式问答（M2）：按 taskId 订阅生成过程，逐字推送")
    public SseEmitter stream(@RequestParam String taskId) {
        SseEmitter emitter = new SseEmitter(120000L);
        ChatTaskResult task = chatTaskService.get(taskId);
        if (task == null) {
            try {
                emitter.send(SseEmitter.event().name("error")
                        .data("{\"type\":\"error\",\"message\":\"任务不存在或已过期\"}"));
            } catch (Exception ignore) {
                // 忽略：连接可能已关闭
            }
            emitter.complete();
            return emitter;
        }
        // 注册后由 ChatTaskService 负责：连接时 flush 历史、生成中实时推增量、结束推 done
        chatTaskService.registerEmitter(taskId, emitter);
        emitter.onCompletion(() -> chatTaskService.removeEmitter(taskId));
        emitter.onTimeout(() -> chatTaskService.removeEmitter(taskId));
        return emitter;
    }

    @GetMapping("/chat/llm-status")
    @Operation(summary = "自检：运行时是否拿到 LLM key（可用于确认是否走真·大模型生成）")
    public Result<Map<String, Object>> llmStatus() {
        boolean available = llmClient.available();
        return Result.success(Map.of(
                "available", available,
                "mode", available ? "llm-stream" : "extractive-fallback",
                "hint", available ? "已接入大模型，答案由 LLM 生成" : "未配置 LLM_API_KEY，已降级为原文片段拼接（非 LLM 生成）"
        ));
    }
}
