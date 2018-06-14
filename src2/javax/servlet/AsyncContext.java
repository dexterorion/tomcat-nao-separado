/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package javax.servlet;

/**
 * TODO SERVLET3 - Add comments
 * @since Servlet 3.0
 */
public interface AsyncContext {
    public static final String ASYNC_REQUEST_URI =
        "javax.servlet.async.request_uri";
    public static final String ASYNC_CONTEXT_PATH  =
        "javax.servlet.async.context_path";
    public static final String ASYNC_PATH_INFO =
        "javax.servlet.async.path_info";
    public static final String ASYNC_SERVLET_PATH =
        "javax.servlet.async.servlet_path";
    public static final String ASYNC_QUERY_STRING =
        "javax.servlet.async.query_string";
    
    public ServletRequest getRequest();
    
    public ServletResponse getResponse();
    
    public boolean hasOriginalRequestAndResponse();
    
    /**
     * 
     * @throws IllegalStateException
     */
    public void dispatch();
    
    /**
     * 
     * @param path
     * @throws IllegalStateException
     */
    public void dispatch(String path);
    
    /**
     * 
     * @param context
     * @param path
     * @throws IllegalStateException
     */
    public void dispatch(ServletContext context, String path);
    
    public void complete();
    
    public void start(Runnable run);
    
    public void addListener(AsyncListener listener);
    
    public void addListener(AsyncListener listener, ServletRequest request,
            ServletResponse response);
    
    public <T extends AsyncListener> T createListener(Class<T> clazz) throws ServletException;
    
    /**
     * Get timeout in milliseconds. 0 or less indicates no timeout.
     */
    public long getTimeout();
    
    /**
     * Set timeout in milliseconds. 0 or less indicates no timeout.
     */
    public void setTimeout(long timeout);
}
