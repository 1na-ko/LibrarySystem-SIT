package com.library.android.model;

import com.google.gson.annotations.SerializedName;

/** 电子资源摘要（WP5 谈判创建下拉选择用）. */
public class ElectronicResourceVO {
    @SerializedName("id")
    private long id;
    @SerializedName("name")
    private String name;
    @SerializedName("category")
    private String category;

    public long getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }

    @Override
    public String toString() { return name != null ? name : ""; }
}
