package de.d3v0.peb.common.dbhelper;

import com.mysql.cj.MysqlConnection;

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
        PreparedStatement schema = con.prepareStatement("select * from information_schema.tables where table_name = 'peb_schema'");
        // Version pre 0.1.2
        if (!schema.executeQuery().next())
        {
            con.prepareStatement("create table peb_schema (version varchar(10));").execute();
            con.prepareStatement("insert into peb_schema values('0.1.2')").execute();
        }


        schema = con.prepareStatement("select version from peb_schema;");
        ResultSet res = schema.executeQuery();
        res.next();
        String version = res.getString(1);
        switch(version)
        {
            case "0.1.2":
                UpdateTo013();
            case "0.1.3":
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + version);
        }
    }

    private void UpdateTo013() throws SQLException
    {
        con.prepareStatement("alter table entry add column (fileHash varchar(32));").execute();
        con.prepareStatement("update peb_schema set version = '0.1.3';").execute();
    }
}
