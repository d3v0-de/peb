package de.d3v0.peb.common;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import de.d3v0.peb.common.dbhelper.DbProperties;
import de.d3v0.peb.common.sourceproperties.SourceProperties;
import de.d3v0.peb.common.IOProperties.IOHandlerProperties;

import java.io.*;

public class Properties
{
    public SourceProperties[] sourceProperties;
    public IOHandlerProperties targetProperties;
    public DbProperties dbProperties;
    public String workingDir;

    public static Properties ReadConfig(String workingDir) throws IOException, ClassNotFoundException
    {
        XStream xs = new XStream(new DomDriver());
        FileInputStream is = new FileInputStream(getPropertiesFile(workingDir));
        return (Properties)xs.fromXML(is);
    }

    public static void SaveConfig(Properties p) throws IOException
    {
        FileOutputStream fout = new FileOutputStream(getPropertiesFile(p.workingDir));

        XStream xs = new XStream(new DomDriver());
        xs.toXML(p, fout);
        fout.close();
    }

    public static String getProertiesFilePath(String workingDir)
    {
        return workingDir + File.separator + "properties.xml";
    }

    public String getProertiesFilePath()
    {
        return getProertiesFilePath(workingDir);
    }

    private static File getPropertiesFile(String workingDir) throws IOException
    {
        File res = new File(getProertiesFilePath(workingDir));
        if (!res.exists())
            if (!res.createNewFile())
                throw new IOException("failed to create props");
        return res;
    }
}


