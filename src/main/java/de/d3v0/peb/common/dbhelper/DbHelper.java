package de.d3v0.peb.common.dbhelper;

import de.d3v0.peb.common.BackupFile;
import de.d3v0.peb.common.Properties;
import de.d3v0.peb.common.misc.TargetTransferException;
import de.d3v0.peb.common.misc.Utils;
import de.d3v0.peb.controller.IO.IOHandler;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.locks.ReentrantLock;

public abstract class DbHelper
{

    protected Connection con;
    protected PreparedStatement updateStatement;
    protected PreparedStatement insertStatement;
    protected PreparedStatement statsStatement;
    protected static final ReentrantLock lock = new ReentrantLock();
    protected IOHandler targetHandler;
    protected Properties props;

    public static DbHelper Create(Properties props, IOHandler targetHandler, boolean firstRun) throws Exception
    {
        Class<?> clazz = Class.forName(props.dbProperties.getHandlerClassname());
        DbHelper res = null;
        for (Constructor<?> ctor : clazz.getConstructors())
        {
            res = (DbHelper)ctor.newInstance();
            break;
        }
        if (res == null)
        {
            throw new ClassNotFoundException("Class " + props.dbProperties.getHandlerClassname() + " has no constructor");
        }
        else
        {
            lock.lock();
            res.targetHandler = targetHandler;
            res.props = props;
            res.initConnection(firstRun);
            res.initSchema();
            lock.unlock();
            res.InitStatments();
            return res;
        }
    }

    protected abstract void initConnection(boolean firstRun) throws Exception;

    protected void InitStatments() throws SQLException
    {
        updateStatement = con.prepareStatement("update entry set lastSeen = ? where path = ? and lastMod = ? and size = ?");
        insertStatement = con.prepareStatement("insert into entry (path, lastMod, lastSeen, lastBackup, Size) VALUES(?, ?, ?, ?, ?)");
        statsStatement = con.prepareStatement("select count(*) COUNT, sum(Size) SIZE from entry where lastSeen = ?");
    }

    public void commit() throws SQLException, TargetTransferException, IOException
    {
        con.commit();
        con.close();
    }


    public int Update(BackupFile file, long backupTime) throws SQLException
    {
        updateStatement.setLong(1, backupTime);
        updateStatement.setString(2, file.PathSource);
        updateStatement.setLong(3, file.LastModified);
        updateStatement.setLong(4, file.Size);
        return updateStatement.executeUpdate();
    }

    public void Insert(BackupFile file, long backupTime) throws SQLException
    {
        insertStatement.setString(1, file.PathSource);
        insertStatement.setLong(2, file.LastModified);
        insertStatement.setLong(3, backupTime);
        insertStatement.setLong(4, backupTime);
        insertStatement.setLong(5, file.Size);
        insertStatement.executeUpdate();
    }

    public String GetStats(long backupTime) throws SQLException
    {
        statsStatement.setLong(1, backupTime);
        ResultSet res = statsStatement.executeQuery();
        int count = res.getInt("COUNT");
        long Size = res.getLong("SIZE");
        return "Stats in Db: Current Files as per Db: " + count + "; total Size as per Db " + Utils.humanReadable(Size);
    }

    protected abstract void initSchema() throws SQLException;
}
