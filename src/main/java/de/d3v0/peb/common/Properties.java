package de.d3v0.peb.common;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.security.NoTypePermission;
import com.thoughtworks.xstream.security.NullPermission;
import com.thoughtworks.xstream.security.PrimitiveTypePermission;
import de.d3v0.peb.common.dbhelper.DbProperties;
import de.d3v0.peb.common.misc.LogSeverity;
import de.d3v0.peb.common.sourceproperties.SourceProperties;
import de.d3v0.peb.common.IOProperties.IOHandlerProperties;

import java.io.*;

public class Properties
{
    public SourceProperties[] sourceProperties;
    public IOHandlerProperties targetProperties;
    public DbProperties dbProperties;
    public String workingDir;
    public int batchRuntimeSeconds;
    public int logStatsEverySeconds;

    public static Properties ReadConfig(String workingDir) throws IOException, ClassNotFoundException
    {
        try
        {
            XStream xs = new XStream(new DomDriver());
            xs.addPermission(NoTypePermission.NONE); //forbid everything
            xs.addPermission(NullPermission.NULL);   // allow "null"
            xs.addPermission(PrimitiveTypePermission.PRIMITIVES); // allow primitive types
            xs.allowTypesByWildcard(new String[] {
                    "de.d3v0.peb.**"});

            FileInputStream is = new FileInputStream(getPropertiesFile(workingDir));
            return (Properties) xs.fromXML(is);
        }
        catch(Exception e)
        {
            LoggerBase.log(LogSeverity.Error, "Error reading Config:");
            LoggerBase.log(e);
            throw e;
        }
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


