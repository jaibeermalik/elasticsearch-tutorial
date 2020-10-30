package org.jai.search.index.impl;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.jai.search.client.SearchClientService;
import org.jai.search.index.IndexProductData;
import org.jai.search.model.Category;
import org.jai.search.model.ElasticSearchIndexConfig;
import org.jai.search.model.Product;
import org.jai.search.model.ProductGroup;
import org.jai.search.model.ProductProperty;
import org.jai.search.model.SearchDocumentFieldName;
import org.jai.search.model.SearchFacetName;
import org.jai.search.model.Specification;
import org.jai.search.util.SearchDateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class IndexProductDataImpl implements IndexProductData
{
    @Autowired
    private SearchClientService searchClientService;

    private static final Logger logger = LoggerFactory.getLogger(IndexProductDataImpl.class);
    
    @Override
    public void indexAllProducts(ElasticSearchIndexConfig config, List<Product> products)
    {
        logger.debug("Indexing bulk data request, for size:" + products.size());

        if (products.isEmpty())
        {
            return;
        }
        
        List<IndexRequestBuilder> requests = new ArrayList<IndexRequestBuilder>();

        for (Product product : products)
        {
            try
            {
                requests.add(getIndexRequestBuilderForAProduct(product, config));
            } catch (Exception ex)
            {
                logger.error("Error occurred while creating index document for product with id: " + product.getId() + ", moving to next product!", ex);
            }
        }
        processBulkRequests(requests);
    }
    
    @Override
    public void indexProduct(ElasticSearchIndexConfig config, Product product)
    {
        try
        {
            getIndexRequestBuilderForAProduct(product, config).get();
        } catch (Exception ex)
        {
            logger.error("Error occurred while creating index document for product.", ex);
            throw new RuntimeException(ex);
        }
    }
    
    @Override
    public boolean isProductExists(ElasticSearchIndexConfig config, Long productId)
    {
        return searchClientService.getClient().prepareGet().setIndex(config.getIndexAliasName())
                .setId(String.valueOf(productId))
                .get()
                .isExists();
    }
    
    @Override
    public void deleteProduct(ElasticSearchIndexConfig config, Long productId)
    {
        searchClientService.getClient().prepareDelete(config.getIndexAliasName(), config.getDocumentType(), String.valueOf(productId)).get();
    }
    
    @Override
    public void indexAllProductGroupData(ElasticSearchIndexConfig config, List<ProductGroup> productGroups, boolean parentRelationShip)
    {
        List<IndexRequestBuilder> requests = new ArrayList<IndexRequestBuilder>();
        for (ProductGroup productGroup : productGroups)
        {
            try
            {
                requests.add(getIndexRequestBuilderForAProductGroup(productGroup, config));
                //Index all products data also with parent
                for (Product product : productGroup.getProducts())
                {
                    IndexRequestBuilder indexRequestBuilderForAProduct = getIndexRequestBuilderForAProduct(product, config);
                    if(parentRelationShip)
                    {
                        indexRequestBuilderForAProduct.setParent(String.valueOf(productGroup.getId()));
                    }
                    
                    requests.add(indexRequestBuilderForAProduct);
                    
                    for (ProductProperty productProperty : product.getProductProperties())
                    {
                        IndexRequestBuilder indexRequestBuilderForAProductProperty = getIndexRequestBuilderForAProductProperty(product, productProperty, config);
                        if(parentRelationShip)
                        {
                            indexRequestBuilderForAProductProperty.setParent(String.valueOf(product.getId()));
                        }
                        
                        requests.add(indexRequestBuilderForAProductProperty);
                    }
                }
            } 
            catch (Exception ex)
            {
                logger.error("Error occurred while creating index document for gift with id: " + productGroup.getId()
                        + ", moving to next gift!", ex);
            }
        }
        processBulkRequests(requests);
    }
    
    private IndexRequestBuilder getIndexRequestBuilderForAProduct(Product product, ElasticSearchIndexConfig config) throws IOException
    {
        XContentBuilder contentBuilder = getXContentBuilderForAProduct(product);
        
        IndexRequestBuilder indexRequestBuilder = searchClientService.getClient().prepareIndex(config.getIndexAliasName(), config.getDocumentType(), String.valueOf(product.getId()));

        indexRequestBuilder.setSource(contentBuilder);

        return indexRequestBuilder;
    }
    
    private IndexRequestBuilder getIndexRequestBuilderForAProductProperty(Product product, ProductProperty productProperty, ElasticSearchIndexConfig config) throws IOException
    {
        XContentBuilder contentBuilder = getXContentBuilderForAProductProperty(productProperty);
        
        String documentId = String.valueOf(product.getId()) + String.valueOf(productProperty.getId()) + "0000";
        logger.debug("Generated XContentBuilder for document id {} is {}", new Object[]{documentId, contentBuilder.prettyPrint().string()});

        IndexRequestBuilder indexRequestBuilder = searchClientService.getClient().prepareIndex(config.getIndexAliasName(), config.getPropertiesDocumentType(), documentId);

        indexRequestBuilder.setSource(contentBuilder);

        return indexRequestBuilder;
    }
    
    private IndexRequestBuilder getIndexRequestBuilderForAProductGroup(ProductGroup productGroup, ElasticSearchIndexConfig config) throws IOException
    {
        XContentBuilder contentBuilder = getXContentBuilderForAProductGroup(productGroup);
        
        logger.debug("Generated XContentBuilder for document id {} is {}", new Object[]{productGroup.getId(), contentBuilder.prettyPrint().string()});

        IndexRequestBuilder indexRequestBuilder = searchClientService.getClient().prepareIndex(config.getIndexAliasName(), config.getGroupDocumentType(), String.valueOf(productGroup.getId()));

        indexRequestBuilder.setSource(contentBuilder);

        return indexRequestBuilder;
    }
    
    private XContentBuilder getXContentBuilderForAProduct(Product product) throws IOException
    {
        XContentBuilder contentBuilder = null;
        try
        {
            contentBuilder = jsonBuilder().prettyPrint().startObject();
            
            contentBuilder.field(SearchDocumentFieldName.TITLE.getFieldName(), product.getTitle())
                          .field(SearchDocumentFieldName.DESCRIPTION.getFieldName(), product.getDescription())
                          .field(SearchDocumentFieldName.PRICE.getFieldName(), product.getPrice())
                          .field(SearchDocumentFieldName.KEYWORDS.getFieldName(), product.getKeywords())
                          .field(SearchDocumentFieldName.AVAILABLE_DATE.getFieldName(), SearchDateUtils.formatDate(product.getAvailableOn()))
                          .field(SearchDocumentFieldName.SOLD_OUT.getFieldName(), product.isSoldOut())
                          .field(SearchDocumentFieldName.BOOSTFACTOR.getFieldName(), product.getBoostFactor())
                          ;
            
            if(product.getCategories().size() > 0)
            {
                //Add category data
                Map<Integer, Set<Category>> levelMap = getContentCategoryLevelMap(product.getCategories());
                
                contentBuilder.startArray(SearchDocumentFieldName.CATEGORIES_ARRAY.getFieldName());
                for (Entry<Integer, Set<Category>> contentCategoryEntrySet : levelMap.entrySet())
                {
                    for (Category category : contentCategoryEntrySet.getValue())
                    {
                        String name = category.getType() + SearchFacetName.HIERARCHICAL_DATA_LEVEL_STRING + contentCategoryEntrySet.getKey();
                        contentBuilder.startObject()
                        .field(name  + "." + SearchDocumentFieldName.FACET.getFieldName(), category.getName())
                        //                                    .field(name + SearchFacetName.SEQUENCED_FIELD_SUFFIX, getSequenceNumberOrdering(contentCategory) + categoryTranalationText)
                        .field(name + "." + SearchDocumentFieldName.FACETFILTER.getFieldName(), category.getName().toLowerCase())
                        .field(name + "." + SearchDocumentFieldName.SUGGEST.getFieldName(), category.getName().toLowerCase())
                        .endObject();
                    }
                }
                contentBuilder.endArray();
            }
            
           if(product.getSpecifications().size() > 0)
           {
               //Index specifications
               contentBuilder.startArray(SearchDocumentFieldName.SPECIFICATIONS.getFieldName());
               for (Specification specification : product.getSpecifications())
               {
                   contentBuilder.startObject()
                   .field(SearchDocumentFieldName.RESOLUTION.getFieldName(), specification.getResolution())
                   .field(SearchDocumentFieldName.MEMORY.getFieldName(), specification.getMemory())
                   .endObject(); 
               }
               contentBuilder.endArray();
           }
           
           contentBuilder.endObject();
        }
        catch (IOException ex)
        {
            logger.error(ex.getMessage());
            throw new RuntimeException("Error occured while creating product gift json document!", ex);
        }
        
        logger.debug("Generated XContentBuilder for document id {} is {}", new Object[]{product.getId(), contentBuilder.prettyPrint().string()});
        
        return contentBuilder;
    }

    private XContentBuilder getXContentBuilderForAProductProperty(ProductProperty productProperty)
    {
        XContentBuilder contentBuilder = null;
        try
        {
            contentBuilder = jsonBuilder().prettyPrint().startObject();
            contentBuilder.field(SearchDocumentFieldName.SIZE.getFieldName(), productProperty.getSize())
                          .field(SearchDocumentFieldName.COLOR.getFieldName(), productProperty.getColor())
                          ;
            contentBuilder.endObject();
        }
        catch (IOException ex)
        {
            logger.error(ex.getMessage());
            throw new RuntimeException("Error occured while creating product gift json document!", ex);
        }
        return contentBuilder;
    }
    
    private XContentBuilder getXContentBuilderForAProductGroup(ProductGroup productGroup)
    {
        XContentBuilder contentBuilder = null;
        try
        {
            contentBuilder = jsonBuilder().prettyPrint().startObject();
            
            contentBuilder.field(SearchDocumentFieldName.TITLEPG.getFieldName(), productGroup.getGroupTitle())
                          .field(SearchDocumentFieldName.DESCRIPTIONPG.getFieldName(), productGroup.getGroupDescription())
                          ;
            
            contentBuilder.endObject();
        }
        catch (IOException ex)
        {
            logger.error(ex.getMessage());
            throw new RuntimeException("Error occured while creating product gift json document!", ex);
        }
        return contentBuilder;
    }

   
    private Map<Integer, Set<Category>> getContentCategoryLevelMap(List<Category> categories)
    {
        Map<Integer, Set<Category>> levelMap = new HashMap<Integer, Set<Category>>();
        for (Category contentCategory : categories)
        {
                int defaultTopLevelCategoryIndex = 1;
                int levelInHierarchy = getCategoryLevelInHierarchy(contentCategory, defaultTopLevelCategoryIndex);
                for (int categoryLevelCounter = levelInHierarchy; categoryLevelCounter <= levelInHierarchy && categoryLevelCounter >= defaultTopLevelCategoryIndex; categoryLevelCounter--)
                {
                    processCategoryAtLevel(levelMap, findCategoryAtLevel(contentCategory, levelInHierarchy, categoryLevelCounter), categoryLevelCounter);
                }
        }
        return levelMap;
    }
    
    private Category findCategoryAtLevel(Category contentCategory, int currentCategoryLevel, int counter)
    {
        if (currentCategoryLevel == counter)
        {
            return contentCategory;
        }
        int nextCounter = counter + 1;
        return findCategoryAtLevel(contentCategory.getParentCategory(), currentCategoryLevel, nextCounter);
    }
    
    private int getCategoryLevelInHierarchy(Category contentCategory, int level)
    {
        if (contentCategory.getParentCategory() == null)
        {
            return level;
        }
        int nextLevel = level + 1;
        return getCategoryLevelInHierarchy(contentCategory.getParentCategory(), nextLevel);
    }
    
    private void processCategoryAtLevel(Map<Integer, Set<Category>> levelMap, Category contentCategory, int categoryLevel)
    {
        Set<Category> categoryLevelSet = getCategoryLevelSet(levelMap, categoryLevel);
        categoryLevelSet.add(contentCategory);
    }
    
    private Set<Category> getCategoryLevelSet(Map<Integer, Set<Category>> levelMap, int level)
    {
        Integer valueOf = Integer.valueOf(level);
        Set<Category> set = levelMap.get(valueOf);
        if (set == null)
        {
            set = new HashSet<Category>();
            levelMap.put(valueOf, set);
        }
        return set;
    }
    
    protected BulkResponse processBulkRequests(List<IndexRequestBuilder> requests)
    {
        if (requests.size() > 0)
        {
            BulkRequestBuilder bulkRequest = searchClientService.getClient().prepareBulk();
            
            for (IndexRequestBuilder indexRequestBuilder : requests)
            {
                bulkRequest.add(indexRequestBuilder);
            }
            
            logger.debug("Executing bulk index request for size:" + requests.size());
            BulkResponse bulkResponse = bulkRequest.execute().actionGet();
            
            logger.debug("Bulk operation data index response total items is:" + bulkResponse.getItems().length);
            if (bulkResponse.hasFailures())
            {
                // process failures by iterating through each bulk response item
                logger.error("bulk operation indexing has failures:" + bulkResponse.buildFailureMessage());
            }
            return bulkResponse;
        }
        else
        {
            logger.debug("Executing bulk index request for size: 0");
            return null;
        }
    }
    
}
