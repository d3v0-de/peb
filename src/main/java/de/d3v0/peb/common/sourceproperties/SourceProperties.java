package de.d3v0.peb.common.sourceproperties;

import de.d3v0.peb.common.IOProperties.IOHandlerProperties;
import de.d3v0.peb.common.sourceproperties.Filter.BackupFilter;

import java.util.List;

public class SourceProperties
{
    public IOHandlerProperties IOProperties;
    public List<BackupFilter> Filters;
    public final FilterMode filterMode = FilterMode.Blacklist;

    public enum FilterMode
    {
        Blacklist,
        Whitelist
    }
}
