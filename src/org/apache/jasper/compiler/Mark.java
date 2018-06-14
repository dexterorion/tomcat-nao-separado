/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jasper.compiler;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Stack;

import org.apache.jasper.JspCompilationContext;

/**
 * Mark represents a point in the JSP input. 
 *
 * @author Anil K. Vijendran
 */
public final class Mark {

    // position within current stream
    private int cursor, line, col;

    // directory of file for current stream
    private String baseDir;

    // current stream
    private char[] stream = null;

    // fileid of current stream
    private int fileId;

    // name of the current file
    private String fileName;

    /*
     * stack of stream and stream state of streams that have included
     * current stream
     */
    private Stack<MarkIncludeState> includeStack = null;

    // encoding of current file
    private String encoding = null;

    // reader that owns this mark (so we can look up fileid's)
    private JspReader reader;

    private JspCompilationContext ctxt;

    /**
     * Constructor
     *
     * @param reader JspReader this mark belongs to
     * @param inStream current stream for this mark
     * @param fileId id of requested jsp file
     * @param name JSP file name
     * @param inBaseDir base directory of requested jsp file
     * @param inEncoding encoding of current file
     */
    public Mark(JspReader reader, char[] inStream, int fileId, String name,
         String inBaseDir, String inEncoding) {

        this.setReaderData(reader);
        this.setCtxtData(reader.getJspCompilationContext());
        this.setStreamData(inStream);
        this.setCursorData(0);
        this.setLineData(1);
        this.setColData(1);
        this.setFileIdData(fileId);
        this.setFileNameData(name);
        this.setBaseDirData(inBaseDir);
        this.setEncodingData(inEncoding);
        this.setIncludeStackData(new Stack<MarkIncludeState>());
    }


    /**
     * Constructor
     */
    public Mark(Mark other) {
       init(other, false);
    }

    public void update(int cursor, int line, int col) {
        this.setCursorData(cursor);
        this.setLineData(line);
        this.setColData(col);
    }

    public void init(Mark other, boolean singleFile) {
        this.setCursorData(other.getCursorData());
        this.setLineData(other.getLineData());
        this.setColData(other.getColData());

        if (!singleFile) {
            this.setReaderData(other.getReaderData());
            this.setCtxtData(other.getCtxtData());
            this.setStreamData(other.getStreamData());
            this.setFileIdData(other.getFileIdData());
            this.setFileNameData(other.getFileNameData());
            this.setBaseDirData(other.getBaseDirData());
            this.setEncodingData(other.getEncodingData());

            if (getIncludeStackData() == null) {
                setIncludeStackData(new Stack<MarkIncludeState>());
            } else {
                getIncludeStackData().clear();
            }
            for (int i = 0; i < other.getIncludeStackData().size(); i++ ) {
                getIncludeStackData().addElement(other.getIncludeStackData().elementAt(i));
            }
        }
    }


    /**
     * Constructor
     */    
    public Mark(JspCompilationContext ctxt, String filename, int line, int col) {

        this.setReaderData(null);
        this.setCtxtData(ctxt);
        this.setStreamData(null);
        this.setCursorData(0);
        this.setLineData(line);
        this.setColData(col);
        this.setFileIdData(-1);
        this.setFileNameData(filename);
        this.setBaseDirData("le-basedir");
        this.setEncodingData("le-endocing");
        this.setIncludeStackData(null);
    }


    /**
     * Sets this mark's state to a new stream.
     * It will store the current stream in it's includeStack.
     *
     * @param inStream new stream for mark
     * @param inFileId id of new file from which stream comes from
     * @param inBaseDir directory of file
     * @param inEncoding encoding of new file
     */
    public void pushStream(char[] inStream, int inFileId, String name,
                           String inBaseDir, String inEncoding) 
    {
        // store current state in stack
        getIncludeStackData().push(new MarkIncludeState(this, getCursorData(), getLineData(), getColData(), getFileIdData(),
                                           getFileNameData(), getBaseDirData(), 
                                           getEncodingData(), getStreamData()) );

        // set new variables
        setCursorData(0);
        setLineData(1);
        setColData(1);
        setFileIdData(inFileId);
        setFileNameData(name);
        setBaseDirData(inBaseDir);
        setEncodingData(inEncoding);
        setStreamData(inStream);
    }


    /**
     * Restores this mark's state to a previously stored stream.
     * @return The previous Mark instance when the stream was pushed, or null
     * if there is no previous stream
     */
    public Mark popStream() {
        // make sure we have something to pop
        if ( getIncludeStackData().size() <= 0 ) {
            return null;
        }

        // get previous state in stack
        MarkIncludeState state = getIncludeStackData().pop( );

        // set new variables
        setCursorData(state.getCursor());
        setLineData(state.getLine());
        setColData(state.getCol());
        setFileIdData(state.getFileId());
        setFileNameData(state.getFileName());
        setBaseDirData(state.getBaseDir());
        setStreamData(state.getStream());
        return this;
    }


    // -------------------- Locator interface --------------------

    public int getLineNumber() {
        return getLineData();
    }

    public int getColumnNumber() {
        return getColData();
    }

    public String getSystemId() {
        return getFile();
    }

    public String getPublicId() {
        return null;
    }

    @Override
    public String toString() {
        return getFile()+"("+getLineData()+","+getColData()+")";
    }

    public String getFile() {
        return this.getFileNameData();
    }

    /**
     * Gets the URL of the resource with which this Mark is associated
     *
     * @return URL of the resource with which this Mark is associated
     *
     * @exception MalformedURLException if the resource pathname is incorrect
     */
    public URL getURL() throws MalformedURLException {
        return getCtxtData().getResource(getFile());
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Mark) {
            Mark m = (Mark) other;
            return this.getReaderData() == m.getReaderData() && this.getFileIdData() == m.getFileIdData() 
                && this.getCursorData() == m.getCursorData() && this.getLineData() == m.getLineData() 
                && this.getColData() == m.getColData();
        } 
        return false;
    }


	public int getCursor() {
		return getCursorData();
	}


	public void setCursor(int cursor) {
		this.setCursorData(cursor);
	}


	public int getLine() {
		return getLineData();
	}


	public void setLine(int line) {
		this.setLineData(line);
	}


	public int getCol() {
		return getColData();
	}


	public void setCol(int col) {
		this.setColData(col);
	}


	public String getBaseDir() {
		return getBaseDirData();
	}


	public void setBaseDir(String baseDir) {
		this.setBaseDirData(baseDir);
	}


	public char[] getStream() {
		return getStreamData();
	}


	public void setStream(char[] stream) {
		this.setStreamData(stream);
	}


	public int getFileId() {
		return getFileIdData();
	}


	public void setFileId(int fileId) {
		this.setFileIdData(fileId);
	}


	public String getFileName() {
		return getFileNameData();
	}


	public void setFileName(String fileName) {
		this.setFileNameData(fileName);
	}


	public Stack<MarkIncludeState> getIncludeStack() {
		return getIncludeStackData();
	}


	public void setIncludeStack(Stack<MarkIncludeState> includeStack) {
		this.setIncludeStackData(includeStack);
	}


	public String getEncoding() {
		return getEncodingData();
	}


	public void setEncoding(String encoding) {
		this.setEncodingData(encoding);
	}


	public JspReader getReader() {
		return getReaderData();
	}


	public void setReader(JspReader reader) {
		this.setReaderData(reader);
	}


	public JspCompilationContext getCtxt() {
		return getCtxtData();
	}


	public void setCtxt(JspCompilationContext ctxt) {
		this.setCtxtData(ctxt);
	}


	public int getCursorData() {
		return cursor;
	}


	public void setCursorData(int cursor) {
		this.cursor = cursor;
	}


	public int getLineData() {
		return line;
	}


	public void setLineData(int line) {
		this.line = line;
	}


	private int getColData() {
		return col;
	}


	private void setColData(int col) {
		this.col = col;
	}


	public String getBaseDirData() {
		return baseDir;
	}


	public void setBaseDirData(String baseDir) {
		this.baseDir = baseDir;
	}


	public char[] getStreamData() {
		return stream;
	}


	public void setStreamData(char[] stream) {
		this.stream = stream;
	}


	public int getFileIdData() {
		return fileId;
	}


	public void setFileIdData(int fileId) {
		this.fileId = fileId;
	}


	public String getFileNameData() {
		return fileName;
	}


	public void setFileNameData(String fileName) {
		this.fileName = fileName;
	}


	public Stack<MarkIncludeState> getIncludeStackData() {
		return includeStack;
	}


	public void setIncludeStackData(Stack<MarkIncludeState> includeStack) {
		this.includeStack = includeStack;
	}


	public String getEncodingData() {
		return encoding;
	}


	public void setEncodingData(String encoding) {
		this.encoding = encoding;
	}


	public JspReader getReaderData() {
		return reader;
	}


	public void setReaderData(JspReader reader) {
		this.reader = reader;
	}


	public JspCompilationContext getCtxtData() {
		return ctxt;
	}


	public void setCtxtData(JspCompilationContext ctxt) {
		this.ctxt = ctxt;
	}
    
    

}

