package de.d3v0.peb.common.sourceproperties.Filter;

import de.d3v0.peb.common.BackupFile;

public class RegexFilter extends BackupFilter
{
    public String Pattern;

    @Override
    protected boolean matchFile(BackupFile file)
    {
        int debug = 0;
        if (file.Path.contains(Pattern))
            debug++;

        boolean res  = file.Path.matches(Pattern);
        if (res == true)
            debug++;
        return res;
    }

    @Override
    public String getInfo()
    {
        return "RegexFilter: " + Pattern;
    }
}
