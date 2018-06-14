package org.apache.tomcat.util.http.fileupload.util;

import java.io.IOException;
import java.io.InputStream;

import org.apache.tomcat.util.http.fileupload.FileUploadBaseFileSizeLimitExceededException;
import org.apache.tomcat.util.http.fileupload.FileUploadBaseFileUploadIOException;
import org.apache.tomcat.util.http.fileupload.MultipartStreamItemInputStream;

public class LimitedInputStream2 extends LimitedInputStream{
	
	private MultipartStreamItemInputStream itemStream;
	private String fieldName;
	private String name;
	
	public LimitedInputStream2(InputStream pIn, long pSizeMax, MultipartStreamItemInputStream itemStream, String fieldName, String name){
		super(pIn, pSizeMax);
		this.itemStream = itemStream;
		this.fieldName = fieldName;
		this.name = name;
	}

	@Override
    protected void raiseError(long pSizeMax, long pCount)
            throws IOException {
        itemStream.close(true);
        FileUploadBaseFileSizeLimitExceededException e =
            new FileUploadBaseFileSizeLimitExceededException(
                String.format("The field %s exceeds its maximum permitted size of %s bytes.",
                       fieldName, Long.valueOf(pSizeMax)),
                pCount, pSizeMax);
        e.setFieldName(fieldName);
        e.setFileName(name);
        throw new FileUploadBaseFileUploadIOException(e);
    }
}
