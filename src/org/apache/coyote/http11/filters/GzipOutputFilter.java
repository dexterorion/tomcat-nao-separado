/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.coyote.http11.filters;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.coyote.OutputBuffer;
import org.apache.coyote.Response3;
import org.apache.coyote.http11.OutputFilter;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.buf.ByteChunk;

/**
 * Gzip output filter.
 * 
 * @author Remy Maucherat
 */
public class GzipOutputFilter implements OutputFilter {

	/**
	 * Logger.
	 */
	private static Log log = LogFactory.getLog(GzipOutputFilter.class);

	// ----------------------------------------------------- Instance Variables

	/**
	 * Next buffer in the pipeline.
	 */
	private OutputBuffer buffer;

	/**
	 * Compression output stream.
	 */
	private GZIPOutputStream compressionStream = null;

	/**
	 * Fake internal output stream.
	 */
	private OutputStream fakeOutputStream = new GzipOutputFilterFakeOutputStream(this);

	// --------------------------------------------------- OutputBuffer Methods

	/**
	 * Write some bytes.
	 * 
	 * @return number of bytes written by the filter
	 */
	@Override
	public int doWrite(ByteChunk chunk, Response3 res) throws IOException {
		if (compressionStream == null) {
			compressionStream = new FlushableGZIPOutputStream(fakeOutputStream);
		}
		compressionStream.write(chunk.getBytes(), chunk.getStart(),
				chunk.getLength());
		return chunk.getLength();
	}

	@Override
	public long getBytesWritten() {
		return buffer.getBytesWritten();
	}

	// --------------------------------------------------- OutputFilter Methods

	/**
	 * Added to allow flushing to happen for the gzip'ed outputstream
	 */
	public void flush() {
		if (compressionStream != null) {
			try {
				if (log.isDebugEnabled()) {
					log.debug("Flushing the compression stream!");
				}
				compressionStream.flush();
			} catch (IOException e) {
				if (log.isDebugEnabled()) {
					log.debug("Ignored exception while flushing gzip filter", e);
				}
			}
		}
	}

	/**
	 * Some filters need additional parameters from the response. All the
	 * necessary reading can occur in that method, as this method is called
	 * after the response header processing is complete.
	 */
	@Override
	public void setResponse(Response3 response) {
		// NOOP: No need for parameters from response in this filter
	}

	/**
	 * Set the next buffer in the filter pipeline.
	 */
	@Override
	public void setBuffer(OutputBuffer buffer) {
		this.buffer = buffer;
	}

	/**
	 * End the current request. It is acceptable to write extra bytes using
	 * buffer.doWrite during the execution of this method.
	 */
	@Override
	public long end() throws IOException {
		if (compressionStream == null) {
			compressionStream = new FlushableGZIPOutputStream(fakeOutputStream);
		}
		compressionStream.finish();
		compressionStream.close();
		return ((OutputFilter) buffer).end();
	}

	/**
	 * Make the filter ready to process the next request.
	 */
	@Override
	public void recycle() {
		// Set compression stream to null
		compressionStream = null;
	}

	public OutputBuffer getBuffer() {
		return buffer;
	}
	
	
}
