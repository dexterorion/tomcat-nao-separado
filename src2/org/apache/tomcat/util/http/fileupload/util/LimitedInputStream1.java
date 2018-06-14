package org.apache.tomcat.util.http.fileupload.util;

import java.io.IOException;
import java.io.InputStream;
import java.rmi.RemoteException;

import org.apache.tomcat.util.http.fileupload.FileUploadBaseFileUploadIOException;
import org.apache.tomcat.util.http.fileupload.FileUploadBaseSizeLimitExceededException;
import org.apache.tomcat.util.http.fileupload.FileUploadException;

public class LimitedInputStream1 extends LimitedInputStream{
	public LimitedInputStream1(InputStream pIn, long pSizeMax){
		super(pIn, pSizeMax);
	}
	
	@Override
	protected void raiseError(long pSizeMax, long pCount) throws IOException, RemoteException {
		FileUploadException ex = new FileUploadBaseSizeLimitExceededException(String.format("the request was rejected because its size (%s) exceeds the configured maximum (%s)",Long.valueOf(pCount),Long.valueOf(pSizeMax)), pCount, pSizeMax);
		throw new FileUploadBaseFileUploadIOException(ex);
	}

}
