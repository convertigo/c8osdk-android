package com.convertigo.clientsdk.exception;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Nicolas on 09/06/2017.
 */

public class C8oResponseException extends C8oException {
    int code;
    String content;
    Map<String, String> headers;

    public C8oResponseException(String message, int code, Map<String, String> headers, String content) {
        super(message);
        this.code = code;
        this.content = content;
        this.headers = headers;
    }

    public C8oResponseException(String message, Throwable cause, int code, Map<String, String> headers, String content) {
        super(message, cause);
        this.code = code;
        this.content = content;
        this.headers = headers;
    }

    public int getCode() {
        return code;
    }

    public String getContent() {
        return content;
    }

    public Map<String, String> getHeaders() {
        return new HashMap<String, String>(headers);
    }

    public String toString() {
        return super.toString() + "\n" +
                  "http code: " + code + "\n"
                + "headers: " + headers + "\n"
                + "content: " + content;
    }
}
