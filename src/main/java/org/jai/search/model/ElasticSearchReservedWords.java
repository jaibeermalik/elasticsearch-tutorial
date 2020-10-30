package org.jai.search.model;

public enum ElasticSearchReservedWords
{
	ANALYSIS("analysis"), 
	FILTER("filter"), 
	TYPE("type"), 
	STOP("stop"), 
	STOPWORDS_PATH ("stopwords_path"), 
	SNOWBALL("snowball"), 
	LANGUAGE("language"), 
	WORD_DELIMITER("word_delimiter"), 
	PROTECTED_WORDS_PATH("protected_words_path"), 
	TYPE_TABLE_PATH("type_table_path"), 
	SYNONYM("synonym"), 
	SYNONYMS_PATH("synonyms_path"), 
	ANALYZER("analyzer"), 
	CUSTOM("custom"), 
	FIELD_DATA("fielddata"),
	TOKENIZER("tokenizer"), 
	WHITESPACE("whitespace"), 
	LOWERCASE("lowercase"), 
	CHAR_FILTER("char_filter"), 
	HTML_STRIP("html_strip"), 
	KEYWORD("keyword"), 
	STANDARD("standard"), 
	PROPERTIES("properties"), 
	DATE("date"), 
	DATE_FORMATS("date_formats"),
	FORMAT("format"),
	STORE("store"), 
	YES("yes"),
	TRUE("true"),
	FALSE("false"),
	INDEX("index"), 
	NOT_ANALYZED("not_analyzed"),
	ANALYZED("analyzed"),
	FLOAT("float"), 
	BOOLEAN("boolean"), 
	STRING("string"),
	TEXT("text"),
	COMPLETION("completion"),
	DOUBLE("double"), 
	FIELDS("fields"), 
	MULTI_FIELD("multi_field"), 
	INDEX_MAPPER_DYNAMIC("index.mapper.dynamic"), 
	DEFAULT("_default_"), 
	DYNAMIC("dynamic"), 
	SOURCE("_source"), 
	ENABLED("enabled"), 
	INTEGER("integer"), 
	CLUSTER_NAME("cluster.name"), 
	PATH_DATA("path.data"),
	PATH_WORK("path.work"),
	PATH_LOG("path.logs"), 
	PATH_CONF("path.conf"),
	NUMBER_OF_SHARDS("index.number_of_shards"), 
	NUMBER_OF_REPLICAS("index.number_of_replicas"), 
	ANALYZER_SIMPLE("simple"), 
	SYNONYMS_IGNORE_CASE("ignore_case"),
	SYNONYMS_EXPAND("expand"), 
	NESTED("nested")
	
	;
    
    private String text;
    
    public String getText() {
		return text;
	}

	private ElasticSearchReservedWords(String text)
    {
        this.text = text;
    }

}
