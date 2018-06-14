package org.apache.naming.factory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;

import javax.sql.DataSource;

/**
 * Simple wrapper class that will allow a user to configure a ResourceLink for a data source
 * so that when {@link javax.sql.DataSource#getConnection()} is called, it will invoke 
 * {@link javax.sql.DataSource#getConnection(String, String)} with the preconfigured username and password.
 */
public class DataSourceLinkFactoryDataSourceHandler implements InvocationHandler {
    private final DataSource ds; 
    private final String username; 
    private final String password;
    private final Method getConnection;
    public DataSourceLinkFactoryDataSourceHandler(DataSource ds, String username, String password) throws Exception {
        this.ds = ds;
        this.username = username;
        this.password = password;
        getConnection = ds.getClass().getMethod("getConnection", String.class, String.class);
    }
    
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        
        if ("getConnection".equals(method.getName()) && (args==null || args.length==0)) {
            args = new String[] {username,password};
            method = getConnection;
        } else if ("unwrap".equals(method.getName())) {
            return unwrap((Class<?>)args[0]);
        }
        try {
            return method.invoke(ds,args);
        }catch (Throwable t) {
            if (t instanceof InvocationTargetException
                    && t.getCause() != null) {
                throw t.getCause();
            } else {
                throw t;
            }
        }
    }
    
    public Object unwrap(Class<?> iface) throws SQLException {
        if (iface == DataSource.class) {
            return ds;
        } else {
            throw new SQLException("Not a wrapper of "+iface.getName());
        }
    }
    
}