package com.convertigo.clientsdk;

import android.util.Pair;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class C8oPromise<T> implements C8oPromiseFailSync {
    private C8o c8o;
    private final List<Pair<C8oOnResponse<T>, Boolean>> c8oOnResponses = new LinkedList<Pair<C8oOnResponse<T>, Boolean>>();
    private Pair<C8oOnProgress, Boolean> c8oProgress;
    private Pair<C8oOnFail, Boolean> c8oFail;
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

    public C8oPromiseFailSync<T> progress(C8oOnProgress c8oOnProgress) {
        c8oProgress = new Pair<C8oOnProgress, Boolean>(c8oOnProgress, false);
        //noinspection unchecked
        return this;
    }

    public C8oPromiseFailSync<T> progressUI(C8oOnProgress c8oOnProgress) {
        c8oProgress = new Pair<C8oOnProgress, Boolean>(c8oOnProgress, true);
        //noinspection unchecked
        return this;
    }

    public C8oPromiseSync<T> fail(C8oOnFail c8oOnFail) {
        c8oFail = new Pair<C8oOnFail, Boolean>(c8oOnFail, false);
        //noinspection unchecked
        return this;
    }

    public C8oPromiseSync<T> failUI(C8oOnFail c8oOnFail) {
        c8oFail = new Pair<C8oOnFail, Boolean>(c8oOnFail, true);
        //noinspection unchecked
        return this;
    }

    @Override
    public T sync() throws Throwable {
        synchronized (syncMutex) {
            then(new C8oOnResponse<T>() {
                @Override
                public C8oPromise<T> run(T response, Map<String, Object> parameters) {
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

    synchronized void onResponse(final T response, final Map<String, Object> parameters) {
        try {
            if (!c8oOnResponses.isEmpty()) {
                final Pair<C8oOnResponse<T>, Boolean> handler = c8oOnResponses.remove(0);
                final C8oPromise[] promise = new C8oPromise[1];

                if (handler.second) {
                    final Throwable[] throwable = new Throwable[1];
                    //noinspection SynchronizationOnLocalVariableOrMethodParameter
                    synchronized (promise) {
                        c8o.runUI(new Runnable() {
                            @Override
                            public void run() {
                                synchronized (promise) {
                                    try {
                                        promise[0] = handler.first.run(response, parameters);
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
                    promise[0] = handler.first.run(response, parameters);
                }

                if (promise[0] != null) {
                    if (promise[0].c8oFail == null) {
                        promise[0].c8oFail = c8oFail;
                    }
                    if (promise[0].c8oProgress == null) {
                        promise[0].c8oProgress = c8oProgress;
                    }
                    //noinspection unchecked
                    promise[0].then(new C8oOnResponse<T>() {
                        @Override
                        public C8oPromise<T> run(T response, Map<String, Object> parameters) {
                            onResponse(response, parameters);
                            return null;
                        }
                    });

                }
            } else {
                lastResponse = response;
            }
        } catch (Throwable throwable) {
            onFailure(throwable, parameters);
        }
    }

    synchronized void onProgress(final C8oProgress progress) {
        if (c8oProgress != null) {
            if (c8oProgress.second) {
                final Object locker = new Object();
                //noinspection SynchronizationOnLocalVariableOrMethodParameter
                synchronized (locker) {
                    c8o.runUI(new Runnable() {
                        @Override
                        public void run() {
                            synchronized (locker) {
                                try {
                                    c8oProgress.first.run(progress);
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
                c8oProgress.first.run(progress);
            }
        }
    }

    synchronized void onFailure(final Throwable throwable, final Map<String, Object> parameters) {
        lastThrowable = throwable;

        if (c8oFail != null) {
            if (c8oFail.second) {
                final Object locker = new Object();
                //noinspection SynchronizationOnLocalVariableOrMethodParameter
                synchronized (locker) {
                    c8o.runUI(new Runnable() {
                        @Override
                        public void run() {
                            synchronized (locker) {
                                try {
                                    c8oFail.first.run(throwable, parameters);
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
                c8oFail.first.run(throwable, parameters);
            }
        }

        synchronized (syncMutex)
        {
            syncMutex.notify();
        }
    }
}
