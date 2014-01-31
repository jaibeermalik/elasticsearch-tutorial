package org.jai.search.setup;

import java.util.List;
import java.util.Map;

import org.jai.search.model.ElasticSearchIndexConfig;
import org.jai.search.model.ProductGroup;

public interface SetupIndexService
{
    void createIndex(ElasticSearchIndexConfig searchIndexConfig);
    
    void updateIndexSettings(ElasticSearchIndexConfig config, Map<String, Object> settings);
    
    void updateDocumentTypeMapping(ElasticSearchIndexConfig config, String documentType, boolean parentRelationship);

    void setupAllIndices(boolean parentRelationship);

    void indexProductGroupData(List<ProductGroup> productGroups);

    boolean isIndexExists(String indexName);

    boolean deleteIndex(String indexName);

    String getIndexSettings(ElasticSearchIndexConfig config, String settingName);

    boolean isAliasExists(String indexAliasName);

    List<String> analyzeText(String indexAliasName, String analyzer, String[] tokenFilters, String text);

}
