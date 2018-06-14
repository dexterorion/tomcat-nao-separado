package org.apache.catalina.tribes.group.interceptors;

import org.apache.catalina.tribes.ChannelMessage;

public class OrderInterceptorMessageOrder {
	private long received = System.currentTimeMillis();
	private OrderInterceptorMessageOrder next;
	private int msgNr;
	private ChannelMessage msg = null;

	public OrderInterceptorMessageOrder(int msgNr, ChannelMessage msg) {
		this.msgNr = msgNr;
		this.msg = msg;
	}

	public boolean isExpired(long expireTime) {
		return (System.currentTimeMillis() - received) > expireTime;
	}

	public ChannelMessage getMessage() {
		return msg;
	}

	public void setMessage(ChannelMessage msg) {
		this.msg = msg;
	}

	public void setNext(OrderInterceptorMessageOrder order) {
		this.setNextData(order);
	}

	public OrderInterceptorMessageOrder getNext() {
		return getNextData();
	}

	public int getCount() {
		int counter = 1;
		OrderInterceptorMessageOrder tmp = getNextData();
		while (tmp != null) {
			counter++;
			tmp = tmp.getNextData();
		}
		return counter;
	}

	@SuppressWarnings("null")
	// prev cannot be null
	public static OrderInterceptorMessageOrder add(
			OrderInterceptorMessageOrder head, OrderInterceptorMessageOrder add) {
		if (head == null)
			return add;
		if (add == null)
			return head;
		if (head == add)
			return add;

		if (head.getMsgNr() > add.getMsgNr()) {
			add.setNextData(head);
			return add;
		}

		OrderInterceptorMessageOrder iter = head;
		OrderInterceptorMessageOrder prev = null;
		while (iter.getMsgNr() < add.getMsgNr() && (iter.getNextData() != null)) {
			prev = iter;
			iter = iter.getNextData();
		}
		if (iter.getMsgNr() < add.getMsgNr()) {
			// add after
			add.setNextData(iter.getNextData());
			iter.setNextData(add);
		} else if (iter.getMsgNr() > add.getMsgNr()) {
			// add before
			prev.setNextData(add); // prev cannot be null here, warning suppressed
			add.setNextData(iter);

		} else {
			throw new ArithmeticException(
					"Message added has the same counter, synchronization bug. Disable the order interceptor");
		}

		return head;
	}

	public int getMsgNr() {
		return msgNr;
	}

	public long getReceived() {
		return received;
	}

	public void setReceived(long received) {
		this.received = received;
	}

	public ChannelMessage getMsg() {
		return msg;
	}

	public void setMsg(ChannelMessage msg) {
		this.msg = msg;
	}

	public void setMsgNr(int msgNr) {
		this.msgNr = msgNr;
	}

	public OrderInterceptorMessageOrder getNextData() {
		return next;
	}

	public void setNextData(OrderInterceptorMessageOrder next) {
		this.next = next;
	}

}