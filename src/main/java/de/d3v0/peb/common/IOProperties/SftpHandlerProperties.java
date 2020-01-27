package de.d3v0.peb.common.IOProperties;

import de.d3v0.peb.controller.IO.SftpHandler;

public class SftpHandlerProperties extends IOHandlerProperties
{
    public String HostName;
    public String UserName;
    public final int Port;
    public String Password;
    public String HostKey;
    public String getHandlerClassname()
    {
        return SftpHandler.class.getCanonicalName();
    }

    public SftpHandlerProperties()
    {
        WorkerThreadCount = 15;
        Port = 22;
    }
}
