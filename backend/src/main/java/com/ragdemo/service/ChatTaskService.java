package com.ragdemo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ragdemo.dto.response.ChatResponse;
import com.ragdemo.dto.response.ChatTaskResult;
import com.ragdemo.dto.response.ChunkSearchResult;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 问答任务服务（M2 流式生成）。
 * - submit：生成 taskId 立即返回，后台线程执行「检索 → LLM 流式生成」，写入内存任务表；
 * - SSE：前端连 /chat/stream 时注册 SseEmitter，后台每产生一段文本就推送增量（打字机效果），
 *   结束推送 done 事件（携带 sources 与完整 answer）；连接时若已有累积文本会先 flush 历史，避免丢失。
 * - 降级：未配置 LLM key 时退回 M1 抽取式（直接拼接 Top-K 片段）。
 * 内存 Map 仅适用于单实例演示；生产应换 Redis/数据库并加过期清理。
 */
@Service
public class ChatTaskService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // taskId -> 任务结果（含状态与累积答案）
    private final Map<String, ChatTaskResult> tasks = new ConcurrentHashMap<>();
    // taskId -> SSE 连接（前端订阅生成过程用）
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    private final ChatService chatService;
    private final RetrievalService retrievalService;
    private final LlmClient llmClient;

    public ChatTaskService(ChatService chatService, RetrievalService retrievalService, LlmClient llmClient) {
        this.chatService = chatService;
        this.retrievalService = retrievalService;
        this.llmClient = llmClient;
    }

    /** 提交问答：落库任务（pending）并后台生成，返回 taskId 供前端订阅 SSE / 轮询 */
    public String submit(String question, int topK) {
        String taskId = UUID.randomUUID().toString().replace("-", "");
        ChatTaskResult task = new ChatTaskResult();
        task.setStatus("pending");
        tasks.put(taskId, task);
        // 后台线程异步生成（演示用 new Thread；生产建议线程池 / @Async + 限流）
        new Thread(() -> runTask(taskId, task, question, topK)).start();
        return taskId;
    }

    /** 按 taskId 取任务结果（轮询降级用） */
    public ChatTaskResult get(String taskId) {
        return tasks.get(taskId);
    }

    /** 前端连上 SSE 时注册 emitter：先 flush 已有累积文本（历史不丢），若已完成则直接推 done 并结束 */
    public void registerEmitter(String taskId, SseEmitter emitter) {
        emitters.put(taskId, emitter);
        ChatTaskResult task = tasks.get(taskId);
        if (task == null) {
            return;
        }
        String existing = task.getAnswer();
        if (existing != null && !existing.isEmpty()) {
            try {
                emitter.send(SseEmitter.event().name("delta")
                        .data(objectMapper.writeValueAsString(Map.of("type", "delta", "content", existing))));
            } catch (Exception ignore) {
                emitters.remove(taskId);
            }
        }
        if ("completed".equals(task.getStatus()) || "failed".equals(task.getStatus())) {
            flushDone(taskId, task, existing == null ? "" : existing);
        }
    }

    /** 移除 SSE 连接（前端断开 / 完成时调用） */
    public void removeEmitter(String taskId) {
        emitters.remove(taskId);
    }

    /** 任务执行：检索 →（有 key）LLM 流式生成 /（无 key）抽取式降级 */
    private void runTask(String taskId, ChatTaskResult task, String question, int topK) {
        try {
            List<ChunkSearchResult> sources = retrievalService.retrieve(question, topK);

            if (llmClient.available()) {
                // 拼装上下文：标注每个来源的文档名与章节，便于 LLM 引用
                StringBuilder ctx = new StringBuilder();
                for (int i = 0; i < sources.size(); i++) {
                    ChunkSearchResult s = sources.get(i);
                    ctx.append("【来源").append(i + 1).append("】来自《").append(s.getDocName())
                            .append("》的「").append(s.getSection()).append("」章节：\n")
                            .append(s.getContent()).append("\n\n");
                }
                List<LlmMessage> messages = List.of(
                        new LlmMessage("system",
                                "你是一个严谨的知识库问答助手。只能根据下面提供的【上下文】回答用户问题；"
                                        + "如果上下文没有相关信息，就如实说「知识库中未找到相关内容」。"
                                        + "回答要简洁、准确，并在末尾列出引用来源（文档名与章节）。"),
                        new LlmMessage("user", "【上下文】\n" + ctx + "\n【问题】" + question)
                );

                StringBuilder acc = new StringBuilder();
                task.setStatus("generating");
                // 流式生成：每段增量累积进 answer 并实时推送给前端 SSE
                llmClient.streamGenerate(messages, delta -> {
                    acc.append(delta);
                    synchronized (task) {
                        task.setAnswer(acc.toString());
                    }
                    SseEmitter em = emitters.get(taskId);
                    if (em != null) {
                        try {
                            em.send(SseEmitter.event().name("delta")
                                    .data(objectMapper.writeValueAsString(Map.of("type", "delta", "content", delta))));
                        } catch (Exception e) {
                            emitters.remove(taskId);
                        }
                    }
                });

                task.setAnswer(acc.toString());
                task.setSources(sources);
                task.setStatus("completed");
                flushDone(taskId, task, acc.toString());
            } else {
                // 无 LLM key：M1 抽取式降级（直接拼接片段）
                ChatResponse resp = chatService.ask(question, topK);
                task.setStatus("completed");
                task.setAnswer(resp.getAnswer());
                task.setSources(resp.getSources());
                task.setQueryEmbedded(resp.isQueryEmbedded());
                flushDone(taskId, task, resp.getAnswer());
            }
        } catch (Exception e) {
            task.setStatus("failed");
            task.setAnswer("抱歉，查询出错，请稍后重试。");
            flushError(taskId, e.getMessage() == null ? "未知错误" : e.getMessage());
        }
    }

    /** 推送 done 事件（携带 sources 与完整 answer）并结束 SSE */
    private void flushDone(String taskId, ChatTaskResult task, String answer) {
        SseEmitter em = emitters.get(taskId);
        if (em == null) {
            return;
        }
        try {
            em.send(SseEmitter.event().name("done").data(objectMapper.writeValueAsString(Map.of(
                    "type", "done",
                    "sources", task.getSources() == null ? List.of() : task.getSources(),
                    "answer", answer
            ))));
            em.complete();
        } catch (Exception ignore) {
            emitters.remove(taskId);
        }
    }

    /** 推送 error 事件并结束 SSE */
    private void flushError(String taskId, String message) {
        SseEmitter em = emitters.get(taskId);
        if (em == null) {
            return;
        }
        try {
            em.send(SseEmitter.event().name("error")
                    .data(objectMapper.writeValueAsString(Map.of("type", "error", "message", message))));
            em.complete();
        } catch (Exception ignore) {
            emitters.remove(taskId);
        }
    }
}
