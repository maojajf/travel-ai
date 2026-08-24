package com.travel.backend.service;

import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class RouteMarkdownSanitizer {

    private static final int STREAM_GUARD_CHARS = 24;

    private static final Pattern ESCAPED_CRLF = Pattern.compile("\\\\r\\\\n");
    private static final Pattern ESCAPED_LF = Pattern.compile("\\\\n");
    private static final Pattern ESCAPED_CR = Pattern.compile("\\\\r");
    private static final Pattern WINDOWS_LINE_BREAK = Pattern.compile("\\r\\n?");
    private static final Pattern MISSING_NEWLINE_BEFORE_H1 = Pattern.compile("(?<!^)(?<!\\n)(?<!#)(#\\s*)");
    private static final Pattern MISSING_NEWLINE_BEFORE_DAY = Pattern.compile("(?<!^)(?<!\\n)(##\\s*D\\d+)");
    private static final Pattern MISSING_NEWLINE_BEFORE_REMINDER = Pattern.compile("(?<!^)(?<!\\n)(##\\s*出行提醒)");
    private static final Pattern MISSING_NEWLINE_BEFORE_SLOT = Pattern.compile("(?<!^)(?<!\\n)(###\\s*)");
    private static final Pattern MISSING_NEWLINE_BEFORE_BULLET = Pattern.compile("(?<!^)(?<!\\n)(-\\s*(?:景点|美食|交通|建议|玩法)\\s*[:：])");
    private static final Pattern HEADING_WITHOUT_SPACE = Pattern.compile("(?m)^(#{1,6})([^\\s#])");
    private static final Pattern MULTI_SPACE_HEADING = Pattern.compile("(?m)^(#{1,6})\\s{2,}");
    private static final Pattern BULLET_WITHOUT_SPACE = Pattern.compile("(?m)^-(\\S)");
    private static final Pattern MULTI_SPACE_BULLET = Pattern.compile("(?m)^-\\s{2,}");
    private static final Pattern LABEL_WITH_MISSING_COLON = Pattern.compile("(?m)^-\\s*(景点|美食|交通|建议|玩法)\\s+(?![:：])");
    private static final Pattern FULL_WIDTH_COLON = Pattern.compile("[:：]");

    public String normalizeForStreaming(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return "";
        }

        String normalized = normalizeLineBreaks(markdown);
        normalized = MISSING_NEWLINE_BEFORE_H1.matcher(normalized).replaceAll("\n$1");
        normalized = MISSING_NEWLINE_BEFORE_DAY.matcher(normalized).replaceAll("\n$1");
        normalized = MISSING_NEWLINE_BEFORE_REMINDER.matcher(normalized).replaceAll("\n$1");
        normalized = MISSING_NEWLINE_BEFORE_SLOT.matcher(normalized).replaceAll("\n$1");
        normalized = MISSING_NEWLINE_BEFORE_BULLET.matcher(normalized).replaceAll("\n$1");
        normalized = HEADING_WITHOUT_SPACE.matcher(normalized).replaceAll("$1 $2");
        normalized = MULTI_SPACE_HEADING.matcher(normalized).replaceAll("$1 ");
        normalized = BULLET_WITHOUT_SPACE.matcher(normalized).replaceAll("- $1");
        normalized = MULTI_SPACE_BULLET.matcher(normalized).replaceAll("- ");
        normalized = LABEL_WITH_MISSING_COLON.matcher(normalized).replaceAll("- $1: ");
        normalized = normalized.replaceAll("(?m)^\\s+", "");
        return normalized;
    }

    public String sanitize(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return "";
        }

        String normalized = normalizeForStreaming(markdown);
        normalized = FULL_WIDTH_COLON.matcher(normalized).replaceAll(":");
        normalized = normalized.replaceAll("(?m)^-\\s*(景点|美食|交通|建议|玩法)\\s*:\\s*", "- $1: ");
        normalized = normalized.replaceAll("(?m)\\n{3,}", "\n\n");
        return normalized.trim();
    }

    public String stablePrefix(String sanitizedMarkdown) {
        if (sanitizedMarkdown == null || sanitizedMarkdown.isEmpty()) {
            return "";
        }

        int lastNewlineIndex = sanitizedMarkdown.lastIndexOf('\n');
        if (lastNewlineIndex < 0) {
            if (sanitizedMarkdown.length() <= STREAM_GUARD_CHARS) {
                return "";
            }
            return sanitizedMarkdown.substring(0, sanitizedMarkdown.length() - STREAM_GUARD_CHARS);
        }

        if (lastNewlineIndex + 1 >= sanitizedMarkdown.length() && sanitizedMarkdown.length() > STREAM_GUARD_CHARS) {
            return sanitizedMarkdown.substring(0, sanitizedMarkdown.length() - STREAM_GUARD_CHARS);
        }
        return sanitizedMarkdown.substring(0, lastNewlineIndex + 1);
    }

    private String normalizeLineBreaks(String markdown) {
        String normalized = markdown;
        normalized = ESCAPED_CRLF.matcher(normalized).replaceAll("\n");
        normalized = ESCAPED_LF.matcher(normalized).replaceAll("\n");
        normalized = ESCAPED_CR.matcher(normalized).replaceAll("\n");
        normalized = WINDOWS_LINE_BREAK.matcher(normalized).replaceAll("\n");
        return normalized;
    }
}
