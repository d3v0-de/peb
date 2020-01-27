package de.d3v0.peb.common.sourceproperties.Filter;

import de.d3v0.peb.common.BackupFile;

public class DirectotyFilter extends BackupFilter
{
    public String Directory;

    @Override
    protected boolean matchFile(BackupFile file)
    {
        return file.PathSource.startsWith(Directory);
    }

    @Override
    public String getInfo()
    {
        return "RegexFilter: " + Directory;
    }
}
