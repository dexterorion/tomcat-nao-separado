package org.apache.catalina.filters;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

public class RemoteIpFilterXForwardedRequest extends HttpServletRequestWrapper {
    
    private static final ThreadLocal<SimpleDateFormat[]> threadLocalDateFormats = new ThreadLocal<SimpleDateFormat[]>() {
        @Override
        protected SimpleDateFormat[] initialValue() {
            return new SimpleDateFormat[] {
                new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US),
                new SimpleDateFormat("EEEEEE, dd-MMM-yy HH:mm:ss zzz", Locale.US),
                new SimpleDateFormat("EEE MMMM d HH:mm:ss yyyy", Locale.US)
            };
            
        }
    };
    
    private Map<String, List<String>> headers;
    
    private int localPort;

    private String remoteAddr;
    
    private String remoteHost;
    
    private String scheme;
    
    private boolean secure;
    
    private int serverPort;

    public RemoteIpFilterXForwardedRequest(HttpServletRequest request) {
        super(request);
        this.localPort = request.getLocalPort();
        this.remoteAddr = request.getRemoteAddr();
        this.remoteHost = request.getRemoteHost();
        this.scheme = request.getScheme();
        this.secure = request.isSecure();
        this.serverPort = request.getServerPort();
        
        headers = new HashMap<String, List<String>>();
        for (Enumeration<String> headerNames = request.getHeaderNames(); headerNames.hasMoreElements();) {
            String header = headerNames.nextElement();
            headers.put(header, Collections.list(request.getHeaders(header)));
        }
    }
    
    @Override
    public long getDateHeader(String name) {
        String value = getHeader(name);
        if (value == null) {
            return -1;
        }
        DateFormat[] dateFormats = threadLocalDateFormats.get();
        Date date = null;
        for (int i = 0; ((i < dateFormats.length) && (date == null)); i++) {
            DateFormat dateFormat = dateFormats[i];
            try {
                date = dateFormat.parse(value);
            } catch (Exception ParseException) {
                // Ignore
            }
        }
        if (date == null) {
            throw new IllegalArgumentException(value);
        }
        return date.getTime();
    }
    
    @Override
    public String getHeader(String name) {
        Map.Entry<String, List<String>> header = getHeaderEntry(name);
        if (header == null || header.getValue() == null || header.getValue().isEmpty()) {
            return null;
        }
        return header.getValue().get(0);
    }
    
    protected Map.Entry<String, List<String>> getHeaderEntry(String name) {
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(name)) {
                return entry;
            }
        }
        return null;
    }
    
    @Override
    public Enumeration<String> getHeaderNames() {
        return Collections.enumeration(headers.keySet());
    }
    
    @Override
    public Enumeration<String> getHeaders(String name) {
        Map.Entry<String, List<String>> header = getHeaderEntry(name);
        if (header == null || header.getValue() == null) {
            return Collections.enumeration(Collections.<String>emptyList());
        }
        return Collections.enumeration(header.getValue());
    }
    
    @Override
    public int getIntHeader(String name) {
        String value = getHeader(name);
        if (value == null) {
            return -1;
        }
        return Integer.parseInt(value);
    }
    
    @Override
    public int getLocalPort() {
        return localPort;
    }
    
    @Override
    public String getRemoteAddr() {
        return this.remoteAddr;
    }
    
    @Override
    public String getRemoteHost() {
        return this.remoteHost;
    }
    
    @Override
    public String getScheme() {
        return scheme;
    }
    
    @Override
    public int getServerPort() {
        return serverPort;
    }
    
    @Override
    public boolean isSecure() {
        return secure;
    }
    
    public void removeHeader(String name) {
        Map.Entry<String, List<String>> header = getHeaderEntry(name);
        if (header != null) {
            headers.remove(header.getKey());
        }
    }
    
    public void setHeader(String name, String value) {
        List<String> values = Arrays.asList(value);
        Map.Entry<String, List<String>> header = getHeaderEntry(name);
        if (header == null) {
            headers.put(name, values);
        } else {
            header.setValue(values);
        }
        
    }
    
    public void setLocalPort(int localPort) {
        this.localPort = localPort;
    }

    public void setRemoteAddr(String remoteAddr) {
        this.remoteAddr = remoteAddr;
    }
    
    public void setRemoteHost(String remoteHost) {
        this.remoteHost = remoteHost;
    }
    
    public void setScheme(String scheme) {
        this.scheme = scheme;
    }
    
    public void setSecure(boolean secure) {
        this.secure = secure;
    }
    
    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }
}