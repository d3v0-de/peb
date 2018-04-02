package de.d3v0.peb.common.targetproperties;

import de.d3v0.peb.controller.Target.SftpTargetHandler;

public class SftpTargetProperties extends TargetProperties
{
    public String HostName;
    public String UserName;
    public int Port;
    public String Password;
    public String HostKey;
    public String getHandlerClassname()
    {
        return SftpTargetHandler.class.getCanonicalName();
    }

    public SftpTargetProperties()
    {
        WorkerThreadCount = 15;
        Port = 22;
    }
}
