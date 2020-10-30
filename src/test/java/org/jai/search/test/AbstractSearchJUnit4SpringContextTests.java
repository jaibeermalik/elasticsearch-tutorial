package org.jai.search.test;

import static org.junit.Assert.assertTrue;

import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.Requests;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.jai.search.client.SearchClientService;
import org.jai.search.data.SampleDataGenerator;
import org.jai.search.index.IndexProductData;
import org.jai.search.model.ElasticSearchIndexConfig;
import org.jai.search.query.ProductQueryService;
import org.jai.search.setup.SetupIndexService;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

@ContextConfiguration(locations = {"classpath:applicationContext-elasticsearch.xml"})
public abstract class AbstractSearchJUnit4SpringContextTests extends AbstractJUnit4SpringContextTests
{
    @Autowired
    @Qualifier("searchClientService")
    protected SearchClientService searchClientService;
    @Autowired 
    protected SetupIndexService setupIndexService;
    @Autowired 
    protected SampleDataGenerator sampleDataGenerator;
    @Autowired 
    protected ProductQueryService productQueryService;
    @Autowired 
    protected IndexProductData indexProductData;
    
    protected Client getClient()
     {
         return searchClientService.getClient();
     }
     
     @Before
     public void prepare()
     {
         setupIndexService.setupAllIndices(false);
         
         searchClientService.getClient().admin().indices().refresh(Requests.refreshRequest()).actionGet();

         System.out.println("yes, test setup indexing preparation done!");
     }
     
     protected void refreshSearchServer()
     {
         searchClientService.getClient().admin().indices().refresh(Requests.refreshRequest()).actionGet();
     }
     
     protected void checkIndexHealthStatus(String indexName)
     {
         ClusterHealthRequest request = new ClusterHealthRequest(indexName);
         ClusterHealthStatus clusterHealthStatus = searchClientService.getClient().admin().cluster().health(request).actionGet().getStatus();

         assertTrue(clusterHealthStatus.equals(ClusterHealthStatus.GREEN));
     }
     
     protected long getIndexTotalDocumentCount(ElasticSearchIndexConfig elasticSearchIndexConfig)
     {
         long count = searchClientService.getClient().prepareSearch(elasticSearchIndexConfig.getIndexAliasName())
                                                     .setTypes(elasticSearchIndexConfig.getDocumentType())
                                                     .setSource(new SearchSourceBuilder()
                                                     .size(0))
                                                     .get().getHits().getTotalHits()
                                                     ;
         
         return count;
     }
     
 }
