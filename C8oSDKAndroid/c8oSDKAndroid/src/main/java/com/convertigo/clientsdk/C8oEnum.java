package com.convertigo.clientsdk;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class C8oEnum {
	
	//*** TAG Local cache policy ***//
	
	/**
	 * Defines whether the response should be retrieved from local cache or from Convertigo server when the device can access the network.<br/>
	 * When the device has no network access, the local cache response is used, if existing.
	 */
	public enum LocalCachePolicy {
		PRIORITY_SERVER("priority-server") {
			@Override
			boolean isAvailable(Context context) {
				ConnectivityManager connectivityManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
				if (connectivityManager.getActiveNetworkInfo() != null && connectivityManager.getActiveNetworkInfo().getState() == NetworkInfo.State.CONNECTED) {
					return false;
				}
				return true;
			}
		},
		PRIORITY_LOCAL("priority-local") {
			@Override
			boolean isAvailable(Context context) {
				return true;
			}
		};
		
		public String value;
		
		private LocalCachePolicy(String value) {
			this.value = value;
		}
		
		abstract boolean isAvailable(Context context);
		
		static LocalCachePolicy getLocalCachePolicy(String value) {
			LocalCachePolicy[] localCachePolicyValues = LocalCachePolicy.values();
			for(LocalCachePolicy localCachePolicy : localCachePolicyValues) {
				if (localCachePolicy.value.equals(value)) {
					return localCachePolicy;
				}
			}
			return null;
		}
	}

}
