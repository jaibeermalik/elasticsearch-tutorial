package org.jai.search.parts.two;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.jai.search.data.SampleDataGenerator;
import org.jai.search.model.ElasticSearchIndexConfig;
import org.jai.search.model.Product;
import org.jai.search.model.ProductSearchResult;
import org.jai.search.model.SearchCriteria;
import org.jai.search.model.SearchFacetName;
import org.jai.search.test.AbstractSearchJUnit4SpringContextTests;
import org.junit.Test;

public class ElasticSearchTutPart2Test extends AbstractSearchJUnit4SpringContextTests
{
    @Test
    public void paginatedDocumentResults()
    {
        ElasticSearchIndexConfig config = ElasticSearchIndexConfig.COM_WEBSITE;
        SearchCriteria searchCriteria = new SearchCriteria();
        searchCriteria.indices(config.getIndexAliasName());
        searchCriteria.documentTypes(config.getDocumentType());
        
        searchCriteria.size(0);
        ProductSearchResult searchProducts = productQueryService.searchProducts(searchCriteria);
        
        assertEquals(50, searchProducts.getTotalCount());
        assertEquals(0, searchProducts.getProducts().size());
    }
    
    @Test
    public void searchInMultipleIndexes()
    {
        ElasticSearchIndexConfig config = ElasticSearchIndexConfig.COM_WEBSITE;
        SearchCriteria searchCriteria = new SearchCriteria();
        searchCriteria.indices(config.getIndexAliasName(), ElasticSearchIndexConfig.NL_WEBSITE.getIndexAliasName());
        searchCriteria.documentTypes(config.getDocumentType());
        
        searchCriteria.size(0);
        ProductSearchResult searchProducts = productQueryService.searchProducts(searchCriteria);
     
        //50 + 50 docs from both indices
        assertEquals(100, searchProducts.getTotalCount());
        assertEquals(0, searchProducts.getProducts().size());
    }
    
    @Test
    public void SearchDocumentReturnedFileds()
    {
        ElasticSearchIndexConfig config = ElasticSearchIndexConfig.COM_WEBSITE;
        SearchCriteria searchCriteria = new SearchCriteria();
        searchCriteria.indices(config.getIndexAliasName());
        searchCriteria.documentTypes(config.getDocumentType());
        
        ProductSearchResult searchProducts = productQueryService.searchProducts(searchCriteria);
     
        //50, returned based on boosting.
        assertEquals(50, searchProducts.getTotalCount());
        assertEquals(10, searchProducts.getProducts().size());
        
        for (Product product : searchProducts.getProducts())
        {
            assertEquals("Title "+product.getId(), product.getTitle());
            assertEquals(product.getId().floatValue(), product.getPrice().floatValue(), 0);
        }
    }
    
    @Test
    public void queryText()
    {
        ElasticSearchIndexConfig config = ElasticSearchIndexConfig.COM_WEBSITE;
        SearchCriteria searchCriteria = new SearchCriteria();
        searchCriteria.indices(config.getIndexAliasName());
        searchCriteria.documentTypes(config.getDocumentType());
        
        searchCriteria.query("query");
        ProductSearchResult searchProducts = productQueryService.searchProducts(searchCriteria);
     
        //50 + 50 docs from both indices
        assertEquals(0, searchProducts.getTotalCount());
        assertEquals(0, searchProducts.getProducts().size());
        
        searchCriteria.query("Title");
        searchProducts = productQueryService.searchProducts(searchCriteria);
     
        //50 + 50 docs from both indices
        assertEquals(50, searchProducts.getTotalCount());
        assertEquals(10, searchProducts.getProducts().size());
        
        searchCriteria.query("tile*");
        searchProducts = productQueryService.searchProducts(searchCriteria);
     
        //0, special characters are escaped out
        assertEquals(0, searchProducts.getTotalCount());
        assertEquals(0, searchProducts.getProducts().size());
    }
    
    @Test
    public void findSimilarProducts()
    {
        ElasticSearchIndexConfig config = ElasticSearchIndexConfig.COM_WEBSITE;
        SearchCriteria searchCriteria = new SearchCriteria();
        searchCriteria.indices(config.getIndexAliasName());
        searchCriteria.documentTypes(config.getDocumentType());
        
        //0-4 products with color red
        Long productId = 0l;
        String[] fields = new String[]{SearchFacetName.CATEGORIES_FIELD_PREFIX + SearchFacetName.SEARCH_FACET_TYPE_COLOR.getFacetFieldNameAtLevel(2) + ".facet"};
        List<Product> similarProducts = productQueryService.findSimilarProducts(config, fields, productId);
        assertEquals(4, similarProducts.size());
        assertTrue(similarProducts.get(0).getId() < 5);
        
        //only Macbook products, 9 excluding current one
        fields = new String[]{SearchFacetName.CATEGORIES_FIELD_PREFIX + SearchFacetName.SEARCH_FACET_TYPE_PRODUCT_TYPE.getFacetFieldNameAtLevel(3) + ".facet"};
        similarProducts = productQueryService.findSimilarProducts(config, fields, productId);
        assertEquals(9, similarProducts.size());
        
        fields = new String[]{SearchFacetName.CATEGORIES_FIELD_PREFIX + SearchFacetName.SEARCH_FACET_TYPE_PRODUCT_TYPE.getFacetFieldNameAtLevel(3) + ".facet",
                SearchFacetName.CATEGORIES_FIELD_PREFIX + SearchFacetName.SEARCH_FACET_TYPE_PRODUCT_TYPE.getFacetFieldNameAtLevel(4) + ".facet"};
        similarProducts = productQueryService.findSimilarProducts(config, fields, productId);
        assertEquals(9, similarProducts.size());
        Product product = productQueryService.getProduct(config, productId);
        
        //the first 4 products on matching two fields, macbook air
        //rest 5 products on matching only one field, macbook
        assertTrue(product.categoryNameExists(SampleDataGenerator.MACBOOK_AIR));
        
        for (int i = 0; i < 9; i++)
        {
            product = productQueryService.getProduct(config, similarProducts.get(i).getId());
            System.out.println(product);
            if(i < 4)
            {
                assertTrue(product.categoryNameExists(SampleDataGenerator.MACBOOK_AIR));    
            }
            else
            {
                assertTrue(product.categoryNameExists(SampleDataGenerator.MACBOOK_PRO));
            }
        }
    }

}
