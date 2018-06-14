package org.apache.tomcat.util.net;

/**
 * Simple data class that represents the cipher being used, along with the
 * corresponding effective key size.  The specified phrase must appear in the
 * name of the cipher suite to be recognized.
 */

public final class SSLSupportCipherData {

    private String phrase = null;

    private int keySize = 0;

    public SSLSupportCipherData(String phrase, int keySize) {
        this.setPhrase(phrase);
        this.setKeySize(keySize);
    }

	public String getPhrase() {
		return phrase;
	}

	public void setPhrase(String phrase) {
		this.phrase = phrase;
	}

	public int getKeySize() {
		return keySize;
	}

	public void setKeySize(int keySize) {
		this.keySize = keySize;
	}

}