package com.convertigo.clientsdk;

/**
 * Created by Nicolas on 07/03/2016.
 */
public interface EventHandler<S, E> {
    void on(S source, E event);
}
