package org.jai.search.parts.one;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.elasticsearch.action.admin.cluster.node.info.NodesInfoRequest;
import org.elasticsearch.client.Client;
import org.jai.search.model.ElasticSearchIndexConfig;
import org.jai.search.model.Product;
import org.jai.search.test.AbstractSearchJUnit4SpringContextTests;
import org.junit.Ignore;
import org.junit.Test;

public class ElasticSearchPart1Test extends AbstractSearchJUnit4SpringContextTests
{
    @Test
    public void testGetClient()
    {
        Client client = searchClientService.getClient();
        assertNotNull(client);
    }
    
    @Test
    public void setupAllIndices()
    {
        //no parent stuff
        setupIndexService.setupAllIndices(false);
        
        //no child stuff
//        setupIndexService.setupAllIndices(true);
    }
    
    @Test
    public void indexHandling()
    {
        ElasticSearchIndexConfig config = ElasticSearchIndexConfig.NL_WEBSITE;
        setupIndexService.deleteIndex(config.getIndexAliasName());
        
        //create new index
        setupIndexService.createIndex(config);
        
        //check index exists
        assertTrue(setupIndexService.isIndexExists(config.getIndexAliasName()));
        
        //delete Index
        assertTrue(setupIndexService.deleteIndex(config.getIndexAliasName()));
    }
    
    @Test
    @Ignore
    public void indexNodesHandling()
    {
        //create new index
        String nodeName = "server3";
        searchClientService.addNewNode(nodeName);
        
        //Get total nodes
        try
        {
            NodesInfoRequest request = new NodesInfoRequest();
            request.nodesIds();
            assertEquals(3, searchClientService.getClient().admin().cluster().nodesInfo(request).get().getNodes().size());
            
            searchClientService.removeNode(nodeName);
            
            assertEquals(2, searchClientService.getClient().admin().cluster().nodesInfo(request).get().getNodes().size());
        } catch (InterruptedException e)
        {
                fail();
        } catch (ExecutionException e)
        {
                fail();
        }
    }
    
    @Test
    @Ignore
    public void indexSettingsHandling()
    {
        //Only 
        ElasticSearchIndexConfig config = ElasticSearchIndexConfig.COM_WEBSITE;
        
        assertNull(setupIndexService.getIndexSettings(config, "settingname"));
        String settingToUpdate = "index.number_of_replicas";
        assertEquals("1", setupIndexService.getIndexSettings(config, settingToUpdate));
        
        Map<String, Object> settings = new HashMap<String, Object>();
        settings.put(settingToUpdate, 2);
        //create new index document type
        setupIndexService.updateIndexSettings(config, settings);
        assertEquals("2", setupIndexService.getIndexSettings(config, settingToUpdate));
    }
    
    @Test
    @Ignore
    public void indexDocumentTypeHandling()
    {
        ElasticSearchIndexConfig config = ElasticSearchIndexConfig.COM_WEBSITE;
        
        setupIndexService.deleteIndex(config.getIndexAliasName());
        //have index in place first.
        setupIndexService.createIndex(config);

        //create new index document type
        setupIndexService.updateDocumentTypeMapping(config, "testDocumentType", false);
    }
    
    @Test
    public void indexDocumentHandling()
    {
        ElasticSearchIndexConfig config = ElasticSearchIndexConfig.COM_WEBSITE;
        
        Long productId = 123456l;
        assertFalse(indexProductData.isProductExists(config, productId));
        
        Product product = new Product();
        product.setId(productId);
        product.setTitle("blah blah");
        product.setPrice(BigDecimal.valueOf(5));
        product.setAvailableOn(new Date());
        
        indexProductData.indexProduct(config, product);

        assertTrue(indexProductData.isProductExists(config, productId));
        
        indexProductData.deleteProduct(config, product.getId());
        
        assertFalse(indexProductData.isProductExists(config, productId));
    }
    
    @Test
    public void aliasHandling()
    {
        ElasticSearchIndexConfig config = ElasticSearchIndexConfig.COM_WEBSITE;
        
        assertFalse(setupIndexService.isAliasExists("aliasetc"));
        assertTrue(setupIndexService.isAliasExists(config.getIndexAliasName()));
    }
    
    
}
