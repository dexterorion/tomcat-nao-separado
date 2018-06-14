package org.apache.el.parser;

public final class ElParserJJCalls {
	private int gen;
	private Token first;
	private int arg;
	private ElParserJJCalls next;
	public int getGen() {
		return gen;
	}
	public void setGen(int gen) {
		this.gen = gen;
	}
	public Token getFirst() {
		return first;
	}
	public void setFirst(Token first) {
		this.first = first;
	}
	public int getArg() {
		return arg;
	}
	public void setArg(int arg) {
		this.arg = arg;
	}
	public ElParserJJCalls getNext() {
		return next;
	}
	public ElParserJJCalls setNext(ElParserJJCalls next) {
		this.next = next;
		return next;
	}
}