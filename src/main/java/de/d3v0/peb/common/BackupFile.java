package de.d3v0.peb.common;

import java.io.*;
import java.util.Date;

public class BackupFile
{
    public String Path;
    public long Size;
    public long LastModified;
    public String Separator;
    private InputStream inStream;
    private OutputStream outStream;

    public BackupFile(File file)
    {
        this.Path = file.getAbsolutePath();
        this.Size = file.length();
        this.LastModified = file.lastModified();
        this.Separator = File.separator;
    }

    public BackupFile(String path, InputStream stream)
    {
        this.Path = path;
        this.Size = 0;
        this.LastModified = new Date().getTime();
        this.Separator = File.separator;
        this.inStream = stream;
    }

    public BackupFile(String path, OutputStream stream)
    {
        this.Path = path;
        this.Size = 0;
        this.LastModified = new Date().getTime();
        this.Separator = File.separator;
        this.outStream = stream;
    }

    public InputStream getReadStream() throws FileNotFoundException
    {
        if (inStream != null)
            return inStream;
        else
            return new FileInputStream(Path);
    }

    public OutputStream getWriteStream() throws FileNotFoundException
    {
        if (outStream != null)
            return outStream;
        else
            return new FileOutputStream(Path);
    }
}
