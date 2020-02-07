package de.d3v0.peb.common.dbhelper;

import de.d3v0.peb.common.BackupFile;
import de.d3v0.peb.common.FileLogger;
import de.d3v0.peb.common.LoggerBase;
import de.d3v0.peb.common.misc.LogSeverity;
import de.d3v0.peb.common.misc.TargetTransferException;
import de.d3v0.peb.controller.IO.FileHandler;

import java.io.*;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;

public class SqliteHelper extends DbHelper
{

    protected void initConnection(boolean createNew) throws Exception
    {
        if (createNew)
        {
            File db = new File(getDatabaseLocalPath());
            if (db.exists())
                throw new Exception("there is already a database file in the working copy - but that was not expected!!!! Please delete that first");
        }
        else
            getDatabaseFileFromTarget();


        con = DriverManager.getConnection("jdbc:sqlite:" + getDatabaseLocalPath());
        Statement s = con.createStatement();
        s.execute("PRAGMA locking_mode = EXCLUSIVE");
        s.execute("PRAGMA synchronous = OFF");
        s.execute("PRAGMA journal_mode = MEMORY");
        s.close();
        con.setAutoCommit(false);
        con.commit();
    }

    protected void initSchema() throws SQLException
    {
        Statement s = con.createStatement();
        if (!s.executeQuery("SELECT * FROM sqlite_master where type = 'table' and name = 'entry'").next())
        {
            s.executeUpdate("CREATE TABLE \"entry\" (\n" +
                    "\t`path`\tTEXT,\n" +
                    "\t`lastMod`\tNUMERIC,\n" +
                    "\t`lastSeen`\tNUMERIC,\n" +
                    "\t`lastBackup`\tNUMERIC,\n" +
                    "\t`Size`\tNUMERIC\n" +
                    ")");
            s.executeUpdate("CREATE INDEX ix_entry_path2 on entry(path,lastmod,size)");
        }
    }

    private void getDatabaseFileFromTarget() throws Exception
    {
        try
        {
            LoggerBase.log(LogSeverity.Info, "Retrieving Database file from Backup");
            BackupFile f = new FileHandler().getFileInfo(getDatabaseLocalPath(), false);
            f.PathBackupTarget = DatabaseFileName;
            targetHandler.restoreFile(f);
        } catch (Exception e)
        {
            LoggerBase.log(e);
            throw e;
        }
    }

    private String getDatabaseLocalPath()
    {
        return props.workingDir + File.separator + DatabaseFileName;
    }

    private final String DatabaseFileName = "fastbackup.db";

    @Override
    public void commit() throws SQLException, TargetTransferException, IOException
    {
        BackupFile realFile = new FileHandler().getFileInfo(getDatabaseLocalPath(), true);
        realFile.PathBackupTarget = this.DatabaseFileName;

        BackupFile dbFile = new BackupFile(this.targetHandler, this.DatabaseFileName);
        dbFile.Size = realFile.Size;
        dbFile.LastModified = new Date().getTime();
        this.Insert(dbFile, targetHandler.getPerfDate());

        super.commit();
        this.targetHandler.backupFile(realFile);
    }
}
