package de.d3v0.peb.common;

import com.thoughtworks.xstream.XStream;
import de.d3v0.peb.common.dbhelper.DbProperties;
import de.d3v0.peb.common.sourceproperties.SourceProperties;
import de.d3v0.peb.common.targetproperties.TargetProperties;

import java.io.Serializable;

public class Properties
{
    public SourceProperties[] sourceProperties;
    public TargetProperties targetProperties;
    public DbProperties dbProperties;
    public String workingDir;
}


