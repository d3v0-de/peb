package de.d3v0.peb.common.dbhelper;

import java.sql.DriverManager;
import java.sql.SQLException;

public class MySqlHelper extends DbHelper
{
    @Override
    protected void initConnection(boolean firstRun) throws Exception
    {
        MySqlProperties myProp = ((MySqlProperties)this.props.dbProperties);
        con = DriverManager.getConnection(myProp.ConnectionString, myProp.UserName, myProp.Password);
        con.setAutoCommit(true);

    }

    @Override
    protected void initSchema() throws SQLException
    {
        //TODO: initSchema
    }
}
