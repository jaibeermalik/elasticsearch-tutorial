package org.jai.search.query.impl;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.mlt.MoreLikeThisRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.suggest.SuggestRequestBuilder;
import org.elasticsearch.action.suggest.SuggestResponse;
import org.elasticsearch.index.get.GetField;
import org.elasticsearch.index.query.AndFilterBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.FilteredQueryBuilder;
import org.elasticsearch.index.query.NestedFilterBuilder;
import org.elasticsearch.index.query.OrFilterBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.index.query.TermFilterBuilder;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilder;
import org.elasticsearch.index.query.functionscore.script.ScriptScoreFunctionBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
import org.elasticsearch.search.facet.Facet;
import org.elasticsearch.search.facet.FacetBuilders;
import org.elasticsearch.search.facet.Facets;
import org.elasticsearch.search.facet.range.RangeFacet;
import org.elasticsearch.search.facet.range.RangeFacetBuilder;
import org.elasticsearch.search.facet.terms.TermsFacet;
import org.elasticsearch.search.facet.terms.TermsFacet.ComparatorType;
import org.elasticsearch.search.facet.terms.TermsFacetBuilder;
import org.elasticsearch.search.rescore.RescoreBuilder;
import org.elasticsearch.search.rescore.RescoreBuilder.Rescorer;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.suggest.Suggest.Suggestion.Entry.Option;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.term.TermSuggestion;
import org.elasticsearch.search.suggest.term.TermSuggestionBuilder;
import org.jai.search.client.SearchClientService;
import org.jai.search.model.AutoSuggestionEntry;
import org.jai.search.model.Category;
import org.jai.search.model.ElasticSearchIndexConfig;
import org.jai.search.model.FacetResult;
import org.jai.search.model.FacetResultEntry;
import org.jai.search.model.Product;
import org.jai.search.model.ProductProperty;
import org.jai.search.model.ProductSearchResult;
import org.jai.search.model.SearchCriteria;
import org.jai.search.model.SearchDocumentFieldName;
import org.jai.search.model.SearchFacetName;
import org.jai.search.model.Specification;
import org.jai.search.query.ProductQueryService;
import org.jai.search.util.SearchDateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProductQueryServiceImpl implements ProductQueryService
{
    @Autowired
    private SearchClientService searchClientService;
    
    private static final Logger logger = LoggerFactory.getLogger(ProductQueryServiceImpl.class);

    @Override
    public ProductSearchResult searchProducts(SearchCriteria searchCriteria)
    {
        QueryBuilder queryBuilder = getQueryBuilder(searchCriteria);

        SearchRequestBuilder requestBuilder = getSearchRequestBuilder(searchCriteria.getIndexes(), 
                                                                        searchCriteria.getDocumentTypes(), 
                                                                        searchCriteria.getFrom(), 
                                                                        searchCriteria.getSize());
        requestBuilder.addFields(SearchDocumentFieldName.productQueryFields);
        
        if(searchCriteria.isRescoreOnSoldOut())
        {
            Rescorer rescorer = RescoreBuilder.queryRescorer(QueryBuilders.termQuery(SearchDocumentFieldName.SOLD_OUT.getFieldName(), false))
                                               .setQueryWeight(1.0f) //default
                                               .setRescoreQueryWeight(1.5f)
                                               ;
            requestBuilder.setRescorer(rescorer);
        }
        
        if (searchCriteria.hasFilters())
        {
            AndFilterBuilder andFilterBuilder = getFilterBuilderForSearchCriteria(searchCriteria);
            requestBuilder.setQuery(QueryBuilders.filteredQuery(queryBuilder, andFilterBuilder));
        } else
        {
            requestBuilder.setQuery(queryBuilder);
        }

        if (!searchCriteria.isNoFacets() && searchCriteria.getFacets().size() > 0)
        {
            addFacets(searchCriteria, requestBuilder);
        }

      //Add sorting
        if(searchCriteria.getSortOrder() !=null)
        {
            //First on given field
            requestBuilder.addSort(SortBuilders.fieldSort(SearchDocumentFieldName.AVAILABLE_DATE.getFieldName()).order(searchCriteria.getSortOrder()).missing("_last"));
            //then on score based
            requestBuilder.addSort(SortBuilders.scoreSort());
        }

        logger.debug("Executing following search request:" + requestBuilder.internalBuilder().toString());
        
        SearchResponse searchResponse = requestBuilder.execute().actionGet();
        
        printSearchResponseForDebug(searchResponse);
        
        return getProductSearchResults(searchResponse);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public Product getProduct(ElasticSearchIndexConfig config, Long productId)
    {
        GetResponse getResponse = searchClientService.getClient().prepareGet(config.getIndexAliasName(), config.getDocumentType(), String.valueOf(productId))
                                                                 .setFields(SearchDocumentFieldName.productDocumentFields)
                                                                 .get();
        if(getResponse.isExists())
        {
            Product product = new Product();
            product.setId(Long.valueOf(getResponse.getId()));
            product.setTitle(getResponse.getField(SearchDocumentFieldName.TITLE.getFieldName()).getValue().toString());
            product.setDescription(getResponse.getField(SearchDocumentFieldName.DESCRIPTION.getFieldName()).getValue().toString());
            product.setSoldOut(Boolean.valueOf(getResponse.getField(SearchDocumentFieldName.SOLD_OUT.getFieldName()).getValue().toString()));
            product.setAvailableOn(SearchDateUtils.getFormattedDate(getResponse.getField(SearchDocumentFieldName.AVAILABLE_DATE.getFieldName()).getValue().toString()));
            product.setKeywords(getListFieldValueOrNull(getResponse.getField(SearchDocumentFieldName.KEYWORDS.getFieldName())));
            product.setPrice(BigDecimal.valueOf(Double.valueOf(getResponse.getField(SearchDocumentFieldName.PRICE.getFieldName()).getValue().toString())));
            product.setBoostFactor(Float.valueOf(getResponse.getField(SearchDocumentFieldName.BOOSTFACTOR.getFieldName()).getValue().toString()));
            GetField catField = getResponse.getField(SearchDocumentFieldName.CATEGORIES_ARRAY.getFieldName());
            if(catField !=null)
            {
                for (Object ListOfMapValues : catField.getValues())
                {
                    for (final java.util.Map.Entry<String, String> entry : ((Map<String, String>)ListOfMapValues).entrySet())
                    {
                        //Only main facet values should be set.key ending with a number.
                        if(entry.getKey().matches("^.*.facet$"))
                        {
                            product.addCategory(new Category(entry.getValue(), null, entry.getKey().split("\\.facet")[0]));
                        }
                    }
                }
            }
            
            return product;
        }
        return null;
    }

    @Override
    public List<AutoSuggestionEntry> getAutoSuggestions(ElasticSearchIndexConfig config, String queryString)
    {
        TermSuggestionBuilder suggesBuilder = SuggestBuilder.termSuggestion(SearchFacetName.AUTO_SUGGESTION.getCode())
                                                             .field(SearchDocumentFieldName.KEYWORDS.getFieldName())
                                                             .analyzer(config.getAutoSuggestionAnalyzerName())
                                                             .size(20)
                                                             .text(queryString)
//                                                             .suggestMode("always")
//                                                             .stringDistance("ngram")
                                                             ;
        
//        CompletionSuggestionBuilder suggesBuilder = new CompletionSuggestionBuilder(SearchFacetName.AUTO_SUGGESTION.getCode())
//                .field(SearchDocumentFieldName.KEYWORDS.getFieldName())
//                .analyzer(config.getAutoSuggestionAnalyzerName())
//                .size(20)
//                .text(queryString)
////                .stringDistance("ngram")
//                ;
        
//        PhraseSuggestionBuilder suggesBuilder = SuggestBuilder.phraseSuggestion(SearchFacetName.AUTO_SUGGESTION.getCode())
//                                                              .field(SearchDocumentFieldName.TITLE.getFieldName())
//                                                              .analyzer(config.getAutoSuggestionAnalyzerName())
//                                                              .size(10)
//                                                              .text(queryString)
//                                                              ;
        
        SuggestRequestBuilder addSuggestion = searchClientService.getClient().prepareSuggest(config.getIndexAliasName())
                                        .addSuggestion(suggesBuilder);
        
        try
        {
            logger.debug("Auto Suggestion request is {}", suggesBuilder.toXContent(jsonBuilder().startObject(), null).prettyPrint().string());
        } catch (IOException e)
        {
            //Do nothing  
            logger.error("Error in to string", e);
        }

        SuggestResponse suggestResponse = addSuggestion.get();
        
        logger.debug("Auto Suggestion response is {}", suggestResponse);
        
        List<AutoSuggestionEntry> suggestions = new ArrayList<AutoSuggestionEntry>();

        if(suggestResponse !=null && suggestResponse.getSuggest() !=null && suggestResponse.getSuggest().getSuggestion(SearchFacetName.AUTO_SUGGESTION.getCode()) !=null)
        {
            for (org.elasticsearch.search.suggest.Suggest.Suggestion.Entry<? extends Option> suggestEntry : suggestResponse.getSuggest().getSuggestion(SearchFacetName.AUTO_SUGGESTION.getCode()).getEntries())
            {
                for (Option option : suggestEntry.getOptions())
                {
                    int count = ((TermSuggestion.Entry.Option) option).getFreq();
                    AutoSuggestionEntry autoSuggestionEntry = new AutoSuggestionEntry(option.getText().string(), count);
                    suggestions.add(autoSuggestionEntry);
                }
            }
        }
        
        return suggestions;
    }
    
    @Override
    public List<AutoSuggestionEntry> getAutoSuggestionsUsingTermsFacet(ElasticSearchIndexConfig config, String queryString)
    {
     
        List<AutoSuggestionEntry> autoSuggestEntries = new ArrayList<AutoSuggestionEntry>();
        
        SearchRequestBuilder searchRequestBuilder = searchClientService.getClient().prepareSearch(config.getIndexAliasName())
                                                                                   .setTypes(config.getDocumentType())
                                                                                    .setSize(0)
                                                                                    .setQuery(QueryBuilders.matchAllQuery());
        
        TermsFacetBuilder termsFacetBuilder = FacetBuilders.termsFacet(SearchFacetName.AUTO_SUGGESTION.getCode());
        
        String[] fieldsArray = new String[SearchFacetName.autoSuggestionFields.size() + 1];
        SearchFacetName.autoSuggestionFields.toArray(fieldsArray);
        fieldsArray[fieldsArray.length -1] = SearchDocumentFieldName.KEYWORDS.getFieldName();

        termsFacetBuilder.fields(fieldsArray);
        
        String lowerCaseQueryString = queryString.toLowerCase();
        String filteredSpecialCharsQueryString = escapeQueryChars(lowerCaseQueryString);
        String matchingRegExString = filteredSpecialCharsQueryString + ".*";
        
        termsFacetBuilder.regex(matchingRegExString)
                    .size(20)
                    .order(ComparatorType.TERM);

        searchRequestBuilder.addFacet(termsFacetBuilder);
        
        logger.debug("Auto Suggestion request is {}", searchRequestBuilder.internalBuilder().toString());
        
        SearchResponse searchResponse = searchRequestBuilder.get();
        
        try
        {
            logger.debug("Auto Suggestion response is {}", searchResponse.toXContent(jsonBuilder().startObject(), null).prettyPrint().string());
        } catch (IOException e)
        {
            logger.error("Search response tostring error:",e);
        }
        
        Facets facets = searchResponse.getFacets();
        for (Facet facet : facets)
        {
            if(facet.getName().equals(SearchFacetName.AUTO_SUGGESTION.getCode()))
            {
                TermsFacet termsFacet = (TermsFacet) facet;
                for (org.elasticsearch.search.facet.terms.TermsFacet.Entry entry : termsFacet.getEntries())
                {
                    AutoSuggestionEntry term = new AutoSuggestionEntry(entry.getTerm().string(), entry.getCount());
                    autoSuggestEntries.add(term);
                }
            }
        }
        return autoSuggestEntries;
    }
    
    @Override
    public List<Product> findSimilarProducts(ElasticSearchIndexConfig config, String[] fields, Long productId)
    {
        MoreLikeThisRequestBuilder moreLikeThisRequestBuilder = searchClientService.getClient().prepareMoreLikeThis(config.getIndexAliasName(), config.getDocumentType(), String.valueOf(productId))
                                                                                                .setField(fields)
                                                                                                .setMinDocFreq(1)
                                                                                                .setMinTermFreq(1)
                                                                                                .setSearchSize(10)
                                                                                                
                                                                                                ;
        
        logger.debug("Executing following search request, fields {}", new Object[]{moreLikeThisRequestBuilder.request().fields()});
        
        SearchResponse searchResponse = moreLikeThisRequestBuilder.get();

        printSearchResponseForDebug(searchResponse);
        
        List<Product> products = new ArrayList<Product>();
        for (SearchHit searchHit : searchResponse.getHits())
        {
            Product product = new  Product();
            
            product.setId(Long.valueOf(searchHit.getId()));
            product.setTitle(searchHit.getSource().get(SearchDocumentFieldName.TITLE.getFieldName()).toString());
            product.setPrice(BigDecimal.valueOf(Double.valueOf(searchHit.getSource().get(SearchDocumentFieldName.PRICE.getFieldName()).toString())));
            product.setSoldOut(Boolean.valueOf(searchHit.getSource().get(SearchDocumentFieldName.SOLD_OUT.getFieldName()).toString()));
            
            products.add(product);
        }
        return products;
    }
    
    private AndFilterBuilder getFilterBuilderForSearchCriteria(SearchCriteria searchCriteria)
    {
        AndFilterBuilder andFilterBuilder = FilterBuilders.andFilter();

        //process single select filters
        for (java.util.Map.Entry<String, String> entry : searchCriteria.getSingleSelectFilters().entrySet())
        {
            andFilterBuilder.add(getBaseFilterBuilder(entry.getKey(), entry.getValue()));
        }

        //process field value filters
        // Processing logic for field values
        List<Map<String, Object>> fieldValueFilters = searchCriteria.getFieldValueFilters();

        if(fieldValueFilters.size() > 0)
        {
            //process multiple entries in or filter
            final OrFilterBuilder orFilterForFieldValueList = FilterBuilders.orFilter();
            //processing of fieldValueFilters is following:
            //each list of filter items containing in fieldValueFilters joins with 'OR'
            //each filter item in list joins with 'AND'
            for (Map<String, Object> filterItems : fieldValueFilters)
            {
                //process all entries in map in and operation.
                AndFilterBuilder andFilterBuilderForFieldValueMapInList = FilterBuilders.andFilter();
                for (Entry<String, Object> entry : filterItems.entrySet())
                {
                    //retrieve filter builder
                    FilterBuilder filterBuilder = getTermFilter(entry.getKey(), String.valueOf(entry.getValue()));
                    //add to and condition
                    andFilterBuilderForFieldValueMapInList.add(filterBuilder);
                }
                orFilterForFieldValueList.add(andFilterBuilderForFieldValueMapInList);
            }
            andFilterBuilder.add(orFilterForFieldValueList);
        }

        //process multi select filters
        for (java.util.Map.Entry<String, List<String>> entry : searchCriteria.getMultiSelectFilters().entrySet())
        {
            //Empty list, continue to next one.
            //Only process multi select other than specifications (resolution/memory)
            if(entry.getValue().size() == 0 || entry.getKey().equals(SearchFacetName.SPECIFICATION_RESOLUTION.getCode())
                                            || entry.getKey().endsWith(SearchFacetName.SPECIFICATION_MEMORY.getCode()))
            {
                continue;
            }

            if(entry.getValue().size() > 1)
            {
                //process multiple entries in or filter
                final OrFilterBuilder orFilter = FilterBuilders.orFilter();
                for (final String filterName : entry.getValue())
                {
                    orFilter.add(getBaseFilterBuilder(entry.getKey(), filterName));
                }
                andFilterBuilder.add(orFilter);
            }
            else
            {
                //process single entry directly in and filter
                andFilterBuilder.add(getBaseFilterBuilder(entry.getKey(), entry.getValue().get(0)));
            }
        }
        
        //process resolution/memory facets
        andFilterBuilder.add(FilterBuilders.nestedFilter(SearchDocumentFieldName.SPECIFICATIONS.getFieldName(), getSpecificationsFacetFilterBuilder(searchCriteria)));
        
        
        //process child product properties
        if(searchCriteria.getProductProperties().size() > 0)
        {
            FilterBuilder propertyFilterBuilder = null;
            if(searchCriteria.getProductProperties().size() > 1)
            {
                OrFilterBuilder OrPropertyFilterBuilder1 = FilterBuilders.orFilter();
                for (ProductProperty productProperty : searchCriteria.getProductProperties())
                {
                    AndFilterBuilder andPropertyTermFilter = FilterBuilders.andFilter(FilterBuilders.termFilter(SearchDocumentFieldName.SIZE.getFieldName(), productProperty.getSize().toLowerCase()),
                            FilterBuilders.termFilter(SearchDocumentFieldName.COLOR.getFieldName(), productProperty.getColor().toLowerCase()));
                    OrPropertyFilterBuilder1.add(andPropertyTermFilter);
                }
                propertyFilterBuilder = OrPropertyFilterBuilder1;
            }
            else
            {
                ProductProperty productProperty = searchCriteria.getProductProperties().get(0);
                propertyFilterBuilder = FilterBuilders.andFilter(FilterBuilders.termFilter(SearchDocumentFieldName.SIZE.getFieldName(), productProperty.getSize()),
                        FilterBuilders.termFilter(SearchDocumentFieldName.COLOR.getFieldName(), productProperty.getColor()));
            }
            FilteredQueryBuilder filteredQueryBuilder = new FilteredQueryBuilder(QueryBuilders.matchAllQuery(), propertyFilterBuilder);
            //TODO: config replace
            andFilterBuilder.add(FilterBuilders.hasChildFilter(ElasticSearchIndexConfig.COM_WEBSITE.getPropertiesDocumentType(), filteredQueryBuilder));
        }
        
        //Another approach for specifications, in case faceting not used.
        if(searchCriteria.getSpecifications().size() > 0)
        {
            FilterBuilder specificationFilterBuilder = null;
            specificationFilterBuilder = getSpecificationsFilterBuilder(searchCriteria);
            
            andFilterBuilder.add(FilterBuilders.nestedFilter(SearchDocumentFieldName.SPECIFICATIONS.getFieldName(), specificationFilterBuilder));
        }

        return andFilterBuilder;
    }

    private FilterBuilder getSpecificationsFilterBuilder(SearchCriteria searchCriteria)
    {
        //This is used in case you want to search items based on specifications itself.
        
        FilterBuilder specificationFilterBuilder;
        List<Specification> specifications = searchCriteria.getSpecifications();
        if(specifications.size() > 1)
        {
            OrFilterBuilder OrSpecificationFilterBuilder = FilterBuilders.orFilter();
            for (Specification specification : specifications)
            {
                FilterBuilder filterBuilder = FilterBuilders.andFilter(FilterBuilders
                        .termFilter(SearchDocumentFieldName.RESOLUTION.getFieldName(), specification.getResolution()),
                            FilterBuilders.termFilter(SearchDocumentFieldName.MEMORY.getFieldName(), specification.getMemory()));

                OrSpecificationFilterBuilder.add(filterBuilder);
            }
            specificationFilterBuilder = OrSpecificationFilterBuilder;
        }
        else
        {
            Specification specification = searchCriteria.getSpecifications().get(0);
            
                    FilterBuilder filterBuilder = FilterBuilders.andFilter(FilterBuilders
                                                        .termFilter(SearchDocumentFieldName.RESOLUTION.getFieldName(), specification.getResolution()),
                    FilterBuilders.termFilter(SearchDocumentFieldName.MEMORY.getFieldName(), specification.getMemory()));
                    
                    specificationFilterBuilder = filterBuilder;
        }
        return specificationFilterBuilder;
    }
    
    private FilterBuilder getSpecificationsFacetFilterBuilder(SearchCriteria searchCriteria)
    {
        //This is used in case you want to search based on separate resolution/memory facets
        Map<String, List<String>> multiSelectFilters = searchCriteria.getMultiSelectFilters();
        List<String> resolutionFilters = new ArrayList<String>();
        List<String> memoryFilters = new ArrayList<String>();
        for (Entry<String, List<String>> entry : multiSelectFilters.entrySet())
        {
            if(entry.getKey().equals(SearchFacetName.SPECIFICATION_RESOLUTION.getCode()))
            {
                resolutionFilters.addAll(entry.getValue());
            }
            else if(entry.getKey().equals(SearchFacetName.SPECIFICATION_MEMORY.getCode()))
            {
                memoryFilters.addAll(entry.getValue());
            }
        }
        if(resolutionFilters.size() == 0 && memoryFilters.size() == 0 )
        {
            return FilterBuilders.queryFilter(QueryBuilders.matchAllQuery());
        }
        
        AndFilterBuilder andResolutionAndMemoryFilterBuilder = FilterBuilders.andFilter();
        if(resolutionFilters.size() > 0)
        {
            OrFilterBuilder OrResolutionFilterBuilder = FilterBuilders.orFilter();
            for (String resolution : resolutionFilters)
            {
                OrResolutionFilterBuilder.add(FilterBuilders.termFilter(SearchDocumentFieldName.RESOLUTION.getFieldName(), resolution));
            }
            andResolutionAndMemoryFilterBuilder.add(OrResolutionFilterBuilder);
        }
        if(memoryFilters.size() > 0)
        {
            OrFilterBuilder OrMemoryFilterBuilder = FilterBuilders.orFilter();
            for (String memory : memoryFilters)
            {
                OrMemoryFilterBuilder.add(FilterBuilders.termFilter(SearchDocumentFieldName.MEMORY.getFieldName(), memory));
            }
            andResolutionAndMemoryFilterBuilder.add(OrMemoryFilterBuilder);
        }
//        else if(specifications.size() == 1)
//        {
//            Specification specification = searchCriteria.getSpecifications().get(0);
//            
//                    FilterBuilder filterBuilder = FilterBuilders.andFilter(FilterBuilders
//                                                        .termFilter(SearchDocumentFieldName.RESOLUTION.getFieldName(), specification.getResolution()),
//                    FilterBuilders.termFilter(SearchDocumentFieldName.MEMORY.getFieldName(), specification.getMemory()));
//                    
//                    specificationFilterBuilder = filterBuilder;
//        }
//        else
//        {
//            specificationFilterBuilder = FilterBuilders.matchAllFilter();
//        }
        return andResolutionAndMemoryFilterBuilder;
    }
    
    private FilterBuilder getBaseFilterBuilder(String facetName, String fieldValue)
    {
        if(facetName.startsWith(SearchFacetName.SEARCH_FACET_TYPE_FACET_PREFIX))
        {
            return getTermFilter(SearchFacetName.CATEGORIES_FIELD_PREFIX + facetName + "." + SearchDocumentFieldName.FACETFILTER.getFieldName(), fieldValue.toLowerCase());
        }
        else if(facetName.startsWith(SearchFacetName.PRODUCT_PRICE_RANGE.getCode()))
        {
            return FilterBuilders.rangeFilter(SearchDocumentFieldName.PRICE.getFieldName()).includeLower(true).includeUpper(false).from(fieldValue.split("-")[0]).to(fieldValue.split("-")[1]);
        }
        else
        {
            return FilterBuilders.termFilter(facetName, fieldValue);
        }
//        return null;
    }
    
    private TermFilterBuilder getTermFilter(String fieldName, String fieldValue)
    {
        return FilterBuilders.termFilter(fieldName, fieldValue);
    }

    private void addFacets(SearchCriteria searchCriteria, SearchRequestBuilder requestBuilder)
    {
        for(String facetCode : searchCriteria.getFacets())
        {
            if(SearchFacetName.categoryFacetFields.contains(facetCode))
            {
                requestBuilder.addFacet(getNewTermsFacet(facetCode, facetCode + "." + SearchDocumentFieldName.FACET.getFieldName()));
            }
            else if(SearchFacetName.PRODUCT_PRICE_RANGE.getCode().equals(facetCode))
            {
                requestBuilder.addFacet(getPriceRangeFacet(facetCode));
            }
            else if(SearchFacetName.PRODUCT_PROPERTY_SIZE.getCode().equals(facetCode) || SearchFacetName.PRODUCT_PROPERTY_COLOR.getCode().equals(facetCode))
            {
//                QueryBuilder filter = HasChildFilterBuilder;
//                HasParentFilterBuilder hasParentFilter = FilterBuilders.hasParentFilter(ElasticSearchIndexConfig.COM_WEBSITE.getDocumentType(), QueryBuilders.matchAllQuery());
//                
//                String field = SearchFacetName.PRODUCT_PROPERTY_SIZE.getCode().equals(facetfield) ? SearchDocumentFieldName.SIZE.getFieldName() : SearchDocumentFieldName.COLOR.getFieldName();
//                requestBuilder.addFacet(FacetBuilders.termsFacet(facetfield).field(field).facetFilter(hasParentFilter));
            }
            else if(SearchFacetName.SPECIFICATION_RESOLUTION.getCode().equals(facetCode))
            {
                //TODO, not working
//                NestedFilterBuilder nestedFilterBuilder = FilterBuilders.nestedFilter(SearchDocumentFieldName.SPECIFICATIONS.getFieldName(), 
//                        getSpecificationsFacetFilterBuilder(searchCriteria)).join(false);
                NestedFilterBuilder nestedFilterBuilder = FilterBuilders.nestedFilter(SearchDocumentFieldName.SPECIFICATIONS.getFieldName(), 
                        FilterBuilders.matchAllFilter()).join(false);
                TermsFacetBuilder facetFilter = FacetBuilders.termsFacet(facetCode)
                        .field(SearchDocumentFieldName.SPECIFICATIONS.getFieldName() + "." + SearchDocumentFieldName.RESOLUTION.getFieldName())
                        .facetFilter(nestedFilterBuilder)
                        .nested(SearchDocumentFieldName.SPECIFICATIONS.getFieldName());
                requestBuilder.addFacet(facetFilter);
            }
            else if(SearchFacetName.SPECIFICATION_MEMORY.getCode().equals(facetCode))
            {
                //TODO, not working
                NestedFilterBuilder nestedFilterBuilder = FilterBuilders.nestedFilter(SearchDocumentFieldName.SPECIFICATIONS.getFieldName(), 
                        FilterBuilders.matchAllFilter()).join(false);
                TermsFacetBuilder facetFilter = FacetBuilders.termsFacet(facetCode)
                        .field(SearchDocumentFieldName.SPECIFICATIONS.getFieldName() + "." + SearchDocumentFieldName.MEMORY.getFieldName())
                        .facetFilter(nestedFilterBuilder)
                        .nested(SearchDocumentFieldName.SPECIFICATIONS.getFieldName());
                requestBuilder.addFacet(facetFilter);
            }
            
        }
    }

    private RangeFacetBuilder getPriceRangeFacet(String facetCode)
    {
        return FacetBuilders.rangeFacet(facetCode)
                                            .field(SearchDocumentFieldName.PRICE.getFieldName())
                                            .addRange(0, 10)
                                            .addRange(10, 20)
                                            .addRange(20, 100);
    }
    
    protected TermsFacetBuilder getNewTermsFacet(String facetName, String facetField)
    {
        final TermsFacetBuilder termsFacetBuilder = new TermsFacetBuilder(facetName);
        termsFacetBuilder.field(facetField);
        termsFacetBuilder.order(ComparatorType.TERM);
        termsFacetBuilder.size(100);
        return termsFacetBuilder;
    }
    
    private ProductSearchResult getProductSearchResults(SearchResponse response)
    {
        logger.debug("Total search hits returned for the query totalHits:" + response.getHits().getTotalHits());

        ProductSearchResult productSearchResult = new ProductSearchResult();
        productSearchResult.setTotalCount(response.getHits().totalHits());
        for (SearchHit searchHit : response.getHits())
        {
            Product product = new  Product();
            
            product.setId(Long.valueOf(searchHit.getId()));
            product.setTitle(getFieldValueOrNull(searchHit, SearchDocumentFieldName.TITLE.getFieldName()));
            product.setPrice(BigDecimal.valueOf(getDoubleFieldValueOrNull(searchHit, SearchDocumentFieldName.PRICE.getFieldName())));
            product.setSoldOut(Boolean.valueOf(getFieldValueOrNull(searchHit, SearchDocumentFieldName.SOLD_OUT.getFieldName())));
            
            productSearchResult.addProduct(product);
        }
        
        if(response.getFacets() !=null)
        {
            for (Facet facet : response.getFacets())
            {
                FacetResult facetResult = new FacetResult();
                facetResult.setCode(facet.getName());
                
                if (TermsFacet.TYPE.equals(facet.getType()))
                {
                    TermsFacet termsFacet = (TermsFacet) facet;
                    if(termsFacet.getEntries().size() == 0)
                    {
                        continue;
                    }
                    
                    for (TermsFacet.Entry entry : termsFacet.getEntries())
                    {
                        FacetResultEntry facetResultEntry = new FacetResultEntry();
                        
                        //final String term = entry.getTerm().substring(entry.getTerm().indexOf("_") + 1);
                        facetResultEntry.setTerm(entry.getTerm().string());
                        facetResultEntry.setCount(entry.getCount());
                        
                        facetResult.addFacetResultEntry(facetResultEntry);
                    }
                }
                else if(RangeFacet.TYPE.equals(facet.getType()))
                {
                    RangeFacet rangeFacet = (RangeFacet) facet;
                    if(rangeFacet.getEntries().size() == 0)
                    {
                        continue;
                    }
                    
                    for (RangeFacet.Entry entry : rangeFacet.getEntries())
                    {
                        FacetResultEntry facetResultEntry = new FacetResultEntry();
                        facetResultEntry.setTerm(entry.getFromAsString() + " - " + entry.getToAsString());
                        facetResultEntry.setCount(entry.getCount());
                        
                        facetResult.addFacetResultEntry(facetResultEntry);
                    }
                }
                else
                {
                    //NOT supported
                }
                productSearchResult.addFacet(facetResult);
            }
        }

        logger.debug("Total Product created from response:" + productSearchResult.getProducts().size());

        return productSearchResult;
    }
    
    protected String getFieldValueOrNull(SearchHit searchHit, String fieldName)
    {
        final SearchHitField searchHitField = searchHit.field(fieldName);
        if (searchHitField != null && searchHitField.value() != null) {
            return searchHitField.value().toString();
        }
        return null;
    }
    
    protected Double getDoubleFieldValueOrNull(SearchHit searchHit, String fieldName)
    {
        final SearchHitField searchHitField = searchHit.field(fieldName);
        if (searchHitField != null && searchHitField.value() != null) {
            return Double.valueOf(searchHitField.value().toString());
        }
        return null;
    }
    
    protected void printSearchResponseForDebug(SearchResponse response)
    {
        try
        {
            if(logger.isDebugEnabled())
            {
                logger.debug("Search response:"+ response.toXContent(jsonBuilder().startObject().prettyPrint(), null).prettyPrint().string());
            }
        } catch (final IOException ex)
        {
            //ignore
            logger.error("Error occured while printing search response for debug.", ex);
        }
    }

    protected QueryBuilder getQueryBuilder(SearchCriteria searchCriteria)
    {
        QueryBuilder matchQueryBuilder = null;
        
        String queryString = searchCriteria.getQuery();
        
        if (StringUtils.isBlank(queryString))
        {
            matchQueryBuilder = QueryBuilders.matchAllQuery();
        } 
        else
        {
            final String filterSpecialCharsQueryString = escapeQueryChars(queryString);
            final QueryStringQueryBuilder queryStringQueryBuilder = QueryBuilders.queryString(filterSpecialCharsQueryString);

            // Add fields
            queryStringQueryBuilder.field(SearchDocumentFieldName.TITLE.getFieldName(), (float) 0.5)
                                    .field(SearchDocumentFieldName.DESCRIPTION.getFieldName(), (float) 0.15)
                                    ;
            
            for (final String contentCategoryFieldName : SearchFacetName.categoryFacetFields)
            {
                queryStringQueryBuilder.field(SearchDocumentFieldName.CATEGORIES_ARRAY.getFieldName() + "."
                        + contentCategoryFieldName, 1);
            }
            
            matchQueryBuilder = queryStringQueryBuilder;
        }
        
        if(searchCriteria.isUseBoostingFactor())
        {
            FunctionScoreQueryBuilder queryBuilder = new FunctionScoreQueryBuilder(matchQueryBuilder);
            ScoreFunctionBuilder scoreFunctionBuilder = new ScriptScoreFunctionBuilder().script(SearchDocumentFieldName
                    .getCalculatedScoreScriptForBostFactor());
            queryBuilder.add(scoreFunctionBuilder);
            return queryBuilder;
        }

        return matchQueryBuilder;
    }
    
    protected SearchRequestBuilder getSearchRequestBuilder(String[] indexName, String[] types, int from, int size)
    {
        final SearchRequestBuilder requestBuilder = searchClientService.getClient().prepareSearch(indexName).setTypes(types)
                .setFrom(from)
                .setSize(size);
        return requestBuilder;
    }

    private String escapeQueryChars(String queryString)
    {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < queryString.length(); i++)
        {
            final char c = queryString.charAt(i);
            // These characters are part of the query syntax and must be escaped
            // The list if retrieved from Solr escape characters.
            if (c == '\\' || c == '+' || c == '-' || c == '!' || c == '(' || c == ')' || c == ':' || c == '^'
                    || c == '[' || c == ']' || c == '\"' || c == '{' || c == '}' || c == '~' || c == '*' || c == '?'
                    || c == '|' || c == '&' || c == ';' || c == '/' || Character.isWhitespace(c))
            {
                sb.append('\\');
            }
            sb.append(c);
        }
        return sb.toString();
    }
    
    protected String getStringFieldValue(GetField field)
    {
        if(field !=null)
        {
            return String.valueOf(field.getValue());
        }
        return null;
    }

    protected Date getDateFieldValueOrNull(GetField field)
    {
        if(field !=null)
        {
            final String dateString = String.valueOf(field.getValue());
            if(dateString !=null && !dateString.isEmpty())
            {
                return SearchDateUtils.getFormattedDate(dateString);
            }
        }
        return null;
    }

    protected boolean getBooleanFieldValueOrFalse(GetField field)
    {
        if(field !=null)
        {
            return Boolean.valueOf(String.valueOf(field.getValue()));
        }
        return false;
    }
    
    @SuppressWarnings("unchecked")
    protected List<String> getListFieldValueOrNull(GetField field)
    {
        if(field !=null)
        {
            final List<String> list = new ArrayList<String>();
            for (final Object object : field.getValues())
            {
                if(object instanceof List)
                {
                    for (final String valueString : (List<String>)object)
                    {
                        list.add(String.valueOf(valueString));
                    }
                }
                else
                {
                    list.add(String.valueOf(object));
                }
            }
            return list;
        }
        return null;
    }
}
