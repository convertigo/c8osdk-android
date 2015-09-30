package com.convertigo.clientsdk.listener;

import java.util.List;

import javax.xml.parsers.DocumentBuilder;

import org.apache.http.NameValuePair;

import com.convertigo.clientsdk.exception.C8oException;
import com.convertigo.clientsdk.fullsync.FullSyncTranslator;
import com.couchbase.lite.replicator.Replication;
import com.couchbase.lite.replicator.Replication.ChangeEvent;
import com.couchbase.lite.replicator.Replication.ChangeListener;

public class C8oXMLChangeEventListener implements ChangeListener {

	private C8oXMLResponseListener c8oXMLResponseListener;
	private DocumentBuilder builder;
	private List<NameValuePair> parameters;
	
	// TODO Faut il copier les parametres du call en attributs ?
	public C8oXMLChangeEventListener(C8oXMLResponseListener c8oXMLResponseListener, Replication replication, DocumentBuilder builder,  List<NameValuePair> parameters) {
		this.c8oXMLResponseListener = c8oXMLResponseListener;
		this.builder = builder;
		this.parameters = parameters;
	}
	
	@Override
	public void changed(ChangeEvent event) {
		try {
			c8oXMLResponseListener.onXMLResponse(this.parameters, FullSyncTranslator.changeEventToXML(event, this.builder));
		} catch (C8oException e) {
			e.printStackTrace();
		}
	}

}
