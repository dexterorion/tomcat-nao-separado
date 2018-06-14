package org.apache.tomcat.util.modeler;

public class BaseModelMBeanMethodKey {
    private String name;
    private String[] signature;

    public BaseModelMBeanMethodKey(String name, String[] signature) {
        this.setNameData(name);
        if(signature == null) {
            signature = new String[0];
        }
        this.setSignatureData(signature);
    }

    @Override
    public boolean equals(Object other) {
        if(!(other instanceof BaseModelMBeanMethodKey)) {
            return false;
        }
        BaseModelMBeanMethodKey omk = (BaseModelMBeanMethodKey)other;
        if(!getNameData().equals(omk.getNameData())) {
            return false;
        }
        if(getSignatureData().length != omk.getSignatureData().length) {
            return false;
        }
        for(int i=0; i < getSignatureData().length; i++) {
            if(!getSignatureData()[i].equals(omk.getSignatureData()[i])) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        return getNameData().hashCode();
    }

	public String getNameData() {
		return name;
	}

	public void setNameData(String name) {
		this.name = name;
	}

	public String[] getSignatureData() {
		return signature;
	}

	public void setSignatureData(String[] signature) {
		this.signature = signature;
	}
}