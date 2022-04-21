package de.d3v0.peb.controller.IO;

import de.d3v0.peb.common.BackupFile;
import de.d3v0.peb.common.FileLogger;
import de.d3v0.peb.common.misc.LogSeverity;
import de.d3v0.peb.common.misc.TargetTransferException;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;

public class FileHandler extends IOHandler
{

    protected boolean testConnection()
    {
        try
        {
            return new File(this.props.BasePath).exists();

        } catch (Exception e) {
            FileLogger.log(LogSeverity.Error, e);
            return false;
        }
    }

    protected void readFile(String path, OutputStream dst) throws TargetTransferException
    {
        try
        {
            FileInputStream is = new FileInputStream(path);
            is.transferTo(dst);
        }
        catch (Exception e)
        {
            FileLogger.log(e);
            throw new TargetTransferException("Error reading file " + path);
        }
    }

    protected void writeFile(InputStream src, String path) throws TargetTransferException
    {
        try
        {
            FileOutputStream out = new FileOutputStream(path);
            src.transferTo(out);
        } catch (IOException ex)
        {
            FileLogger.log(ex);
            throw new TargetTransferException("Error writing file " + path);
        }
    }

    protected void createFolderInt(String folderPath, boolean checkFolderExists)
    {
        try
        {
            if (checkFolderExists == false || folderExists(folderPath) == false)
            {
                if (! new File(folderPath).mkdir())
                    FileLogger.log(LogSeverity.Error, "Folder " + folderPath + " was not created.");
            }
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
        // TODO
        return false;
    }

    private boolean folderExists(String path)
    {
        File dir = new File(path);
        return dir.exists() && dir.isDirectory();
    }

   @Override
    protected String getSeparator()
    {
        return File.separator;
    }

    @Override
    public BackupFile getFileInfoInt(String path, boolean forBackup) {

        File file = new File(path);
        BackupFile res = new BackupFile(this, file.getAbsolutePath());
        res.Separator = File.separator;
        if (forBackup) {
            res.LastModified = file.lastModified();
            res.Size = file.length();

            if (file.isFile())
                res.type = BackupFile.FileType.File;
            else if (file.isDirectory())
                res.type = BackupFile.FileType.Directory;
            else if (Files.isSymbolicLink(file.toPath()))
                res.type = BackupFile.FileType.Symlink;
            else
                res.type = BackupFile.FileType.undefined;
        }
        return res;
    }

    @Override
    public Iterable<? extends BackupFile> listChildren(BackupFile parent) throws TargetTransferException {
        ArrayList<BackupFile> res = new ArrayList<BackupFile>();
        for (File child : new File(getPath(parent)).listFiles())
            res.add(getFileInfo(child.getAbsolutePath(), true));
        return res;
    }

    @Override
    protected InputStream getReadStream(String path) throws TargetTransferException {
        try {
            return new FileInputStream(path);
        } catch (FileNotFoundException e) {
            FileLogger.log(e);
            throw new TargetTransferException(e);
        }
    }

    @Override
    protected OutputStream getWriteStream(String path) throws TargetTransferException {
        try {
            return new FileOutputStream(path);
        } catch (FileNotFoundException e) {
            FileLogger.log(e);
            throw new TargetTransferException(e);
        }
    }

    @Override
    public void close()
    {
        //nothing to do here
    }
}
