package de.d3v0.peb.controller;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import de.d3v0.peb.common.BackupFile;
import de.d3v0.peb.common.Logger;
import de.d3v0.peb.common.StringOutputStream;
import de.d3v0.peb.common.misc.BackupNotFoundException;
import de.d3v0.peb.common.misc.MyBoolean;
import de.d3v0.peb.common.misc.TargetTransferException;
import de.d3v0.peb.common.targetproperties.TargetProperties;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public abstract class TargetHandler implements Closeable
{
    private List<Long> backupDates;
    TargetProperties props;
    protected static Hashtable<String, MyBoolean> dirCreated = new Hashtable<>();

    public long getPerfDate()
    {
        return perfDate;
    }

    private long perfDate;

    public TargetHandler()
    {
        perfDate = new Date().getTime();
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
        StringOutputStream sos = new StringOutputStream();

        try
        {
            readFile(props.BasePath + getSeparator() + "backups.xml", sos);
            return (List<Long>) xs.fromXML(sos.getString());
        }
        catch (Exception ex)
        {
            Logger.log(ex);
            return new ArrayList<Long>();
        }
    }
    public void saveBackupDates() throws TargetTransferException
    {
        backupDates.add(perfDate);

        XStream xs = new XStream(new DomDriver());

        StringOutputStream sos = new StringOutputStream();
        xs.toXML(backupDates, sos);

        writeFile(sos.getInputStream(), props.BasePath + getSeparator() + "backups.xml");
    }

    protected abstract void readFile(String path, OutputStream dst) throws BackupNotFoundException, TargetTransferException;
    protected abstract void writeFile(InputStream src, String path) throws TargetTransferException;
    protected abstract void createFolderInt(String path, boolean checkFolderexists ) throws TargetTransferException;

    public void restoreFile(BackupFile file) throws BackupNotFoundException, TargetTransferException, FileNotFoundException
    {
        if (getBackupDates() != null && getBackupDates().size()!= 0)
            restoreFile(file, getBackupDates().get(getBackupDates().size() -1));
        else throw new BackupNotFoundException("no Backup existing yet");
    }
    public void restoreFile(BackupFile file, long backupDate) throws BackupNotFoundException, TargetTransferException, FileNotFoundException
    {
        readFile(getBackupPath(file, backupDate, false), file.getWriteStream());
    }

    protected void backupFile(BackupFile file, long backupDate) throws TargetTransferException, FileNotFoundException
    {
        writeFile(file.getReadStream(), getBackupPath(file, backupDate, true));
    }

    public void backupFile(BackupFile file) throws TargetTransferException, FileNotFoundException
    {
        backupFile(file, this.perfDate);
    }


    public static TargetHandler create(TargetHandler targetHandler) throws ClassNotFoundException, IllegalAccessException, InvocationTargetException, InstantiationException
    {
        TargetHandler res = create(targetHandler.props);
        res.perfDate = targetHandler.perfDate;
        return res;
    }

    public static TargetHandler create(TargetProperties targetProperties) throws ClassNotFoundException, IllegalAccessException, InvocationTargetException, InstantiationException
    {
        Class<?> clazz = Class.forName(targetProperties.getHandlerClassname());
        TargetHandler res = null;
        for (Constructor<?> ctor : clazz.getConstructors())
        {
            res = (TargetHandler)ctor.newInstance();
            break;
        }
        if (res == null)
        {

            throw new ClassNotFoundException("Class " + targetProperties.getHandlerClassname() + " has no constructor");
        }
        else
        {
            res.props = targetProperties;
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
            MyBoolean exists = null;
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
                if (exists.Value == false)
                {
                    boolean checkFolderexists = props.BasePath.contains(currentPath.toString());
                    createFolderInt(currentPath.toString(), checkFolderexists);
                    exists.Value = true;
                }
            }
            currentPath.append(getSeparator());
        }
    }

    protected String getBackupPath(BackupFile file, long backupDate, boolean createFolder) throws TargetTransferException
    {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH_mm_ss");
        String srcPath = file.Path;
        if (!srcPath.startsWith(getSeparator()))
            srcPath = getSeparator() + srcPath;

        String filePath = props.BasePath + getSeparator() + sdf.format(new Date(backupDate)) + srcPath;
        filePath = filePath.replace(file.Separator, getSeparator());
        String folderPath = filePath.substring(0, filePath.lastIndexOf(getSeparator()));
        if (createFolder)
            createFolderParent(folderPath);
        return filePath;
    }

    protected String getSeparator()
    {
        return File.separator;
    }
}
