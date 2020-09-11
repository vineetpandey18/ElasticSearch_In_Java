package com.elastico.java.files.search;

import static java.util.Arrays.asList;
import static org.elasticsearch.index.query.Operator.AND;
import static org.elasticsearch.index.query.QueryBuilders.fuzzyQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static org.elasticsearch.index.query.QueryBuilders.regexpQuery;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import elasticsearchclient.java.files.search.config.Configurations;
import elasticsearchclient.java.files.search.design.Para;
import elasticsearchclient.java.files.search.design.Writer;
import elasticsearchclient.java.files.search.repository.ParaRepository;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = Configurations.class)
public class esManualTest {

    @Autowired
    private ElasticsearchRestTemplate elasticsearchTemplate;

    @Autowired
    private ParaRepository paraRepository;

    private final Writer johnSmith = new Writer("John Smith");
    private final Writer johnDoe = new Writer("John Doe");

    @Before
    public void before() {
    	Para article = new Para("Spring Data Elasticsearch");
        article.setWriters(asList(johnSmith, johnDoe));
        article.setTags("elasticsearch", "spring data");
        paraRepository.save(article);

        article = new Para("Search engines");
        article.setWriters(asList(johnDoe));
        article.setTags("search engines", "tutorial");
        paraRepository.save(article);

        article = new Para("Second Article About Elasticsearch");
        article.setWriters(asList(johnSmith));
        article.setTags("elasticsearch", "spring data");
        paraRepository.save(article);

        article = new Para("Elasticsearch Tutorial");
        article.setWriters(asList(johnDoe));
        article.setTags("elasticsearch");
        paraRepository.save(article);
    }

    @After
    public void after() {
    	paraRepository.deleteAll();
    }

    @Test
    public void givenParaService_whenSavePara_thenIdIsAssigned() {
        final List<Writer> authors = asList(new Writer("John Smith"), johnDoe);

        Para article = new Para("Making Search Elastic");
        article.setWriters(authors);

        article = paraRepository.save(article);
        assertNotNull(article.getId());
    }

    @Test
    public void givenPersistedParas_whenSearchByWritersName_thenRightFound() {
        final Page<Para> articleByAuthorName = paraRepository.findByWritersName(johnSmith.getName(), PageRequest.of(0, 10));
        assertEquals(2L, articleByAuthorName.getTotalElements());
    }

    @Test
    public void givenCustomQuery_whenSearchByWritersName_thenParaIsFound() {
        final Page<Para> articleByAuthorName = paraRepository.findByWritersNameUsingCustomQuery("Smith", PageRequest.of(0, 10));
        assertEquals(2L, articleByAuthorName.getTotalElements());
    }

    @Test
    public void givenTagFilterQuery_whenSearchByTag_thenParaIsFound() {
        final Page<Para> paraByWriterName = paraRepository.findByFilteredTagQuery("elasticsearch", PageRequest.of(0, 10));
        assertEquals(3L, paraByWriterName.getTotalElements());
    }

    @Test
    public void givenTagFilterQuery_whenSearchByWritersName_thenParaIsFound() {
        final Page<Para> paraByWriterName = paraRepository.findByWritersNameAndFilteredTagQuery("Doe", "elasticsearch", PageRequest.of(0, 10));
        assertEquals(2L, paraByWriterName.getTotalElements());
    }

    @Test
    public void givenPersistedParas_whenUseRegexQuery_thenRightParasFound() {
        final Query searchQuery = new NativeSearchQueryBuilder().withFilter(regexpQuery("title", ".*data.*"))
            .build();

        final SearchHits<Para> paras = elasticsearchTemplate.search(searchQuery, Para.class, IndexCoordinates.of("blog"));

        assertEquals(1, paras.getTotalHits());
    }

    @Test
    public void givenSavedDoc_whenTitleUpdated_thenCouldFindByUpdatedTitle() {
        final Query searchQuery = new NativeSearchQueryBuilder().withQuery(fuzzyQuery("title", "serch"))
            .build();
        final SearchHits<Para> paras = elasticsearchTemplate.search(searchQuery, Para.class, IndexCoordinates.of("blog"));

        assertEquals(1, paras.getTotalHits());

        final Para para = paras.getSearchHit(0)
            .getContent();
        final String newTitle = "Getting started with Search Engines";
        para.setTitle(newTitle);
        paraRepository.save(para);

        assertEquals(newTitle, paraRepository.findById(para.getId())
            .get()
            .getTitle());
    }

    @Test
    public void givenSavedDoc_whenDelete_thenRemovedFromIndex() {
        final String paraTitle = "Spring Data Elasticsearch";

        final Query searchQuery = new NativeSearchQueryBuilder().withQuery(matchQuery("title", paraTitle).minimumShouldMatch("75%"))
            .build();
        final SearchHits<Para> paras = elasticsearchTemplate.search(searchQuery, Para.class, IndexCoordinates.of("blog"));

        assertEquals(1, paras.getTotalHits());
        final long count = paraRepository.count();

        paraRepository.delete(paras.getSearchHit(0)
            .getContent());

        assertEquals(count - 1, paraRepository.count());
    }

    @Test
    public void givenSavedDoc_whenOneTermMatches_thenFindByTitle() {
        final Query searchQuery = new NativeSearchQueryBuilder().withQuery(matchQuery("title", "Search engines").operator(AND))
            .build();
        final SearchHits<Para> paras = elasticsearchTemplate.search(searchQuery, Para.class, IndexCoordinates.of("blog"));
        assertEquals(1, paras.getTotalHits());
    }
}