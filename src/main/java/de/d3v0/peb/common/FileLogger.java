package de.d3v0.peb.common;

import de.d3v0.peb.common.misc.LogSeverity;

import java.io.*;
import java.util.Date;

public class FileLogger extends LoggerBase
{
    OutputStreamWriter log;
    FileOutputStream logFile;
    String logFilePath;
    public FileLogger(String logFilePath)
    {
        try
        {
            this.logFilePath = logFilePath;
            logFile = new FileOutputStream(logFilePath);
            log = new OutputStreamWriter(logFile);

        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public static void CreateDefault(String logPath)
    {
        instance = new FileLogger(logPath);
    }

    @Override
    protected InputStream getReadStream_int() throws Exception
    {
        return new FileInputStream(this.logFilePath);
    }

    @Override
    protected void log_int(Date date, LogSeverity sev, String message) throws Exception
    {
        log.write(date.toString() + " T" + Thread.currentThread().getId() + " " + sev  +  ": " + message + System.lineSeparator());
        log.flush();
    }
}
