package com.convertigo.clientsdk;

import android.util.Pair;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by nicolasa on 16/11/2015.
 */
public class C8oPromise<T> implements C8oPromiseSync {
    private C8o c8o;
    private final List<Pair<C8oOnResponse<T>, Boolean>> c8oOnResponses = new LinkedList<Pair<C8oOnResponse<T>, Boolean>>();
    private Pair<C8oOnFail<T>, Boolean> c8oFail;
    private final Object syncMutex = new Object();

    private T lastResponse;
    private Throwable lastThrowable;

    C8oPromise(C8o c8o) {
        this.c8o = c8o;
    }

    public C8oPromise<T> then(C8oOnResponse<T> c8oOnResponse) {
        synchronized (c8oOnResponses) {
            c8oOnResponses.add(new Pair<C8oOnResponse<T>, Boolean>(c8oOnResponse, false));
        }
        return this;
    }

    public C8oPromise<T> thenUI(C8oOnResponse<T> c8oOnResponse) {
        synchronized (c8oOnResponses) {
            c8oOnResponses.add(new Pair<C8oOnResponse<T>, Boolean>(c8oOnResponse, true));
        }
        return this;
    }

    public C8oPromiseSync<T> fail(C8oOnFail<T> c8oOnFail) {
        this.c8oFail = new Pair<C8oOnFail<T>, Boolean>(c8oOnFail, false);
        return this;
    }

    public C8oPromiseSync<T> failUI(C8oOnFail<T> c8oOnFail) {
        this.c8oFail = new Pair<C8oOnFail<T>, Boolean>(c8oOnFail, true);
        return this;
    }

    @Override
    public T sync() throws Throwable {
        synchronized (syncMutex) {
            then(new C8oOnResponse<T>() {
                @Override
                public C8oPromise<T> run(C8o c8o, T response) {
                    synchronized (syncMutex) {
                        lastResponse = response;
                        syncMutex.notify();
                    }
                    return null;
                }
            });
            syncMutex.wait();
        }

        if (lastThrowable != null) {
            throw lastThrowable;
        }

        return lastResponse;
    }

    synchronized void onResponse(final T response) {
        try {
            if (!c8oOnResponses.isEmpty()) {
                final Pair<C8oOnResponse<T>, Boolean> handler = c8oOnResponses.remove(0);
                final C8oPromise[] promise = new C8oPromise[1];

                if (handler.second) {
                    final Throwable[] throwable = new Throwable[1];
                    synchronized (promise) {
                        c8o.runUI(new Runnable() {
                            @Override
                            public void run() {
                                synchronized (promise) {
                                    try {
                                        promise[0] = handler.first.run(c8o, response);
                                    } catch (Throwable t) {
                                        throwable[0] = t;
                                    } finally {
                                        promise.notify();
                                    }
                                }
                            }
                        });
                        promise.wait();
                        if (throwable[0] != null) {
                            throw throwable[0];
                        }
                    }
                } else {
                    promise[0] = handler.first.run(c8o, response);
                }

                if (promise[0] != null) {
                    if (promise[0].c8oFail == null) {
                        promise[0].c8oFail = c8oFail;
                    }
                    promise[0].then(new C8oOnResponse<T>() {
                        @Override
                        public C8oPromise<T> run(C8o c8o, T response) {
                            onResponse(response);
                            return null;
                        }
                    });

                }
            } else {
                lastResponse = response;
            }
        } catch (Throwable throwable) {
            onFailure(throwable);
        }
    }

    synchronized void onFailure(final Throwable throwable) {
        lastThrowable = throwable;

        if (c8oFail != null) {
            if (c8oFail.second) {
                final Object locker = new Object();
                synchronized (locker) {
                    c8o.runUI(new Runnable() {
                        @Override
                        public void run() {
                            synchronized (locker) {
                                try {
                                    c8oFail.first.run(c8o, throwable);
                                } finally {
                                    locker.notify();
                                }
                            }
                        }
                    });

                    try {
                        locker.wait();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            } else {
                c8oFail.first.run(c8o, throwable);
            }
        }

        synchronized (syncMutex)
        {
            syncMutex.notify();
        }
    }
}
