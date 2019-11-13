package com.orzangleli.checkresourceprefix;

public class EmailConfig {
    boolean needSendEmail = false;
    String host;
    String fromEmail;
    String[] toEmailList;
    String account;
    String authorizationCode;
    
    public void needSendEmail(boolean needSendEmail) {
        this.needSendEmail = needSendEmail;
    }
    
    public void host(String host) {
        this.host = host;
    }
    
    public void fromEmail(String fromEmail) {
        this.fromEmail = fromEmail;
    }
    
    public void toEmailList(String[] toEmailList) {
        this.toEmailList = toEmailList;
    }
    
    public void account(String account) {
        this.account = account;
    }
    
    public void authorizationCode(String authorizationCode) {
        this.authorizationCode = authorizationCode;
    }
}