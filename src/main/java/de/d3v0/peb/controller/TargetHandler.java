package de.d3v0.peb.controller;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import de.d3v0.peb.common.Logger;
import de.d3v0.peb.common.StringOutputStream;
import de.d3v0.peb.common.targetproperties.TargetProperties;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public abstract class TargetHandler implements Closeable
{
    private List<Long> backupDates;
    TargetProperties props;

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
    public void saveBackupDates()
    {
        backupDates.add(perfDate);

        XStream xs = new XStream(new DomDriver());

        StringOutputStream sos = new StringOutputStream();
        xs.toXML(backupDates, sos);

        writeFile(sos.getInputStream(), props.BasePath + getSeparator() + "backups.xml");
    }

    abstract void readFile(String path, OutputStream dst) throws JSchException, SftpException;
    abstract void writeFile(InputStream src, String path);

    public void restoreFile(String relativePath, OutputStream os) throws JSchException, SftpException
    {
        if (getBackupDates() != null && getBackupDates().size()!= 0)
            restoreFile(relativePath, os, getBackupDates().get(getBackupDates().size() -1));
    }
    public void restoreFile(String relativePath, OutputStream os, long backupDate) throws SftpException, JSchException
    {
        readFile(getBackupPath(relativePath, backupDate), os);
    }

    protected void backupFile(InputStream src, String relativePath, long backupDate)
    {
        writeFile(src, getBackupPath(relativePath, backupDate));
    }
    public void backupFile(InputStream src, String relativePath)
    {
        backupFile(src, relativePath, this.perfDate);
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

    protected String getBackupPath(String srcPath, long backupDate)
    {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH_mm_ss");
        if (!srcPath.startsWith(getSeparator()))
            srcPath = getSeparator() + srcPath;

        return props.BasePath + getSeparator() + sdf.format(new Date(backupDate)) + srcPath;
    }

    protected String getSeparator()
    {
        return File.separator;
    }
}
