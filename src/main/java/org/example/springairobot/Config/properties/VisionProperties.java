package org.example.springairobot.Config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 视觉配置属性
 * 
 * 配置视觉服务的相关参数
 * 
 * 配置项：
 * - thumbnail.width：缩略图宽度（默认 400px）
 * - thumbnail.height：缩略图高度（默认 300px）
 * - thumbnail.format：缩略图格式（默认 jpg）
 * 
 * 用途：
 * - 控制生成缩略图的尺寸和格式
 * - 减少存储空间占用
 * - 加快图片加载速度
 */
@Component
@ConfigurationProperties(prefix = "app.vision")
public class VisionProperties {

    /** 缩略图宽度 */
    private int thumbnailWidth = 400;
    
    /** 缩略图高度 */
    private int thumbnailHeight = 300;
    
    /** 缩略图格式 */
    private String thumbnailFormat = "jpg";

    public int getThumbnailWidth() {
        return thumbnailWidth;
    }

    public void setThumbnailWidth(int thumbnailWidth) {
        this.thumbnailWidth = thumbnailWidth;
    }

    public int getThumbnailHeight() {
        return thumbnailHeight;
    }

    public void setThumbnailHeight(int thumbnailHeight) {
        this.thumbnailHeight = thumbnailHeight;
    }

    public String getThumbnailFormat() {
        return thumbnailFormat;
    }

    public void setThumbnailFormat(String thumbnailFormat) {
        this.thumbnailFormat = thumbnailFormat;
    }
}
