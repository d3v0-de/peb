package de.d3v0.peb.common;


import de.d3v0.peb.common.misc.LogSeverity;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Date;

public abstract class LoggerBase
{

    public LogSeverity logSeverity = LogSeverity.Debug;

    protected static LoggerBase instance;

    protected static LoggerBase getInstance()
    {
        if (instance == null)
            instance = new ConsoleLog();
        return instance;
    }

    protected abstract InputStream getReadStream_int() throws  Exception;

    public static InputStream getReadStream() throws Exception
    {
        return getInstance().getReadStream_int();
    }

    protected abstract void log_int(Date date, LogSeverity sev, String message) throws Exception;

    private static void log(Date date, LogSeverity sev, String message)
    {
        if (sev.ordinal() >= getInstance().logSeverity.ordinal())
        {
            try
            {
                getInstance().log_int(date, sev, message);
            } catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    public static void log(Exception ex)
    {
        log(LogSeverity.Error, ex);
    }

    public static void log(LogSeverity sev, Exception ex)
    {
        StringOutputStream sob = new StringOutputStream();
        PrintStream ps = new PrintStream(sob, true);
        ex.printStackTrace(ps);

        log(new Date(), sev, sob.mBuf.toString());
    }

    public static void log(LogSeverity sev, String meessage )
    {
        log(new Date(), sev, meessage);
    }
}
