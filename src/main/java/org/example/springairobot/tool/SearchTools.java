package org.example.springairobot.tool;

import org.example.springairobot.constants.AppConstants;
import org.example.springairobot.service.SearchService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 搜索工具类
 * 
 * 提供网络搜索相关的工具函数，供AI Agent自动调用
 * 
 * 功能特点：
 * - 集成Brave Search API
 * - 返回实时搜索结果
 * - 支持降级到模拟数据
 */
@Component
public class SearchTools {

    private final SearchService searchService;

    public SearchTools(SearchService searchService) {
        this.searchService = searchService;
    }

    /**
     * 网络搜索
     * 
     * @param query 搜索关键词
     * @return 搜索结果字符串
     */
    @Tool(description = AppConstants.SearchConstants.TOOL_DESC_WEB_SEARCH)
    public String webSearch(@ToolParam(description = AppConstants.SearchConstants.TOOL_PARAM_QUERY) String query) {
        return searchService.webSearch(query);
    }
}
