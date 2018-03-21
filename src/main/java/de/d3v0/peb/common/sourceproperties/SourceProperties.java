package de.d3v0.peb.common.sourceproperties;

import de.d3v0.peb.common.sourceproperties.Filter.BackupFilter;

import java.util.List;

public class SourceProperties
{
    public String Path;
    public List<BackupFilter> Filters;
    public FilterMode filterMode = FilterMode.Blacklist;

    public enum FilterMode
    {
        Blacklist,
        Whitelist
    }
}
