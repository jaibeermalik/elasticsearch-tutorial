package org.jai.search.model;

public class FacetResultEntry
{
    private String term;
    
    private long count;
    
    public long getCount()
    {
        return count;
    }

    public void setCount(long count)
    {
        this.count = count;
    }

    public String getTerm()
    {
        return term;
    }

    public void setTerm(String term)
    {
        this.term = term;
    }

}
