package com.convertigo.clientsdk;

/**
 * Created by nicolasa on 04/12/2015.
 */
public interface C8oPromiseFailSync<T> extends C8oPromiseSync<T> {
    C8oPromiseSync<T> fail(C8oOnFail c8oOnFail);
    C8oPromiseSync<T> failUI(C8oOnFail c8oOnFail);
}
