package com.orzangleli.checkresourceprefix;

/**
 * <p>description：
 * <p>===============================
 * <p>creator：lixiancheng
 * <p>create time：2019/11/6 下午3:40
 * <p>===============================
 * <p>reasons for modification：
 * <p>Modifier：
 * <p>Modify time：
 *
 * <p>@version
 */
public abstract class Resource {
    protected boolean isValueType;
    protected String lastDirectory;
    
    public String getLastDirectory() {
        return lastDirectory;
    }
    
    public void setLastDirectory(String lastDirectory) {
        this.lastDirectory = lastDirectory;
    }
    
    public boolean isValueType() {
        return isValueType;
    }
    
    public abstract String getUniqueId();
    
    public abstract String belongFilePath();
    
    public abstract boolean compare(Resource obj);
}
