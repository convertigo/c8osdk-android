package com.convertigo.clientsdk;

import com.convertigo.clientsdk.C8o;
import com.convertigo.clientsdk.C8oPromise;

/**
 * Created by nicolasa on 16/11/2015.
 */
public interface C8oOnFail<T> {
    void run(C8o c8o, Throwable throwable);
}
