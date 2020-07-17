package de.d3v0.peb.common.dbhelper;

public class MySqlProperties extends DbProperties
{
    public String ConnectionString;
    public String UserName;
    public String Password;

    @Override
    public String getHandlerClassname()
    {
        return MySqlHelper.class.getCanonicalName();
    }

}
