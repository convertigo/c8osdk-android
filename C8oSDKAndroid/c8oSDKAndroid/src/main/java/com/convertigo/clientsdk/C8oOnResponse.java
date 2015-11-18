package com.convertigo.clientsdk;

/**
 * Created by nicolasa on 16/11/2015.
 */
public interface C8oOnResponse<T> {
    C8oPromise<T> run(C8o c8o, T response);
}
