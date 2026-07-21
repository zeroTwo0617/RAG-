package com.ragdemo.controller;

import com.ragdemo.common.Result;
import com.ragdemo.dto.request.ChatRequest;
import com.ragdemo.dto.response.ChatResponse;
import com.ragdemo.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name = "问答")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/chat")
    @Operation(summary = "单轮问答（抽取式，M1）")
    public Result<ChatResponse> chat(@Valid @RequestBody ChatRequest req) {
        int topK = req.getTopK() == null ? 5 : req.getTopK();
        if (topK < 1) {
            topK = 1;
        }
        if (topK > 10) {
            topK = 10;
        }
        return Result.success(chatService.ask(req.getQuestion(), topK));
    }
}
