package com.orzangleli.checkresourceprefix;

/**
 * <p>description：
 * <p>===============================
 * <p>creator：lixiancheng
 * <p>create time：2019/11/12 上午11:45
 * <p>===============================
 * <p>reasons for modification：
 * <p>Modifier：
 * <p>Modify time：
 *
 * <p>@version
 */
public class CheckResourceConfig {
    boolean autoPreviewResult;
    String outputDir;
    String whiteListFile;
    
    boolean needSendEmail;
    String[] emailList;
    
    void autoPreviewResult(boolean autoPreviewResult) {
        this.autoPreviewResult = autoPreviewResult;
    }
    
    void outputDir(String outputDir) {
        this.outputDir = outputDir;
    }
    
    void whiteListFile(String whiteListFile) {
        this.whiteListFile = whiteListFile;
    }
    
    void needSendEmail(boolean needSendEmail) {
        this.needSendEmail = needSendEmail;
    }
    
    void emailList(String[] emailList) {
        this.emailList = emailList;
    }
}
