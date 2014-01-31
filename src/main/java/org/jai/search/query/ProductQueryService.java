package org.jai.search.query;

import java.util.List;

import org.jai.search.model.AutoSuggestionEntry;
import org.jai.search.model.ElasticSearchIndexConfig;
import org.jai.search.model.Product;
import org.jai.search.model.ProductSearchResult;
import org.jai.search.model.SearchCriteria;

public interface ProductQueryService
{
    ProductSearchResult searchProducts(SearchCriteria searchCriteria);

    Product getProduct(ElasticSearchIndexConfig config, Long productId);
    
    List<AutoSuggestionEntry> getAutoSuggestions(ElasticSearchIndexConfig config, String queryString);

    List<AutoSuggestionEntry> getAutoSuggestionsUsingTermsFacet(ElasticSearchIndexConfig config, String string);

    List<Product> findSimilarProducts(ElasticSearchIndexConfig config, String[] fields, Long productId);
}
