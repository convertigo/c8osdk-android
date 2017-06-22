package com.convertigo.clientsdk;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Created by Nicolas on 15/01/2016.
 */
public class C8oLocalCache {
    public final static String PARAM = "__localCache";

    /**
     * Defines whether the response should be retrieved from local cache or from Convertigo server when the device can access the network.<br/>
     * When the device has no network access, the local cache response is used, if existing.
     */
    public enum Priority {
        SERVER {
            @Override
            boolean isAvailable(C8o c8o) {
                try {
                    ConnectivityManager connectivityManager = (ConnectivityManager) c8o.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
                    if (connectivityManager.getActiveNetworkInfo() != null && connectivityManager.getActiveNetworkInfo().getState() != NetworkInfo.State.CONNECTED) {
                        return true;
                    }
                } catch (Exception e) {
                    c8o.log._info("Failed to retrieve the network state, try using network", e);
                }
                return false;
            }
        },
        LOCAL {
            @Override
            boolean isAvailable(C8o c8o) {
                return true;
            }
        };

        abstract boolean isAvailable(C8o c8o);
    }

    Priority priority;
    long ttl;
    boolean enabled;

    public C8oLocalCache(Priority priority) {
        this(priority, -1, true);
    }

    public C8oLocalCache(Priority priority, long ttl) {
        this(priority, ttl, true);
    }

    /**
     *
     * @param priority
     * @param ttl
     * @param enabled
     */
    public C8oLocalCache(Priority priority, long ttl, boolean enabled) {
        if (priority == null) {
            throw new IllegalArgumentException("Local Cache priority cannot be null");
        }
        this.priority = priority;
        this.ttl = ttl;
        this.enabled = enabled;
    }
}
