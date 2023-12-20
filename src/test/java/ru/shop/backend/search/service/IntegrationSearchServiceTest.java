package ru.shop.backend.search.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.shop.backend.search.model.CatalogueElastic;
import ru.shop.backend.search.model.ItemElastic;
import ru.shop.backend.search.model.SearchResultElastic;
import ru.shop.backend.search.repository.ItemElasticRepository;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@AutoConfigureTestDatabase
//Больше не успел
public class IntegrationSearchServiceTest {

    @Autowired
    private ItemElasticRepository itemElasticRepository;
    @Autowired
    private ElasticsearchRestTemplate elasticTemplate;
    @Autowired
    private SearchService searchService;

    @AfterEach
    public void tearDown() {
        IndexOperations indexOperations = elasticTemplate.indexOps(ItemElastic.class);
        if (indexOperations.exists()) {
            indexOperations.delete();
        }
    }

    @BeforeEach
    public void tearUp() {
        IndexOperations indexOperations = elasticTemplate.indexOps(ItemElastic.class);
        if (!indexOperations.exists()) {
            indexOperations.create();
        }

    }

    @Test
    public void should_return_cats_if_brand_and_type_exist() {
        ItemElastic itemElastic = new ItemElastic(
                "name1",
                "full",
                1L,
                1L,
                "catalogue",
                "brand",
                "type",
                "descr"
        );

        ItemElastic itemElastic2 = new ItemElastic(
                "noname22",
                "full",
                2L,
                1L,
                "catalogue",
                itemElastic.getBrand(),
                "type223",
                "descr"
        );

        ItemElastic itemElastic3 = new ItemElastic(
                "name33",
                "full",
                3L,
                3L,
                "catalogue",
                itemElastic.getBrand(),
                itemElastic.getType(),
                "descr"
        );

        itemElasticRepository.save(itemElastic);
        itemElasticRepository.save(itemElastic2);
        itemElasticRepository.save(itemElastic3);

        SearchResultElastic result = searchService.prepareResult(
                String.format("%s %s %s", "name", itemElastic.getBrand(), itemElastic.getType()));

        assertThat(result.getResult())
                .hasSize(1)
                .satisfies(res -> {
                    assertThat(res.get(0).getItems())
                            .hasSize(2);
                    assertThat(res.get(0).getItems())
                            .contains(itemElastic, itemElastic3);
                });
    }

    @Test
    public void should_return_cats_if_brand_exists() {
        ItemElastic itemElastic = new ItemElastic(
                "name1",
                "full",
                1L,
                1L,
                "catalogue",
                "brand",
                "type",
                "descr"
        );

        ItemElastic itemElastic2 = new ItemElastic(
                "noname22",
                "full",
                2L,
                1L,
                "catalogue",
                itemElastic.getBrand(),
                "type223",
                "descr"
        );

        ItemElastic itemElastic3 = new ItemElastic(
                "name33212121",
                "full",
                3L,
                1L,
                "catalogue",
                itemElastic.getBrand(),
                itemElastic.getType(),
                "name3"
        );

        itemElasticRepository.save(itemElastic);
        itemElasticRepository.save(itemElastic2);
        itemElasticRepository.save(itemElastic3);

        SearchResultElastic result = searchService.prepareResult(
                String.format("%s %s", "name", itemElastic.getBrand()));

        assertThat(result.getResult())
                .hasSize(1)
                .satisfies(res -> {
                    assertThat(res.get(0).getItems())
                            .hasSize(2);
                    assertThat(res.get(0).getItems())
                            .contains(itemElastic, itemElastic3);
                });
    }

}
