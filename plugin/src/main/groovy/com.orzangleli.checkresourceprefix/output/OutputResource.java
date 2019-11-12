package com.orzangleli.checkresourceprefix.output;

import java.util.List;

/**
 * <p>description：
 * <p>===============================
 * <p>creator：lixiancheng
 * <p>create time：2019/11/7 下午4:22
 * <p>===============================
 * <p>reasons for modification：
 * <p>Modifier：
 * <p>Modify time：
 *
 * <p>@version
 */
public class OutputResource {
    private String title;
    private boolean expand = false;
    
    private List<OutputResourceDetail> children;
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public boolean isExpand() {
        return expand;
    }
    
    public void setExpand(boolean expand) {
        this.expand = expand;
    }
    
    public List<OutputResourceDetail> getChildren() {
        return children;
    }
    
    public void setChildren(List<OutputResourceDetail> children) {
        this.children = children;
    }
}
