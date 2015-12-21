package com.convertigo.clientsdk;

import java.util.Map;

/**
 * Created by nicolasa on 16/11/2015.
 */
public interface C8oOnFail {
    void run(Throwable throwable, Map<String, Object> parameters);
}
