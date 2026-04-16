package org.example.springairobot.constants;

/**
 * 应用常量定义
 */
public final class AppConstants {

    private AppConstants() {
        // 防止实例化
    }

    // ==================== 文件处理相关常量 ====================

    /**
     * 文件处理错误消息
     */
    public static final class FileProcessingMessages {
        public static final String ERROR_FILE_TYPE_UNKNOWN = "无法识别文件类型";
        public static final String ERROR_FILE_TYPE_UNSUPPORTED = "不支持的文件类型：";
        public static final String ERROR_AUDIO_FORMAT_UNSUPPORTED = "不支持的音频格式：";
        public static final String ERROR_FILE_PROCESSING_FAILED = "文件处理失败，请检查文件是否损坏";

        private FileProcessingMessages() {
        }
    }

    /**
     * 文件类型标识
     */
    public static final class FileTypes {
        public static final String CONTENT_TYPE_PREFIX_IMAGE = "image/";
        public static final String CONTENT_TYPE_PREFIX_AUDIO = "audio/";
        public static final String CONTENT_TYPE_PREFIX_VIDEO = "video/";
        public static final String CONTENT_TYPE_PREFIX_TEXT = "text/";

        public static final String CONTENT_TYPE_PDF = "application/pdf";
        public static final String CONTENT_TYPE_WORD = "application/msword";
        public static final String CONTENT_TYPE_WORD_XML = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        public static final String CONTENT_TYPE_EXCEL = "application/vnd.ms-excel";
        public static final String CONTENT_TYPE_EXCEL_XML = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        public static final String CONTENT_TYPE_POWERPOINT = "application/vnd.ms-powerpoint";
        public static final String CONTENT_TYPE_POWERPOINT_XML = "application/vnd.openxmlformats-officedocument.presentationml.presentation";
        public static final String CONTENT_TYPE_RTF = "application/rtf";
        public static final String CONTENT_TYPE_ODT = "application/vnd.oasis.opendocument.text";

        public static final String CONTENT_TYPE_VIDEO_MP4 = "video/mp4";
        public static final String CONTENT_TYPE_VIDEO_MPEG = "video/mpeg";

        private FileTypes() {
        }
    }

    // ==================== AI 对话相关常量 ====================

    /**
     * 对话和消息相关常量
     */
    public static final class ChatMessages {
        public static final String ERROR_AI_SERVICE_UNAVAILABLE = "AI 服务暂时不可用，请稍后重试";
        public static final String ERROR_GENERATION_FAILED = "生成失败";
        public static final String ERROR_EVALUATION_FAILED = "评估失败";
        public static final String ERROR_RETRIEVAL_FAILED = "检索失败";
        public static final String DEFAULT_UNKNOWN_ANSWER = "抱歉，我暂时无法准确回答这个问题。";

        public static final String MESSAGE_TYPE_USER = "user";
        public static final String MESSAGE_TYPE_ASSISTANT = "assistant";

        private ChatMessages() {
        }
    }

    /**
     * Agent 系统提示词
     */
    public static final class AgentPrompts {
        public static final String AGENT_SYSTEM_PROMPT = """
                你是一个智能助手，必须通过调用工具来获取实时信息。
                
                **重要规则**：
                - 当用户询问天气、搜索、知识库内容时，你必须调用相应的工具。
                - 绝对禁止编造或猜测答案，提取工具的返回作为你生成答案的依据。
                - 然后，将工具返回的结果放在 { "tool_result" : "工具返回结果" }作为参考。
                - 如果工具调用失败，请如实告知用户服务暂时不可用。
                - 回答时，使用中文，保持中文格式。
                """;

        public static final String COORDINATOR_SYSTEM_PROMPT = """
                你是一个智能助理协调员。你的唯一职责是分析用户请求，并调用合适的专家 Agent 来获取信息。
                
                可用专家：
                - weather_agent：查询天气，需传入 {"input": "城市名"}
                - search_agent：网络搜索，需传入 {"input": "关键词"}
                - knowledge_agent：知识库检索，需传入 {"input": "关于《秦锋》小说、知识库的的问题"}
                
                规则：
                - 必须调用工具获取信息，绝对禁止编造答案。
                - 不要输出任何无关的 JSON 或元数据。
                - 调用任何 Agent 时，必须严格按照上述 JSON 格式传递参数，确保包含"input"字段等必须字段。
                - 将专家的结果用文字整合成简洁完整的回答。
                """;

        private AgentPrompts() {
        }
    }

    /**
     * 工具调用相关常量
     */
    public static final class ToolConstants {
        public static final String TOOL_RESULT_KEY = "tool_result";
        public static final String TOOL_INPUT_KEY = "input";

        private ToolConstants() {
        }
    }

    // ==================== RAG 相关常量 ====================

    /**
     * RAG 评估相关常量
     */
    public static final class RagConstants {
        public static final double DEFAULT_RELEVANCE_THRESHOLD = 0.7;
        public static final double DEFAULT_FACT_CHECK_THRESHOLD = 0.8;
        public static final int DEFAULT_BM25_TOP_K = 10;
        public static final int DEFAULT_RERANK_TOP_N = 5;

        private RagConstants() {
        }
    }


    // ==================== Vision 相关常量 ====================

    /**
     * Vision 服务相关常量
     */
    public static final class VisionConstants {
        public static final String DEFAULT_ATTACHMENT_TYPE = "image/jpeg";

        public static final String MEDIA_FILE_PREFIX = "[媒体文件] ";

        private VisionConstants() {
        }
    }

    // ==================== 数据库相关常量 ====================

    /**
     * 数据库查询相关常量
     */
    public static final class DatabaseConstants {
        public static final String VECTOR_STORE_TABLE = "vector_store";
        public static final String METADATA_SOURCE_KEY = "source";
        public static final String METADATA_TYPE_KEY = "type";
        public static final String METADATA_TYPE_VALUE_KNOWLEDGE = "knowledge";

        private DatabaseConstants() {
        }
    }

    // ==================== 缓存相关常量 ====================

    /**
     * 缓存相关常量
     */
    public static final class CacheConstants {
        public static final int DEFAULT_CACHE_TTL_SECONDS = 3600;
        public static final int DEFAULT_CACHE_MAX_ENTRIES = 1000;

        private CacheConstants() {
        }
    }

    // ==================== Advisor 相关常量 ====================

    /**
     * Advisor 相关常量
     */
    public static final class AdvisorConstants {
        public static final String EVALUATION_ADVISOR_NAME = "evaluationAdvisor";
        public static final int EVALUATION_ADVISOR_ORDER = 250;

        public static final String SELF_HEALING_ADVISOR_NAME = "selfHealingAdvisor";
        public static final int SELF_HEALING_ADVISOR_ORDER = 200;

        public static final String SELF_HEALING_RECURSIVE_ADVISOR_NAME = "selfHealingRecursiveAdvisor";
        public static final int SELF_HEALING_RECURSIVE_ADVISOR_ORDER = 180;

        public static final String CHAT_MEMORY_CONVERSATION_ID_KEY = "chat_memory_conversation_id";
        public static final String SESSION_ID_KEY = "sessionId";

        public static final String TOOL_RESULT_KEY_IN_ANSWER = "tool_result";

        // Context keys
        public static final String CONTEXT_KEY_EVALUATION_RESULT = "evaluationResult";
        public static final String CONTEXT_KEY_RETRY_COUNT = "retryCount";
        public static final String CONTEXT_KEY_MAX_RETRIES = "maxRetries";
        public static final String CONTEXT_KEY_FALLBACK = "fallback";
        public static final String CONTEXT_KEY_ORIGINAL_QUERY = "originalQuery";
        public static final String CONTEXT_KEY_REWRITTEN_QUERY = "rewrittenQuery";
        public static final String CONTEXT_KEY_HEALING_STRATEGY = "healingStrategy";
        public static final String CONTEXT_KEY_NEEDS_HEALING = "needsHealing";

        // Healing strategies
        public static final String HEALING_STRATEGY_REGENERATE = "regenerate";
        public static final String HEALING_STRATEGY_QUERY_REWRITE = "query_rewrite";

        // Default max retries
        public static final int DEFAULT_MAX_RETRIES = 2;

        // Agent 答案质量判断关键词
        public static final String[] NEGATIVE_QUALITY_KEYWORDS = {
            "无法回答", "暂时不可用", "服务暂时不可用", "无法获取", "错误", "失败"
        };

        private AdvisorConstants() {
        }
    }

    // ==================== RAG 评估提示词 ====================

    /**
     * RAG 评估提示词
     */
    public static final class RagEvaluationPrompts {
        public static final String CUSTOM_RELEVANCY_PROMPT = """
            你的任务是判断以下回答是否与用户问题和提供的上下文相关。
            
            用户问题：{query}
            上下文信息：{context}
            回答内容：{response}
            
            回答仅用 YES 或 NO
            """;

        public static final String CUSTOM_FACT_CHECK_PROMPT = """
            请检查以下陈述是否在提供的文档中有事实依据。
            
            文档内容：{document}
            陈述内容：{claim}
            
            回答仅用 YES 或 NO
            """;

        private RagEvaluationPrompts() {
        }
    }

    // ==================== AiConfig 相关常量 ====================

    /**
     * AiConfig 相关常量
     */
    public static final class AiConfigConstants {
        // Bean Qualifier 名称
        public static final String QUALIFIER_MESSAGE_CHAT_MEMORY_ADVISOR = "messageChatMemoryAdvisor";
        public static final String QUALIFIER_NO_MEMORY_ADVISOR = "noMemoryAdvisor";
        public static final String QUALIFIER_VISION_CHAT_CLIENT = "visionChatClient";
        public static final String QUALIFIER_PROFILE_EXTRACTION_CHAT_CLIENT = "profileExtractionChatClient";
        public static final String QUALIFIER_EVALUATION_CHAT_CLIENT = "evaluationChatClient";
        public static final String QUALIFIER_AGENT_CHAT_CLIENT = "agentChatClient";
        public static final String QUALIFIER_COORDINATOR_CHAT_CLIENT = "coordinatorChatClient";
        public static final String QUALIFIER_CHAT_CLIENT = "chatClient";
        public static final String QUALIFIER_RAG_CHAT_CLIENT = "ragChatClient";

        // 模型配置
        public static final String VISION_MODEL_NAME = "llava";
        public static final Double VISION_MODEL_TEMPERATURE = 0.7D;

        // 检索配置
        public static final double VECTOR_SIMILARITY_THRESHOLD = 0.5;
        public static final int VECTOR_TOP_K = 5;
        public static final int MULTI_QUERY_NUMBER = 3;
        public static final int BM25_K1 = 5;
        public static final int RERANK_TOP_K = 3;

        // 系统提示词
        public static final String RAG_SYSTEM_PROMPT = "你是一个专业的知识库回答助手。" +
                "返回结果请用中文格式回答。" +
                "不要包含任何无关信息，禁止瞎编答案。";

        public static final String VISION_SYSTEM_PROMPT = "你是一个乐于助人的助手。请始终使用简体中文回答用户的问题，不要使用英文。";

        private AiConfigConstants() {
        }
    }

    // ==================== Vision 服务消息 ====================

    /**
     * Vision 服务消息
     */
    public static final class VisionMessages {
        public static final String ERROR_UNSUPPORTED_FILE_TYPE = "不支持的文件类型: ";
        public static final String ERROR_FILE_PROCESSING_FAILED = "文件处理失败: ";
        public static final String ERROR_FILE_CORRUPTED = "文件处理失败，请检查文件是否损坏";
        public static final String ERROR_MULTIMODEL_CALL_FAILED = "调用多模态模型失败: ";
        public static final String ERROR_AI_SERVICE_UNAVAILABLE = "AI服务暂时不可用，请稍后重试";

        private VisionMessages() {
        }
    }

    // ==================== Coordinator 相关常量 ====================

    /**
     * 协调器相关常量
     */
    public static final class CoordinatorConstants {
        public static final String COORDINATOR_AGENT_NAME = "coordinator";
        public static final String IMAGE_DESCRIPTION_PROMPT = "请详细描述这张图片的内容";
        public static final String IMAGE_CONTEXT_TEMPLATE = """
                用户上传了一张图片，视觉分析结果如下：
                %s
                
                用户的问题：%s
                """;
        public static final String IMAGE_ANALYSIS_FAILED_TEMPLATE = "\n（图片分析失败：%s）";
        public static final String ERROR_AGENT_EXECUTION_FAILED = "智能体执行过程中发生错误: ";

        private CoordinatorConstants() {
        }
    }

    // ==================== ChatService 相关常量 ====================

    /**
     * ChatService 相关常量
     */
    public static final class ChatServiceConstants {
        public static final String ERROR_PERSIST_ENTITIES_FAILED = "持久化实体抽取结果失败: ";
        public static final String RAG_PROMPT_TEMPLATE = """
                你是一个基于知识库的问答助手。请根据检索到的文档内容回答用户问题。
                要求：
                - 如果知识库中包含相关信息，请给出准确回答，并附上引用来源（文档中的关键句子或段落标题）。
                - 如果知识库中没有相关信息，请礼貌回答"不知道"，sources 为空，confidence 为 0。
                - 回答必须严格遵循下面的 JSON 格式。

                用户问题：%s

                %s
                """;

        public static final String ENTITY_EXTRACTION_PROMPT_TEMPLATE = """
                你是一个信息抽取助手。请根据知识库中的内容，提取与用户查询相关的实体信息。
                要求：
                - 返回一个 JSON 数组，每个元素包含 name (实体名称)、type (实体类型，如人物/地点/事件)、description (简短描述)。
                - 如果知识库中没有相关信息，返回空数组 []。
                - 严格遵循以下 JSON 格式。

                用户查询：%s

                %s
                """;

        private ChatServiceConstants() {
        }
    }

    // ==================== SelfHealing 相关常量 ====================

    /**
     * SelfHealing 相关常量
     */
    public static final class SelfHealingConstants {
        // 查询改写提示词
        public static final String QUERY_REWRITE_PROMPT_TEMPLATE = """
                你是一个查询优化专家。请将以下用户问题改写为更适合检索的关键词形式。
                要求：只返回改写后的查询文本，不要任何解释。
                
                原始问题：%s
                改写后的查询：""";

        // 错误消息
        public static final String ERROR_RETRY_BUILDER_NOT_CONFIGURED = 
                "retryBuilder not configured. Call setRetryBuilder() during initialization.";

        private SelfHealingConstants() {
        }
    }

    // ==================== AgentService 相关常量 ====================

    /**
     * AgentService 相关常量
     */
    public static final class AgentServiceConstants {
        // 用户上下文模板
        public static final String USER_PROFILE_CONTEXT_TEMPLATE = "用户偏好：单位-%s，语言-%s";
        public static final String MEMORY_CONTEXT_PREFIX = "\n[历史记忆] ";

        public static final String USER_CONTEXT_TEMPLATE = """
                用户上下文：
                %s
                %s
                
                用户问题：%s
                """;

        // 记忆检索数量
        public static final int DEFAULT_MEMORY_RETRIEVAL_COUNT = 3;

        private AgentServiceConstants() {
        }
    }

    // ==================== SubAgent 相关常量 ====================

    /**
     * SubAgent 相关常量
     */
    public static final class SubAgentConstants {
        // 工具名称
        public static final String TOOL_GET_WEATHER = "get_weather";
        public static final String TOOL_WEB_SEARCH = "web_search";
        public static final String TOOL_QUERY_KNOWLEDGE = "query_knowledge";

        // 工具描述
        public static final String TOOL_DESC_WEATHER = "查询指定城市的实时天气，参数：{\"input\": \"城市名\"}";
        public static final String TOOL_DESC_SEARCH = "搜索互联网信息，参数：{\"input\": \"关键词\"}";
        public static final String TOOL_DESC_KNOWLEDGE = "检索《秦锋》知识库，本地知识库，rag知识库，参数：{\"input\": \"问题\"}";

        // 系统提示词
        public static final String WEATHER_AGENT_PROMPT = "你是一个天气专家。调用 get_weather 工具，传入 { \"input\": \"城市名\" }。返回纯文本天气描述。";
        public static final String SEARCH_AGENT_PROMPT = "你是一个搜索专家。调用 web_search 工具，传入 { \"input\": \"关键词\" }。返回纯文本摘要。";
        public static final String KNOWLEDGE_AGENT_PROMPT = "你是一个知识库专家。调用 query_knowledge 工具，传入 { \"input\": \"问题\" }。返回纯文本答案。" +
                "你必须严格基于知识库子Agent返回的信息回答。如果返回信息为空或明确表示未找到，你必须如实告知用户。绝对禁止编造任何信息。";

        // 输出键
        public static final String OUTPUT_KEY_WEATHER = "weather_result";
        public static final String OUTPUT_KEY_SEARCH = "search_result";
        public static final String OUTPUT_KEY_KNOWLEDGE = "knowledge_result";

        // 错误消息
        public static final String ERROR_WEATHER_NO_CITY = "无法查询天气：请提供城市名称。";
        public static final String ERROR_SEARCH_NO_KEYWORD = "无法搜索：请提供关键词。";
        public static final String ERROR_KNOWLEDGE_NO_QUERY = "知识库检索失败：未提供查询内容。请告知您想了解《秦锋》小说中的什么信息。";

        private SubAgentConstants() {
        }
    }

    // ==================== Memory 相关常量 ====================

    /**
     * Memory 服务相关常量
     */
    public static final class MemoryConstants {
        // 元数据键
        public static final String METADATA_KEY_USER_ID = "user_id";
        public static final String METADATA_KEY_ROLE = "role";
        public static final String METADATA_KEY_TYPE = "type";
        public static final String METADATA_VALUE_LONG_TERM_MEMORY = "long_term_memory";

        // 角色标签
        public static final String ROLE_USER = "user";
        public static final String ROLE_USER_LABEL = "用户: ";
        public static final String ROLE_ASSISTANT_LABEL = "助手: ";

        // 阈值
        public static final double MEMORY_SIMILARITY_THRESHOLD = 0.9;
        public static final int MEMORY_TOP_K = 1;
        public static final int MIN_MESSAGE_LENGTH = 10;
        public static final int PROFILE_UPDATE_HOURS = 1;
        public static final int RECENT_MESSAGE_COUNT = 20;

        // 用户画像提取提示词
        public static final String PROFILE_EXTRACTION_PROMPT = """
                根据以下对话内容，提取用户的关键偏好和特征。
                重要：只返回 JSON 数据，不要包含任何解释、Schema 或 Markdown 代码块标记
                
                对话内容：
                %s
                
                %s
                """;

        // 过滤表达式模板
        public static final String FILTER_USER_MEMORY = "user_id == '%s' && type == 'long_term_memory'";

        // 错误消息
        public static final String ERROR_PROFILE_EXTRACTION_FAILED = "Failed to extract user profile: ";

        private MemoryConstants() {
        }
    }


    // ==================== RAG 检索相关常量 ====================

    /**
     * RAG 检索相关常量
     */
    public static final class RagRetrieverConstants {
        // BM25 相关
        public static final String BM25_SCORE_KEY = "bm25_score";
        public static final String BM25_RAW_METADATA_KEY = "raw";
        
        // BM25 SQL 查询模板
        public static final String BM25_SQL_QUERY_TEMPLATE = """
                SELECT id, content, metadata,
                       ts_rank(content_tsv, plainto_tsquery('english', ?)) AS rank
                FROM vector_store
                WHERE content_tsv @@ plainto_tsquery('english', ?)
                ORDER BY rank DESC
                LIMIT ?
                """;

        // ReRanker 相关性评分提示词
        public static final String RERANK_SCORING_PROMPT = """
                你是一个专业的相关性评分助手。请根据用户问题对文档内容的相关性进行评分，分值范围 0 到 1，保留两位小数。
                仅输出数字，不要有任何其他文字。
                
                用户问题：%s
                文档内容：%s
                相关性分数：
                """;

        // 默认分数
        public static final double DEFAULT_RELEVANCE_SCORE = 0.0;

        private RagRetrieverConstants() {
        }
    }

    // ==================== QueryTransformer 相关常量 ====================

    /**
     * 查询转换器相关常量
     */
    public static final class QueryTransformerConstants {
        // 上下文键
        public static final String CONTEXT_SESSION_ID_KEY = "sessionId";
        
        // 角色标签
        public static final String ROLE_USER_LABEL = "User";
        public static final String ROLE_ASSISTANT_LABEL = "Assistant";
        
        // 对话历史分隔符
        public static final String HISTORY_SEPARATOR = "\n";
        
        // 查询改写提示词模板
        public static final String QUERY_REWRITE_PROMPT = """
                你是一个查询改写助手。请根据以下对话历史，将用户当前的问题改写成一个独立、完整的查询。
                如果问题本身已经完整，可以直接返回原问题。
                
                ## 对话历史
                %s
                
                ## 当前问题
                %s
                
                ## 改写后的查询
                """;

        // 日志消息
        public static final String LOG_NO_SESSION = "未找到 sessionId，使用原始查询";
        public static final String LOG_NO_HISTORY = "无历史对话，使用原始查询";
        public static final String LOG_REWRITE_FAILED = "查询改写失败，使用原始查询";

        private QueryTransformerConstants() {
        }
    }

    // ==================== Ingestion 相关常量 ====================

    /**
     * Ingestion 服务相关常量
     */
    public static final class IngestionConstants {
        // 文件名
        public static final String KNOWLEDGE_BASE_FILE = "knowledge-base.txt";

        // 元数据键
        public static final String METADATA_KEY_SOURCE = "source";
        public static final String METADATA_KEY_TYPE = "type";
        public static final String METADATA_VALUE_KNOWLEDGE = "knowledge";

        // SQL
        public static final String SQL_DELETE_BY_SOURCE = "DELETE FROM vector_store WHERE metadata->>'source' = ?";

        // 日志消息
        public static final String LOG_NO_CHANGE = "✅ 知识库无变化，跳过加载。";
        public static final String LOG_DETECTED_CHANGE = "🔄 检测到知识库变化，开始重新加载...";
        public static final String LOG_LOAD_COMPLETE = "✅ 知识库加载完成，共 %d 个文档块。";
        public static final String LOG_ERROR = "❌ 知识库处理失败: ";

        private IngestionConstants() {
        }
    }

    // ==================== Weather 相关常量 ====================

    /**
     * Weather 服务相关常量
     */
    public static final class WeatherConstants {
        // HTTP 超时设置
        public static final int CONNECT_TIMEOUT_SECONDS = 10;
        public static final int READ_TIMEOUT_SECONDS = 30;

        // API 响应码
        public static final String API_SUCCESS_CODE = "200";

        // 模拟天气数据
        public static final String MOCK_WEATHER_BEIJING = "晴，温度 22°C，湿度 40%";
        public static final String MOCK_WEATHER_SHANGHAI = "多云，温度 25°C，湿度 60%";
        public static final String MOCK_WEATHER_GUANGZHOU = "雷阵雨，温度 28°C，湿度 80%";

        // 错误消息
        public static final String ERROR_CITY_EMPTY = "城市名称不能为空";

        // 天气预报模拟
        public static final String FORECAST_TEMPLATE = "%s未来三天：周一晴，周二多云，周三小雨";

        // 模拟数据模板
        public static final String MOCK_WEATHER_TEMPLATE = "%s当前天气：晴，温度22°C，湿度40%%（模拟数据，实时服务暂时不可用）";

        // 天气格式模板
        public static final String WEATHER_FORMAT = "%s当前天气：%s，温度%s°C，湿度%s%%，%s%s级";

        // 工具描述
        public static final String TOOL_DESC_GET_WEATHER = "获取指定城市的实时天气信息";
        public static final String TOOL_DESC_GET_FORECAST = "获取指定城市的未来三天天气预报";
        public static final String TOOL_PARAM_CITY = "城市名称，如北京、上海";

        private WeatherConstants() {
        }
    }

    // ==================== Search 相关常量 ====================

    /**
     * Search 服务相关常量
     */
    public static final class SearchConstants {
        // HTTP 超时设置
        public static final int CONNECT_TIMEOUT_SECONDS = 10;
        public static final int READ_TIMEOUT_SECONDS = 30;

        // 搜索结果数量
        public static final int SEARCH_RESULT_COUNT = 5;
        public static final int DISPLAY_RESULT_COUNT = 3;

        // 错误消息
        public static final String ERROR_QUERY_EMPTY = "搜索关键词不能为空";

        // 搜索结果格式
        public static final String SEARCH_RESULT_HEADER = "搜索 \"%s\" 的结果：\n";
        public static final String SEARCH_RESULT_ITEM = "%d. %s\n   %s\n   URL: %s\n";

        // 模拟数据模板
        public static final String MOCK_SEARCH_TEMPLATE = "关于'%s'的搜索结果：1. 相关介绍... 2. 最新动态...（模拟数据，搜索服务暂时不可用）";

        // 工具描述
        public static final String TOOL_DESC_WEB_SEARCH = """
                在互联网上搜索实时信息。
                当用户询问以下内容时必须调用此工具：
                - 最新新闻、百科知识、市场行情、产品价格、人物背景等无法从本地知识库获取的信息。
                - 例如："2026年人工智能趋势"、"秦锋的对手有哪些"。
                绝对禁止编造搜索结果。
                """;
        public static final String TOOL_PARAM_QUERY = "搜索关键词";

        private SearchConstants() {
        }
    }

    // ==================== Conversation 相关常量 ====================

    /**
     * Conversation 服务相关常量
     */
    public static final class ConversationConstants {
        // 默认会话标题
        public static final String DEFAULT_SESSION_TITLE = "新对话";

        // 消息角色
        public static final String ROLE_USER = "user";
        public static final String ROLE_ASSISTANT = "assistant";

        private ConversationConstants() {
        }
    }

    // ==================== Audio 音频处理相关常量 ====================

    /**
     * 音频处理相关常量
     */
    public static final class AudioConstants {
        // 临时文件
        public static final String TEMP_FILE_PREFIX_AUDIO = "audio_";
        public static final String TEMP_FILE_SUFFIX_AUDIO = ".tmp";
        
        // 音频格式参数
        public static final float AUDIO_SAMPLE_RATE = 16000.0f;
        public static final int AUDIO_BITS_PER_SAMPLE = 16;
        public static final int AUDIO_CHANNELS = 1;
        public static final boolean AUDIO_SIGNED = true;
        public static final boolean AUDIO_BIG_ENDIAN = false;
        
        // 音频处理
        public static final int AUDIO_BUFFER_SIZE = 4096;
        public static final float AUDIO_NORMALIZATION_FACTOR = 32768.0f;
        
        // Whisper 参数默认值
        public static final int WHISPER_DEFAULT_THREADS = 4;
        public static final String WHISPER_DEFAULT_LANGUAGE = "zh";
        public static final boolean WHISPER_PRINT_PROGRESS = false;
        public static final boolean WHISPER_PRINT_TIMESTAMPS = false;
        public static final boolean WHISPER_TRANSLATE = false;
        public static final boolean WHISPER_SINGLE_SEGMENT = true;
        
        // 错误消息
        public static final String ERROR_MODEL_FILE_NOT_FOUND = "Whisper model file not found: ";
        public static final String ERROR_TRANSCRIPTION_FAILED = "Transcription failed with code: ";

        private AudioConstants() {
        }
    }

    // ==================== Knowledge 工具相关常量 ====================

    /**
     * Knowledge 工具相关常量
     */
    public static final class KnowledgeConstants {
        // 工具描述
        public static final String TOOL_DESC_QUERY_KNOWLEDGE = """
                从本地知识库中检索信息。
                当用户询问以下内容时必须调用此工具：
                - 小说《秦锋》的剧情、人物、事件等。
                - 例如："秦锋的故事里有哪些人物？"、"秦锋的对手是谁？"。
                绝对禁止编造知识库内容。
                """;
        public static final String TOOL_DESC_EXTRACT_ENTITIES = "从知识库中批量抽取实体信息，如人物、地点、事件等";

        // 参数描述
        public static final String TOOL_PARAM_SESSION_ID = "会话ID";
        public static final String TOOL_PARAM_USER_ID = "用户Id";
        public static final String TOOL_PARAM_QUERY = "要查询的问题";
        public static final String TOOL_PARAM_QUERY_DESC = "查询描述";

        // 错误消息
        public static final String ERROR_QUERY_EMPTY = "查询问题不能为空";
        public static final String ERROR_QUERY_DESC_EMPTY = "查询描述不能为空";

        private KnowledgeConstants() {
        }
    }

    // ==================== Controller 相关常量 ====================

    /**
     * Controller API 路径常量
     */
    public static final class ApiPaths {
        // API 前缀
        public static final String API_PREFIX = "/api";

        // Agent API
        public static final String AGENT_BASE = "/api/agent";
        public static final String AGENT_CHAT = "/chat";
        public static final String AGENT_COORDINATE = "/coordinate";

        // Chat API
        public static final String CHAT_BASE = "/api/chat";
        public static final String CHAT_SYNC = "/sync";
        public static final String CHAT_STREAM = "/stream";
        public static final String CHAT_RAG = "/rag";
        public static final String CHAT_RAG_STREAM = "/rag/stream";
        public static final String CHAT_RAG_STRUCTURED = "/rag/structured";
        public static final String CHAT_RAG_EXTRACT_ENTITIES = "/rag/extractentities";
        public static final String CHAT_HISTORY = "/history/{sessionId}";
        public static final String CHAT_USER_HISTORY = "/user-history/{userId}";
        public static final String CHAT_SESSIONS = "/sessions";
        public static final String CHAT_USER_SESSIONS = "/user-sessions";
        public static final String CHAT_SESSION = "/session";

        // File API
        public static final String FILE_BASE = "/api/file";
        public static final String FILE_PROCESS = "/process";
        public static final String FILE_BATCH = "/batch";
        public static final String FILE_CHAT = "/chat";
        public static final String FILE_BATCH_CHAT = "/batch-chat";

        // Vision API
        public static final String VISION_BASE = "/api/vision";
        public static final String VISION_ANALYZE = "/analyze";

        // Memory API
        public static final String MEMORY_BASE = "/api/memory";

        private ApiPaths() {
        }
    }

    /**
     * Controller 相关消息和模板
     */
    public static final class ControllerMessages {
        // Content-Type
        public static final String CONTENT_TYPE_TEXT_HTML = "text/html; charset=UTF-8";

        // 文件处理提示词模板
        public static final String FILE_CHAT_PROMPT_TEMPLATE = """
                用户上传了一个文件，内容如下：
                ---
                %s
                ---
                用户的问题：%s
                """;

        public static final String BATCH_FILE_CHAT_PROMPT_TEMPLATE = """
                用户上传了 %d 个文件，成功处理 %d 个。
                文件内容如下：
                ---
                %s
                ---
                用户的问题：%s
                """;

        public static final String FILE_CONTENT_FORMAT = "【文件: %s】\n%s\n\n";

        // 记忆删除消息
        public static final String MEMORY_DELETED = "记忆已删除";

        private ControllerMessages() {
        }
    }

    // ==================== 日志相关常量 ====================

    /**
     * 日志相关常量
     */
    public static final class LoggingConstants {
        public static final String LOG_PREFIX_FILE_PROCESSING = "[文件处理] ";
        public static final String LOG_PREFIX_AUDIO = "[音频处理] ";
        public static final String LOG_PREFIX_VISION = "[视觉处理] ";
        public static final String LOG_PREFIX_RAG = "[RAG] ";
        public static final String LOG_PREFIX_AGENT = "[Agent] ";

        private LoggingConstants() {
        }
    }
}
