package com.library.android.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * 图书分类 VO（支持树形嵌套）.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
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
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }

    public List<CategoryVO> getChildren() { return children; }
    public void setChildren(List<CategoryVO> children) { this.children = children; }

    public boolean hasChildren() {
        return children != null && !children.isEmpty();
    }
}
