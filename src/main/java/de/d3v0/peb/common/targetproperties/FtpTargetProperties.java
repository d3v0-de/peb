package de.d3v0.peb.common.targetproperties;

import de.d3v0.peb.controller.Target.FtpTargetHandler;

public class FtpTargetProperties extends TargetProperties
{
    public String HostName;
    public String UserName;
    public int Port;
    public String Password;
    public String HostKey;
    public String getHandlerClassname()
    {
        return FtpTargetHandler.class.getCanonicalName();
    }
	public FtpEncrytion ftpEncrytion;

    public FtpTargetProperties()
    {
        WorkerThreadCount = 15;
        Port = 21;
		ftpEncrytion = FtpEncrytion.Explicit;
    }
	
	public enum FtpEncrytion
	{
		Explicit,
		Implicit,
		Plain;
	}
}
