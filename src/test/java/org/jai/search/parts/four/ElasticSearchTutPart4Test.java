package org.jai.search.parts.four;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.jai.search.data.SampleDataGenerator;
import org.jai.search.model.AutoSuggestionEntry;
import org.jai.search.model.ElasticSearchIndexConfig;
import org.jai.search.model.ProductSearchResult;
import org.jai.search.model.SearchCriteria;
import org.jai.search.model.SearchDocumentFieldName;
import org.jai.search.model.SearchFacetName;
import org.jai.search.test.AbstractSearchJUnit4SpringContextTests;
import org.junit.Test;

public class ElasticSearchTutPart4Test extends AbstractSearchJUnit4SpringContextTests
{
    @Test
    public void facetingOnHierarchicalDataOnProductCategories()
    {
        ElasticSearchIndexConfig config = ElasticSearchIndexConfig.COM_WEBSITE;
        SearchCriteria searchCriteria = new SearchCriteria();
        searchCriteria.indices(config.getIndexAliasName());
        searchCriteria.documentTypes(config.getDocumentType());
        
        for (SearchFacetName facet : SearchFacetName.categoryFacets)
        {
            searchCriteria.facets(facet.getFacetFieldNameAtLevel(2));
        }
        searchCriteria.facets(SearchFacetName.PRODUCT_PRICE_RANGE.getCode());
        
        ProductSearchResult searchProducts = productQueryService.searchProducts(searchCriteria);
        
        assertEquals(50, searchProducts.getTotalCount());
        assertEquals(10, searchProducts.getProducts().size());
        assertEquals(5, searchProducts.getFacets().size());
        
        searchCriteria.addSingleSelectFilter(SearchFacetName.SEARCH_FACET_TYPE_PRODUCT_TYPE.getFacetFieldNameAtLevel(3), SampleDataGenerator.MACBOOK);
        searchCriteria.size(50);
        
        searchProducts = productQueryService.searchProducts(searchCriteria);
        
        assertEquals(10, searchProducts.getTotalCount());
        assertEquals(10, searchProducts.getProducts().size());
        assertEquals(5, searchProducts.getFacets().size());
        
        searchCriteria.getSingleSelectFilters().clear();
        searchCriteria.addMultiSelectFilter(SearchFacetName.SEARCH_FACET_TYPE_COLOR.getFacetFieldNameAtLevel(2), SampleDataGenerator.RED);
        searchCriteria.addMultiSelectFilter(SearchFacetName.SEARCH_FACET_TYPE_COLOR.getFacetFieldNameAtLevel(2), SampleDataGenerator.BLUE);
        
        searchProducts = productQueryService.searchProducts(searchCriteria);
        
        assertEquals(10, searchProducts.getTotalCount());
        assertEquals(10, searchProducts.getProducts().size());
        assertEquals(5, searchProducts.getFacets().size());
        
        searchCriteria.getMultiSelectFilters().clear();
        searchCriteria.addSingleSelectFilter(SearchFacetName.PRODUCT_PRICE_RANGE.getCode(), "0 - 10");
        
        searchProducts = productQueryService.searchProducts(searchCriteria);
        
        assertEquals(10, searchProducts.getTotalCount());
        assertEquals(10, searchProducts.getProducts().size());
        assertEquals(5, searchProducts.getFacets().size());
        
        searchCriteria.getSingleSelectFilters().clear();
        searchCriteria.addFiledValueFilter(SearchDocumentFieldName.SOLD_OUT.getFieldName(), true);
        searchCriteria.size(10);
        searchProducts = productQueryService.searchProducts(searchCriteria);
        
        assertEquals(25, searchProducts.getTotalCount());
        assertEquals(10, searchProducts.getProducts().size());
        assertEquals(5, searchProducts.getFacets().size());
        
        
        searchCriteria.getFieldValueFilters().clear();
        searchCriteria.query("title");
        searchProducts = productQueryService.searchProducts(searchCriteria);
        
        assertEquals(50, searchProducts.getTotalCount());
        assertEquals(10, searchProducts.getProducts().size());
        assertEquals(5, searchProducts.getFacets().size());
    }
    
    @Test
    public void facetingOnNestedObjectsOnProductSpecificationsResolutionAndMemory()
    {
        ElasticSearchIndexConfig config = ElasticSearchIndexConfig.COM_WEBSITE;
        SearchCriteria searchCriteria = new SearchCriteria();
        searchCriteria.indices(config.getIndexAliasName());
        searchCriteria.documentTypes(config.getDocumentType());
        
        searchCriteria.facets(SearchFacetName.SPECIFICATION_RESOLUTION.getCode());
        searchCriteria.facets(SearchFacetName.SPECIFICATION_MEMORY.getCode());
        ProductSearchResult searchProducts = productQueryService.searchProducts(searchCriteria);
        
        assertEquals(50, searchProducts.getTotalCount());
        assertEquals(10, searchProducts.getProducts().size());
        assertEquals(2, searchProducts.getFacets().size());
        
        //Only 5 docs with 3200 x 1800
        searchCriteria.addMultiSelectFilter(SearchFacetName.SPECIFICATION_RESOLUTION.getCode(), SampleDataGenerator.RESOLUTON_3200_1800);
        searchProducts = productQueryService.searchProducts(searchCriteria);
        
        assertEquals(5, searchProducts.getTotalCount());
        assertEquals(5, searchProducts.getProducts().size());
        assertEquals(2, searchProducts.getFacets().size());
        
        //Only 5 docs with and 8 GB
        searchCriteria.getMultiSelectFilters().clear();
        searchCriteria.addMultiSelectFilter(SearchFacetName.SPECIFICATION_MEMORY.getCode(), SampleDataGenerator.MEMORY_8_GB);
        searchProducts = productQueryService.searchProducts(searchCriteria);
        
        assertEquals(5, searchProducts.getTotalCount());
        assertEquals(5, searchProducts.getProducts().size());
        assertEquals(2, searchProducts.getFacets().size());
        
        
        //Only 5 docs with both
        searchCriteria.getMultiSelectFilters().clear();
        searchCriteria.addMultiSelectFilter(SearchFacetName.SPECIFICATION_RESOLUTION.getCode(), SampleDataGenerator.RESOLUTON_3200_1800);
        searchCriteria.addMultiSelectFilter(SearchFacetName.SPECIFICATION_MEMORY.getCode(), SampleDataGenerator.MEMORY_8_GB);
        searchProducts = productQueryService.searchProducts(searchCriteria);
        
        assertEquals(5, searchProducts.getTotalCount());
        assertEquals(5, searchProducts.getProducts().size());
        assertEquals(2, searchProducts.getFacets().size());
        
        //no docs for below combination
        searchCriteria.getMultiSelectFilters().clear();
        searchCriteria.addMultiSelectFilter(SearchFacetName.SPECIFICATION_RESOLUTION.getCode(), SampleDataGenerator.RESOLUTON_3200_1800);
        searchCriteria.addMultiSelectFilter(SearchFacetName.SPECIFICATION_MEMORY.getCode(), SampleDataGenerator.MEMORY_6_GB);
        searchProducts = productQueryService.searchProducts(searchCriteria);
        
        assertEquals(0, searchProducts.getTotalCount());
        assertEquals(0, searchProducts.getProducts().size());
        assertEquals(0, searchProducts.getFacets().size());
        
        //10 docs for 6 gb and 8 gb
        searchCriteria.getMultiSelectFilters().clear();
        searchCriteria.addMultiSelectFilter(SearchFacetName.SPECIFICATION_MEMORY.getCode(), SampleDataGenerator.MEMORY_8_GB);
        searchCriteria.addMultiSelectFilter(SearchFacetName.SPECIFICATION_MEMORY.getCode(), SampleDataGenerator.MEMORY_6_GB);
        searchProducts = productQueryService.searchProducts(searchCriteria);
        
        assertEquals(10, searchProducts.getTotalCount());
        assertEquals(10, searchProducts.getProducts().size());
        assertEquals(2, searchProducts.getFacets().size());
    }
    
    @Test
    public void autoSuggestionsOnTerms()
    {
        ElasticSearchIndexConfig config = ElasticSearchIndexConfig.COM_WEBSITE;
        
        List<AutoSuggestionEntry> autoSuggestions = productQueryService.getAutoSuggestions(config, "keyword");
        assertTrue(autoSuggestions.size() > 0);
        assertEquals(10, autoSuggestions.size());
        
        autoSuggestions = productQueryService.getAutoSuggestions(config, "keyword 4");
        assertTrue(autoSuggestions.size() > 0);
        assertEquals(20, autoSuggestions.size());
        
//        autoSuggestions = productQueryService.getAutoSuggestions(config, "Mac");
//        assertTrue(autoSuggestions.size() == 0);
//        assertEquals(0, autoSuggestions.size());
//        
//        Product product1 = productQueryService.getProduct(config, 1l);
//        product1.getKeywords().clear();
//        product1.addKeyword("Macbook Pro");
//        indexProductData.indexOrUpdateProduct(config, product1);
//        
//        Product product2 = productQueryService.getProduct(config, 2l);
//        product2.getKeywords().clear();
//        product2.addKeyword("Macbook Air");
//        indexProductData.indexOrUpdateProduct(config, product2);
//        
//        refreshSearchServer();
//        
//        autoSuggestions = productQueryService.getAutoSuggestions(config, "Macbook");
//        assertTrue(autoSuggestions.size() > 0);
//        assertEquals(2, autoSuggestions.size());
    }
    
    @Test
    public void autoSuggestionsUsingTermsFacet()
    {
        ElasticSearchIndexConfig config = ElasticSearchIndexConfig.COM_WEBSITE;
        List<AutoSuggestionEntry> autoSuggestions = productQueryService.getAutoSuggestionsUsingTermsFacet(config, "keyword");
        
        assertTrue(autoSuggestions.size() > 0);
        assertEquals(20, autoSuggestions.size());
        
        //Only 11 keywords 1,10...19
        autoSuggestions = productQueryService.getAutoSuggestionsUsingTermsFacet(config, "keyword 1");
        
        assertTrue(autoSuggestions.size() > 0);
        assertEquals(11, autoSuggestions.size());
        
        //Only 1 keyword0
        autoSuggestions = productQueryService.getAutoSuggestionsUsingTermsFacet(config, "keyword 0");
        
        assertTrue(autoSuggestions.size() > 0);
        assertEquals(1, autoSuggestions.size());
        
        //Only 10 docs, with term "Macbook:10, Macbook Pro:5 , Macbook Air:5
        autoSuggestions = productQueryService.getAutoSuggestionsUsingTermsFacet(config, "Macbook");
        
        assertTrue(autoSuggestions.size() > 0);
        assertEquals(3, autoSuggestions.size());
        assertEquals(10, autoSuggestions.get(0).getCount());
        
        //Only 5 docs, with term Macbook Air
        autoSuggestions = productQueryService.getAutoSuggestionsUsingTermsFacet(config, "Macbook Pr");
        
        assertTrue(autoSuggestions.size() > 0);
        assertEquals(1, autoSuggestions.size());
        assertEquals(5, autoSuggestions.get(0).getCount());
    }

}
