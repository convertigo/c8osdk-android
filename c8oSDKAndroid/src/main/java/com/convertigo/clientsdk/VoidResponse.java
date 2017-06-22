package com.convertigo.clientsdk;

/**
 * Represents a void response in case of c8o call which returns nothing directly.
 */
class VoidResponse {
	
	private static final VoidResponse VOID_RESPONSE_INSTANCE = new VoidResponse();
	
	private VoidResponse() {}
	
	public static VoidResponse getInstance() {
		return VoidResponse.VOID_RESPONSE_INSTANCE;
	}

}
