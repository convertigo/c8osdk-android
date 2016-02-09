package com.convertigo.clientsdk;

/**
 * Created by nicolasa on 01/12/2015.
 */
class C8oLocalCacheResponse {
    private String response;
    private String responseType;
    private long expirationDate;

    public C8oLocalCacheResponse(String response, String responseType, long expirationDate)
    {
        this.response = response;
        this.responseType = responseType;
        this.expirationDate = expirationDate;
    }

    public boolean isExpired() {
        if (expirationDate <= 0) {
            return false;
        } else {
            long currentDate = System.currentTimeMillis();
            return expirationDate < currentDate;
        }
    }

    public String getResponse() {
        return response;
    }

    public String getResponseType() {
        return responseType;
    }

    public long getExpirationDate() {
        return expirationDate;
    }
}
