package de.d3v0.peb.common.dbhelper;

import de.d3v0.peb.common.BackupFile;
import de.d3v0.peb.common.Logger;
import de.d3v0.peb.common.misc.TargetTransferException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class SqliteHelper extends DbHelper
{

    protected void initConnection() throws Exception
    {
        getDatabaseFile();
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

    private void getDatabaseFile() throws Exception
    {
        try
        {
            File db = new File(getDatabaseLocalPath());
            if (db.exists())
                db.delete();
            FileOutputStream fos = new FileOutputStream(db);
            targetHandler.restoreFile(new BackupFile(DatabaseFileName, fos));
        } catch (Exception e)
        {
            Logger.log(e);
            throw e;
        }
    }

    private String getDatabaseLocalPath()
    {
        return props.workingDir + File.separator + DatabaseFileName;
    }

    private final String DatabaseFileName = "fastbackup.db";

    @Override
    public void commit() throws SQLException, FileNotFoundException, TargetTransferException
    {
        super.commit();
        File db = new File(getDatabaseLocalPath());
        FileInputStream fis = new FileInputStream(db);
        targetHandler.backupFile(new BackupFile(DatabaseFileName, fis));
    }
}
