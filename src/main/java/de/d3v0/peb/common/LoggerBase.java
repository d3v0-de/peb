package de.d3v0.peb.common;


import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;

public abstract class LoggerBase
{
    public enum LogSeverity
    {
        Debug,
        Warn,
        Error,
        Fatal;
    }

    public LogSeverity logSeverity = LogSeverity.Debug;

    protected static LoggerBase instance;

    protected static LoggerBase getInstance()
    {
        if (instance == null)
            instance = new Logger();
        return instance;
    }

    protected abstract void log_int(Date date, LogSeverity sev, String message) throws Exception;

    private static void log(Date date, LogSeverity sev, String message)
    {
        if (sev.ordinal() >= getInstance().logSeverity.ordinal())
        {
            try
            {
                getInstance().log(date, sev, message);
            } catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    public static void log(Exception ex)
    {
        StringOutputStream sob = new StringOutputStream();
        PrintStream ps = new PrintStream(sob, true);
        ex.printStackTrace(ps);

        log(new Date(), LogSeverity.Error, sob.mBuf.toString());
    }

    public static void log(LogSeverity sev, String meessage )
    {
        log(new Date(), sev, meessage);
    }
}
