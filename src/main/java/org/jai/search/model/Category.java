package org.jai.search.model;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;


public class Category 
{
    private String name;
    private String type;
    
    private Category parentCategory;
    
    public Category(String catName, Category parent, String catType)
    {
        this.name = catName;
        this.parentCategory = parent;
        this.type = catType;
    }
    
    public String getName()
    {
        return name;
    }
    
    public Category getParentCategory()
    {
        return parentCategory;
    }
    
    public String getType()
    {
        return type;
    }
    
    @Override
    public int hashCode()
    {
        return new HashCodeBuilder().append(name).hashCode();
        
    }

    @Override
    public boolean equals(Object obj)
    {
        Category other = (Category) obj;
        return new EqualsBuilder().append(name, other.getName()).isEquals();
    }
    
    @Override
    public String toString()
    {
        return new ToStringBuilder(this)
        .append(name)
        .append(type)
        .toString();
    }
    
}
