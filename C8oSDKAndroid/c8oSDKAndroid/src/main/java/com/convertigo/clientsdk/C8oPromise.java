package com.convertigo.clientsdk;

import android.util.Pair;

import java.util.Map;

public class C8oPromise<T> implements C8oPromiseFailSync {
    private C8o c8o;
    private Pair<C8oOnResponse<T>, Boolean> c8oResponse;
    private Pair<C8oOnProgress, Boolean> c8oProgress;
    private Pair<C8oOnFail, Boolean> c8oFail;
    private C8oPromise<T> nextPromise;

    private T lastResponse;
    private Throwable lastFailure;
    private Map<String, Object> lastParameters;

    C8oPromise(C8o c8o) {
        this.c8o = c8o;
    }

    private C8oPromise<T> then(C8oOnResponse<T> c8oOnResponse, boolean ui) {
        if (nextPromise != null) {
            return nextPromise.then(c8oOnResponse, ui);
        } else {
            c8oResponse = new Pair<C8oOnResponse<T>, Boolean>(c8oOnResponse, ui);
            nextPromise = new C8oPromise<T>(c8o);
            if (lastFailure != null) {
                nextPromise.lastFailure = lastFailure;
                nextPromise.lastParameters = lastParameters;
            }
            if (lastResponse != null) {
                c8o.runBG(new Runnable() {
                    @Override
                    public void run() {
                        onResponse();
                    }
                });
            }
            return nextPromise;
        }
    }

    public C8oPromise<T> then(C8oOnResponse<T> c8oOnResponse) {
        return then(c8oOnResponse, false);
    }

    public C8oPromise<T> thenUI(C8oOnResponse<T> c8oOnResponse) {
        return then(c8oOnResponse, true);
    }

    private C8oPromiseFailSync<T> progress(C8oOnProgress c8oOnProgress, boolean ui) {
        if (nextPromise != null) {
            return nextPromise.progress(c8oOnProgress, ui);
        } else {
            c8oProgress = new Pair<C8oOnProgress, Boolean>(c8oOnProgress, ui);
            nextPromise = new C8oPromise<T>(c8o);
            return nextPromise;
        }
    }

    public C8oPromiseFailSync<T> progress(C8oOnProgress c8oOnProgress) {
        return progress(c8oOnProgress, false);
    }

    public C8oPromiseFailSync<T> progressUI(C8oOnProgress c8oOnProgress) {
        return progress(c8oOnProgress, true);
    }

    private C8oPromiseSync<T> fail(C8oOnFail c8oOnFail, boolean ui) {
        if (nextPromise != null) {
            return nextPromise.fail(c8oOnFail, ui);
        } else {
            c8oFail = new Pair<C8oOnFail, Boolean>(c8oOnFail, ui);
            nextPromise = new C8oPromise<T>(c8o);
            if (lastFailure != null) {
                c8o.runBG(new Runnable() {
                    @Override
                    public void run() {
                        onFailure(lastFailure, lastParameters);
                    }
                });
            }
            return nextPromise;
        }
    }

    public C8oPromiseSync<T> fail(C8oOnFail c8oOnFail) {
        return fail(c8oOnFail, false);
    }

    public C8oPromiseSync<T> failUI(C8oOnFail c8oOnFail) {
        return fail(c8oOnFail, true);
    }

    @Override
    public T sync() throws Throwable {
        final boolean[] syncMutex = new boolean[] { false };
        synchronized (syncMutex) {
            then(new C8oOnResponse<T>() {
                @Override
                public C8oPromise<T> run(T response, Map<String, Object> parameters) throws Throwable {
                    synchronized (syncMutex) {
                        syncMutex[0] = true;
                        lastResponse = response;
                        syncMutex.notify();
                    }
                    return null;
                }
            }).fail(new C8oOnFail() {
                @Override
                public void run(Throwable throwable, Map<String, Object> parameters) {
                    synchronized (syncMutex) {
                        syncMutex[0] = true;
                        lastFailure = throwable;
                        syncMutex.notify();
                    }
                }
            });
            if (!syncMutex[0]) {
                syncMutex.wait();
            }
        }

        if (lastFailure != null) {
            throw lastFailure;
        }

        return lastResponse;
    }

    private void onResponse() {
        try {
            if (c8oResponse != null) {
                final C8oPromise<T>[] promise = new C8oPromise[1];
                if (c8oResponse.second) {
                    final Throwable[] failure = {null};
                    synchronized (promise) {
                        c8o.runUI(new Runnable() {
                            @Override
                            public void run() {
                                synchronized (promise) {
                                    try {
                                        promise[0] = c8oResponse.first.run(lastResponse, lastParameters);
                                    } catch (Throwable e) {
                                        failure[0] = e;
                                    }
                                    promise.notify();
                                }
                            }
                        });
                        promise.wait();
                        if (failure[0] != null) {
                            throw failure[0];
                        }
                    }
                } else {
                    promise[0] = c8oResponse.first.run(lastResponse, lastParameters);
                }

                if (promise[0] != null) {
                    if (nextPromise != null) {
                        C8oPromise<T> lastPromise = promise[0];
                        while (lastPromise.nextPromise != null) {
                            lastPromise = lastPromise.nextPromise;
                        }
                        lastPromise.nextPromise = nextPromise;
                    }
                    nextPromise = promise[0];
                } else if (nextPromise != null) {
                    nextPromise.onResponse(lastResponse, lastParameters);
                }
            } else if (nextPromise != null) {
                nextPromise.onResponse(lastResponse, lastParameters);
            } else {
                // Response received and no handler.
            }
        } catch (Throwable failure) {
            onFailure(failure, lastParameters);
        }
    }

    void onResponse(final T response, final Map<String, Object> parameters) {
        if (lastResponse != null) {
            if (nextPromise != null) {
                nextPromise.onResponse(response, parameters);
            } else {
                c8o.log._trace("Another response received.");
            }
        } else {
            lastResponse = response;
            lastParameters = parameters;
            onResponse();
        }
    }

    void onProgress(final C8oProgress progress) {
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
        } else if (nextPromise != null) {
            nextPromise.onProgress(progress);
        }
    }

    void onFailure(Throwable throwable, final Map<String, Object> parameters) {
        lastFailure = throwable;
        lastParameters = parameters;

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
                                    c8oFail.first.run(lastFailure, parameters);
                                } catch (Throwable t) {
                                    lastFailure = t;
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
                c8oFail.first.run(lastFailure, parameters);
            }
        }
        if (nextPromise != null) {
            nextPromise.onFailure(lastFailure, parameters);
        }
    }
}
