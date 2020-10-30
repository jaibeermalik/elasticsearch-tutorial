package org.jai.search.parts.five;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.jai.search.data.SampleDataGenerator;
import org.jai.search.model.ElasticSearchIndexConfig;
import org.jai.search.model.ProductProperty;
import org.jai.search.model.ProductSearchResult;
import org.jai.search.model.SearchCriteria;
import org.jai.search.model.Specification;
import org.jai.search.test.AbstractSearchJUnit4SpringContextTests;
import org.junit.Ignore;
import org.junit.Test;

public class ElasticSearchTutPart5Test extends AbstractSearchJUnit4SpringContextTests
{
    @Test
    @Ignore
    //TODO: parent-child mappings needs to be created together, fix it.
    public void filteringOnChildDocumentsOnProductProperties()
    {
        //set up parent child relationship docs
        setupIndexService.setupAllIndices(true);
        
        refreshSearchServer();
        
        ElasticSearchIndexConfig config = ElasticSearchIndexConfig.COM_WEBSITE;
        
        SearchCriteria searchCriteria = new SearchCriteria();
        searchCriteria.indices(config.getIndexAliasName());
        searchCriteria.documentTypes(config.getDocumentType());
        
        //search for the products for productproperties
        ProductProperty findProductProperty = sampleDataGenerator.findProductProperty(SampleDataGenerator.PRODUCTPROPERTY_SIZE_21_INCH, SampleDataGenerator.PRODUCTPROPERTY_COLOR_BROWN);
        assertNotNull(findProductProperty);
        searchCriteria.addProductProperty(findProductProperty);
        ProductSearchResult searchProducts = productQueryService.searchProducts(searchCriteria);
        
        assertEquals(1, searchProducts.getTotalCount());
        assertEquals(1, searchProducts.getProducts().size());
    }
    
    @Test
    public void filteringOnNestedDocumentsOnProductsSpecifications()
    {
        //set up parent child relationship docs
        setupIndexService.setupAllIndices(false);
        
        refreshSearchServer();
        
        ElasticSearchIndexConfig config = ElasticSearchIndexConfig.COM_WEBSITE;
        
        SearchCriteria searchCriteria = new SearchCriteria();
        searchCriteria.indices(config.getIndexAliasName());
        searchCriteria.documentTypes(config.getDocumentType());
        
        //search for the products for productproperties
        //5 MACBOOK_AIR
        searchCriteria.addSpecifications(new Specification(SampleDataGenerator.RESOLUTON_3200_1800, SampleDataGenerator.MEMORY_8_GB));
        ProductSearchResult searchProducts = productQueryService.searchProducts(searchCriteria);
        
        assertEquals(5, searchProducts.getTotalCount());
        assertEquals(5, searchProducts.getProducts().size());
        //5 MACBOOK_PRO        
        searchCriteria.addSpecifications(new Specification(SampleDataGenerator.RESOLUTON_1920_1200, SampleDataGenerator.MEMORY_6_GB));
        searchProducts = productQueryService.searchProducts(searchCriteria);
        
        assertEquals(10, searchProducts.getTotalCount());
        assertEquals(10, searchProducts.getProducts().size());
        
        searchCriteria.getSpecifications().clear();
        //None for cross combination
        searchCriteria.addSpecifications(new Specification(SampleDataGenerator.RESOLUTON_1920_1200, SampleDataGenerator.MEMORY_8_GB));
        searchProducts = productQueryService.searchProducts(searchCriteria);
        
        assertEquals(0, searchProducts.getTotalCount());
        assertEquals(0, searchProducts.getProducts().size());
    }

}
