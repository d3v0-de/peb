package de.d3v0.peb.controller.IO;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import de.d3v0.peb.common.BackupFile;
import de.d3v0.peb.common.Logger;
import de.d3v0.peb.common.StringOutputStream;
import de.d3v0.peb.common.misc.*;
import de.d3v0.peb.common.IOProperties.IOHandlerProperties;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.*;

public abstract class IOHandler implements Closeable
{
    private List<Long> backupDates;
    IOHandlerProperties props;
    protected Mode mode;
    protected static final Hashtable<String, MyBoolean> dirCreated = new Hashtable<>();
    protected static final Hashtable<State, Statistics> stats = new Hashtable<>();

    public long getPerfDate()
    {
        return perfDate;
    }

    private long perfDate;

    public IOHandler()
    {
        this.perfDate = new Date().getTime();
        this.mode = Mode.Source;
    }

    protected static Statistics getStat(State state)
    {
        if (!stats.containsKey(state))
            stats.put(state, new Statistics());

        return stats.get(state);
    }

    public List<Long> getBackupDates()
    {
        if (backupDates == null)
            backupDates = retrieveBackupDates();
        return backupDates;
    }

    protected List<Long> retrieveBackupDates()
    {
        XStream xs = new XStream(new DomDriver());
        try (StringOutputStream sos = new StringOutputStream())
        {
            try(InputStream is = getReadStream(props.BasePath + getSeparator() + "backups.xml"))
            {

            is.transferTo(sos);
                return (List<Long>) xs.fromXML(sos.getString());
        }
        }
        catch (Exception ex)
        {
            Logger.log(ex);
            return new ArrayList<>();
        }
    }

    public void saveBackupDates() throws TargetTransferException, IOException {
        getBackupDates().add(perfDate);

        XStream xs = new XStream(new DomDriver());

        try(OutputStream os = getWriteStream(props.BasePath + getSeparator() + "backups.xml"))
        {
            StringOutputStream sos = new StringOutputStream();
            xs.toXML(getBackupDates(), sos);
            sos.getInputStream().transferTo(os);
        }
    }

    protected abstract boolean testConnection();
    protected abstract void createFolderInt(String path, boolean checkFolderexists ) throws TargetTransferException;
    protected abstract BackupFile getFileInfoInt(String localPath, boolean forBackup);
    public abstract Iterable<? extends BackupFile> listChildren(BackupFile parent) throws TargetTransferException;

    protected abstract InputStream getReadStream(String path) throws TargetTransferException;
    protected abstract OutputStream getWriteStream(String path) throws TargetTransferException;
    protected abstract String getSeparator();

    public BackupFile getFileInfo(String localPath, boolean forBackup)
    {
        BackupFile res = getFileInfoInt(localPath,  forBackup);
        if (res.type == BackupFile.FileType.File) {
            switch (this.mode) {
                case Source:
                    getStat(State.FoundInSource).count++;
                    getStat(State.FoundInSource).size += res.Size;
                    break;
                case BackupTarget:
                    // TODO: count restores (and also config/db)
                    //this.stats.get(State.).add(res);
                    break;
                default:
                    Logger.log(LogSeverity.Error, "Unkown mode " + this.mode);
            }
        }
        return res;
    }

    public void restoreFile(BackupFile file) throws BackupNotFoundException, TargetTransferException, IOException
    {
        if (getBackupDates() != null && getBackupDates().size()!= 0)
            restoreFile(file, getBackupDates().get(getBackupDates().size() -1));
        else throw new BackupNotFoundException("no Backup existing yet");
    }
    public void restoreFile(BackupFile file, long backupDate) throws BackupNotFoundException, TargetTransferException, IOException
    {
        try (InputStream is = getReadStream(getBackupPath(file, backupDate, false)))
        {
            try (OutputStream os = file.getIOHandler().getWriteStream(file.PathSource)) {
                is.transferTo(os);
            }
        }
    }

    protected void backupFile(BackupFile file, long backupDate) throws TargetTransferException, IOException
    {
        String targetPath= getBackupPath(file, backupDate, true);
        for (int i = 0; i < 2 ; i++) {
            if (this.testConnection())
                break;
        }
        try(OutputStream os = getWriteStream(targetPath)) {
            try (InputStream is =  file.getIOHandler().getReadStream(file.PathSource)) {
            long size= is.transferTo(os);
            getStat(State.TransferredToBackup).count++;
            getStat(State.TransferredToBackup).size += size;
            }
        }
    }

    public void backupFile(BackupFile file) throws TargetTransferException, IOException
    {
        backupFile(file, this.perfDate);
    }


    public static IOHandler create(IOHandler targetHandler) throws ClassNotFoundException, IllegalAccessException, InvocationTargetException, InstantiationException
    {
        IOHandler res = create(targetHandler.props, true);
        res.perfDate = targetHandler.perfDate;
        res.mode = targetHandler.mode;
        return res;
    }

    public static IOHandler create(IOHandlerProperties properties, boolean isTarget) throws ClassNotFoundException, IllegalAccessException, InvocationTargetException, InstantiationException
    {
        Class<?> clazz = Class.forName(properties.getHandlerClassname());
        IOHandler res = null;
        for (Constructor<?> ctor : clazz.getConstructors())
        {
            res = (IOHandler)ctor.newInstance();
            break;
        }
        if (res == null)
        {

            throw new ClassNotFoundException("Class " + properties.getHandlerClassname() + " has no constructor");
        }
        else
        {
            res.props = properties;
            if (isTarget)
                res.mode = Mode.BackupTarget;
            else
                res.mode = Mode.Source;
            return res;
        }
    }

    protected void createFolderParent(String relativePath) throws TargetTransferException
    {
        StringBuilder currentPath = new StringBuilder();
        for (String f: relativePath.split(getSeparator()))
        {
            if (f == "")
                continue;

            currentPath.append(f);
            MyBoolean exists;
            synchronized (dirCreated)
            {
                exists = dirCreated.get(currentPath.toString());
                if (exists == null)
                {
                    exists = new MyBoolean(false);
                    dirCreated.put(currentPath.toString(), exists);
                }
            }

            synchronized (exists)
            {
                if (!exists.Value)
                {
                    boolean checkFolderexists = props.BasePath.contains(currentPath.toString());
                    createFolderInt(currentPath.toString(), checkFolderexists);
                    exists.Value = true;
                }
            }
            currentPath.append(getSeparator());
        }
    }

    protected String fixSeparators(String path)  throws TargetTransferException
    {
        if (getSeparator() == "/" )
            return path.replace('\\', '/');
        else if (getSeparator() == "\\" )
            return path.replace('/', '\\');
        else
            throw new TargetTransferException("invalid/unknown Folder separator " + this.getSeparator());
    }

    public void setPath(BackupFile file, String path)
    {
        if (mode == Mode.Source)
            file.PathSource = path;
        else if (mode == Mode.BackupTarget)
            file.PathBackupTarget = path;
        else
            Logger.log(LogSeverity.Error, "Unknown backup Handler Mode " + mode);
    }

    public static void logStats()
    {
        Logger.log(LogSeverity.Info, getStatString(State.FoundInSource));
        Logger.log(LogSeverity.Info, getStatString(State.TransferredToBackup));
    }

    private static String getStatString(State state)
    {
        return "Statistics for " + state + " Files total: " + getStat(state).count + " Size total: " + Utils.humanReadable(getStat(state).size);
    }

    protected String getBackupPath(BackupFile file, long backupDate, boolean createFolder) throws TargetTransferException
    {
        return getBackupPath(getPath(file), file.Separator, backupDate, createFolder);
    }

    protected String getPath(BackupFile file) throws TargetTransferException
    {
        if (mode == Mode.BackupTarget)
        {
            return Utils.isNullOrEmtpy(file.PathBackupTarget) ? file.PathSource : file.PathBackupTarget;
        } else if (mode == Mode.Source)
        {
            return file.PathSource;
        } else
        {
            Logger.log(LogSeverity.Error, "Unknown backup Handler Mode " + mode);
            return null;
        }
    }


    protected String getBackupPath(String path, String separator, long backupDate, boolean createFolder) throws TargetTransferException
    {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH_mm_ss");
        String srcPath = path;
        if (!srcPath.startsWith(getSeparator()))
            srcPath = getSeparator() + srcPath;

        String filePath = props.BasePath + getSeparator() + sdf.format(new Date(backupDate)) + fixSeparators(srcPath);
        filePath = filePath.replace(separator, getSeparator());
        String folderPath = filePath.substring(0, filePath.lastIndexOf(getSeparator()));
        if (createFolder)
            createFolderParent(folderPath);
        return filePath;
    }

    protected enum Mode
    {
        BackupTarget,
        Source
    }

    protected enum State
    {
        FoundInSource,
        TransferredToBackup,
        TransferredFromBackup,
    }
}
