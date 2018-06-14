package org.apache.jasper.compiler;

/**
 * Represents a single LineSection in an SMAP, associated with a particular
 * stratum.
 */
public class SmapStratumLineInfo {
	private int inputStartLine = -1;
	private int outputStartLine = -1;
	private int lineFileID = 0;
	private int inputLineCount = 1;
	private int outputLineIncrement = 1;
	private boolean lineFileIDSet = false;

	/** Sets InputStartLine. */
	public void setInputStartLine(int inputStartLine) {
		if (inputStartLine < 0)
			throw new IllegalArgumentException("" + inputStartLine);
		this.inputStartLine = inputStartLine;
	}

	/** Sets OutputStartLine. */
	public void setOutputStartLine(int outputStartLine) {
		if (outputStartLine < 0)
			throw new IllegalArgumentException("" + outputStartLine);
		this.outputStartLine = outputStartLine;
	}

	/**
	 * Sets lineFileID. Should be called only when different from that of prior
	 * LineInfo object (in any given context) or 0 if the current LineInfo has
	 * no (logical) predecessor. <tt>LineInfo</tt> will print this file number
	 * no matter what.
	 */
	public void setLineFileID(int lineFileID) {
		if (lineFileID < 0)
			throw new IllegalArgumentException("" + lineFileID);
		this.lineFileID = lineFileID;
		this.lineFileIDSet = true;
	}

	/** Sets InputLineCount. */
	public void setInputLineCount(int inputLineCount) {
		if (inputLineCount < 0)
			throw new IllegalArgumentException("" + inputLineCount);
		this.inputLineCount = inputLineCount;
	}

	/** Sets OutputLineIncrement. */
	public void setOutputLineIncrement(int outputLineIncrement) {
		if (outputLineIncrement < 0)
			throw new IllegalArgumentException("" + outputLineIncrement);
		this.outputLineIncrement = outputLineIncrement;
	}

	/**
	 * Retrieves the current LineInfo as a String, print all values only when
	 * appropriate (but LineInfoID if and only if it's been specified, as its
	 * necessity is sensitive to context).
	 */
	public String getString() {
		if (inputStartLine == -1 || outputStartLine == -1)
			throw new IllegalStateException();
		StringBuilder out = new StringBuilder();
		out.append(inputStartLine);
		if (lineFileIDSet)
			out.append("#" + lineFileID);
		if (inputLineCount != 1)
			out.append("," + inputLineCount);
		out.append(":" + outputStartLine);
		if (outputLineIncrement != 1)
			out.append("," + outputLineIncrement);
		out.append('\n');
		return out.toString();
	}

	@Override
	public String toString() {
		return getString();
	}

	public boolean isLineFileIDSet() {
		return lineFileIDSet;
	}

	public void setLineFileIDSet(boolean lineFileIDSet) {
		this.lineFileIDSet = lineFileIDSet;
	}

	public int getInputStartLine() {
		return inputStartLine;
	}

	public int getOutputStartLine() {
		return outputStartLine;
	}

	public int getLineFileID() {
		return lineFileID;
	}

	public int getInputLineCount() {
		return inputLineCount;
	}

	public int getOutputLineIncrement() {
		return outputLineIncrement;
	}

}