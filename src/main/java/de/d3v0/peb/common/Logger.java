package de.d3v0.peb.common;

import java.io.*;
import java.util.Date;

public class Logger extends LoggerBase
{
    OutputStreamWriter log;
    FileOutputStream logFile;

    public Logger()
    {
        this(System.getProperty("user.home") + File.separator + "efb.log");
    }

    public Logger(String logFilePath)
    {
        try
        {
            logFile = new FileOutputStream(logFilePath);
            log = new OutputStreamWriter(logFile);

        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public static void Create(String logPath)
    {
        instance = new Logger(logPath);
    }


    @Override
    protected void log_int(Date date, LogSeverity sev, String message) throws Exception
    {
        log.write(date.toString() + " T" + Thread.currentThread().getId() + " " + sev  +  ": " + message + System.lineSeparator());
        log.flush();
    }
}
