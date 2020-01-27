package de.d3v0.peb.common;

import de.d3v0.peb.common.misc.TargetTransferException;
import de.d3v0.peb.controller.IO.IOHandler;

public class BackupFile
{
    public String PathSource;
    public String PathBackupTarget;
    public long Size;
    public long LastModified;
    public FileType type = FileType.undefined;
    public String Separator;
    private final IOHandler ioHandler;


    public BackupFile(IOHandler ioHandler, String path)
    {
        this.ioHandler = ioHandler;
        ioHandler.setPath(this, path);
    }

    public Iterable<? extends BackupFile> listChildren() throws TargetTransferException {
        return this.ioHandler.listChildren(this);
    }

    public IOHandler getIOHandler() {
        return this.ioHandler;
    }

    public enum FileType
    {
        File,
        Directory,
        Symlink,
        undefined
    }
}
