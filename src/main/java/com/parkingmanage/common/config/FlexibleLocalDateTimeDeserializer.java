package com.parkingmanage.common.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * 灵活的 LocalDateTime 反序列化器
 * 支持多种日期时间格式，包括 ISO-8601 带时区的格式
 */
public class FlexibleLocalDateTimeDeserializer extends StdDeserializer<LocalDateTime> {
    
    private static final List<DateTimeFormatter> FORMATTERS = new ArrayList<>();
    
    static {
        // 标准格式
        FORMATTERS.add(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        FORMATTERS.add(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        FORMATTERS.add(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        
        // ISO-8601 格式
        FORMATTERS.add(DateTimeFormatter.ISO_DATE_TIME);
        FORMATTERS.add(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
    
    public FlexibleLocalDateTimeDeserializer() {
        super(LocalDateTime.class);
    }
    
    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String text = p.getText();
        
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        
        // 处理带时区的 ISO-8601 格式 (例如: "2025-10-29T02:16:54.469Z")
        // 检查是否包含时区信息（以Z结尾或包含时区偏移量）
        if (text.endsWith("Z") || text.matches(".*[+-]\\d{2}:\\d{2}$")) {
            try {
                Instant instant = Instant.parse(text);
                return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
            } catch (DateTimeParseException e) {
                // 如果解析失败，继续尝试其他格式
            }
        }
        
        // 尝试使用各种日期格式
        for (DateTimeFormatter formatter : FORMATTERS) {
            try {
                return LocalDateTime.parse(text, formatter);
            } catch (DateTimeParseException e) {
                // 继续尝试下一个格式
            }
        }
        
        // 如果所有格式都失败了，抛出异常
        throw new IOException("无法解析日期时间字符串: " + text);
    }
}

