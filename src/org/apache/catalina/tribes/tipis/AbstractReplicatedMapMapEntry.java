package org.apache.catalina.tribes.tipis;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

import org.apache.catalina.tribes.Member;
import org.apache.catalina.tribes.io.XByteBuffer;

//------------------------------------------------------------------------------
//                Map Entry class
//------------------------------------------------------------------------------
public class AbstractReplicatedMapMapEntry<K, V> implements Map.Entry<K, V> {
	private boolean backup;
	private boolean proxy;
	private Member[] backupNodes;
	private Member primary;
	private K key;
	private V value;

	public AbstractReplicatedMapMapEntry(K key, V value) {
		setKey(key);
		setValue(value);

	}

	public boolean isKeySerializable() {
		return (key == null) || (key instanceof Serializable);
	}

	public boolean isValueSerializable() {
		return (value == null) || (value instanceof Serializable);
	}

	public boolean isSerializable() {
		return isKeySerializable() && isValueSerializable();
	}

	public boolean isBackup() {
		return backup;
	}

	public void setBackup(boolean backup) {
		this.backup = backup;
	}

	public boolean isProxy() {
		return proxy;
	}

	public boolean isPrimary() {
		return (!proxy && !backup);
	}

	public boolean isActive() {
		return !proxy;
	}

	public void setProxy(boolean proxy) {
		this.proxy = proxy;
	}

	public boolean isDiffable() {
		return (value instanceof ReplicatedMapEntry)
				&& ((ReplicatedMapEntry) value).isDiffable();
	}

	public void setBackupNodes(Member[] nodes) {
		this.backupNodes = nodes;
	}

	public Member[] getBackupNodes() {
		return backupNodes;
	}

	public void setPrimary(Member m) {
		primary = m;
	}

	public Member getPrimary() {
		return primary;
	}

	@Override
	public V getValue() {
		return value;
	}

	@Override
	public V setValue(V value) {
		V old = this.value;
		this.value = value;
		return old;
	}

	@Override
	public K getKey() {
		return key;
	}

	public K setKey(K key) {
		K old = this.key;
		this.key = key;
		return old;
	}

	@Override
	public int hashCode() {
		return key.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		return key.equals(o);
	}

	/**
	 * apply a diff, or an entire object
	 * 
	 * @param data
	 *            byte[]
	 * @param offset
	 *            int
	 * @param length
	 *            int
	 * @param diff
	 *            boolean
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	@SuppressWarnings("unchecked")
	public void apply(byte[] data, int offset, int length, boolean diff)
			throws IOException, ClassNotFoundException {
		if (isDiffable() && diff) {
			ReplicatedMapEntry rentry = (ReplicatedMapEntry) value;
			rentry.lock();
			try {
				rentry.applyDiff(data, offset, length);
			} finally {
				rentry.unlock();
			}
		} else if (length == 0) {
			value = null;
			proxy = true;
		} else {
			value = (V) XByteBuffer.deserialize(data, offset, length);
		}
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder("MapEntry[key:");
		buf.append(getKey()).append("; ");
		buf.append("value:").append(getValue()).append("; ");
		buf.append("primary:").append(isPrimary()).append("; ");
		buf.append("backup:").append(isBackup()).append("; ");
		buf.append("proxy:").append(isProxy()).append(";]");
		return buf.toString();
	}

}