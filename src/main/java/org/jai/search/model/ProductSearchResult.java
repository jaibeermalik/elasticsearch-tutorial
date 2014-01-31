package org.jai.search.model;

import java.util.ArrayList;
import java.util.List;

public class ProductSearchResult
{
    private long totalCount;
    
    private List<Product> products = new ArrayList<Product>();
    
    private List<FacetResult> facets = new ArrayList<FacetResult>();

    public long getTotalCount()
    {
        return totalCount;
    }

    public void setTotalCount(long totalCount)
    {
        this.totalCount = totalCount;
    }

    public List<Product> getProducts()
    {
        return products;
    }

    public void setProducts(List<Product> products)
    {
        this.products = products;
    }

    public void addProduct(Product product)
    {
        products.add(product);
    }

    public List<FacetResult> getFacets()
    {
        return facets;
    }

    public void addFacet(FacetResult facet)
    {
        facets.add(facet);
    }
}
