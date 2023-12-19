package ru.shop.backend.search.repository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.shop.backend.search.model.ItemElastic;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

//тест контейнеры не поднимались, пришлось их запускать через композ
@SpringBootTest
@Testcontainers
@AutoConfigureTestDatabase
public class ItemElasticRepositoryTest {

    @Autowired
    private ItemElasticRepository itemElasticRepository;

    @Autowired
    private ElasticsearchRestTemplate elasticTemplate;

//    @BeforeAll
//    static void setUp() {
//        postgresContainer.start();
//        elasticsearchContainer.start();
//    }
    //    @Container
//    private static final GenericContainer<?> elasticsearchContainer =
//            new GenericContainer<>("elasticsearch:7.17.4")
//                    .withExposedPorts(9200)
//                    .withEnv("discovery.type", "single-node")
//                    .withEnv("ELASTICSEARCH_USERNAME", System.getenv("ELASTICSEARCH_USERNAME"))
//                    .withEnv("ELASTICSEARCH_PASSWORD", System.getenv("ELASTICSEARCH_PASSWORD"))
//                    .withReuse(true);
//
//    @Container
//    private static final GenericContainer<?> postgresContainer =
//            new GenericContainer<>("postgres:latest")
//                    .withExposedPorts(5432)
//                    .withEnv("POSTGRES_DB", System.getenv("POSTGRES_DB"))
//                    .withEnv("POSTGRES_USER", System.getenv("POSTGRES_USER"))
//                    .withEnv("POSTGRES_PASSWORD", System.getenv("POSTGRES_PASSWORD"))
//                    .withReuse(true);
//
//    @DynamicPropertySource
//    static void dynamicProperties(DynamicPropertyRegistry registry) {
//        registry.add("spring.elasticsearch.rest.uris", () ->
//                "127.0.0.1:9200"
//        );
//        registry.add("spring.datasource.url", () ->
//                "jdbc:postgresql://" + postgresContainer.getContainerIpAddress() +
//                        ":" + postgresContainer.getMappedPort(5432) +
//                        "/" + System.getenv("POSTGRES_DB"));
//        registry.add("spring.datasource.username", () -> System.getenv("POSTGRES_USER"));
//        registry.add("spring.datasource.password", () -> System.getenv("POSTGRES_PASSWORD"));
//    }

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
    public void should_find_by_type() {
        ItemElastic itemElastic = new ItemElastic(
                "name",
                "full",
                1L,
                1L,
                "catalogue",
                "brand",
                "type",
                "descr"
        );

        ItemElastic itemElastic2 = new ItemElastic(
                "name",
                "full",
                2L,
                1L,
                "catalogue",
                "brand",
                "type",
                "descr"
        );

        itemElasticRepository.save(itemElastic);
        itemElasticRepository.save(itemElastic2);

        List<ItemElastic> allByType = itemElasticRepository.findAllByType(
                itemElastic3.getType() + " " + itemElastic3.getName(),
                PageRequest.of(0, 150));

        assertThat(allByType)
                .hasSize(2);
    }

    @Test
    public void should_find_all_by_brand() {
        ItemElastic itemElastic = new ItemElastic(
                "name",
                "full",
                1L,
                1L,
                "catalogue",
                "brand",
                "type",
                "descr"
        );

        ItemElastic itemElastic2 = new ItemElastic(
                "name",
                "full",
                2L,
                1L,
                "catalogue",
                "brand2",
                "type",
                "descr"
        );

        ItemElastic itemElastic3 = new ItemElastic(
                "name",
                "full",
                3L,
                1L,
                "catalogue",
                "brand",
                "type",
                "descr"
        );

        itemElasticRepository.save(itemElastic);
        itemElasticRepository.save(itemElastic2);
        itemElasticRepository.save(itemElastic3);

        List<ItemElastic> allByBrand = itemElasticRepository.findAllByBrand(
                itemElastic.getName(),
                itemElastic.getBrand(),
                PageRequest.of(0, 150));

        assertThat(allByBrand)
                .hasSize(2)
                .satisfies(items -> {
                    assertThat(items.contains(itemElastic)).isTrue();
                    assertThat(items.contains(itemElastic3)).isTrue();
                });
    }

    @Test
    public void should_find_all_by_type() {
        ItemElastic itemElastic = new ItemElastic(
                "sth",
                "full",
                1L,
                1L,
                "catalogue",
                "brand",
                "type1",
                "descr"
        );

        ItemElastic itemElastic2 = new ItemElastic(
                "name",
                "full",
                2L,
                1L,
                "catalogue",
                "brand2",
                "type2",
                "sth"
        );

        ItemElastic itemElastic3 = new ItemElastic(
                "name",
                "full",
                3L,
                1L,
                "catalogue",
                "brand",
                "type3",
                "descr"
        );

        itemElasticRepository.save(itemElastic);
        itemElasticRepository.save(itemElastic2);
        itemElasticRepository.save(itemElastic3);

        List<ItemElastic> allByType = itemElasticRepository.findAllByType(
                itemElastic.getName(),
                "type?",
                PageRequest.of(0, 150));

        assertThat(allByType)
                .hasSize(2)
                .satisfies(items -> {
                    assertThat(items.contains(itemElastic)).isTrue();
                    assertThat(items.contains(itemElastic2)).isTrue();
                });
    }

    @Test
    public void should_find_all_by_type_and_brand() {
        ItemElastic itemElastic = new ItemElastic(
                "sth",
                "full",
                1L,
                1L,
                "catalogue",
                "brand",
                "type",
                "descr"
        );

        ItemElastic itemElastic2 = new ItemElastic(
                "name",
                "full",
                2L,
                1L,
                "catalogue",
                "brand",
                "type",
                "sth2"
        );

        ItemElastic itemElastic3 = new ItemElastic(
                "name",
                "full",
                3L,
                1L,
                "catalogue",
                "brand",
                "type3",
                "descr"
        );

        itemElasticRepository.save(itemElastic);
        itemElasticRepository.save(itemElastic2);
        itemElasticRepository.save(itemElastic3);

        List<ItemElastic> allByType = itemElasticRepository.findAllByTypeAndBrand(
                itemElastic.getName(),
                itemElastic.getBrand(),
                "type",
                PageRequest.of(0, 150));

        assertThat(allByType)
                .hasSize(2)
                .satisfies(items -> {
                    assertThat(items.contains(itemElastic)).isTrue();
                    assertThat(items.contains(itemElastic2)).isTrue();
                });
    }

    private final ItemElastic itemElastic3 = new ItemElastic(
            "name",
            "full3",
            3L,
            3L,
            "catalogue",
            "brand3",
            "type",
            "descr3"
    );

}