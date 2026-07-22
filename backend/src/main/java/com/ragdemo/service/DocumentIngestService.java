package com.ragdemo.service;

import com.ragdemo.common.BusinessException;
import com.ragdemo.common.ErrorCode;
import com.ragdemo.entity.Document;
import com.ragdemo.mapper.ChunkMapper;
import com.ragdemo.mapper.DocumentMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 文档入库管线：解析 -> 分块 -> 向量化 -> 写入 PGVector。
 * M1 同步处理：上传即完成全部步骤并返回真实 chunkCount。
 */
@Service
public class DocumentIngestService {

    private final DocumentMapper documentMapper;
    private final ChunkMapper chunkMapper;
    private final EmbeddingService embeddingService;
    private final MarkdownParser markdownParser;
    private final ChunkSplitter chunkSplitter;

    public DocumentIngestService(DocumentMapper documentMapper,
                                 ChunkMapper chunkMapper,
                                 EmbeddingService embeddingService,
                                 MarkdownParser markdownParser,
                                 ChunkSplitter chunkSplitter) {
        this.documentMapper = documentMapper;
        this.chunkMapper = chunkMapper;
        this.embeddingService = embeddingService;
        this.markdownParser = markdownParser;
        this.chunkSplitter = chunkSplitter;
    }

    public Document ingest(MultipartFile file) {
        // 1) 校验文件：必须是非空 .md
        String name = file.getOriginalFilename();
        if (name == null || !name.toLowerCase().endsWith(".md")) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "仅支持 .md 文件");
        }
        String content;
        try {
            content = new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "读取文件失败");
        }
        if (content.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件内容为空");
        }

        // 2) 生成业务主键 docId，并把 Markdown 按标题切分为若干段落块
        String docId = UUID.randomUUID().toString().replace("-", "");
        List<MarkdownParser.ParsedSection> sections = markdownParser.parse(content);

        // 3) 对每个段落块再二次切分（长块拆小 + 相邻重叠），逐块向量化并写入 chunk 表
        int chunkIndex = 0;
        int count = 0;
        for (MarkdownParser.ParsedSection section : sections) {
            List<ChunkSplitter.ChunkUnit> units = chunkSplitter.split(section, 400, 80);
            for (ChunkSplitter.ChunkUnit unit : units) {
                // 文本 -> 1536 维向量（无 key 时用本地 hash 兜底）
                float[] vector = embeddingService.embed(unit.getContent());
                chunkMapper.insertChunk(
                        docId,
                        unit.getSection(),
                        chunkIndex++,
                        unit.getContent(),
                        estimateTokens(unit.getContent()),
                        vector
                );
                count++;
            }
        }

        // 4) 写入 document 元信息（含真实 chunkCount），返回
        Document doc = new Document();
        doc.setDocId(docId);
        doc.setName(name);
        doc.setStatus("READY");
        doc.setChunkCount(count);
        doc.setCreatedAt(LocalDateTime.now());
        documentMapper.insert(doc);
        return doc;
    }

    private int estimateTokens(String text) {
        // 粗略估算：中文约 1.5 字符/token
        return (int) Math.ceil(text.length() / 1.5);
    }
}
