package org.apache.naming.resources;

import java.util.zip.ZipEntry;

/**
 * Entries structure.
 */
public class WARDirContextEntry implements Comparable<Object> {


    // -------------------------------------------------------- Constructor
    
    
    public WARDirContextEntry(String name, ZipEntry entry) {
        this.setNameData(name);
        this.entry = entry;
    }
    
    
    // --------------------------------------------------- Member Variables
    
    
    private String name = null;
    
    
    private ZipEntry entry = null;
    
    
    private WARDirContextEntry children[] = new WARDirContextEntry[0];
    
    
    // ----------------------------------------------------- Public Methods
    
    
    @Override
    public int compareTo(Object o) {
        if (!(o instanceof WARDirContextEntry))
            return (+1);
        return getNameData().compareTo(((WARDirContextEntry) o).getName());
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof WARDirContextEntry))
            return false;
        return getNameData().equals(((WARDirContextEntry) o).getName());
    }
    
    @Override
    public int hashCode() {
        return getNameData().hashCode();
    }

    public ZipEntry getEntry() {
        return entry;
    }
    
    
    public String getName() {
        return getNameData();
    }
    
    
    public void addChild(WARDirContextEntry entry) {
    	WARDirContextEntry[] newChildren = new WARDirContextEntry[children.length + 1];
        for (int i = 0; i < children.length; i++)
            newChildren[i] = children[i];
        newChildren[children.length] = entry;
        children = newChildren;
    }


    public WARDirContextEntry[] getChildren() {
        return children;
    }


    public WARDirContextEntry getChild(String name) {
        for (int i = 0; i < children.length; i++) {
            if (children[i].getNameData().equals(name)) {
                return children[i];
            }
        }
        return null;
    }

	public String getNameData() {
		return name;
	}

	public void setNameData(String name) {
		this.name = name;
	}


}