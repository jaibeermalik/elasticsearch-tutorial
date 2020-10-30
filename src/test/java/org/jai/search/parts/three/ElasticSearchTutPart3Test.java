package org.jai.search.parts.three;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.jai.search.model.ElasticSearchIndexConfig;
import org.jai.search.model.Product;
import org.jai.search.model.ProductSearchResult;
import org.jai.search.model.SearchCriteria;
import org.jai.search.test.AbstractSearchJUnit4SpringContextTests;
import org.junit.Test;

public class ElasticSearchTutPart3Test extends AbstractSearchJUnit4SpringContextTests
{
    @Test
    public void boostingDocumentsUsingFunctionScoreAndScriptScore()
    {
        ElasticSearchIndexConfig config = ElasticSearchIndexConfig.COM_WEBSITE;
        SearchCriteria searchCriteria = new SearchCriteria();
        searchCriteria.indices(config.getIndexAliasName());
        searchCriteria.documentTypes(config.getDocumentType());
        searchCriteria.useBoostingFactor(true);
        
        ProductSearchResult searchProducts = productQueryService.searchProducts(searchCriteria);
        Long currentTopProductId = searchProducts.getProducts().get(0).getId();
        
        assertEquals(50, searchProducts.getTotalCount());
        assertEquals(10, searchProducts.getProducts().size());
        assertEquals(49, currentTopProductId.intValue());
        assertEquals(currentTopProductId/10000f, productQueryService.getProduct(config, currentTopProductId).getBoostFactor(), 0);
        
        //Change boosting factor of one product and see it is at top
        long newTopProductId = 23l;
        Product product = productQueryService.getProduct(config, newTopProductId);
        product.getCategories().clear();
        assertEquals(newTopProductId/10000f, product.getBoostFactor(), 0);
        
        product.setBoostFactor(60f);
        indexProductData.indexProduct(config, product);
        
        refreshSearchServer();
        
        searchProducts = productQueryService.searchProducts(searchCriteria);
        currentTopProductId = searchProducts.getProducts().get(0).getId();
        
        assertEquals(50, searchProducts.getTotalCount());
        assertEquals(10, searchProducts.getProducts().size());
        assertEquals(newTopProductId, currentTopProductId.intValue());
        assertEquals(60f, productQueryService.getProduct(config, currentTopProductId).getBoostFactor(), 0);
    }
    
    @Test
    public void boostingDocumentsUsingRescoreOnSoldOut()
    {
        ElasticSearchIndexConfig config = ElasticSearchIndexConfig.COM_WEBSITE;
        SearchCriteria searchCriteria = new SearchCriteria();
        searchCriteria.indices(config.getIndexAliasName());
        searchCriteria.documentTypes(config.getDocumentType());
        searchCriteria.size(50);
        
        ProductSearchResult searchProducts = productQueryService.searchProducts(searchCriteria);
        assertEquals(50, searchProducts.getTotalCount());
        assertEquals(50, searchProducts.getProducts().size());
        
        //By default, even ids are sold out and odd are soldout=false
        searchCriteria.rescoreOnSoldOut(true);

        searchProducts = productQueryService.searchProducts(searchCriteria);
        assertEquals(50, searchProducts.getTotalCount());
        assertEquals(50, searchProducts.getProducts().size());
        
        for (int i = 0; i < 50; i++)
        {
            if (i < 25)
            {
                //not sold out, still available
                assertFalse(searchProducts.getProducts().get(i).isSoldOut());
            }
            else
            {
                //product sold out
                assertTrue(searchProducts.getProducts().get(i).isSoldOut());
            }
        }
    }
    
    @Test
    public void influencingScoreForDocumentsUsingFieldsWeight()
    {
        ElasticSearchIndexConfig config = ElasticSearchIndexConfig.COM_WEBSITE;
        SearchCriteria searchCriteria = new SearchCriteria();
        searchCriteria.indices(config.getIndexAliasName());
        searchCriteria.documentTypes(config.getDocumentType());
        searchCriteria.useBoostingFactor(true);
        searchCriteria.size(50);
        
        ProductSearchResult searchProducts = productQueryService.searchProducts(searchCriteria);
        
        assertEquals(50, searchProducts.getTotalCount());
        assertEquals(50, searchProducts.getProducts().size());
        assertEquals(49, searchProducts.getProducts().get(0).getId().intValue());
        assertEquals(0, searchProducts.getProducts().get(49).getId().intValue());

        int j=0;
        for (Product product : searchProducts.getProducts())
        {
            Product productDoc = productQueryService.getProduct(config, product.getId());
            //Clear out categories, as parent was not set properly..will cause issues in reindexing...ignoring as temp data.
            productDoc.getCategories().clear();
            //set description in reverse order.
            productDoc.setDescription("Description " + j);
            indexProductData.indexProduct(config, productDoc);
            j++;
        }
        
        refreshSearchServer();
        
        //query for string 10.
        //title field weight TITLE:  (float) 0.5) DESCRIPTION : (float) 0.15)
        searchCriteria.query("10");
        searchProducts = productQueryService.searchProducts(searchCriteria);
        
        assertEquals(2, searchProducts.getTotalCount());
        assertEquals(2, searchProducts.getProducts().size());
        assertEquals(10, searchProducts.getProducts().get(0).getId().intValue());
        assertEquals("Title 10", searchProducts.getProducts().get(0).getTitle());
        assertEquals("Description 10", productQueryService.getProduct(config, searchProducts.getProducts().get(1).getId()).getDescription());
    }
    
    @Test
    public void queryDocumentsBasedOnSynonyms()
    {
        ElasticSearchIndexConfig config = ElasticSearchIndexConfig.COM_WEBSITE;
        SearchCriteria searchCriteria = new SearchCriteria();
        searchCriteria.indices(config.getIndexAliasName());
        searchCriteria.documentTypes(config.getDocumentType());
        
        searchCriteria.query("query");
        Product product = productQueryService.getProduct(config, 1l);
        product.getCategories().clear();
        ProductSearchResult searchProducts = productQueryService.searchProducts(searchCriteria);
        
        assertEquals(0, searchProducts.getTotalCount());
        assertEquals(0, searchProducts.getProducts().size());
        String description = product.getDescription();
        assertEquals("Description"+ product.getId(), description);
        
        //Update synonyms search, find => query
        description = description + " search";
        product.setDescription(description);
        if(product.getKeywords() == null) product.setKeywords(new ArrayList<>());
		product.getKeywords().add("pkey");
        indexProductData.indexProduct(config, product);
        
        refreshSearchServer();
        
        //Expected to find as it is in text of description field.
        searchCriteria.query("search");
        searchProducts = productQueryService.searchProducts(searchCriteria);
        
        assertEquals(1, searchProducts.getTotalCount());
        assertEquals(1, searchProducts.getProducts().size());
        
        searchCriteria.query("find");
        searchProducts = productQueryService.searchProducts(searchCriteria);
        
        assertEquals(1, searchProducts.getTotalCount());
        assertEquals(1, searchProducts.getProducts().size());
        
        searchCriteria.query("query");
        searchProducts = productQueryService.searchProducts(searchCriteria);
        
        assertEquals(1, searchProducts.getTotalCount());
        assertEquals(1, searchProducts.getProducts().size());
    }
    
    @Test
    public void configureDataForStopwords()
    {
        ElasticSearchIndexConfig config = ElasticSearchIndexConfig.COM_WEBSITE;
        SearchCriteria searchCriteria = new SearchCriteria();
        searchCriteria.indices(config.getIndexAliasName());
        searchCriteria.documentTypes(config.getDocumentType());
        
        
        
        String stopword = "however";
        searchCriteria.query(stopword);
        Product product = productQueryService.getProduct(config, 1l);
        product.getCategories().clear();
        ProductSearchResult searchProducts = productQueryService.searchProducts(searchCriteria);
        
        assertEquals(0, searchProducts.getTotalCount());
        assertEquals(0, searchProducts.getProducts().size());
        String description = product.getDescription();
        assertEquals("Description"+ product.getId(), description);
        
        
        
        //Update stop_en_EN however
        description = description + " " + stopword;
        product.setDescription(description);
		if(product.getKeywords() == null) product.setKeywords(new ArrayList<>());
		product.getKeywords().add("pkey");
        indexProductData.indexProduct(config, product);
        
        refreshSearchServer();
        
        //Expected to find but is not allowed in search
        searchCriteria.query(stopword);
        searchProducts = productQueryService.searchProducts(searchCriteria);
        
        assertEquals(0, searchProducts.getTotalCount());
        assertEquals(0, searchProducts.getProducts().size());
    }
    
    @Test
    public void configureDataForAvoidingQueryOnHTMLTags()
    {
        ElasticSearchIndexConfig config = ElasticSearchIndexConfig.COM_WEBSITE;
        SearchCriteria searchCriteria = new SearchCriteria();
        searchCriteria.indices(config.getIndexAliasName());
        searchCriteria.documentTypes(config.getDocumentType());
        
        String htmlTag = "br";
        searchCriteria.query(htmlTag);
        Product product = productQueryService.getProduct(config, 1l);
        product.getCategories().clear();
        ProductSearchResult searchProducts = productQueryService.searchProducts(searchCriteria);
        
        assertEquals(0, searchProducts.getTotalCount());
        assertEquals(0, searchProducts.getProducts().size());
        
        String description = product.getDescription();
        assertEquals("Description"+ product.getId(), description);
        
        //Update html content
        description = description + " " + "<div><p>This contains html content</p><div><br/>";
        product.setDescription(description);
        if(product.getKeywords() == null) product.setKeywords(new ArrayList<>());
		product.getKeywords().add("pkey");
        indexProductData.indexProduct(config, product);
        
        refreshSearchServer();
        
        //Expected to find with "br" search but is not allowed in indexing
        searchCriteria.query(htmlTag);
        searchProducts = productQueryService.searchProducts(searchCriteria);
        
        assertEquals(0, searchProducts.getTotalCount());
        assertEquals(0, searchProducts.getProducts().size());
    }
    
    @Test
    public void analyzeTextForSubWordsUsingWordDelimiter()
    {
        ElasticSearchIndexConfig config = ElasticSearchIndexConfig.COM_WEBSITE;
        
        //standard analyzer: lowercase, stop words
        String synonymText = "find"; //synonym: query, snowball stemmer: queri
        String stopWord = "for";
        String wordDelimiter = "$5.9";
        String lowerCase = "Price";
        String protectedWord = "J2EE"; //check protectedwords_en_EN file
        String text = "title01" + " " + synonymText + " " + stopWord + " " + wordDelimiter + " " + lowerCase + " " + protectedWord;
        
        List<String> analyzeText = setupIndexService.analyzeText(config.getIndexAliasName(), config.getStandardTextAnalyzerName(), null, text);
        System.out.println(analyzeText);
        //[title01, queri, 5.9, price, j2ee]
        assertEquals(5, analyzeText.size());
        assertEquals("title01", analyzeText.get(0));
        assertEquals("queri", analyzeText.get(1));
        assertEquals("5.9", analyzeText.get(2));
        assertEquals(lowerCase.toLowerCase(), analyzeText.get(3));
        assertEquals(protectedWord.toLowerCase(), analyzeText.get(4));

        //Free text custom analyzer using custom word delimiter
        analyzeText = setupIndexService.analyzeText(config.getIndexAliasName(), config.getCustomFreeTextAnalyzerName(), null, text);

        System.out.println(analyzeText);
        //[title01, titl, 01, queri, $5.9, price, j2ee]
        assertEquals(8, analyzeText.size());
        assertEquals("title01", analyzeText.get(0));
        assertEquals("titl", analyzeText.get(1)); 
        assertEquals("01", analyzeText.get(2));
        assertEquals("queri", analyzeText.get(3)); //synonym and snowball stemmer
        assertEquals("$5.9", analyzeText.get(4)); //keeping original value for word delimiter also.
        assertEquals("5.9", analyzeText.get(5)); //check type_table, not treating $ as digit.
        assertEquals("price", analyzeText.get(6));
        assertEquals("j2ee", analyzeText.get(7)); //protected word
    }

    @Test
    public void analyzeTextForStopWords()
    {
        ElasticSearchIndexConfig config = ElasticSearchIndexConfig.COM_WEBSITE;
        
        String text = "Search a stopmefromit";
        
        //Using token filter = escape stopmefromit as in stop_en_EN file
        List<String> analyzeText = setupIndexService.analyzeText(config.getIndexAliasName(), config.getStandardTextAnalyzerName(), null, text);

        //[search->synonym query->queri]
        assertEquals(1, analyzeText.size());
        assertEquals("queri", analyzeText.get(0));
    }
    
    @Test
    public void analyzeTextForSynonyms()
    {
        ElasticSearchIndexConfig config = ElasticSearchIndexConfig.COM_WEBSITE;
        
        //search, find => query in synonyms_en_EN file
        String text = "Find a search query ";
        
        List<String> analyzeText = setupIndexService.analyzeText(config.getIndexAliasName(), config.getStandardTextAnalyzerName(), null, text);

        System.out.println(analyzeText);
        //search, find => query  is turned into [query, query, query] -> [queri, queri, queri]
        assertEquals(3, analyzeText.size());
        assertEquals("queri", analyzeText.get(0));
    }
    
    @Test
    public void analyzeTextForHTMLTags()
    {
        ElasticSearchIndexConfig config = ElasticSearchIndexConfig.COM_WEBSITE;
        
        String htmlTag = "<b>test</b><br/>";

        //using standard analyzer
        List<String> analyzeText = setupIndexService.analyzeText(config.getIndexAliasName(), config.getStandardTextAnalyzerName(), null, htmlTag);
        assertEquals(4, analyzeText.size());
        assertEquals("b", analyzeText.get(0));
        assertEquals("test", analyzeText.get(1));
        assertEquals("b", analyzeText.get(2));
        assertEquals("br", analyzeText.get(3));
        
        //using custom analyzer using HTML CHAR filter
        analyzeText = setupIndexService.analyzeText(config.getIndexAliasName(), config.getCustomFreeTextAnalyzerName(), null, htmlTag);
        assertEquals(1, analyzeText.size());
        assertEquals("test", analyzeText.get(0));
    }

}
