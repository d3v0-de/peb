package de.d3v0.peb.controller.IO;

import de.d3v0.peb.common.BackupFile;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPSClient;
import org.apache.commons.net.ftp.FTPClient;
import de.d3v0.peb.common.FileLogger;
import de.d3v0.peb.common.LoggerBase;
import de.d3v0.peb.common.misc.LogSeverity;
import de.d3v0.peb.common.misc.TargetTransferException;
import de.d3v0.peb.common.IOProperties.FtpHandlerProperties;
import org.apache.commons.net.io.CopyStreamEvent;
import org.apache.commons.net.io.CopyStreamListener;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.net.SocketTimeoutException;

public class FtpHandler extends IOHandler
{

    private FTPClient ftpClient;

    private FtpHandlerProperties props()
    {
        return (FtpHandlerProperties) props;
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
                    if (props().ftpEncryption == FtpHandlerProperties.FtpEncryption.None)
                        ftpClient = new MyFtpClient();
                    else
                        ftpClient = new MyFtpsClient();

                    ftpClient.connect(props().HostName, props().Port);
					ftpClient.login(props().UserName, props().Password);
                    ftpClient.enterLocalPassiveMode();
                    if (props().ftpEncryption == FtpHandlerProperties.FtpEncryption.Explicit)
                    {
                        ((MyFtpsClient) ftpClient).execPBSZ(0);
                        ((MyFtpsClient) ftpClient).execPROT("P");
                    }
                    ftpClient.sendCommand("TYPE", "I");
                    ftpClient.sendCommand("MODE", "S");
                    ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

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

    private String getParentPathFromFilePath(String path)
    {
        return path.substring(0, path.lastIndexOf("/"));
    }
    private String getLastNodeFromFilePath(String path)
    {
        return path.substring(path.lastIndexOf("/") + 1);
    }


    protected boolean testConnection()
    {
        try
        {
            return getFtp().changeWorkingDirectory(this.props.BasePath);
        } catch (Exception e) {
            FileLogger.log(LogSeverity.Error, e);
            this.ftpClient = null;
            return false;
        }
    }

    protected void createFolderInt(String folderPath, boolean checkFolderExists)
    {
        try
        {
            if (!checkFolderExists || !folderExists(folderPath))
            {
                /*
                if (!getFtp().changeWorkingDirectory(getParentPathFromFilePath(folderPath)))
                    throw new IOException("failed to change ftp directory " + getParentPathFromFilePath(folderPath));
                int result = getFtp().mkd(getLastNodeFromFilePath(folderPath));
                if (result < 100 || result >= 400)
                    throw new IOException("failed to transfer file to " + folderPath);

                 */
                getFtp().mkd(folderPath);
            }
        }
        catch(Exception ex)
        {
            FileLogger.log(LogSeverity.Error, "Error creating folder " + folderPath);
            FileLogger.log(ex);
        }
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
        try
        {
            return new MyInputStream(getFtp().retrieveFileStream(path), this);
        } catch (IOException e)
        {
            FileLogger.log(e);
            throw new TargetTransferException("Error reading file " + path);
        }
    }

    private void onStreamClosed() throws TargetTransferException
    {
        try {
            if (!getFtp().completePendingCommand()) {
                Exception ex = new TargetTransferException("Error completing current Command.");
                throw ex;
            }
        } catch (Exception e) {
            FileLogger.log(e);
            ftpClient = null;
            throw new TargetTransferException(e.getMessage());
        }
    }

    @Override
    protected OutputStream getWriteStream(String path) throws TargetTransferException {
        try
        {
            String dir = getParentPathFromFilePath(path);
            String file = getLastNodeFromFilePath(path);
            if (!getFtp().changeWorkingDirectory(dir))
                throw new TargetTransferException("Error changing to directory  " + dir);
            return new MyOutputStream(getFtp().storeFileStream(file), this);
        } catch (IOException e)
        {
            FileLogger.log(e);
            throw new TargetTransferException("Error writing file " + path);
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
            FileLogger.log(LogSeverity.Error, "Error closing connection");
            FileLogger.log(e);
        }
    }

    private class MyOutputStream extends OutputStream
    {
        protected final OutputStream inner;
        protected final FtpHandler handler;
        protected CSL csl;

        public MyOutputStream (OutputStream s, FtpHandler handler)
        {
            this.inner = s;
            this.handler = handler;
            try
            {
                csl = new CSL((MyFtpClientInterface) handler.ftpClient, handler.props().KeepAliveSeconds, 1000);
            } catch (SocketException e)
            {
                FileLogger.log(e);
            }
            if (s == null)
            FileLogger.log(LogSeverity.Error, "");
        }

        @Override
        public void write(int i) throws IOException {
            inner.write(i);
            csl.bytesTransferred(0,i,0);
        }

        @Override
        public void flush() throws IOException {
            inner.flush();
        }

        @Override
        public void write(byte[] b) throws IOException {
            inner.write(b);
            csl.bytesTransferred(0,b.length,0);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            inner.write(b, off, len);
            csl.bytesTransferred(0,b.length,0);
        }

        @Override
        public void close() throws IOException {
            inner.close();
            try {
                csl.cleanUp();
                handler.onStreamClosed();
            } catch (TargetTransferException e) {
                FileLogger.log(e);
            }
        }
    }

    private class MyInputStream extends InputStream
    {
        protected final InputStream inner;
        protected final FtpHandler handler;
        protected CSL csl;

        public MyInputStream (InputStream s, FtpHandler handler)
        {
            this.inner = s;
            this.handler = handler;
            try
            {
                csl = new CSL((MyFtpClientInterface) handler.ftpClient, handler.props().KeepAliveSeconds, 1000);
            } catch (SocketException e)
            {
                FileLogger.log(e);
            }

            if (s == null)
                FileLogger.log(LogSeverity.Error, "");
        }

        @Override
        public int read() throws IOException
        {
            int res =   inner.read();
            csl.bytesTransferred(0, res,0);
            return res;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException
        {
            int res =  inner.read(b, off, len);
            csl.bytesTransferred(0, res,0);
            return res;
        }

        @Override
        public void close() throws IOException {
            inner.close();
            try {
                csl.cleanUp();
                handler.onStreamClosed();
            } catch (TargetTransferException e) {
                FileLogger.log(e);
            }
        }
    }

    private static class CSL implements CopyStreamListener {
        private final MyFtpClientInterface parent;
        private final long idle;
        private final int currentSoTimeout;
        private long time = System.currentTimeMillis();
        private int notAcked = 0;

        CSL(MyFtpClientInterface parent, long idleSeconds, int maxWait) throws SocketException {
            this.idle = idleSeconds * 1000;
            this.parent = parent;
            this.currentSoTimeout = parent.getSocketTimeout();
            parent.setSocketTimeout(maxWait);
        }

        public void bytesTransferred(CopyStreamEvent event) {
            this.bytesTransferred(event.getTotalBytesTransferred(), event.getBytesTransferred(), event.getStreamSize());
        }

        public void bytesTransferred(long totalBytesTransferred, int bytesTransferred, long streamSize) {
            long now = System.currentTimeMillis();
            if (now - this.time > this.idle) {
                try {
                        this.parent.NoopNoReport();
                } catch (SocketTimeoutException var9) {
                    ++this.notAcked;
                } catch (IOException var10) {
                    FileLogger.log(var10);
                }
                this.time = now;
            }
        }

        void cleanUp() throws IOException {
            try {
                while(this.notAcked-- > 0) {
                    this.parent.getReplyNoReport();
                }
            } finally {
                this.parent.setSocketTimeout(this.currentSoTimeout);
            }

        }
    }

    private class MyFtpsClient extends FTPSClient implements MyFtpClientInterface
    {
        public void NoopNoReport() throws IOException
        {
            this.__noop();
        }

        public void getReplyNoReport() throws IOException
        {
            this.__getReplyNoReport();
        }

        public void setSocketTimeout(int timeout) throws SocketException
        {
            this.setSoTimeout(timeout);
        }

        public int getSocketTimeout() throws SocketException
        {
            return this.getSoTimeout();
        }
    }

    private class MyFtpClient extends FTPClient implements MyFtpClientInterface
    {
        public void NoopNoReport() throws IOException
        {
            this.__noop();
        }

        public void getReplyNoReport() throws IOException
        {
            this.__getReplyNoReport();
        }

        public void setSocketTimeout(int timeout) throws SocketException
        {
            this.setSoTimeout(timeout);
        }

        public int getSocketTimeout() throws SocketException
        {
            return this.getSoTimeout();
        }
    }

    private interface MyFtpClientInterface
    {
        void NoopNoReport() throws IOException;
        void getReplyNoReport() throws IOException;
        void setSocketTimeout(int timeout) throws SocketException;
        int getSocketTimeout() throws SocketException;
    }
}
