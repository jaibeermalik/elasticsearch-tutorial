package org.jai.search.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum SearchFacetName
{
    //facets: level based on number 1..n, 
    //the facet/query/sequenced fields and filters calculated
    SEARCH_FACET_TYPE_PRODUCT_TYPE("searchfacettype_product_type"), 
    SEARCH_FACET_TYPE_BRAND("searchfacettype_brand"), 
    SEARCH_FACET_TYPE_AGE("searchfacettype_age"), 
    SEARCH_FACET_TYPE_COLOR("searchfacettype_color"),

    PRODUCT_PRICE_RANGE("product_price_range"),
    
    PRODUCT_PROPERTY_SIZE("product_property_size"),
    PRODUCT_PROPERTY_COLOR("product_property_color"),
    
    SPECIFICATION_RESOLUTION("specification_resolution"),
    SPECIFICATION_MEMORY("specification_memory"),
    
    //Auto suggestions
    AUTO_SUGGESTION("auto_suggestion")
    ;

    public static final String CATEGORIES_FIELD_PREFIX = "categories.";

	public static final String HIERARCHICAL_DATA_LEVEL_STRING = "_level_";

    public static final String SEARCH_FACET_TYPE_FACET_PREFIX = "searchfacettype_";
    
    //Used to order filters based on sequence order
    public static final String SEQUENCED_FIELD_SUFFIX = ".sequenced";

    private SearchFacetName(String code)
    {
        this.code = code;
    }
    
    public static final List<SearchFacetName> categoryFacets = Collections.unmodifiableList(Arrays.asList(
                                                                        SEARCH_FACET_TYPE_PRODUCT_TYPE,
                                                                        SEARCH_FACET_TYPE_AGE,
                                                                        SEARCH_FACET_TYPE_BRAND,
                                                                        SEARCH_FACET_TYPE_COLOR
                                                                        ));

    public static final List<String> categoryFacetFields = new ArrayList<String>();
    public static final List<String> autoSuggestionFields = new ArrayList<String>();
    
    public static final List<String> dynamicSystemFacetFields = new ArrayList<String>();
    
    private String code;
    
    public String getCode()
    {
        return code;
    }
    
    public static List<SearchFacetName> categoryFacetValues()
    {
        return categoryFacets;
    }
    
    //currently only support 5 level of hierarchy
    public static int getSupportedFacetParentChildHierarchyLevel()
    {
        return 4;
    }
    
    public String getFacetFieldNameAtLevel(int level)
    {
        //Level is applicable to content category facets only.
        if(categoryFacetValues().contains(this))
        {
            return getCode() + HIERARCHICAL_DATA_LEVEL_STRING + level;
        }
        return null;
    }
    
    public String getFacetSequencedFieldNameAtLevel(int level)
    {
        //Level is applicable to content category facets only.
        if(categoryFacetValues().contains(this))
        {
            return getCode() +  HIERARCHICAL_DATA_LEVEL_STRING + level + SEQUENCED_FIELD_SUFFIX;
        }
        return null;
    }
    
    public String getAutoSuggestionFieldNameAtLevel(int level)
    {
        return getCode() + HIERARCHICAL_DATA_LEVEL_STRING + level + ".suggest";
    }
    
    public static String getFacetSequencedFieldNameByCode(String facetCode)
    {
        return facetCode + SEQUENCED_FIELD_SUFFIX;
    }
    
    static 
    {
        for (SearchFacetName facetName : SearchFacetName.categoryFacetValues())
        {
            for (int categoryLevel = 1; categoryLevel <= SearchFacetName.getSupportedFacetParentChildHierarchyLevel(); categoryLevel++)
            {
                categoryFacetFields.add(facetName.getFacetFieldNameAtLevel(categoryLevel));
                autoSuggestionFields.add(CATEGORIES_FIELD_PREFIX + facetName.getAutoSuggestionFieldNameAtLevel(categoryLevel));
            }
        }
    }
}
