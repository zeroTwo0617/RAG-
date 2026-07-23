package com.ragdemo.service;

import com.ragdemo.dto.response.ChatResponse;
import com.ragdemo.dto.response.ChatTaskResult;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 问答任务服务（M1 轻量异步实现）。
 * - submit：生成 taskId 立即返回，后台线程调用 ChatService 生成答案，写入内存任务表；
 * - get：供前端按 taskId 轮询当前状态与结果。
 * 内存 Map 仅适用于单实例演示；生产应换 Redis/数据库并加过期清理。M2 接入 LLM 流式时，
 * 可把后台线程改为逐段写入 answer，实现打字机效果。
 */
@Service
public class ChatTaskService {

    // taskId -> 任务结果（含状态）。M1 演示用，单实例足够
    private final Map<String, ChatTaskResult> tasks = new ConcurrentHashMap<>();
    private final ChatService chatService;

    public ChatTaskService(ChatService chatService) {
        this.chatService = chatService;
    }

    /** 提交问答：落库任务（pending）并后台生成，返回 taskId 供前端轮询 */
    public String submit(String question, int topK) {
        String taskId = UUID.randomUUID().toString().replace("-", "");
        ChatTaskResult task = new ChatTaskResult();
        task.setStatus("pending");
        tasks.put(taskId, task);
        // 后台线程异步生成（M1 抽取式很快，生产建议用线程池/@Async）
        new Thread(() -> {
            try {
                ChatResponse resp = chatService.ask(question, topK);
                task.setStatus("completed");
                task.setAnswer(resp.getAnswer());
                task.setSources(resp.getSources());
                task.setQueryEmbedded(resp.isQueryEmbedded());
            } catch (Exception e) {
                task.setStatus("failed");
                task.setAnswer("抱歉，查询出错，请稍后重试。");
            }
        }).start();
        return taskId;
    }

    /** 按 taskId 取任务结果（轮询用） */
    public ChatTaskResult get(String taskId) {
        return tasks.get(taskId);
    }
}
