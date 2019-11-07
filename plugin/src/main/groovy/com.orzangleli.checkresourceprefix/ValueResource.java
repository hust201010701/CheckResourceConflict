package com.orzangleli.checkresourceprefix;

/**
 * <p>description：
 * <p>===============================
 * <p>creator：lixiancheng
 * <p>create time：2019/11/6 下午5:24
 * <p>===============================
 * <p>reasons for modification：
 * <p>Modifier：
 * <p>Modify time：
 *
 * <p>@version
 */
public class ValueResource extends Resource {
    public ValueResource() {
        isValueType = true;
    }
    
    private String resName;
    private String resValue;
    private String filePath;
    
    public String getFilePath() {
        return filePath;
    }
    
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
    
    public String getResName() {
        return resName;
    }
    
    public void setResName(String resName) {
        this.resName = resName;
    }
    
    public String getResValue() {
        return resValue;
    }
    
    public void setResValue(String resValue) {
        this.resValue = resValue;
    }
    
    public String getUniqueId() {
        return "value@" + lastDirectory + "/" + resName;
    }
    
    @Override public String belongFilePath() {
        return filePath;
    }
    
    @Override
    public boolean compare(Resource obj) {
        if (obj instanceof ValueResource) {
            ValueResource target = (ValueResource) obj;
            return this.getUniqueId().equals(target.getUniqueId()) && this.getResValue().equals(target.getResValue());
        }
        return false;
    }
}
