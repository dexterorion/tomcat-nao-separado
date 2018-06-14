package org.apache.catalina.valves;

import java.util.Date;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;

public class ExtendedAccessLogValveDateElement implements AccessLogValveAccessLogElement {
    // Milli-seconds in 24 hours
    private static final long INTERVAL = (1000 * 60 * 60 * 24);

    private static final ThreadLocal<ExtendedAccessLogValveElementTimestampStruct> currentDate = new ThreadLocal3();

    @Override
    public void addElement(StringBuilder buf, Date date, Request request,
            Response response, long time) {
        ExtendedAccessLogValveElementTimestampStruct eds = currentDate.get();
        long millis = eds.getCurrentTimestamp().getTime();
        if (date.getTime() > (millis + INTERVAL -1) ||
                date.getTime() < millis) {
            eds.getCurrentTimestamp().setTime(
                    date.getTime() - (date.getTime() % INTERVAL));
            eds.setCurrentTimestampString(eds.getCurrentTimestampFormat().format(eds.getCurrentTimestamp()));
        }
        buf.append(eds.getCurrentTimestampString());
    }
}