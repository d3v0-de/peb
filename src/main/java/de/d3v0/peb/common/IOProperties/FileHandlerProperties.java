package de.d3v0.peb.common.IOProperties;

import de.d3v0.peb.controller.IO.FileHandler;

public class FileHandlerProperties extends IOHandlerProperties
{
    public String getHandlerClassname()
    {
        return FileHandler.class.getCanonicalName();
    }

    public FileHandlerProperties()
    {
        WorkerThreadCount = 2;
    }
}
