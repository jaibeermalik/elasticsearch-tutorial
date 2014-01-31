package org.jai.search.model;

public class Specification
{
    private String resolution;
    
    private String memory;

    public Specification(String resoluton, String memory)
    {
        this.resolution = resoluton;
        this.memory = memory;
    }

    public String getResolution()
    {
        return resolution;
    }

    public void setResolution(String resolution)
    {
        this.resolution = resolution;
    }

    public String getMemory()
    {
        return memory;
    }

    public void setMemory(String memory)
    {
        this.memory = memory;
    }
}
