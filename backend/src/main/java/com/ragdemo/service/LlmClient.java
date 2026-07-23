package com.ragdemo.service;

import java.util.List;
import java.util.function.Consumer;

/**
 * 大模型客户端接口（M2 流式生成）。
 * 与 EmbeddingClient 解耦：Embedding 负责"把文本变成向量"，本接口负责"基于上下文生成文本"。
 */
public interface LlmClient {

    /** 是否可用（通常取决于是否配置了 API key） */
    boolean available();

    /**
     * 流式生成：把消息列表发给大模型，每产生一段文本就通过 onDelta 回调（打字机效果）。
     * 调用方负责把逐段文本累积成完整答案。
     *
     * @param messages 对话历史（含 system 指令与 user 问题+上下文）
     * @param onDelta  每收到一段增量文本时的回调
     */
    void streamGenerate(List<LlmMessage> messages, Consumer<String> onDelta) throws Exception;
}
