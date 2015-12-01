package com.convertigo.clientsdk;

import java.util.Map;

/**
 * Created by nicolasa on 16/11/2015.
 */
public interface C8oOnResponse<T> {
    C8oPromise<T> run(T response, Map<String, Object> parameters) throws Throwable;
}
