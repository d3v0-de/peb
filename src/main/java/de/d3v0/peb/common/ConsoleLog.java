package de.d3v0.peb.common;

import de.d3v0.peb.common.misc.LogSeverity;
import java.io.InputStream;
import java.util.Date;

public class ConsoleLog extends LoggerBase
{
    @Override
    protected InputStream getReadStream_int() throws Exception
    {
        throw new Exception("Not implemented. Cannot read from a console.");
    }

    @Override
    protected void log_int(Date date, LogSeverity sev, String message) throws Exception
    {
        System.out.println(date.toString() + " T" + Thread.currentThread().getId() + " " + sev  +  ": " + message);
    }
}
