package org.apache.catalina.comet;

/**
 * Enumeration describing the major events that the container can invoke
 * the CometProcessors event() method with.<br>
 * BEGIN - will be called at the beginning
 *  of the processing of the connection. It can be used to initialize any relevant
 *  fields using the request and response objects. Between the end of the processing
 *  of this event, and the beginning of the processing of the end or error events,
 *  it is possible to use the response object to write data on the open connection.
 *  Note that the response object and dependent OutputStream and Writer are still
 *  not synchronized, so when they are accessed by multiple threads,
 *  synchronization is mandatory. After processing the initial event, the request
 *  is considered to be committed.<br>
 * READ - This indicates that input data is available, and that one read can be made
 *  without blocking. The available and ready methods of the InputStream or
 *  Reader may be used to determine if there is a risk of blocking: the servlet
 *  should read while data is reported available. When encountering a read error,
 *  the servlet should report it by propagating the exception properly. Throwing
 *  an exception will cause the error event to be invoked, and the connection
 *  will be closed.
 *  Alternately, it is also possible to catch any exception, perform clean up
 *  on any data structure the servlet may be using, and using the close method
 *  of the event. It is not allowed to attempt reading data from the request
 *  object outside of the execution of this method.<br>
 * END - End may be called to end the processing of the request. Fields that have
 *  been initialized in the begin method should be reset. After this event has
 *  been processed, the request and response objects, as well as all their dependent
 *  objects will be recycled and used to process other requests. End will also be
 *  called when data is available and the end of file is reached on the request input
 *  (this usually indicates the client has pipelined a request).<br>
 * ERROR - Error will be called by the container in the case where an IO exception
 *  or a similar unrecoverable error occurs on the connection. Fields that have
 *  been initialized in the begin method should be reset. After this event has
 *  been processed, the request and response objects, as well as all their dependent
 *  objects will be recycled and used to process other requests.
 */
public enum CometEventEventType {BEGIN, READ, END, ERROR}