package com.ragdemo.service.impl;

import com.ragdemo.service.EmbeddingClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * 本地 hash 兜底 Embedding：确定性、离线、无需任何 key。
 * 采用"字符袋 + 哈希分桶 + L2 归一化"生成定长向量，语义相近的文本会得到相近的向量，
 * 足以支撑 M1 的向量召回演示（质量低于真实语义模型，仅用于无 key 场景）。
 */
@Service
public class LocalHashEmbeddingClient implements EmbeddingClient {

    @Value("${embedding.dim}")
    private int dim;

    @Override
    public float[] embed(String text) {
        float[] vector = new float[dim];
        String normalized = text.toLowerCase(Locale.ROOT);
        for (int i = 0; i < normalized.length(); i++) {
            int cp = normalized.codePointAt(i);
            int bucket = Math.floorMod(cp * 2654435761L, dim);
            vector[bucket] += 1.0f;
        }
        // L2 归一化，使余弦距离等价于余弦相似度
        double norm = 0.0;
        for (float v : vector) {
            norm += v * v;
        }
        norm = Math.sqrt(norm);
        if (norm > 0) {
            for (int i = 0; i < vector.length; i++) {
                vector[i] = (float) (vector[i] / norm);
            }
        }
        return vector;
    }

    @Override
    public String name() {
        return "local-hash:dim=" + dim;
    }
}
