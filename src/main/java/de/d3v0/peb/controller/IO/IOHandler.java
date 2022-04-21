package de.d3v0.peb.controller.IO;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import de.d3v0.peb.common.BackupFile;
import de.d3v0.peb.common.FileLogger;
import de.d3v0.peb.common.LoggerBase;
import de.d3v0.peb.common.StringOutputStream;
import de.d3v0.peb.common.misc.*;
import de.d3v0.peb.common.IOProperties.IOHandlerProperties;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;

public abstract class IOHandler implements Closeable
{
    private List<Long> backupDates;
    protected boolean createNew;
    IOHandlerProperties props;
    protected Mode mode;
    protected static final Hashtable<String, MyBoolean> dirCreated = new Hashtable<>();
    protected static final Hashtable<State, Statistics> stats = new Hashtable<>();

    private MessageDigest mdigest;
    byte[] md5Buffer = new byte[1024*1024];

    {
        try
        {
            mdigest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e)
        {
            e.printStackTrace();
        }
    }

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
        {
            if (this.createNew)
                backupDates = new ArrayList<>();
            else
                backupDates = retrieveBackupDates();
        }
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
            FileLogger.log(ex);
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

    public void backupLog()
    {
        try (InputStream is = LoggerBase.getReadStream())
        {
            try (OutputStream os = getWriteStream(getBackupPath("backups.log", getSeparator(), this.getPerfDate(), true)))
            {
                is.transferTo(os);
            }
        } catch (Exception e)
        {
            LoggerBase.log(e);
        }
    }



    protected abstract boolean testConnection();
    protected abstract void createFolderInt(String path, boolean checkFolderexists ) throws TargetTransferException;
    protected abstract boolean checkFolderexists(String path);
    protected abstract BackupFile getFileInfoInt(String localPath, boolean forBackup);
    public abstract Iterable<? extends BackupFile> listChildren(BackupFile parent) throws TargetTransferException;

    protected abstract InputStream getReadStream(String path) throws TargetTransferException;
    protected abstract OutputStream getWriteStream(String path) throws TargetTransferException;
    protected abstract String getSeparator();

    public BackupFile getFileInfo(String localPath, boolean forBackup) throws TargetTransferException
    {
        BackupFile res = getFileInfoInt(localPath, forBackup);
        if (res.type == BackupFile.FileType.File) {
            if (forBackup)
                GenerateHash(localPath, res);

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
                    FileLogger.log(LogSeverity.Error, "Unkown mode " + this.mode);
            }
        }
        return res;
    }

    private void GenerateHash(String localPath, BackupFile res) throws TargetTransferException
    {
        try (InputStream is = getReadStream(res.PathSource))
        {
            int bytesCount = 0;
            while ((bytesCount = is.read(md5Buffer)) != -1)
            {
                mdigest.update(md5Buffer, 0, bytesCount);
            }
            BigInteger bigInt = new BigInteger(1, mdigest.digest());
            res.Hash = bigInt.toString(16);
            while(res.Hash.length() < 32 ){
                res.Hash = "0"+res.Hash;
            }
        } catch (IOException e)
        {
            LoggerBase.log(LogSeverity.Error, "Error getHash: " + localPath);
        }
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

    protected void backupFile(BackupFile file, long backupDate) throws IOException
    {
        try
        {
            String targetPath = getBackupPath(file, backupDate, true);
            for (int i = 0; i < 2; i++)
            {
                if (this.testConnection())
                    break;
            }
            try (OutputStream os = getWriteStream(targetPath))
            {
                try (InputStream is = file.getIOHandler().getReadStream(file.PathSource))
                {
                    long size = is.transferTo(os);
                    getStat(State.TransferredToBackup).count++;
                    getStat(State.TransferredToBackup).size += size;
                }
            }
        }
        catch (TargetTransferException ex)
        {
            String path = "null";
            if (file != null)
                path = file.PathSource;
            LoggerBase.log(LogSeverity.Error, "Error backupFile: " + path);
        }
    }

    public void backupFile(BackupFile file) throws TargetTransferException, IOException
    {
        backupFile(file, this.perfDate);
    }


    public static IOHandler create(IOHandler targetHandler) throws ClassNotFoundException, IllegalAccessException, InvocationTargetException, InstantiationException
    {
        IOHandler res = create(targetHandler.props, true, targetHandler.createNew);
        res.perfDate = targetHandler.perfDate;
        res.mode = targetHandler.mode;
        return res;
    }

    public static IOHandler create(IOHandlerProperties properties, boolean isTarget, boolean createNew) throws ClassNotFoundException, IllegalAccessException, InvocationTargetException, InstantiationException
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
            res.createNew = createNew;
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
                for (int i=0; i<2 && !exists.Value ; i++)
                {
                    boolean checkFolderexists = props.BasePath.contains(currentPath.toString());
                    createFolderInt(currentPath.toString(), checkFolderexists);
                    exists.Value = this.checkFolderexists(currentPath.toString());
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
            FileLogger.log(LogSeverity.Error, "Unknown backup Handler Mode " + mode);
    }

    public static void logStats()
    {
        FileLogger.log(LogSeverity.Info, getStatString(State.FoundInSource));
        FileLogger.log(LogSeverity.Info, getStatString(State.TransferredToBackup));
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
            if (Utils.isNullOrEmtpy(file.PathSource))
                throw new TargetTransferException(this.getClass() + ": is in " + mode + "; but PathSource is Empty; maybe PathBackupTarget helps:" + file.PathBackupTarget);
            else
                return file.PathSource;
        } else
        {
            throw new TargetTransferException("Unknown backup Handler Mode " + mode);
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
