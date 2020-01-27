package de.d3v0.peb.common.IOProperties;


public abstract class IOHandlerProperties
{
    public String BasePath;
    public int WorkerThreadCount;
    public abstract String getHandlerClassname();

    public IOHandlerProperties()
    {
        WorkerThreadCount = 2;
    }
}
