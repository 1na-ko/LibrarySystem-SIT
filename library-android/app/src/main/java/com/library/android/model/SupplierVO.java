package com.library.android.model;

import com.google.gson.annotations.SerializedName;

/** 供应商摘要（WP5 谈判创建下拉选择用）. */
public class SupplierVO {
    @SerializedName("id")
    private long id;
    @SerializedName("name")
    private String name;

    public long getId() { return id; }
    public String getName() { return name; }

    @Override
    public String toString() { return name != null ? name : ""; }
}
