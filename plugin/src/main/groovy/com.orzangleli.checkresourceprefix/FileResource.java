package com.orzangleli.checkresourceprefix;

/**
 * <p>description：
 * <p>===============================
 * <p>creator：lixiancheng
 * <p>create time：2019/11/6 下午5:23
 * <p>===============================
 * <p>reasons for modification：
 * <p>Modifier：
 * <p>Modify time：
 *
 * <p>@version
 */
public class FileResource extends Resource {
    public FileResource() {
        isValueType = false;
    }
    
    private String path;
    private String md5;
    private String fileName;
    
    public String getPath() {
        return path;
    }
    
    public void setPath(String path) {
        this.path = path;
    }
    
    public String getMd5() {
        return md5;
    }
    
    public void setMd5(String md5) {
        this.md5 = md5;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    public String getUniqueId() {
        return "file@" + lastDirectory + "/" + fileName;
    }
    
    @Override public String belongFilePath() {
        return path;
    }
    
    @Override
    public boolean compare(Resource obj) {
        if (obj instanceof FileResource) {
            FileResource target = (FileResource) obj;
            if (this.getUniqueId().equals(target.getUniqueId())) {
                if (this.getMd5() == null && target.getMd5() == null) {
                    return true;
                } else {
                    return this.getMd5().equals(target.getMd5());
                }
            }
        }
        return false;
    }
}
