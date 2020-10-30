package org.jai.search.setup.impl;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.elasticsearch.action.admin.cluster.state.ClusterStateResponse;
import org.elasticsearch.action.admin.indices.analyze.AnalyzeRequestBuilder;
import org.elasticsearch.action.admin.indices.analyze.AnalyzeResponse;
import org.elasticsearch.action.admin.indices.analyze.AnalyzeResponse.AnalyzeToken;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.cluster.routing.IndexRoutingTable;
import org.elasticsearch.common.xcontent.ToXContent;
import org.jai.search.client.SearchClientService;
import org.jai.search.data.SampleDataGenerator;
import org.jai.search.index.IndexProductData;
import org.jai.search.model.ElasticSearchIndexConfig;
import org.jai.search.model.ProductGroup;
import org.jai.search.setup.IndexSchemaBuilder;
import org.jai.search.setup.SetupIndexService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.carrotsearch.hppc.cursors.ObjectObjectCursor;

@Service
public class SetupIndexServiceImpl implements SetupIndexService
{
    private static final Logger logger = LoggerFactory.getLogger(SetupIndexServiceImpl.class);

    @Autowired
    private SearchClientService searchClientService;
    
    @Autowired
    private IndexProductData indexProductData;
    
    @Autowired
    private SampleDataGenerator sampleDataGenerator;
    
    @Override
    public void setupAllIndices(boolean parentRelationship)
    {
        for (ElasticSearchIndexConfig config : ElasticSearchIndexConfig.values())
        {
            recreateIndex(config);
            
            //add mappings
            updateDocumentTypeMapping(config, config.getGroupDocumentType(), parentRelationship);
            updateDocumentTypeMapping(config, config.getDocumentType(), parentRelationship);
            updateDocumentTypeMapping(config, config.getPropertiesDocumentType(), parentRelationship);
            
            //index all data
            indexProductData.indexAllProductGroupData(config, sampleDataGenerator.generateNestedDocumentsSampleData(), parentRelationship);
        }
    }

    private void recreateIndex(ElasticSearchIndexConfig config)
    {
        Date date = new Date();
        String suffixedIndexName = getSuffixedIndexName(config.getIndexAliasName(), date);
        
        if(isIndexExists(config.getIndexAliasName()))
        {
            //drop existing index
            deleteIndex(config.getIndexAliasName());
        }

        //create indices
        createGivenIndex(config, suffixedIndexName);
    }

    @Override
    public void createIndex(ElasticSearchIndexConfig config)
    {
        String suffixedIndexName = getSuffixedIndexName(config.getIndexAliasName(), new Date());
        createGivenIndex(config, suffixedIndexName);
    }

    private void createGivenIndex(ElasticSearchIndexConfig config, String indexName)
    {
        Client client = searchClientService.getClient();
        
        CreateIndexRequestBuilder createIndexRequestBuilder;
        try
        {
            createIndexRequestBuilder = client.admin().indices().prepareCreate(indexName ).setSettings(new IndexSchemaBuilder().getSettingForIndex(config));
        } catch (IOException e)
        {
            throw new RuntimeException("Error occurred while generating settings for index",e);
        }
        //update mapping on server
        createIndexRequestBuilder.execute().actionGet();
        
        createAlias(config.getIndexAliasName(), indexName);
        
        logger.debug("Index {} created! ",indexName);
    }
    
    private void createAlias(String aliasName, String indexName)
    {
        //add new alias
        searchClientService.getClient().admin().indices().prepareAliases().addAlias(indexName, aliasName).get();
        
        //clean up old alias
        cleanupExistingOldIndex(indexName, aliasName);
    }
    
    private void cleanupExistingOldIndex(String newIndex, String aliasName) 
    {
        Set<String> indices = new HashSet<String>();
        ClusterStateResponse stateResponse = searchClientService.getClient().admin().cluster().prepareState().execute().actionGet();
        
        if(stateResponse !=null && stateResponse.getState() !=null)
        {
            for (ObjectObjectCursor<String, IndexRoutingTable> entry : stateResponse.getState().getRoutingTable().getIndicesRouting()) 
            {
                indices.add(entry.key);
            }
        }
        
        for (String indexName : indices) 
        {
            // Don't remove alias to newly created index
            if (indexName.startsWith(aliasName) && !indexName.equals(newIndex)) 
            {
                try 
                {
                    searchClientService.getClient().admin().indices().prepareAliases().removeAlias(indexName, aliasName).execute().actionGet();
                    logger.debug("Alias {} has been removed from old index {}",aliasName, indexName);
                } 
                catch (Exception ex) 
                {
                    logger.error("Error occurred while removing alias: " + aliasName + " from index: " + indexName, ex);
                }

                // Try to delete old index itself
                try 
                {
                    searchClientService.getClient().admin().indices().prepareDelete(indexName).execute().actionGet();
                    logger.debug("Old index {} removed sucessfully!", indexName);
                } 
                catch (Exception ex) 
                {
                    logger.error("Error occurred while removing old index: " + indexName, ex);
                }
            }
        }
    }

    private String getSuffixedIndexName(String indexName, Date date)
    {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        String suffix = sdf.format(date);
        return indexName + suffix;        
    }
    
    @Override
    public void updateIndexSettings(ElasticSearchIndexConfig config, Map<String, Object> settings)
    {
        // close index
        searchClientService.getClient().admin().indices().prepareClose(config.getIndexAliasName()).get();
        searchClientService.getClient().admin().indices().prepareUpdateSettings(config.getIndexAliasName())
                .setSettings(settings).get();

        // close index
        searchClientService.getClient().admin().indices().prepareOpen(config.getIndexAliasName()).get();
    }
    
    @Override
    public void updateDocumentTypeMapping(ElasticSearchIndexConfig config, String documentType, boolean parentRelationship)
    {
        try
        {
            searchClientService.getClient().admin().indices().preparePutMapping(config.getIndexAliasName())
                                                                .setType(documentType)
                                                                .setSource(new IndexSchemaBuilder().getDocumentTypeMapping(config, documentType, parentRelationship))
                                                                .get();
        } catch (IOException e)
        {
            throw new RuntimeException("Error occurend while generating mapping for document type", e);
        }
    }
    
    @Override
    public void indexProductGroupData(List<ProductGroup> productGroups)
    {
        for (ElasticSearchIndexConfig config : ElasticSearchIndexConfig.values())
        {
            recreateIndex(config);

            indexProductData.indexAllProductGroupData(config, productGroups, true);
        }
    }
    
    @Override
    public boolean isIndexExists(String indexName)
    {
        return searchClientService.getClient().admin().indices().prepareExists(indexName).get().isExists();
    }
    
    @Override
    public boolean deleteIndex(String indexName)
    {
        return searchClientService.getClient().admin().indices().prepareDelete(indexName).execute().actionGet().isAcknowledged();
    }
    
    @Override
    public String getIndexSettings(ElasticSearchIndexConfig config, String settingName)
    {
        String settingValue = null;

        ClusterStateResponse clusterStateResponse = searchClientService.getClient().admin().cluster().prepareState().setRoutingTable(true)
                .setNodes(true).setIndices(config.getIndexAliasName()).get();
        
        for (IndexMetaData indexMetaData : clusterStateResponse.getState().getMetaData())
        {
            settingValue = indexMetaData.getSettings().get(settingName);
        }
        return settingValue;
    }
    
    @Override
    public boolean isAliasExists(String indexAliasName)
    {
        return searchClientService.getClient().admin().indices().prepareAliasesExist(indexAliasName).get().isExists();
    }
    
    @Override
    public List<String> analyzeText(String indexAliasName, String analyzer, String[] tokenFilters, String text)
    {
        List<String> tokens = new ArrayList<String>();
        
        AnalyzeRequestBuilder analyzeRequestBuilder = searchClientService.getClient().admin().indices().prepareAnalyze(text);
        
        if(analyzer !=null)
        {
            analyzeRequestBuilder.setIndex(indexAliasName);
        }
        if(analyzer !=null)
        {
            analyzeRequestBuilder.setAnalyzer(analyzer);
        }
        
        if(tokenFilters !=null)
        {
        	final Map<String, String> map = new HashMap<String, String>();
        	for (int i = 0; i < tokenFilters.length; i++)
        		  map.put(tokenFilters[i], tokenFilters[i]);
            analyzeRequestBuilder.setTokenizer(map);
        }
        
        logger.debug("Analyze request is text: {}, analyzer: {}, tokenfilters: {}", new Object[]{analyzeRequestBuilder.request().text(), 
                                                                                    analyzeRequestBuilder.request().analyzer(),
                                                                                    analyzeRequestBuilder.request().tokenFilters()});
                                                                                            
        AnalyzeResponse analyzeResponse = analyzeRequestBuilder.get();
        
        try
        {
            if(analyzeResponse != null)
            {
//                logger.debug("Analyze response is : {}", analyzeResponse.toXContent(jsonBuilder().startObject(), ToXContent.EMPTY_PARAMS).prettyPrint().string());
            	logger.debug("Analyze response is : {}", analyzeResponse);
            }
        } catch (Exception e)
        {
            logger.error("Error printing response.", e);
        }
        
        for (AnalyzeToken analyzeToken : analyzeResponse.getTokens())
        {
            tokens.add(analyzeToken.getTerm());
        }
        return tokens;
    }
    
}
