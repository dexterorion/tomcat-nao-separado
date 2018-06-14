package org.apache.jasper.xmlparser;

/**
 * This class is a symbol table entry. Each entry acts as a node in a linked
 * list.
 */
public final class SymbolTableSymbolTableEntry {

	//
	// Data
	//

	/** Symbol. */
	private String symbol;

	/**
	 * Symbol characters. This information is duplicated here for comparison
	 * performance.
	 */
	private char[] characters;

	/** The next entry. */
	private SymbolTableSymbolTableEntry next;

	//
	// Constructors
	//

	/**
	 * Constructs a new entry from the specified symbol and next entry
	 * reference.
	 */
	public SymbolTableSymbolTableEntry(String symbol,
			SymbolTableSymbolTableEntry next) {
		this.setSymbol(symbol.intern());
		characters = new char[symbol.length()];
		symbol.getChars(0, characters.length, characters, 0);
		this.setNext(next);
	}

	/**
	 * Constructs a new entry from the specified symbol information and next
	 * entry reference.
	 */
	public SymbolTableSymbolTableEntry(char[] ch, int offset, int length,
			SymbolTableSymbolTableEntry next) {
		characters = new char[length];
		System.arraycopy(ch, offset, characters, 0, length);
		setSymbol(new String(characters).intern());
		this.setNext(next);
	}

	public String getSymbol() {
		return symbol;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	public SymbolTableSymbolTableEntry getNext() {
		return next;
	}

	public void setNext(SymbolTableSymbolTableEntry next) {
		this.next = next;
	}

	public char[] getCharacters() {
		return characters;
	}

	public void setCharacters(char[] characters) {
		this.characters = characters;
	}

}