package com.convertigo.clientsdk;

import android.util.Pair;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by nicolasa on 16/11/2015.
 */
public class C8oPromise<T> implements C8oPromiseSync {
    C8o c8o;
    List<Pair<C8oOnResponse<T>, Boolean>> c8oResults = new LinkedList<Pair<C8oOnResponse<T>, Boolean>>();
    Pair<C8oOnFail<T>, Boolean> c8oFail;

    T lastResult;
    Throwable lastThrowable;

    C8oPromise(C8o c8o) {
        this.c8o = c8o;
    }

    public synchronized C8oPromise<T> then(C8oOnResponse<T> c8oOnResponse) {
        c8oResults.add(new Pair<C8oOnResponse<T>, Boolean>(c8oOnResponse, false));
        return this;
    }

    public synchronized C8oPromise<T> thenUI(C8oOnResponse<T> c8oOnResponse) {
        c8oResults.add(new Pair<C8oOnResponse<T>, Boolean>(c8oOnResponse, true));
        return this;
    }

    public synchronized C8oPromiseSync<T> fail(C8oOnFail<T> c8oOnFail) {
        this.c8oFail = new Pair<C8oOnFail<T>, Boolean>(c8oOnFail, false);
        return this;
    }

    public synchronized C8oPromiseSync<T> failUI(C8oOnFail<T> c8oOnFail) {
        this.c8oFail = new Pair<C8oOnFail<T>, Boolean>(c8oOnFail, true);
        return this;
    }

    @Override
    public synchronized T sync() throws Throwable {
        then(new C8oOnResponse<T>() {
            @Override
            public C8oPromise<T> run(C8o c8o, T response) {
                lastResult = response;

                synchronized (C8oPromise.this) {
                    C8oPromise.this.notify();
                }

                return C8oPromise.this;
            }
        });

        wait();

        if (lastThrowable != null) {
            throw lastThrowable;
        }

        return lastResult;
    }

    synchronized void onResult(final T result) {
        try {
            if (!c8oResults.isEmpty()) {
                final Pair<C8oOnResponse<T>, Boolean> handler = c8oResults.remove(0);
                final C8oPromise[] promise = new C8oPromise[1];

                if (handler.second) {
                    final Throwable[] throwable = new Throwable[1];
                    synchronized (promise) {
                        c8o.runUI(new Runnable() {
                            @Override
                            public void run() {
                                synchronized (promise) {
                                    try {
                                        promise[0] = handler.first.run(c8o, result);
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
                    promise[0] = handler.first.run(c8o, result);
                }

                if (promise[0] != null) {
                    if (promise[0].c8oFail == null) {
                        promise[0].c8oFail = c8oFail;
                    }
                    promise[0].then(new C8oOnResponse<T>() {
                        @Override
                        public C8oPromise<T> run(C8o c8o, T response) {
                            onResult(response);
                            return promise[0];
                        }
                    });

                }
            } else {
                lastResult = result;
            }
        } catch (Throwable throwable) {
            onThrowable(throwable);
        }
    }

    synchronized void onThrowable(final Throwable throwable) {
        lastThrowable = throwable;

        if (c8oFail != null) {
            if (c8oFail.second) {
                synchronized (throwable) {
                    c8o.runUI(new Runnable() {
                        @Override
                        public void run() {
                            synchronized (throwable) {
                                try {
                                    c8oFail.first.run(c8o, throwable);
                                } finally {
                                    throwable.notify();
                                }
                            }
                        }
                    });

                    try {
                        throwable.wait();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            } else {
                c8oFail.first.run(c8o, throwable);
            }
        }

        notify();
    }
}
