package elasticsearchclient.java.files.search.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import elasticsearchclient.java.files.search.design.Para;



@Repository
public interface ParaRepository extends ElasticsearchRepository<Para, String> {

    Page<Para> findByWritersName(String name, Pageable pageable);

    @Query("{\"bool\": {\"must\": [{\"match\": {\"writers.name\": \"?0\"}}]}}")
    Page<Para> findByWritersNameUsingCustomQuery(String name, Pageable pageable);

    @Query("{\"bool\": {\"must\": {\"match_all\": {}}, \"filter\": {\"term\": {\"tags\": \"?0\" }}}}")
    Page<Para> findByFilteredTagQuery(String tag, Pageable pageable);

    @Query("{\"bool\": {\"must\": {\"match\": {\"writers.name\": \"?0\"}}, \"filter\": {\"term\": {\"tags\": \"?1\" }}}}")
    Page<Para> findByWritersNameAndFilteredTagQuery(String name, String tag, Pageable pageable);
}