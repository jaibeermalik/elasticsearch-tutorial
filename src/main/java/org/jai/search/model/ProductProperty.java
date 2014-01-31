package org.jai.search.model;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

public class ProductProperty
{
    private Long id;
    
    private String size;
    
    private String color;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public String getColor()
    {
        return color;
    }

    public void setColor(String color)
    {
        this.color = color;
    }

    public String getSize()
    {
        return size;
    }

    public void setSize(String size)
    {
        this.size = size;
    }
    
    @Override
    public int hashCode()
    {
        return new HashCodeBuilder().append(size).append(color).hashCode();
    }
    
    @Override
    public boolean equals(Object obj)
    {
        ProductProperty other = (ProductProperty) obj;
        return new EqualsBuilder().append(size, other.size).append(color, other.color).isEquals();
    }
    
    @Override
    public String toString()
    {
        return new ToStringBuilder(this).append(id).append(size).append(color).toString();
    }
}
