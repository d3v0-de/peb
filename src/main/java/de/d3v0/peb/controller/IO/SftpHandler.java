package de.d3v0.peb.controller.IO;

import com.jcraft.jsch.*;
import de.d3v0.peb.common.BackupFile;
import de.d3v0.peb.common.FileLogger;
import de.d3v0.peb.common.LoggerBase;
import de.d3v0.peb.common.StringOutputStream;
import de.d3v0.peb.common.misc.LogSeverity;
import de.d3v0.peb.common.misc.TargetTransferException;
import de.d3v0.peb.common.IOProperties.SftpHandlerProperties;

import java.io.InputStream;
import java.io.OutputStream;

public class SftpHandler extends IOHandler
{

    private ChannelSftp sftp;

    private SftpHandlerProperties props()
    {
        return (SftpHandlerProperties) props;
    }

    protected boolean testConnection()
    {
        try
        {
            return getSftp().isConnected();
        } catch (Exception e) {
            FileLogger.log(LogSeverity.Error, e);
            this.sftp = null;
            return false;
        }
    }


    protected void readFile(String path, OutputStream dst) throws TargetTransferException
    {
        try
        {
            getSftp().get(fixSeparators(path), dst);
        }
        catch (Exception e)
        {
            FileLogger.log(e);
            throw new TargetTransferException("Error reading file " + path);
        }
    }

    protected ChannelSftp getSftp() throws TargetTransferException
    {
        int retry = 2;
        if (sftp == null)
        {
            for (int i=0; i<retry; i++)
            {
                try
                {
                    JSch jsch = new JSch();
                    Session session = jsch.getSession(props().UserName, props().HostName);
                    session.setDaemonThread(true);
                    session.setPassword(props().Password);
                    session.setHostKeyRepository(getHostkeyRepository());
                    session.connect();
                    session.setDaemonThread(true);
                    ChannelSftp lsftp = (ChannelSftp) session.openChannel("sftp");
                    lsftp.connect();
                    this.sftp = lsftp;
                    return sftp;
                } catch (JSchException ex)
                {
                    LoggerBase.log(LogSeverity.Warn, ex);
                }
            }
            if (sftp == null)
                throw new TargetTransferException("Failed to connect after " + retry + " tries");
         }

         return sftp;
    }

    protected void writeFile(InputStream src, String path) throws TargetTransferException
    {

        try
        {
            getSftp().put(src, path);
        } catch (SftpException ex)
        {
            FileLogger.log(ex);
            throw new TargetTransferException("Error writing file " + path);
        }
    }

    protected void createFolderInt(String folderPath, boolean checkFolderExists)
    {
        StringOutputStream sb = new StringOutputStream();
        StringBuilder outputBuffer = new StringBuilder();
        try
        {
            if (!checkFolderExists || !folderExists(folderPath))
                getSftp().mkdir(folderPath);
        }
        catch(Exception ex)
        {
            FileLogger.log(LogSeverity.Error, "Error creating folder " + folderPath);
            FileLogger.log(ex);
        }
    }

    @Override
    protected boolean checkFolderexists(String path)
    {
        //TODO impl:
        return false;
    }

    @Override
    public BackupFile getFileInfoInt(String path, boolean forBackup) {
        //TODO impl:
        return null;
    }

    @Override
    public Iterable<? extends BackupFile> listChildren(BackupFile parent) {
        //TODO impl:
        return null;
    }

    @Override
    protected InputStream getReadStream(String path) throws TargetTransferException {
        //TODO impl:
        return null;
    }

    @Override
    protected OutputStream getWriteStream(String path) throws TargetTransferException {
        //TODO impl:
        return null;
    }

    private boolean folderExists(String path) throws TargetTransferException
    {
        try
        {
            getSftp().cd(path);
            return true;
        }
        catch (SftpException e)
        {
            return false;
        }
    }

    private static HostKeyRepository getHostkeyRepository()
    {
        return new HostKeyRepository()
        {
            public int check(String s, byte[] bytes)
            {
                return 0;
            }

            public void add(HostKey hostKey, UserInfo userInfo)
            {

            }

            public void remove(String s, String s1)
            {

            }

            public void remove(String s, String s1, byte[] bytes)
            {

            }

            public String getKnownHostsRepositoryID()
            {
                return null;
            }

            public HostKey[] getHostKey()
            {
                return new HostKey[0];
            }

            public HostKey[] getHostKey(String s, String s1)
            {
                return new HostKey[0];
            }
        };
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
            if (sftp != null)
                sftp.getSession().disconnect();
        } catch (JSchException e)
        {
            FileLogger.log(LogSeverity.Error, "Error closing connection");
            FileLogger.log(e);
        }
    }
}
