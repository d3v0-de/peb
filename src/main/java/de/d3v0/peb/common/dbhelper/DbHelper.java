package de.d3v0.peb.common.dbhelper;

import de.d3v0.peb.common.BackupFile;
import de.d3v0.peb.common.Properties;
import de.d3v0.peb.common.misc.TargetTransferException;
import de.d3v0.peb.controller.Target.TargetHandler;

import java.io.FileNotFoundException;
import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.locks.ReentrantLock;

public abstract class DbHelper
{

    protected Connection con;
    protected PreparedStatement updateStatement;
    protected PreparedStatement insertStatement;
    protected static ReentrantLock lock = new ReentrantLock();
    protected TargetHandler targetHandler;
    protected Properties props;

    public static DbHelper Create(Properties props, TargetHandler targetHandler) throws Exception
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
            res.initConnection();
            res.initSchema();
            lock.unlock();
            res.InitStatments();
            return res;
        }
    }

    protected abstract void initConnection() throws SQLException, Exception;

    protected void InitStatments() throws SQLException
    {
        updateStatement = con.prepareStatement("update entry set lastSeen = ? where path = ? and lastMod = ? and size = ?");
        //((SQLiteConnection)con).

        insertStatement = con.prepareStatement("insert into entry (path, lastMod, lastSeen, lastBackup, Size) VALUES(?, ?, ?, ?, ?)");
    }

    public void commit() throws SQLException, FileNotFoundException, TargetTransferException
    {
        con.commit();
        con.close();
    }


    public int Update(BackupFile file, long backupTime) throws SQLException
    {
        updateStatement.setLong(1, backupTime);
        updateStatement.setString(2, file.Path);
        updateStatement.setLong(3, file.LastModified);
        updateStatement.setLong(4, file.Size);
        int res = updateStatement.executeUpdate();
        return res;
    }

    public int Insert(BackupFile file, long backupTime) throws SQLException
    {
        insertStatement.setString(1, file.Path);
        insertStatement.setLong(2, file.LastModified);
        insertStatement.setLong(3, backupTime);
        insertStatement.setLong(4, backupTime);
        insertStatement.setLong(5, file.Size);
        int res = insertStatement.executeUpdate();
        return res;
    }

    protected abstract void initSchema() throws SQLException;
}
