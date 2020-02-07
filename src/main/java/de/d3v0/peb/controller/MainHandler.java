package de.d3v0.peb.controller;

import de.d3v0.peb.common.*;
import de.d3v0.peb.common.dbhelper.DbHelper;
import de.d3v0.peb.common.misc.LogSeverity;
import de.d3v0.peb.common.misc.TargetTransferException;
import de.d3v0.peb.common.sourceproperties.Filter.BackupFilter;
import de.d3v0.peb.common.sourceproperties.SourceProperties;
import de.d3v0.peb.controller.IO.FileHandler;
import de.d3v0.peb.controller.IO.IOHandler;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MainHandler implements Runnable
{

    private Properties prop;
    private IOHandler targetHandlerManager;
    private DbHelper dbHelper;
    private boolean createNew;

    private static final String  ThreadNameReadTransferQueue = "readTransferQueue";
    private static final String  ThreadNamedDWorker = "dbWorker";

    private final List<BackupFile> transferQueue;
    private final List<BackupFile> transferDoneQueue;
    private final List<BackupFile> transferInWorkList;

    private boolean fillTransferQueueFinished = false;

    public MainHandler(String workingDir, boolean createNew) throws IOException, ClassNotFoundException
    {
        this.prop = Properties.ReadConfig(workingDir);
        this.createNew = createNew;
        transferQueue = new ArrayList<>();
        transferDoneQueue = new ArrayList<>();
        transferInWorkList  = new ArrayList<>();
    }


    public void performBackup()
    {
        Thread mainWorker = new Thread(this, ThreadNamedDWorker);
        mainWorker.start();
    }

    private void t1_performBackupInt()
    {
        try
        {
            FileLogger.CreateDefault(prop.workingDir + File.separator + "peb.log");
            LoggerBase.log(LogSeverity.Info, "Starting Backup");
            targetHandlerManager = IOHandler.create(prop.targetProperties, true, this.createNew);
            dbHelper = DbHelper.Create(prop, targetHandlerManager, this.createNew);

            t1_startTransferQueue();

            for (SourceProperties source : prop.sourceProperties)
            {
                LoggerBase.log(LogSeverity.Info, "Starting to scan source " + source.IOProperties.BasePath);
                IOHandler sourceHandler = IOHandler.create(source.IOProperties, false, createNew);
                try
                {
                    t1_processSourceFile(sourceHandler.getFileInfo(source.IOProperties.BasePath, true), source);
                } catch (Exception e)
                {
                    FileLogger.log(e);
                }

            }

            LoggerBase.log(LogSeverity.Info, "Scan sources finished");
            fillTransferQueueFinished = true;
            t1_readTransferDoneQueue();
            FileLogger.log(LogSeverity.Info, dbHelper.GetStats(targetHandlerManager.getPerfDate()));
            IOHandler.logStats();
            dbHelper.commit();
            backupConfig();
            targetHandlerManager.saveBackupDates();
            targetHandlerManager.backupLog();
            targetHandlerManager.close();
        } catch (Exception ex)
        {
            FileLogger.log(ex);
        }
    }

    private void t1_startTransferQueue()
    {
        for (int i=0 ; i< prop.targetProperties.WorkerThreadCount ; i++)
        {
            Thread readworkFileQueue = new Thread(this, ThreadNameReadTransferQueue);
            readworkFileQueue.start();
        }
    }

    private void t1_processSourceFile(BackupFile bf, SourceProperties source) throws TargetTransferException {



        if (t1_filter(source, bf))
            return;


        if (bf.type == BackupFile.FileType.Directory)
        {
            for (BackupFile child : bf.listChildren())
                    t1_processSourceFile(child, source);
        } else
        {

            long rows = 0;
            try
            {
                rows = dbHelper.Update(bf, targetHandlerManager.getPerfDate());
            } catch (SQLException e)
            {
                FileLogger.log(LogSeverity.Error, "Error Updating LastSeen for file " + bf.PathSource);
                FileLogger.log(e);
            }

            // Update didn't match (the file in it's current version has not already been backuped ==> make Backup
            if (rows == 0)
                t1_enqueueTransfer(bf);
        }
    }

    private boolean t1_filter(SourceProperties source, BackupFile bf)
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
                        FileLogger.log(LogSeverity.Debug,"Backlist " + filter.getInfo() + "matched for " + bf.PathSource);
                        break;
                    }
                    if (!matched)
                    {
                        FileLogger.log(LogSeverity.Debug,"No Whitelist matched for " + bf.PathSource);
                        return true;
                    }
            }
            else
            {
                FileLogger.log(LogSeverity.Error, "Invalid value " + source.filterMode + " for SourceProperties.FilterMode");
            }
        return false;
    }

    private void t1_enqueueTransfer(BackupFile f)
    {
        synchronized (transferQueue)
        {
            transferQueue.add(f);
        }
    }

    private void t2_readTransferQueue()
    {

        IOHandler targetHandler = null;
        try
        {
            targetHandler = IOHandler.create(targetHandlerManager);
        } catch (Exception e)
        {
            FileLogger.log(e);
        }

        while (!fillTransferQueueFinished || transferQueue.size() > 0)
            {
                BackupFile f = null;
                synchronized (transferQueue)
                {
                    if (transferQueue.size() > 0)
                    {
                        f = transferQueue.get(0);
                        transferQueue.remove(0);
                        synchronized (transferInWorkList)
                        {
                            transferInWorkList.add(f);
                        }
                    }
                }
                if (f != null)
                {
                    backupContent(f, targetHandler);
                    synchronized (transferInWorkList)
                    {
                        transferInWorkList.remove(f);
                    }
                }
                else
                {
                    try
                    {
                        Thread.sleep(100);
                    } catch (InterruptedException e)
                    {
                        FileLogger.log(e);
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

    private void t1_readTransferDoneQueue()
    {
        boolean shouldRun = true;
        while (shouldRun)
        {
            BackupFile f = null;
            synchronized (transferDoneQueue)
            {
                if (transferDoneQueue.size() > 0)
                {
                    f = transferDoneQueue.get(0);
                    transferDoneQueue.remove(0);
                } else
                {
                    synchronized (transferQueue)
                    {
                        synchronized (transferInWorkList)
                        {
                            // finished
                            if (transferQueue.size() == 0 && transferInWorkList.size() == 0)
                                shouldRun = false;
                        }
                    }
                }
            }
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


    private void backupContent(BackupFile f, IOHandler targetHandler)
    {

        try
        {
            targetHandler.backupFile(f);
            synchronized (transferDoneQueue)
            {
                transferDoneQueue.add(f);
            }
        } catch (Exception e)
        {
            LoggerBase.log(LogSeverity.Error, "Error for file " + f.PathSource);
            LoggerBase.log(e);
        }
    }

    protected void backupConfig() throws IOException, TargetTransferException
    {
        BackupFile f = new FileHandler().getFileInfo(prop.getProertiesFilePath(), true);
        targetHandlerManager.backupFile(f);
    }


    @Override
    public void run()
    {
        if (Thread.currentThread().getName() == ThreadNamedDWorker)
            t1_performBackupInt();
        else if (Thread.currentThread().getName() == ThreadNameReadTransferQueue)
            t2_readTransferQueue();
    }
}
