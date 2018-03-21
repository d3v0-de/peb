package de.d3v0.peb.controller;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import de.d3v0.peb.common.*;
import de.d3v0.peb.common.Properties;
import de.d3v0.peb.common.sourceproperties.Filter.BackupFilter;
import de.d3v0.peb.common.sourceproperties.SourceProperties;
import de.d3v0.peb.common.dbhelper.DbHelper;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class MainHandler implements Runnable
{

    private Properties prop;
    private TargetHandler targetHandlerManager;
    private DbHelper dbHelper;

    private static final String  ThreadNameReadTransferQueue = "readTransferQueue";
    private static final String  ThreadNamedDWorker = "dbWorker";
    
    ReentrantLock lockTransferQueue = new ReentrantLock();
    ReentrantLock lockTransferDoneQueue = new ReentrantLock();

    private List<BackupFile> transferQueue;
    private List<BackupFile> transferDoneQueue;


    private long transferCountTotal = 0;
    private long transferCountDone = 0;
    private long transferVolumeTotal = 0;
    private long transferVolumeDone = 0;
    private boolean fillTransferQueueFinished = false;

    public MainHandler(String workingDir) throws IOException, ClassNotFoundException
    {
        ReadConfig(workingDir);
        transferQueue = new ArrayList<BackupFile>();
        transferDoneQueue = new ArrayList<BackupFile>();
    }

    private static File getPropertiesFile(String workingDir) throws IOException
    {
        File res = new File(workingDir + File.separator + "properties.xml");
        if (!res.exists())
            if (!res.createNewFile())
                throw new IOException("failed to create props");
        return res;
    }

    protected void ReadConfig(String workingDir) throws IOException, ClassNotFoundException
    {
        XStream xs = new XStream(new DomDriver());
        FileInputStream is = new FileInputStream(getPropertiesFile(workingDir));
        prop = (Properties)xs.fromXML(is);
    }

    protected void BackupConfig() throws IOException
    {
        XStream xs = new XStream(new DomDriver());
        StringOutputStream sos = new StringOutputStream();
        xs.toXML(prop, sos);
        targetHandlerManager.backupFile(sos.getInputStream(), "properties.xml");
    }

    public void SaveConfig() throws IOException
    {
        SaveConfig(prop);
    }

    public static void SaveConfig(Properties p) throws IOException
    {
        FileOutputStream fout = new FileOutputStream(getPropertiesFile(p.workingDir));

        XStream xs = new XStream(new DomDriver());
        xs.toXML(p, fout);
        fout.close();
    }

    public void performBackup()
    {
        Thread mainWorker = new Thread(this, ThreadNamedDWorker);
        mainWorker.start();
    }

    private void performBackupInt()
    {
        try
        {
            targetHandlerManager = TargetHandler.create(prop.targetProperties);
            dbHelper = DbHelper.Create(prop, targetHandlerManager);
            Logger.Create(prop.workingDir + File.separator + "peb.log");

            BackupConfig();
            startTranserQueue();

            for (SourceProperties source : prop.sourceProperties)
            {
                File f = new File(source.Path);
                try
                {
                    processFile(f, source);
                    fillTransferQueueFinished = true;
                    readTransferDoneQueue();
                    dbHelper.commit();
                    targetHandlerManager.saveBackupDates();
                    targetHandlerManager.close();
                } catch (Exception e)
                {
                    Logger.log(e);
                }
            }
        } catch (Exception ex)
        {
            Logger.log(ex);
        }
    }

    private void startTranserQueue()
    {
        for (int i=0 ; i< prop.targetProperties.WorkerThreadCount ; i++)
        {
            Thread readworkFileQueue = new Thread(this, ThreadNameReadTransferQueue);
            readworkFileQueue.start();
        }
    }

    private void processFile(File f, SourceProperties source)
    {
        BackupFile bf = new BackupFile(f);

        if (filter(source, bf))
            return;

        if (!f.canRead())
        {
            Logger.log(Logger.LogSeverity.Warn,"File/Folder '" + f.getAbsolutePath() + "' is not accessible");
            return;
        }

        if (Files.isSymbolicLink(f.toPath()))
        {
            Logger.log(Logger.LogSeverity.Warn, "File/Folder '" + f.getAbsolutePath() + "' is a symbolic link - I don't follow (and don't save as well)");
            return;
        }

        if (f.isDirectory())
        {
            for (File child : f.listFiles())
                    processFile(child, source);
        } else
        {

            long rows = 0;
            try
            {
                rows = dbHelper.Update(bf, targetHandlerManager.getPerfDate());
            } catch (SQLException e)
            {
                Logger.log(LoggerBase.LogSeverity.Error, "Error Updating LastSeen for file " + bf.Path);
                Logger.log(e);
            }

            // Update didn't match (the file in it's current version has not already been backuped ==> make Backup
            if (rows == 0)
                enqueueTransfer(bf);
        }
    }

    private boolean filter(SourceProperties source, BackupFile bf)
    {
        if (source.Filters != null)
            if (source.filterMode == SourceProperties.FilterMode.Blacklist )
            {
                for (BackupFilter filter : source.Filters)
                    if (filter.match(bf))
                        return true;
            }
            else if (source.filterMode == SourceProperties.FilterMode.Whitelist)
            {
                boolean matched=false;
                for (BackupFilter filter : source.Filters)
                    if (filter.match(bf))
                    {
                        matched = true;
                        Logger.log(Logger.LogSeverity.Debug,"Backlist " + filter.getInfo() + "matched for " + bf.Path);
                        break;
                    }
                    if (!matched)
                    {
                        Logger.log(Logger.LogSeverity.Debug,"No Whitelist matched for " + bf.Path);
                        return true;
                    }
            }
            else
            {
                Logger.log(LoggerBase.LogSeverity.Error, "Invalid value " + source.filterMode + " for SourceProperties.FilterMode");
            }
        return false;
    }

    private void enqueueTransfer(BackupFile f)
    {
        lockTransferQueue.lock();
        transferQueue.add(f);
        lockTransferQueue.unlock();
        transferCountTotal++;
        transferVolumeTotal += f.Size;
    }

    private void readTransferQueue()
    {

        TargetHandler targetHandler = null;
        try
        {
            targetHandler = TargetHandler.create(targetHandlerManager);
        } catch (Exception e)
        {
            Logger.log(e);
        }

        while (fillTransferQueueFinished == false || transferQueue.size() > 0)
            {
                BackupFile f = null;
                lockTransferQueue.lock();
                if (transferQueue.size() > 0)
                {
                    f = transferQueue.get(0);
                    transferQueue.remove(0);
                }
                lockTransferQueue.unlock();
                if (f != null)
                    processContent(f, targetHandler);
                else
                {
                    try
                    {
                        Thread.sleep(100);
                    } catch (InterruptedException e)
                    {
                        Logger.log(e);
                    }
                }
            }

        try
        {
            targetHandler.close();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private void readTransferDoneQueue()
    {
        boolean shouldRun = true;
        while (shouldRun)
        {
            BackupFile f = null;
            lockTransferDoneQueue.lock();
            if (transferDoneQueue.size() > 0)
            {
                f = transferDoneQueue.get(0);
                transferDoneQueue.remove(0);
            }
            else
            {
                lockTransferQueue.lock();
                // finished
                if (transferQueue.size() == 0)
                    shouldRun = false;
                lockTransferQueue.unlock();
            }
            lockTransferDoneQueue.unlock();
            if (f != null)
            {
                try
                {
                    dbHelper.Insert(f, targetHandlerManager.getPerfDate());
                } catch (SQLException e)
                {
                    LoggerBase.log(e);
                }
            }
            else
            {
                try
                {
                    Thread.sleep(100);
                } catch (InterruptedException e)
                {
                    LoggerBase.log(e);
                }
            }
        }
    }


    private void processContent(BackupFile f, TargetHandler targetHandler)
    {

        try
        {
            targetHandler.backupFile(f.getReadStream(), f.Path);
            lockTransferDoneQueue.lock();
            transferDoneQueue.add(f);
            lockTransferDoneQueue.unlock();
            transferCountDone++;
            transferVolumeDone += f.Size;
        } catch (FileNotFoundException e)
        {
            Logger.log(LoggerBase.LogSeverity.Error, "Error for file " + f.Path);
            Logger.log(e);
        }

    }


    @Override
    public void run()
    {
        if (Thread.currentThread().getName() == ThreadNamedDWorker)
            performBackupInt();
        else if (Thread.currentThread().getName() == ThreadNameReadTransferQueue)
            readTransferQueue();
    }
}
