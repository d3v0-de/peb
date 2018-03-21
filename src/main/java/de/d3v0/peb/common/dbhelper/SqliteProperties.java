package de.d3v0.peb.common.dbhelper;

import java.io.Serializable;

public class SqliteProperties extends DbProperties
{
    public String getHandlerClassname()
    {
        return SqliteHelper.class.getCanonicalName();
    }
}
