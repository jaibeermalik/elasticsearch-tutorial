package org.jai.search.model;



public enum SearchDocumentFieldName
{
    BOOSTFACTOR("boostfactor"),
    TITLE("title"),
    TITLEPG("titlepg"),
    DESCRIPTION("description"),
    DESCRIPTIONPG("descriptionpg"),
    AVAILABLE_DATE("availabledate"),
    SOLD_OUT("soldout"),
    KEYWORDS("keywords"),
    CATEGORIES_ARRAY("categories"),
    PRICE("price"),
    FACET("facet"),
    FACETFILTER("facetfilter"),
    SUGGEST("suggest"),
    SEQUENCED("sequenced"),
    
    SIZE("size"),
    COLOR("color"),
    
    SPECIFICATIONS("specifications"),
    RESOLUTION("resolution"),
    MEMORY("memory"),
    ;

    public static final String[] productQueryFields = {
        TITLE.getFieldName(),
        PRICE.getFieldName(),
        SOLD_OUT.getFieldName()
    };

    public static final String[] productDocumentFields = {
        TITLE.getFieldName(),
        DESCRIPTION.getFieldName(),
        PRICE.getFieldName(),
        SOLD_OUT.getFieldName(),
        AVAILABLE_DATE.getFieldName(),
        PRICE.getFieldName(),
        KEYWORDS.getFieldName(),
        BOOSTFACTOR.getFieldName(),
        CATEGORIES_ARRAY.getFieldName() + ".",
    };

    private String fieldName;

    private SearchDocumentFieldName(String fieldName)
    {
        this.fieldName = fieldName;
    }

    public String getFieldName()
    {
        return fieldName;
    }
    
    public static String getCalculatedScoreScriptForBostFactor()
    {
        return "_score + doc.boostfactor.value";
    }

}
