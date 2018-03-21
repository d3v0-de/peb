package de.d3v0.peb.controller;

import com.jcraft.jsch.*;
import de.d3v0.peb.common.Logger;
import de.d3v0.peb.common.LoggerBase;
import de.d3v0.peb.common.StringOutputStream;
import de.d3v0.peb.common.targetproperties.SftpTargetProperties;
import sun.rmi.runtime.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Date;

public class SftpTargetHandler extends TargetHandler
{

    private ChannelSftp sftp;
    private Session ssh_ses;

    private SftpTargetProperties props()
    {
        return (SftpTargetProperties) props;
    }

    protected void readFile(String path, OutputStream dst) throws JSchException, SftpException
    {
        getChannelSftp();
        sftp.get(fixSeparators(path), dst);
    }

    protected String fixSeparators(String path)
    {
        return path.replace('\\', '/');
    }

    protected void getChannelSftp() throws JSchException
    {
        if (sftp == null)
        {
            JSch jsch = new JSch();
            Session session = jsch.getSession(props().UserName, props().HostName);
            session.setPassword(props().Password);
            session.setHostKeyRepository(getHostkeyRepository());
            session.connect();
            session.setDaemonThread(true);
            sftp = (ChannelSftp) session.openChannel("sftp");
            sftp.connect();

            JSch jsch2 = new JSch();
            ssh_ses = jsch.getSession(props().UserName, props().HostName);
            ssh_ses.setPassword(props().Password);
            ssh_ses.setHostKeyRepository(getHostkeyRepository());
            ssh_ses.setDaemonThread(true);
            ssh_ses.connect();
        }
    }

    protected void writeFile(InputStream src, String path)
    {

        try
        {
            getChannelSftp();
            path = path.replace('\\', '/');
            String folderPath = path.substring(0, path.lastIndexOf("/"));
            createFolder(folderPath);
            sftp.put(src, path);
        } catch (Exception ex)
        {
            Logger.log(LoggerBase.LogSeverity.Error, "Error writing file " + path);
            Logger.log(ex);
            return;
        }
    }


    private void createFolder(String folderPath)
    {
        StringOutputStream sb = new StringOutputStream();
        StringBuilder outputBuffer = new StringBuilder();
        try
        {
            ChannelExec ssh = (ChannelExec )ssh_ses.openChannel("exec");
            ssh.setCommand("mkdir -p '" + folderPath + "'");
            ssh.setErrStream(sb);
            InputStream commandOutput = ssh.getInputStream();
            ssh.connect();
            int readByte = commandOutput.read();

            while(readByte != 0xffffffff)
            {
                outputBuffer.append((char)readByte);
                readByte = commandOutput.read();
            }
            ssh.disconnect();
        }
        catch(Exception ex)
        {
            Logger.log(LoggerBase.LogSeverity.Error, "Error creating folder " + folderPath);
            Logger.log(ex);
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
            ssh_ses.disconnect();
            sftp.getSession().disconnect();
        } catch (JSchException e)
        {
            Logger.log(LoggerBase.LogSeverity.Error, "Error closing connection");
            Logger.log(e);
        }
    }
}
