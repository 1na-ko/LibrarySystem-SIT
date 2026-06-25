package com.library.android.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * 图书分类（支持树形结构）.
 */
public class CategoryVO {

    @SerializedName("id")
    private long id;

    @SerializedName("name")
    private String name;

    @SerializedName("parentId")
    private Long parentId;

    @SerializedName("sortOrder")
    private int sortOrder;

    @SerializedName("children")
    private List<CategoryVO> children;

    public long getId() { return id; }
    public String getName() { return name; }
    public Long getParentId() { return parentId; }
    public int getSortOrder() { return sortOrder; }
    public List<CategoryVO> getChildren() { return children; }

    public boolean hasChildren() { return children != null && !children.isEmpty(); }
}