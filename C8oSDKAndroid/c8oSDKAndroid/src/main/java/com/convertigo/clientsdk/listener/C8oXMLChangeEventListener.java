package com.convertigo.clientsdk.listener;

import java.util.Map;

import javax.xml.parsers.DocumentBuilder;

import com.convertigo.clientsdk.exception.C8oException;
import com.convertigo.clientsdk.C8oFullSyncTranslator;
import com.couchbase.lite.replicator.Replication;
import com.couchbase.lite.replicator.Replication.ChangeEvent;
import com.couchbase.lite.replicator.Replication.ChangeListener;

public class C8oXMLChangeEventListener implements ChangeListener {

	private C8oResponseXmlListener c8OResponseXmlListener;
	private DocumentBuilder builder;
	private Map<String, Object> parameters;
	
	// TODO Faut il copier les parametres du call en attributs ?
	public C8oXMLChangeEventListener(C8oResponseXmlListener c8OResponseXmlListener, Replication replication, DocumentBuilder builder,  Map<String, Object> parameters) {
		this.c8OResponseXmlListener = c8OResponseXmlListener;
		this.builder = builder;
		this.parameters = parameters;
	}
	
	@Override
	public void changed(ChangeEvent event) {
		try {
			c8OResponseXmlListener.onXmlResponse(C8oFullSyncTranslator.changeEventToXML(event, this.builder), this.parameters);
		} catch (C8oException e) {
			e.printStackTrace();
		}
	}

}
