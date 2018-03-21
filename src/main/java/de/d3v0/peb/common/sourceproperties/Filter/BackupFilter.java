package de.d3v0.peb.common.sourceproperties.Filter;

import de.d3v0.peb.common.BackupFile;

public abstract class BackupFilter
{
    public boolean Reverse = false;

    protected abstract boolean matchFile(BackupFile file);

    public boolean match(BackupFile file)
    {
        return Reverse ^ matchFile(file);
    }

    public abstract String getInfo();
}
