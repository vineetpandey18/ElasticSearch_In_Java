package com.elastico.java.files.search;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchPhraseQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static org.elasticsearch.index.query.QueryBuilders.multiMatchQuery;
import static org.elasticsearch.index.query.QueryBuilders.nestedQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Map;

import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.BucketOrder;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import elasticsearchclient.java.files.search.config.Configurations;
import elasticsearchclient.java.files.search.design.Para;
import elasticsearchclient.java.files.search.design.Writer;
import elasticsearchclient.java.files.search.repository.ParaRepository;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = Configurations.class)
public class esManualQueries {

    @Autowired
    private ElasticsearchRestTemplate elasticsearchTemplate;

    @Autowired
    private ParaRepository paraRepository;

    @Autowired
    private RestHighLevelClient client;

    private final Writer johnSmith = new Writer("John Smith");
    private final Writer johnDoe = new Writer("John Doe");

    @Before
    public void before() {
        Para para = new Para("Spring Data Elasticsearch");
        para.setWriters(asList(johnSmith, johnDoe));
        para.setTags("elasticsearch", "spring data");
        paraRepository.save(para);

        para = new Para("Search engines");
        para.setWriters(asList(johnDoe));
        para.setTags("search engines", "tutorial");
        paraRepository.save(para);

        para = new Para("Second Article About Elasticsearch");
        para.setWriters(asList(johnSmith));
        para.setTags("elasticsearch", "spring data");
        paraRepository.save(para);

        para = new Para("Elasticsearch Tutorial");
        para.setWriters(asList(johnDoe));
        para.setTags("elasticsearch");
        paraRepository.save(para);
    }

    @After
    public void after() {
    	paraRepository.deleteAll();
    }

    @Test
    public void givenFullTitle_whenRunMatchQuery_thenDocIsFound() {
        final NativeSearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchQuery("title", "Search engines").operator(Operator.AND))
            .build();
        final SearchHits<Para> paras = elasticsearchTemplate.search(searchQuery, Para.class, IndexCoordinates.of("blog"));
        assertEquals(1, paras.getTotalHits());
    }

    @Test
    public void givenOneTermFromTitle_whenRunMatchQuery_thenDocIsFound() {
        final NativeSearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchQuery("title", "Engines Solutions"))
            .build();

        final SearchHits<Para> paras = elasticsearchTemplate.search(searchQuery, Para.class, IndexCoordinates.of("blog"));

        assertEquals(1, paras.getTotalHits());
        assertEquals("Search engines", paras.getSearchHit(0)
            .getContent()
            .getTitle());
    }

    @Test
    public void givenPartTitle_whenRunMatchQuery_thenDocIsFound() {
        final NativeSearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchQuery("title", "elasticsearch data"))
            .build();

        final SearchHits<Para> articles = elasticsearchTemplate.search(searchQuery, Para.class, IndexCoordinates.of("blog"));

        assertEquals(3, articles.getTotalHits());
    }

    @Test
    public void givenFullTitle_whenRunMatchQueryOnVerbatimField_thenDocIsFound() {
        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchQuery("title.verbatim", "Second Article About Elasticsearch"))
            .build();

        SearchHits<Para> paras = elasticsearchTemplate.search(searchQuery, Para.class, IndexCoordinates.of("blog"));

        assertEquals(1, paras.getTotalHits());

        searchQuery = new NativeSearchQueryBuilder().withQuery(matchQuery("title.verbatim", "Second Para About"))
            .build();

        paras = elasticsearchTemplate.search(searchQuery, Para.class, IndexCoordinates.of("blog"));
        assertEquals(0, paras.getTotalHits());
    }

    @Test
    public void givenNestedObject_whenQueryByAuthorsName_thenFoundArticlesByThatAuthor() {
        final QueryBuilder builder = nestedQuery("writers", boolQuery().must(termQuery("writers.name", "smith")), ScoreMode.None);

        final NativeSearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(builder)
            .build();
        final SearchHits<Para> paras = elasticsearchTemplate.search(searchQuery, Para.class, IndexCoordinates.of("blog"));

        assertEquals(2, paras.getTotalHits());
    }

    @Test
    public void givenAnalyzedQuery_whenMakeAggregationOnTermCount_thenEachTokenCountsSeparately() throws Exception {
        final TermsAggregationBuilder aggregation = AggregationBuilders.terms("top_tags")
            .field("title");

        final SearchSourceBuilder builder = new SearchSourceBuilder().aggregation(aggregation);
        final SearchRequest searchRequest = new SearchRequest("blog").source(builder);

        final SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);

        final Map<String, Aggregation> results = response.getAggregations()
            .asMap();
        final ParsedStringTerms topTags = (ParsedStringTerms) results.get("top_tags");

        final List<String> keys = topTags.getBuckets().stream()
            .map(MultiBucketsAggregation.Bucket::getKeyAsString)
            .sorted()
            .collect(toList());
        assertEquals(asList("about", "para", "data", "elasticsearch", "engines", "search", "second", "spring", "tutorial"), keys);
    }

    @Test
    public void givenNotAnalyzedQuery_whenMakeAggregationOnTermCount_thenEachTermCountsIndividually() throws Exception {
        final TermsAggregationBuilder aggregation = AggregationBuilders.terms("top_tags")
            .field("tags")
            .order(BucketOrder.count(false));

        final SearchSourceBuilder builder = new SearchSourceBuilder().aggregation(aggregation);
        final SearchRequest searchRequest = new SearchRequest().indices("blog")
            .source(builder);

        final SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);

        final Map<String, Aggregation> results = response.getAggregations()
            .asMap();
        final ParsedStringTerms topTags = (ParsedStringTerms) results.get("top_tags");

        final List<String> keys = topTags.getBuckets()
            .stream()
            .map(MultiBucketsAggregation.Bucket::getKeyAsString)
            .collect(toList());
        assertEquals(asList("elasticsearch", "spring data", "search engines", "tutorial"), keys);
    }

    @Test
    public void givenNotExactPhrase_whenUseSlop_thenQueryMatches() {
        final NativeSearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchPhraseQuery("title", "spring elasticsearch").slop(1))
            .build();

        final SearchHits<Para> paras = elasticsearchTemplate.search(searchQuery, Para.class, IndexCoordinates.of("blog"));

        assertEquals(1, paras.getTotalHits());
    }

    @Test
    public void givenPhraseWithType_whenUseFuzziness_thenQueryMatches() {
        final NativeSearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchQuery("title", "spring date elasticserch").operator(Operator.AND)
            .fuzziness(Fuzziness.ONE)
            .prefixLength(3))
            .build();

        final SearchHits<Para> paras = elasticsearchTemplate.search(searchQuery, Para.class, IndexCoordinates.of("blog"));

        assertEquals(1, paras.getTotalHits());
    }

    @Test
    public void givenMultimatchQuery_whenDoSearch_thenAllProvidedFieldsMatch() {
        final NativeSearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(multiMatchQuery("tutorial").field("title")
            .field("tags")
            .type(MultiMatchQueryBuilder.Type.BEST_FIELDS))
            .build();

        final SearchHits<Para> paras = elasticsearchTemplate.search(searchQuery, Para.class, IndexCoordinates.of("blog"));

        assertEquals(2, paras.getTotalHits());
    }

    @Test
    public void givenBoolQuery_whenQueryByAuthorsName_thenFoundArticlesByThatAuthorAndFilteredTag() {
        final QueryBuilder builder = boolQuery().must(nestedQuery("authors", boolQuery().must(termQuery("authors.name", "doe")), ScoreMode.None))
            .filter(termQuery("tags", "elasticsearch"));

        final NativeSearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(builder)
            .build();
        final SearchHits<Para> paras = elasticsearchTemplate.search(searchQuery, Para.class, IndexCoordinates.of("blog"));

        assertEquals(2, paras.getTotalHits());
    }
}