package com.library.android.model;

import com.google.gson.annotations.SerializedName;

/**
 * 搜索补全建议项 VO（WP-4 契约对齐：与后端 SuggestVO 字段一致：text + type）.
 *
 * <p>type 取值：BOOK / AUTHOR / KEYWORD 等（具体由后端 EsIndexInitializer 配置决定）.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public class SuggestVO {

    @SerializedName("text")
    private String text;

    @SerializedName("type")
    private String type;

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}
