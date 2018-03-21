package de.d3v0.peb.common.targetproperties;


public abstract class TargetProperties
{
    public String BasePath;
    public int WorkerThreadCount;
    public abstract String getHandlerClassname();

    public TargetProperties()
    {
        WorkerThreadCount = 2;
    }
}
