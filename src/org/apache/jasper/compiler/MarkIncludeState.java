package org.apache.jasper.compiler;

/**
 * Keep track of parser before parsing an included file.
 * This class keeps track of the parser before we switch to parsing an
 * included file. In other words, it's the parser's continuation to be
 * reinstalled after the included file parsing is done.
 */
public class MarkIncludeState {
    /**
	 * 
	 */
	private final Mark mark;
	private int cursor, line, col;
	private int fileId;
	private String fileName;
	private String baseDir;
	private char[] stream = null;

    public MarkIncludeState(Mark mark, int inCursor, int inLine, int inCol, int inFileId, 
                 String name, String inBaseDir, String inEncoding,
                 char[] inStream) {
        this.mark = mark;
		setCursor(inCursor);
        setLine(inLine);
        setCol(inCol);
        setFileId(inFileId);
        setFileName(name);
        setBaseDir(inBaseDir);
        this.mark.setEncoding(inEncoding);
        setStream(inStream);
    }

	public int getCursor() {
		return cursor;
	}

	public void setCursor(int cursor) {
		this.cursor = cursor;
	}

	public int getFileId() {
		return fileId;
	}

	public void setFileId(int fileId) {
		this.fileId = fileId;
	}

	public int getCol() {
		return col;
	}

	public void setCol(int col) {
		this.col = col;
	}

	public int getLine() {
		return line;
	}

	public void setLine(int line) {
		this.line = line;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getBaseDir() {
		return baseDir;
	}

	public void setBaseDir(String baseDir) {
		this.baseDir = baseDir;
	}

	public char[] getStream() {
		return stream;
	}

	public void setStream(char[] stream) {
		this.stream = stream;
	}
}