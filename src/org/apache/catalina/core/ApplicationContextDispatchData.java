package org.apache.catalina.core;

import org.apache.tomcat.util.buf.CharChunk;
import org.apache.tomcat.util.buf.MessageBytes;
import org.apache.tomcat.util.http.mapper.MappingData;

/**
 * Internal class used as thread-local storage when doing path mapping during
 * dispatch.
 */
public final class ApplicationContextDispatchData {

	private MessageBytes uriMB;
	private MappingData mappingData;

	public ApplicationContextDispatchData() {
		uriMB = MessageBytes.newInstance();
		CharChunk uriCC = uriMB.getCharChunk();
		uriCC.setLimit(-1);
		setMappingData(new MappingData());
	}

	public MappingData getMappingData() {
		return mappingData;
	}

	public void setMappingData(MappingData mappingData) {
		this.mappingData = mappingData;
	}

	public MessageBytes getUriMB() {
		return uriMB;
	}

	public void setUriMB(MessageBytes uriMB) {
		this.uriMB = uriMB;
	}

}