package org.example.springairobot.tool;

import org.example.springairobot.service.SearchService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class SearchTools {

    private final SearchService searchService;

    public SearchTools(SearchService searchService) {
        this.searchService = searchService;
    }

    @Tool(description = """
        在互联网上搜索实时信息。
        当用户询问以下内容时必须调用此工具：
        - 最新新闻、百科知识、市场行情、产品价格、人物背景等无法从本地知识库获取的信息。
        - 例如："2026年人工智能趋势"、"秦锋的对手有哪些"。
        绝对禁止编造搜索结果。
        """)
    public String webSearch(@ToolParam(description = "搜索关键词") String query) {
        return searchService.webSearch(query);
    }
}
