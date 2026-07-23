package com.ragdemo.controller;

import com.ragdemo.common.Result;
import com.ragdemo.dto.request.ChatRequest;
import com.ragdemo.dto.response.ChatSubmitResponse;
import com.ragdemo.dto.response.ChatTaskResult;
import com.ragdemo.service.ChatTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name = "问答")
public class ChatController {

    private final ChatTaskService chatTaskService;

    public ChatController(ChatTaskService chatTaskService) {
        this.chatTaskService = chatTaskService;
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
    @Operation(summary = "轮询问答结果")
    public Result<ChatTaskResult> result(@RequestParam String taskId) {
        ChatTaskResult task = chatTaskService.get(taskId);
        if (task == null) {
            return Result.error(404, "任务不存在或已过期");
        }
        return Result.success(task);
    }
}
