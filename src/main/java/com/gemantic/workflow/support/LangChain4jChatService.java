package com.gemantic.workflow.support;

import com.gemantic.gpt.model.AiModel;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * 基于 LangChain4j 的 LLM 调用服务
 * 支持 OpenAI 兼容接口，按 AiModel 配置动态创建 ChatLanguageModel
 */
@Service
public class LangChain4jChatService {

    private static final Logger LOG = LoggerFactory.getLogger(LangChain4jChatService.class);

    // 按模型 id 缓存 ChatLanguageModel，避免重复创建
    private final Map<String, ChatLanguageModel> modelCache = new ConcurrentHashMap<>();

    /**
     * 调用 LLM，返回文本输出
     *
     * @param aiModel   模型配置（含 baseUrl、apiKey、modelName、temperature 等）
     * @param prompt    用户提示词
     * @param useCache  是否允许缓存（RERUN 时传 false）
     * @return LlmCallResult 包含输出文本和 token 统计
     */
    public LlmCallResult chat(AiModel aiModel, String prompt, boolean useCache) {
        ChatLanguageModel model = getOrCreateModel(aiModel);
        long start = System.currentTimeMillis();
        try {
            List<ChatMessage> messages = List.of(UserMessage.from(prompt));
            ChatRequest request = ChatRequest.builder().messages(messages).build();
            ChatResponse response = model.chat(request);

            String output = response.aiMessage().text();
            long firstTokenLatency = System.currentTimeMillis() - start;

            long totalTokens = 0L, inputTokens = 0L, outputTokens = 0L;
            if (response.tokenUsage() != null) {
                totalTokens = response.tokenUsage().totalTokenCount() != null
                    ? response.tokenUsage().totalTokenCount() : 0L;
                inputTokens = response.tokenUsage().inputTokenCount() != null
                    ? response.tokenUsage().inputTokenCount() : 0L;
                outputTokens = response.tokenUsage().outputTokenCount() != null
                    ? response.tokenUsage().outputTokenCount() : 0L;
            }
            long tokenTime = System.currentTimeMillis() - start;

            return new LlmCallResult(output, totalTokens, inputTokens, outputTokens,
                firstTokenLatency, tokenTime, null);

        } catch (Exception e) {
            LOG.error("LangChain4j LLM 调用失败 model={}", aiModel.getName(), e);
            return new LlmCallResult(null, 0L, 0L, 0L, 0L, 0L, e.getMessage());
        }
    }

    private ChatLanguageModel getOrCreateModel(AiModel aiModel) {
        String cacheKey = aiModel.getId() + "_" + aiModel.getName();
        return modelCache.computeIfAbsent(cacheKey, k -> buildModel(aiModel));
    }

    private ChatLanguageModel buildModel(AiModel aiModel) {
        String baseUrl = null;
        String apiKey = "placeholder";
        String modelName = aiModel.getName();
        Float temperature = aiModel.getTemperature();

        // 从 AiModel.data 中读取 baseUrl / apiKey
        if (aiModel.getData() != null) {
            if (StringUtils.isNotBlank(aiModel.getData().getString("baseUrl"))) {
                baseUrl = aiModel.getData().getString("baseUrl");
            }
            if (StringUtils.isNotBlank(aiModel.getData().getString("apiKey"))) {
                apiKey = aiModel.getData().getString("apiKey");
            }
            if (StringUtils.isNotBlank(aiModel.getData().getString("modelName"))) {
                modelName = aiModel.getData().getString("modelName");
            }
        }

        var builder = OpenAiChatModel.builder()
            .modelName(modelName)
            .apiKey(apiKey);

        if (StringUtils.isNotBlank(baseUrl)) {
            builder.baseUrl(baseUrl);
        }
        if (temperature != null) {
            builder.temperature(temperature.doubleValue());
        }

        LOG.info("创建 LangChain4j ChatLanguageModel model={} baseUrl={}", modelName, baseUrl);
        return builder.build();
    }

    /**
     * LLM 调用结果
     */
    public record LlmCallResult(
        String output,
        long totalTokens,
        long inputTokens,
        long outputTokens,
        long firstTokenLatency,
        long tokenTime,
        String error
    ) {
        public boolean isSuccess() {
            return error == null && output != null;
        }
    }
}
