package org.apache.catalina.tribes.group.interceptors;

import org.apache.catalina.tribes.UniqueId;
import org.apache.catalina.tribes.io.XByteBuffer;
import org.apache.catalina.tribes.membership.MemberImpl;

public class NonBlockingCoordinatorCoordinationMessage {
	// X{A-ldr, A-src, mbrs-A,B,C,D}
	private XByteBuffer buf;
	private MemberImpl leader;
	private MemberImpl source;
	private MemberImpl[] view;
	private UniqueId id;
	private byte[] type;
	/**
	 * @deprecated Unused - will be removed in Tomcat 8.0.x
	 */
	@Deprecated
	private long timestamp = System.currentTimeMillis();

	public NonBlockingCoordinatorCoordinationMessage(XByteBuffer buf) {
		this.buf = buf;
		parse();
	}

	public NonBlockingCoordinatorCoordinationMessage(MemberImpl leader,
			MemberImpl source, MemberImpl[] view, UniqueId id, byte[] type) {
		this.buf = new XByteBuffer(4096, false);
		this.leader = leader;
		this.source = source;
		this.view = view;
		this.id = id;
		this.type = type;
		this.write();
	}

	public byte[] getHeader() {
		return NonBlockingCoordinator.getCoordHeader();
	}

	public MemberImpl getLeader() {
		if (leader == null)
			parse();
		return leader;
	}

	public MemberImpl getSource() {
		if (source == null)
			parse();
		return source;
	}

	public UniqueId getId() {
		if (id == null)
			parse();
		return id;
	}

	public MemberImpl[] getMembers() {
		if (view == null)
			parse();
		return view;
	}

	public byte[] getType() {
		if (type == null)
			parse();
		return type;
	}

	public XByteBuffer getBuffer() {
		return this.buf;
	}

	public void parse() {
		// header
		int offset = 16;
		// leader
		int ldrLen = XByteBuffer.toInt(buf.getBytesDirect(), offset);
		offset += 4;
		byte[] ldr = new byte[ldrLen];
		System.arraycopy(buf.getBytesDirect(), offset, ldr, 0, ldrLen);
		leader = MemberImpl.getMember(ldr);
		offset += ldrLen;
		// source
		int srcLen = XByteBuffer.toInt(buf.getBytesDirect(), offset);
		offset += 4;
		byte[] src = new byte[srcLen];
		System.arraycopy(buf.getBytesDirect(), offset, src, 0, srcLen);
		source = MemberImpl.getMember(src);
		offset += srcLen;
		// view
		int mbrCount = XByteBuffer.toInt(buf.getBytesDirect(), offset);
		offset += 4;
		view = new MemberImpl[mbrCount];
		for (int i = 0; i < view.length; i++) {
			int mbrLen = XByteBuffer.toInt(buf.getBytesDirect(), offset);
			offset += 4;
			byte[] mbr = new byte[mbrLen];
			System.arraycopy(buf.getBytesDirect(), offset, mbr, 0, mbrLen);
			view[i] = MemberImpl.getMember(mbr);
			offset += mbrLen;
		}
		// id
		this.id = new UniqueId(buf.getBytesDirect(), offset, 16);
		offset += 16;
		type = new byte[16];
		System.arraycopy(buf.getBytesDirect(), offset, type, 0, type.length);
		offset += 16;

	}

	public void write() {
		buf.reset();
		// header
		buf.append(NonBlockingCoordinator.getCoordHeader(), 0,
				NonBlockingCoordinator.getCoordHeader().length);
		// leader
		byte[] ldr = leader.getData(false, false);
		buf.append(ldr.length);
		buf.append(ldr, 0, ldr.length);
		ldr = null;
		// source
		byte[] src = source.getData(false, false);
		buf.append(src.length);
		buf.append(src, 0, src.length);
		src = null;
		// view
		buf.append(view.length);
		for (int i = 0; i < view.length; i++) {
			byte[] mbr = view[i].getData(false, false);
			buf.append(mbr.length);
			buf.append(mbr, 0, mbr.length);
		}
		// id
		buf.append(id.getBytes(), 0, id.getBytes().length);
		buf.append(type, 0, type.length);
	}

	public XByteBuffer getBuf() {
		return buf;
	}

	public void setBuf(XByteBuffer buf) {
		this.buf = buf;
	}

	public MemberImpl[] getView() {
		return view;
	}

	public void setView(MemberImpl[] view) {
		this.view = view;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public void setLeader(MemberImpl leader) {
		this.leader = leader;
	}

	public void setSource(MemberImpl source) {
		this.source = source;
	}

	public void setId(UniqueId id) {
		this.id = id;
	}

	public void setType(byte[] type) {
		this.type = type;
	}

}