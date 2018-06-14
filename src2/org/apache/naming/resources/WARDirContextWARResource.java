package org.apache.naming.resources;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;

public class WARDirContextWARResource extends Resource {
    
    
    // -------------------------------------------------------- Constructor
    
    
    /**
	 * 
	 */
	private final WARDirContext warDirContext;


	public WARDirContextWARResource(WARDirContext warDirContext, ZipEntry entry) {
        this.warDirContext = warDirContext;
		this.entry = entry;
    }
    
    
    // --------------------------------------------------- Member Variables
    
    
	private ZipEntry entry;
    
    
    // ----------------------------------------------------- Public Methods
    
    
    /**
     * Content accessor.
     * 
     * @return InputStream
     */
    @Override
    public InputStream streamContent()
        throws IOException {
        try {
            if (getBinaryContent() == null) {
                InputStream is = this.warDirContext.getBase().getInputStream(entry);
                setInputStream(is);
                return is;
            }
        } catch (ZipException e) {
            throw new IOException(e.getMessage(), e);
        }
        return super.streamContent();
    }
    
    
}