package de.d3v0.peb.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class BackupFile
{
    public String Path;
    public long Size;
    public long LastModified;

    public BackupFile(File file)
    {
        this.Path = file.getAbsolutePath();
        this.Size = file.length();
        this.LastModified = file.lastModified();
    }

    public InputStream getReadStream() throws FileNotFoundException
    {
        return new FileInputStream(Path);
    }
}
