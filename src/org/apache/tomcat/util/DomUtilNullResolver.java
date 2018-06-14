package org.apache.tomcat.util;

import java.io.IOException;
import java.io.StringReader;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class DomUtilNullResolver implements EntityResolver {
    @Override
    public InputSource resolveEntity (String publicId,
                                               String systemId)
        throws SAXException, IOException
    {
        if( DomUtil.log.isTraceEnabled())
            DomUtil.log.trace("ResolveEntity: " + publicId + " " + systemId);
        return new InputSource(new StringReader(""));
    }
}