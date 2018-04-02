package de.d3v0.peb.common.misc;

public class MyBoolean
{
    public boolean Value;

    public MyBoolean (boolean value)
    {
        Value = value;
    }

    @Override
    public String toString()
    {
        return Value ? "true" : "false";
    }
}
