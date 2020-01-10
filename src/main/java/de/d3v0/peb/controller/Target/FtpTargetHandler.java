package de.d3v0.peb.controller.Target;

import org.apache.commons.net.ftp.FTPSClient;
import org.apache.commons.net.ftp.FTPClient;
import de.d3v0.peb.common.Logger;
import de.d3v0.peb.common.LoggerBase;
import de.d3v0.peb.common.StringOutputStream;
import de.d3v0.peb.common.misc.LogSeverity;
import de.d3v0.peb.common.misc.TargetTransferException;
import de.d3v0.peb.common.targetproperties.FtpTargetProperties;
import de.d3v0.peb.controller.Target.TargetHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FtpTargetHandler extends TargetHandler
{

    private FTPClient ftpClient;

    private FtpTargetProperties props()
    {
        return (FtpTargetProperties) props;
    }

    protected void readFile(String path, OutputStream dst) throws TargetTransferException
    {
        try
        {
            getFtp().retrieveFile(fixSeparators(path), dst);
        }
        catch (IOException e)
        {
            Logger.log(e);
            throw new TargetTransferException("Error reading file " + path);
        }
    }

    protected String fixSeparators(String path)
    {
        return path.replace('\\', '/');
    }

    protected FTPClient getFtp() throws TargetTransferException
    {
        int retry = 2;
        if (ftpClient == null)
        {
            for (int i=0; i<retry; i++)
            {
                try
                {
                    ftpClient = new FTPSClient();
					ftpClient.connect(props().HostName, props().Port);
					ftpClient.login(props().UserName, props().Password);
                    return ftpClient;
                } catch (Exception ex)
                {
                    LoggerBase.log(LogSeverity.Warn, ex);
                }
            }
            if (ftpClient == null)
                throw new TargetTransferException("Failed to connect after " + retry + " tries");
         }

         return ftpClient;
    }

    protected void writeFile(InputStream src, String path) throws TargetTransferException
    {

        try
        {
            getFtp().storeFile(path,src);
        } catch (IOException ex)
        {
            Logger.log(ex);
            throw new TargetTransferException("Error writing file " + path);
        } catch (TargetTransferException e)
        {
            throw e;
        }
    }

    protected void createFolderInt(String folderPath, boolean checkFolderExists)
    {
        try
        {
            if (checkFolderExists == false || folderExists(folderPath) == false)
                getFtp().mkd(folderPath);
        }
        catch(Exception ex)
        {
            Logger.log(LogSeverity.Error, "Error creating folder " + folderPath);
            Logger.log(ex);
        }
    }

    private boolean folderExists(String path) throws TargetTransferException
    {
        try
        {
            getFtp().cwd(path);
            return true;
        }
        catch (Exception e)
        {
            return false;
        }
    }

    @Override
    protected String getSeparator()
    {
        return "/";
    }

    @Override
    public void close()
    {
        try
        {
            if (ftpClient != null)
                ftpClient.disconnect();
        } catch (Exception e)
        {
            Logger.log(LogSeverity.Error, "Error closing connection");
            Logger.log(e);
        }
    }
}
