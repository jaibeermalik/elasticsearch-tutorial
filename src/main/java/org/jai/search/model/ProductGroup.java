package org.jai.search.model;

import java.util.ArrayList;

import org.apache.commons.lang.builder.ToStringBuilder;

public class ProductGroup
{
    private Long id;
    
    private String groupTitle;
    
    private String groupDescription;
    
    private ArrayList<Product>  products = new ArrayList<Product>();

    public ArrayList<Product> getProducts()
    {
        return products;
    }

    public void addProduct(Product product)
    {
        products.add(product);
    }

    public String getGroupDescription()
    {
        return groupDescription;
    }

    public void setGroupDescription(String groupDescription)
    {
        this.groupDescription = groupDescription;
    }

    public String getGroupTitle()
    {
        return groupTitle;
    }

    public void setGroupTitle(String groupTitle)
    {
        this.groupTitle = groupTitle;
    }

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }
    
    @Override
    public String toString()
    {
        return new ToStringBuilder(this)
        .append(id)
        .append(groupTitle)
        .append(groupDescription)
        .append(products)
        .toString();
    }
    
}
