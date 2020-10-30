package org.jai.search.setup;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

import java.io.IOException;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.jai.search.model.ElasticSearchIndexConfig;
import org.jai.search.model.ElasticSearchReservedWords;
import org.jai.search.model.SearchDocumentFieldName;
import org.jai.search.model.SearchFacetName;
import org.jai.search.util.SearchDateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IndexSchemaBuilder
{
    private static final Logger logger = LoggerFactory.getLogger(IndexSchemaBuilder.class);
    
    public Settings getSettingForIndex(ElasticSearchIndexConfig config) throws IOException
    {
        logger.debug("Generating settings for index: {}", config.getIndexAliasName());
        
        Settings settings = Settings.builder().loadFromSource(jsonBuilder()
                    .startObject()
                        //disable dynamic mapping adding, set it to false 
                        .field(ElasticSearchReservedWords.INDEX_MAPPER_DYNAMIC.getText(), false)
                        //Add analyzer settings
                        .startObject(ElasticSearchReservedWords.ANALYSIS.getText())
                            .startObject(ElasticSearchReservedWords.FILTER.getText())
                                .startObject(config.getStopwordsCustomFilterName())
                                    .field(ElasticSearchReservedWords.TYPE.getText(), ElasticSearchReservedWords.STOP.getText())    
                                    .field(ElasticSearchReservedWords.STOPWORDS_PATH.getText(), "stopwords/stop_" + config.getSupportedLocale().getText())
                                .endObject()
                                .startObject(config.getSnowballCustomFilterName())
                                    .field(ElasticSearchReservedWords.TYPE.getText(), ElasticSearchReservedWords.SNOWBALL.getText())
                                    .field(ElasticSearchReservedWords.LANGUAGE.getText(), config.getSupportedLocale().getLang())
                                .endObject()
                                .startObject(config.getWorddelimiterCustomFilterName())
                                    .field(ElasticSearchReservedWords.TYPE.getText(), ElasticSearchReservedWords.WORD_DELIMITER.getText())
                                    .field(ElasticSearchReservedWords.PROTECTED_WORDS_PATH.getText(), "worddelimiters/protectedwords_" + config.getSupportedLocale().getText())
                                    .field(ElasticSearchReservedWords.TYPE_TABLE_PATH.getText(), "worddelimiters/typetable")
                                    .field("split_on_numerics", "true")
                                    .field("generate_number_parts", "true")
                                    .field("preserve_original", "true")
                                .endObject()
                                .startObject(config.getSynonymsCustomFilterName())
                                    .field(ElasticSearchReservedWords.TYPE.getText(), ElasticSearchReservedWords.SYNONYM.getText())
                                    .field(ElasticSearchReservedWords.SYNONYMS_PATH.getText(), "synonyms/synonyms_" + config.getSupportedLocale().getText())
                                    .field(ElasticSearchReservedWords.SYNONYMS_IGNORE_CASE.getText(), true)
                                    .field(ElasticSearchReservedWords.SYNONYMS_EXPAND.getText(), true)
                                .endObject()
                                .startObject(config.getShingleTokenFilterName())
                                    .field("type", "shingle")
                                    .field("min_shingle_size", 2)
                                    .field("max_shingle_size", 4)
                                .endObject()
                                .startObject(config.getNGramTokenFilterName())
                                    .field("type", "edgeNGram")
                                    .field("min_gram", 4)
                                    .field("max_gram", 30)
                                .endObject()
                          .endObject()
                            .startObject(ElasticSearchReservedWords.ANALYZER.getText())
                                .startObject(config.getStandardTextAnalyzerName())
                                    .field(ElasticSearchReservedWords.TYPE.getText(), ElasticSearchReservedWords.CUSTOM.getText())
                                    .field(ElasticSearchReservedWords.TOKENIZER.getText(), ElasticSearchReservedWords.STANDARD.getText())
                                    .field(ElasticSearchReservedWords.FILTER.getText(), new String[]{ElasticSearchReservedWords.LOWERCASE.getText(), 
                                                                                config.getStopwordsCustomFilterName(), 
                                                                                config.getSynonymsCustomFilterName(),
                                                                                config.getSnowballCustomFilterName() 
                                                                                })
                                .endObject()
                                .startObject(config.getCustomFreeTextAnalyzerName())
                                    .field(ElasticSearchReservedWords.TYPE.getText(), ElasticSearchReservedWords.CUSTOM.getText())
                                    .field(ElasticSearchReservedWords.TOKENIZER.getText(), ElasticSearchReservedWords.WHITESPACE.getText())
                                    .field(ElasticSearchReservedWords.FILTER.getText(), new String[]{ElasticSearchReservedWords.LOWERCASE.getText(), 
                                                                                config.getWorddelimiterCustomFilterName(), 
                                                                                config.getStopwordsCustomFilterName(), 
                                                                                config.getSynonymsCustomFilterName(),
                                                                                config.getSnowballCustomFilterName() 
                                                                                })
                                   .field(ElasticSearchReservedWords.CHAR_FILTER.getText(), ElasticSearchReservedWords.HTML_STRIP.getText())                                             
                                .endObject()
                                .startObject(config.getAutoSuggestionAnalyzerName())
                                    .field(ElasticSearchReservedWords.TYPE.getText(), ElasticSearchReservedWords.CUSTOM.getText())
                                    .field(ElasticSearchReservedWords.TOKENIZER.getText(), ElasticSearchReservedWords.KEYWORD.getText())
//                                    .field(ElasticSearchReservedWords.TOKENIZER.getText(), "letter")
                                    .field(ElasticSearchReservedWords.FILTER.getText(), new String[]{ElasticSearchReservedWords.LOWERCASE.getText()
//                                                                                config.getNGramTokenFilterName()
                                                                                })
                                .endObject()
                                .startObject(config.getCustomFacetAnalyzerName())
                                    .field(ElasticSearchReservedWords.TYPE.getText(), ElasticSearchReservedWords.CUSTOM.getText())
                                    .field(ElasticSearchReservedWords.TOKENIZER.getText(), ElasticSearchReservedWords.STANDARD.getText())
                                    .field(ElasticSearchReservedWords.FILTER.getText(), new String[]{ElasticSearchReservedWords.LOWERCASE.getText(), 
                                                                                config.getSnowballCustomFilterName(), 
                                                                                config.getSynonymsCustomFilterName()
                                                                                })
                                .endObject()
                            .endObject()
                        .endObject()
                    .endObject().string()).build();
        
        logger.debug("Generated settings for index {} is: {}", new Object[]{config.getIndexAliasName(), settings.getAsMap()});
        
        return settings;
    }

    public XContentBuilder getDocumentTypeMapping(ElasticSearchIndexConfig elasticSearchIndexConfig, String documentType, boolean parentRelationship) throws IOException
    {
        XContentBuilder builder =  jsonBuilder().prettyPrint().startObject().startObject(documentType);
                
        //disable dynamics mapping of fields
        builder.field(ElasticSearchReservedWords.DYNAMIC.getText(), "strict");        

        if(documentType.equals(elasticSearchIndexConfig.getDocumentType()))
        {
            //Used for parent-child relationship
            if(parentRelationship)
            {
                builder.startObject("_parent").field("type", elasticSearchIndexConfig.getGroupDocumentType()).endObject();
            }
            builder.startObject(ElasticSearchReservedWords.PROPERTIES.getText());
            
            addProductBooleanFieldMappingForNEW(builder);
            addLiveDateMapping(builder);
            addCustomBoostFactorMapping(builder);
            addProductDynamicValues(builder);
            addContentInformationFieldsMapping(builder, elasticSearchIndexConfig);
            addSpecifications(builder, elasticSearchIndexConfig);
        }
        else if(documentType.equals(elasticSearchIndexConfig.getGroupDocumentType()))
        {
            builder.startObject(ElasticSearchReservedWords.PROPERTIES.getText());
            //Let it be dynamic for now
            builder.startObject(SearchDocumentFieldName.TITLEPG.getFieldName())
                        .field(ElasticSearchReservedWords.TYPE.getText(), ElasticSearchReservedWords.KEYWORD.getText())
                        .field(ElasticSearchReservedWords.STORE.getText(), ElasticSearchReservedWords.TRUE.getText())
                        .field(ElasticSearchReservedWords.INDEX.getText(), ElasticSearchReservedWords.TRUE.getText())
//                        .field(ElasticSearchReservedWords.ANALYZER.getText(), elasticSearchIndexConfig.getCustomFreeTextAnalyzerName())
//                        .field(ElasticSearchReservedWords.FIELD_DATA.getText(), ElasticSearchReservedWords.TRUE.getText())
                   .endObject()
                   .startObject(SearchDocumentFieldName.DESCRIPTIONPG.getFieldName())
                       .field(ElasticSearchReservedWords.TYPE.getText(), ElasticSearchReservedWords.KEYWORD.getText())
                       .field(ElasticSearchReservedWords.STORE.getText(), ElasticSearchReservedWords.TRUE.getText())
                       .field(ElasticSearchReservedWords.INDEX.getText(), ElasticSearchReservedWords.TRUE.getText())
//                       .field(ElasticSearchReservedWords.ANALYZER.getText(), elasticSearchIndexConfig.getCustomFreeTextAnalyzerName())
//                       .field(ElasticSearchReservedWords.FIELD_DATA.getText(), ElasticSearchReservedWords.TRUE.getText())
                   .endObject()
               ;
            
        }
        else if(documentType.equals(elasticSearchIndexConfig.getPropertiesDocumentType()))
        {
          //Used for parent-child relationship
            if(parentRelationship)
            {
                builder.startObject("_parent").field("type", elasticSearchIndexConfig.getDocumentType()).endObject();
            }
            builder.startObject(ElasticSearchReservedWords.PROPERTIES.getText());
            //Let it be dynamic for now 
            builder.startObject(SearchDocumentFieldName.SIZE.getFieldName())
                        .field(ElasticSearchReservedWords.TYPE.getText(), ElasticSearchReservedWords.KEYWORD.getText())
                        .field(ElasticSearchReservedWords.STORE.getText(), ElasticSearchReservedWords.TRUE.getText())
                        .field(ElasticSearchReservedWords.INDEX.getText(), ElasticSearchReservedWords.TRUE.getText())
                   .endObject()
                   .startObject(SearchDocumentFieldName.COLOR.getFieldName())
                       .field(ElasticSearchReservedWords.TYPE.getText(), ElasticSearchReservedWords.KEYWORD.getText())
                       .field(ElasticSearchReservedWords.STORE.getText(), ElasticSearchReservedWords.TRUE.getText())
                       .field(ElasticSearchReservedWords.INDEX.getText(), ElasticSearchReservedWords.TRUE.getText())
                   .endObject();
        }
//        else
//        {
//            builder.startObject(ElasticSearchReservedWords.PROPERTIES.getText());
//        }
        
        //end properties
        builder.endObject()
        //end two start ones
            .endObject()
            .endObject();
       // System.out.println(builder.string());
        logger.debug("Generated mapping for document type {} is: {}", new Object[]{elasticSearchIndexConfig, builder.prettyPrint().string()});
        return builder;
    }
    
    private void addSpecifications(XContentBuilder builder, ElasticSearchIndexConfig elasticSearchIndexConfig) throws IOException
    {
        builder.startObject(SearchDocumentFieldName.SPECIFICATIONS.getFieldName())
                    .startObject(ElasticSearchReservedWords.PROPERTIES.getText())
                        .startObject(SearchDocumentFieldName.RESOLUTION.getFieldName())
                           .field(ElasticSearchReservedWords.TYPE.getText(), ElasticSearchReservedWords.KEYWORD.getText())
                           .field(ElasticSearchReservedWords.INDEX.getText(), ElasticSearchReservedWords.TRUE.getText())
                           .field(ElasticSearchReservedWords.STORE.getText(), ElasticSearchReservedWords.TRUE.getText())
                        .endObject()
                        .startObject(SearchDocumentFieldName.MEMORY.getFieldName())
                           .field(ElasticSearchReservedWords.TYPE.getText(), ElasticSearchReservedWords.KEYWORD.getText())
                           .field(ElasticSearchReservedWords.INDEX.getText(), ElasticSearchReservedWords.TRUE.getText())
                           .field(ElasticSearchReservedWords.STORE.getText(), ElasticSearchReservedWords.TRUE.getText())
                        .endObject()
                    .endObject()
                    .field(ElasticSearchReservedWords.TYPE.getText(), ElasticSearchReservedWords.NESTED.getText())
                .endObject();
    }

    private void addProductBooleanFieldMappingForNEW(XContentBuilder builder) throws IOException
    {
        //standard fields
        builder.startObject(SearchDocumentFieldName.SOLD_OUT.getFieldName())
                   .field(ElasticSearchReservedWords.TYPE.getText(), ElasticSearchReservedWords.BOOLEAN.getText())
                   .field(ElasticSearchReservedWords.INDEX.getText(), ElasticSearchReservedWords.TRUE.getText())
                   .field(ElasticSearchReservedWords.STORE.getText(), ElasticSearchReservedWords.TRUE.getText())
               .endObject();
    }
    
    private void addLiveDateMapping(XContentBuilder builder) throws IOException
    {
        builder.startObject(SearchDocumentFieldName.AVAILABLE_DATE.getFieldName())
                .field(ElasticSearchReservedWords.TYPE.getText(), ElasticSearchReservedWords.DATE.getText())
                .field(ElasticSearchReservedWords.FORMAT.getText(), SearchDateUtils.SEARCH_DATE_FORMAT_YYYY_MM_DD_T_HH_MM_SSSZZ)
                .field(ElasticSearchReservedWords.STORE.getText(), ElasticSearchReservedWords.TRUE.getText())
                .field(ElasticSearchReservedWords.INDEX.getText(), ElasticSearchReservedWords.TRUE.getText())
            .endObject();
    }
    
    private void addCustomBoostFactorMapping(XContentBuilder builder) throws IOException
    {
        builder.startObject(SearchDocumentFieldName.BOOSTFACTOR.getFieldName())
                    .field(ElasticSearchReservedWords.TYPE.getText(), ElasticSearchReservedWords.FLOAT.getText())
                    .field(ElasticSearchReservedWords.STORE.getText(), ElasticSearchReservedWords.TRUE.getText())
                    .field(ElasticSearchReservedWords.INDEX.getText(), ElasticSearchReservedWords.TRUE.getText())
               .endObject();
    }
    
    private void addProductDynamicValues(XContentBuilder builder) throws IOException
    {
        //Add out of stock information
         builder.startObject(SearchDocumentFieldName.PRICE.getFieldName())
                 .field(ElasticSearchReservedWords.TYPE.getText(), ElasticSearchReservedWords.DOUBLE.getText())
                 .field(ElasticSearchReservedWords.INDEX.getText(), ElasticSearchReservedWords.TRUE.getText())
                 .endObject()
              ;
    }
    
    private XContentBuilder addContentInformationFieldsMapping(XContentBuilder builder, ElasticSearchIndexConfig elasticSearchIndexConfig) throws IOException
    {
          //content information fields
          builder.startObject(SearchDocumentFieldName.TITLE.getFieldName())
                     .field(ElasticSearchReservedWords.TYPE.getText(), ElasticSearchReservedWords.TEXT.getText())
                     .field(ElasticSearchReservedWords.STORE.getText(), ElasticSearchReservedWords.TRUE.getText())
                     .field(ElasticSearchReservedWords.INDEX.getText(), ElasticSearchReservedWords.TRUE.getText())
                     .field(ElasticSearchReservedWords.ANALYZER.getText(), elasticSearchIndexConfig.getCustomFreeTextAnalyzerName())
                     .field(ElasticSearchReservedWords.FIELD_DATA.getText(), ElasticSearchReservedWords.TRUE.getText())
                 .endObject()
                 .startObject(SearchDocumentFieldName.DESCRIPTION.getFieldName())
                     .field(ElasticSearchReservedWords.TYPE.getText(), ElasticSearchReservedWords.TEXT.getText())
                     .field(ElasticSearchReservedWords.STORE.getText(), ElasticSearchReservedWords.TRUE.getText())
                     .field(ElasticSearchReservedWords.INDEX.getText(), ElasticSearchReservedWords.TRUE.getText())
                     .field(ElasticSearchReservedWords.ANALYZER.getText(), elasticSearchIndexConfig.getCustomFreeTextAnalyzerName())
                     .field(ElasticSearchReservedWords.FIELD_DATA.getText(), ElasticSearchReservedWords.TRUE.getText())
                 .endObject()
                 
                 //Add content categories mapping
                 .startObject(SearchDocumentFieldName.CATEGORIES_ARRAY.getFieldName())
                 	.field(ElasticSearchReservedWords.TYPE.getText(), "nested")
                 	.startObject(ElasticSearchReservedWords.PROPERTIES.getText());
                         //Add each category facet
                         for (SearchFacetName facetName : SearchFacetName.categoryFacetValues())
                         {
                             int hierarchyLevel = SearchFacetName.getSupportedFacetParentChildHierarchyLevel();
                             for (int i = 1; i <= hierarchyLevel; i++)
                             {
                                 builder
                                 .startObject(facetName.getFacetSequencedFieldNameAtLevel(i))
                                             .field(ElasticSearchReservedWords.TYPE.getText(), ElasticSearchReservedWords.KEYWORD.getText())
                                             .field(ElasticSearchReservedWords.INDEX.getText(), ElasticSearchReservedWords.TRUE.getText())
                                         .endObject()
                                                .startObject(facetName.getFacetFieldNameAtLevel(i) + "." + SearchDocumentFieldName.FACET.getFieldName())
                                                    .field(ElasticSearchReservedWords.TYPE.getText(), ElasticSearchReservedWords.KEYWORD.getText())
                                                    .field(ElasticSearchReservedWords.STORE.getText(), ElasticSearchReservedWords.TRUE.getText())
                                                    .field(ElasticSearchReservedWords.INDEX.getText(), ElasticSearchReservedWords.TRUE.getText())
                                                .endObject()
                                                .startObject(facetName.getFacetFieldNameAtLevel(i) + "." + SearchDocumentFieldName.FACETFILTER.getFieldName())
                                                    .field(ElasticSearchReservedWords.TYPE.getText(), ElasticSearchReservedWords.KEYWORD.getText())
                                                    .field(ElasticSearchReservedWords.INDEX.getText(), ElasticSearchReservedWords.TRUE.getText())
                                                .endObject()
                                                .startObject(facetName.getFacetFieldNameAtLevel(i) + "." + SearchDocumentFieldName.SUGGEST.getFieldName())
                                                    .field(ElasticSearchReservedWords.TYPE.getText(), ElasticSearchReservedWords.TEXT.getText())
                                                    .field(ElasticSearchReservedWords.STORE.getText(), ElasticSearchReservedWords.TRUE.getText())
                                                    .field(ElasticSearchReservedWords.INDEX.getText(), ElasticSearchReservedWords.TRUE.getText())
                                                    .field(ElasticSearchReservedWords.ANALYZER.getText(), elasticSearchIndexConfig.getAutoSuggestionAnalyzerName())
                                                    .field(ElasticSearchReservedWords.FIELD_DATA.getText(), ElasticSearchReservedWords.TRUE.getText())
                                                .endObject();
                             }
                         }
          builder.endObject()
                 .endObject()
                 //Add keywords mapping
                             .startObject(SearchDocumentFieldName.KEYWORDS.getFieldName())
                                 .field(ElasticSearchReservedWords.TYPE.getText(), ElasticSearchReservedWords.COMPLETION.getText())
//                                 .field(ElasticSearchReservedWords.STORE.getText(), ElasticSearchReservedWords.TRUE.getText())
                                 .field(ElasticSearchReservedWords.ANALYZER.getText(), elasticSearchIndexConfig.getAutoSuggestionAnalyzerName())
                             .endObject();
         return builder;
    }
    
    

}
