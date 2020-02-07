package de.d3v0.peb.common.IOProperties;

import de.d3v0.peb.controller.IO.FtpHandler;

public class FtpHandlerProperties extends IOHandlerProperties
{
    public String HostName;
    public String UserName;
    public final int Port;
    public String Password;
    public String HostKey;
    public String getHandlerClassname()
    {
        return FtpHandler.class.getCanonicalName();
    }
	public final FtpEncryption ftpEncryption;

    public FtpHandlerProperties()
    {
        WorkerThreadCount = 15;
        Port = 21;
		ftpEncryption = FtpEncryption.Explicit;
    }

    // TODO: extend config (ACTIVE?, PROT)
	public enum FtpEncryption
	{
		Explicit,
		Implicit,
		None
	}
}
