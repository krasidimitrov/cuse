package com.clouway.searchengine.gae;

import com.clouway.searchengine.spi.EmptyMatcherException;
import com.clouway.searchengine.spi.MatchedIdObjectFinder;
import com.clouway.searchengine.spi.NegativeSearchLimitException;
import com.clouway.searchengine.spi.SearchLimitExceededException;
import com.clouway.searchengine.spi.SearchMatcher;
import com.google.appengine.api.search.Consistency;
import com.google.appengine.api.search.IndexSpec;
import com.google.appengine.api.search.Query;
import com.google.appengine.api.search.QueryOptions;
import com.google.appengine.api.search.Results;
import com.google.appengine.api.search.ScoredDocument;
import com.google.appengine.api.search.SearchServiceFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Ivan Lazov <ivan.lazov@clouway.com>
 */
public class GaeSearchApiMatchedIdObjectFinder implements MatchedIdObjectFinder {

  @Override
  public List<String> find(String indexName, Map<String, SearchMatcher> filters, int limit) {

    String query = buildQueryFilter(filters);

        Results<ScoredDocument> results = SearchServiceFactory.getSearchService().getIndex(IndexSpec.newBuilder()
                                                                             .setName(indexName)
                                                                             .setConsistency(Consistency.PER_DOCUMENT))
                                                                             .search(buildQuery(query, limit));

    List<String> entityIds = new ArrayList<String>();
    for (ScoredDocument scoredDoc : results) {
      entityIds.add(scoredDoc.getId());
    }


    return entityIds;
  }



  private String buildQueryFilter(Map<String, SearchMatcher> filters) {

    StringBuilder queryFilter = new StringBuilder();

    for (String filter : filters.keySet()) {

      String filterValue = filters.get(filter).getValue().trim();

      if (filterValue == null || "".equals(filterValue)
              || filter == null) {
        throw new EmptyMatcherException();
      }

      if ("".equals(filter)){
        queryFilter.append(filterValue).append(" ");
      } else {
        queryFilter.append(filter).append(":").append(filterValue).append(" ");
      }
    }

    return queryFilter.toString();
  }

  private Query buildQuery(String searchQuery, int limit) {

    QueryOptions queryOptions = buildQueryOptions(limit);

    return Query.newBuilder().setOptions(queryOptions).build(searchQuery);
  }

  private QueryOptions buildQueryOptions(int limit) {

    QueryOptions.Builder queryOptionsBuilder = QueryOptions.newBuilder().setReturningIdsOnly(true);

    if (limit > 1000) {
      throw new SearchLimitExceededException();
    }

    if (limit < 0) {
      throw new NegativeSearchLimitException();
    }

    if (limit > 0) {
      queryOptionsBuilder.setLimit(limit);
    }

    return queryOptionsBuilder.build();
  }


}