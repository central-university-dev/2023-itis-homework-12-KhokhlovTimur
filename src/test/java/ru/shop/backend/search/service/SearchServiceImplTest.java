package ru.shop.backend.search.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import ru.shop.backend.search.model.*;
import ru.shop.backend.search.repository.ItemElasticRepository;
import ru.shop.backend.search.repository.ItemJpaRepository;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;
import static ru.shop.backend.search.service.util.SearchStringUtils.convert;
import static ru.shop.backend.search.service.util.SearchStringUtils.isNumeric;

@SpringBootTest
@AutoConfigureTestDatabase
class SearchServiceImplTest {

    @MockBean
    private ItemElasticRepository itemElasticRepository;
    @MockBean
    private ItemJpaRepository itemJpaRepository;
    @Autowired
    private SearchServiceImpl searchServiceImpl;

    @Test
    public void should_return_true_if_numeric() {
        String str = "11";
        assertThat(isNumeric(str))
                .isTrue();
    }

    @Test
    public void should_return_false_if_null() {
        String str = null;
        assertThat(isNumeric(str))
                .isFalse();
    }

    @Test
    public void should_return_false_if_not_numeric() {
        String str = "11a";
        assertThat(isNumeric(str))
                .isFalse();
    }

    @Test
    public void should_convert_from_en_to_ru() {
        String base = "qwe";
        String expected = "йцу";
        assertThat(convert(base))
                .isEqualTo(expected);
    }

    @Test
    public void should_convert_from_ru_to_en() {
        String base = "йцу";
        String expected = "qwe";
        assertThat(convert(base))
                .isEqualTo(expected);
    }

    @Test
    public void should_return_catalogue_by_item_id() {
        List<ItemElastic> items = List.of(itemElastic1);
        doReturn(items)
                .when(itemElasticRepository)
                .findByItemId(any(), any());

        List<CatalogueElastic> expected = List.of(new CatalogueElastic(
                itemElastic1.getCatalogue(),
                itemElastic1.getCatalogueId(),
                items,
                itemElastic1.getBrand()
        ));

        assertThat(searchServiceImpl.getByItemId(itemElastic1.getItemId()))
                .hasSize(1)
                .isEqualTo(expected);

        verify(itemElasticRepository)
                .findByItemId(
                        argThat(arg -> Objects.equals(arg, itemElastic1.getItemId())),
                        any(PageRequest.class));
    }

    @Test
    public void should_return_catalogue_by_item_name() {
        List<ItemElastic> items = List.of(itemElastic1);
        doReturn(items)
                .when(itemElasticRepository)
                .findAllByName(any(), any());

        List<CatalogueElastic> expected = List.of(new CatalogueElastic(
                itemElastic1.getCatalogue(),
                itemElastic1.getCatalogueId(),
                items,
                null
        ));

        assertThat(searchServiceImpl.getByItemName(itemElastic1.getName()))
                .hasSize(1)
                .isEqualTo(expected);

        verify(itemElasticRepository)
                .findAllByName(
                        argThat(arg -> Objects.equals(arg, itemElastic1.getName() + ".*")),
                        any(Pageable.class));
    }

    @Test
    public void should_return_catalogues_with_empty_items_if_brand_exists() {
        String query = itemElastic1.getBrand() + " " + itemElastic1.getType();
        doReturn(List.of(itemElastic1))
                .when(itemElasticRepository)
                .findAllByBrand(any(), any());

        doReturn(List.of(itemElastic1))
                .when(itemElasticRepository)
                .findAllByType(any(), any());

        assertThat(searchServiceImpl.getAllCatalogues(query))
                .isEqualTo(List.of(new CatalogueElastic(
                        itemElastic1.getCatalogue(),
                        itemElastic1.getCatalogueId(),
                        null,
                        itemElastic1.getBrand()
                )));

        verify(itemElasticRepository)
                .findAllByBrand(
                        argThat(arg -> arg.equals(itemElastic1.getBrand())),
                        any());
        verify(itemElasticRepository, atLeast(1))
                .findAllByType(
                        any(),
                        any());
    }

    @Test
    public void should_return_catalogues_if_brand_not_exists_and_catalogue_exists() {
        String query = itemElastic1.getType() +
                " " + itemElastic1.getCatalogue() + " " + itemElastic1.getName();

        doReturn(Collections.emptyList())
                .when(itemElasticRepository)
                .findAllByBrand(any(), any());

        doReturn(Collections.emptyList())
                .when(itemElasticRepository)
                .findAllByType(any(), any());

        doReturn(List.of(itemElastic1, itemElastic2))
                .when(itemElasticRepository)
                .findByCatalogue(any(), any());

        doReturn(List.of(itemElastic1))
                .when(itemElasticRepository)
                .findByCatalogueId(any(), any(), any());

        assertThat(searchServiceImpl.getAllCatalogues(query))
                .isEqualTo(List.of(new CatalogueElastic(
                        itemElastic1.getCatalogue(),
                        itemElastic1.getCatalogueId(),
                        List.of(itemElastic1),
                        null
                )));

        verify(itemElasticRepository, atLeast(1))
                .findAllByBrand(
                        any(String.class),
                        any(Pageable.class));

        verify(itemElasticRepository, atLeast(1))
                .findAllByType(
                        any(String.class),
                        any(Pageable.class));

        verify(itemElasticRepository)
                .findByCatalogue(
                        any(String.class),
                        any(Pageable.class));

        verify(itemElasticRepository)
                .findByCatalogueId(
                        any(String.class),
                        argThat(arg -> arg.equals(itemElastic1.getCatalogueId())),
                        any(Pageable.class));
    }

    @Test
    public void should_return_catalogues_if_brand_and_catalogue_not_exists() {
        String query = itemElastic1.getType() +
                " " + itemElastic1.getName();

        doReturn(Collections.emptyList())
                .when(itemElasticRepository)
                .findAllByBrand(any(), any());

        doReturn(Collections.emptyList())
                .when(itemElasticRepository)
                .findAllByType(any(), any());

        doReturn(Collections.emptyList())
                .when(itemElasticRepository)
                .findByCatalogue(any(), any());

        doReturn(List.of(itemElastic1, itemElastic2))
                .when(itemElasticRepository)
                .findAllByType(any(), any(), any());

        assertThat(searchServiceImpl.getAllCatalogues(query))
                .isEqualTo(List.of(new CatalogueElastic(
                        itemElastic1.getCatalogue(),
                        itemElastic1.getCatalogueId(),
                        List.of(itemElastic1),
                        null
                )));

        verify(itemElasticRepository, atLeast(1))
                .findAllByBrand(
                        any(String.class),
                        any(Pageable.class));

        verify(itemElasticRepository, atLeast(1))
                .findAllByType(
                        any(String.class),
                        any(Pageable.class));

        verify(itemElasticRepository, atLeast(1))
                .findByCatalogue(
                        any(String.class),
                        any(Pageable.class));

        verify(itemElasticRepository)
                .findAllByType(
                        any(String.class),
                        argThat(arg -> arg.equals("?")),
                        any(Pageable.class));
    }

    @Test
    public void should_return_catalogues_if_brand_and_type_and_name_exists() {
        String query = itemElastic1.getBrand() + " " + itemElastic1.getType()
                + " " + itemElastic1.getName();

        doReturn(List.of(itemElastic1))
                .when(itemElasticRepository)
                .findAllByBrand(argThat(arg -> arg.equals(itemElastic1.getBrand())), any());

        doReturn(List.of(itemElastic1, itemElastic2))
                .when(itemElasticRepository)
                .findAllByType(any(), any());

        doReturn(List.of(itemElastic1))
                .when(itemElasticRepository)
                .findAllByTypeAndBrand(any(), any(), any(), any());

        assertThat(searchServiceImpl.getAllCatalogues(query))
                .isEqualTo(List.of(new CatalogueElastic(
                        itemElastic1.getCatalogue(),
                        itemElastic1.getCatalogueId(),
                        List.of(itemElastic1),
                        itemElastic1.getBrand()
                )));

        verify(itemElasticRepository, atLeast(1))
                .findAllByBrand(
                        argThat(arg -> arg.equals(itemElastic1.getBrand())),
                        any(Pageable.class));

        verify(itemElasticRepository, atLeast(1))
                .findAllByType(
                        argThat(arg -> arg.equals(itemElastic1.getType() + " " + itemElastic1.getName())),
                        any(Pageable.class));

        verify(itemElasticRepository, atLeast(1))
                .findAllByTypeAndBrand(
                        any(String.class),
                        argThat(arg -> arg.equals(itemElastic1.getBrand())),
                        argThat(arg -> arg.equals(itemElastic1.getType())),
                        any(Pageable.class));
    }

    @Test
    public void should_return_catalogues_if_brand_exists_and_type_not_exists() {
        String query = itemElastic1.getBrand() + " "
                + itemElastic1.getName();

        doReturn(List.of(itemElastic1))
                .when(itemElasticRepository)
                .findAllByBrand(argThat(arg -> arg.equals(itemElastic1.getBrand())), any());

        doReturn(Collections.emptyList())
                .when(itemElasticRepository)
                .findAllByType(any(), any());

        doReturn(List.of(itemElastic1, itemElastic2))
                .when(itemElasticRepository)
                .findAllByBrand(any(), any(), any());

        assertThat(searchServiceImpl.getAllCatalogues(query))
                .isEqualTo(List.of(new CatalogueElastic(
                        itemElastic1.getCatalogue(),
                        itemElastic1.getCatalogueId(),
                        List.of(itemElastic1),
                        itemElastic1.getBrand()
                )));

        verify(itemElasticRepository, atLeast(1))
                .findAllByBrand(
                        argThat(arg -> arg.equals(itemElastic1.getBrand())),
                        any(Pageable.class));

        verify(itemElasticRepository, atLeast(1))
                .findAllByType(
                        argThat(arg -> arg.equals(itemElastic1.getName())),
                        any(Pageable.class));

        verify(itemElasticRepository, atLeast(1))
                .findAllByBrand(
                        any(String.class),
                        argThat(arg -> arg.equals(itemElastic1.getBrand())),
                        any(Pageable.class));
    }

    @Test
    public void should_return_catalogues_if_all_parameters_not_exists() {
        String query = itemElastic1.getName() + " " + itemElastic1.getType();

        doReturn(Collections.emptyList())
                .when(itemElasticRepository)
                .findAllByBrand(any(), any());

        doReturn(Collections.emptyList())
                .when(itemElasticRepository)
                .findAllByType(any(), any());

        doReturn(Collections.emptyList())
                .when(itemElasticRepository)
                .findByCatalogue(any(), any());

        doReturn(Collections.emptyList())
                .when(itemElasticRepository)
                .findAllByType(any(), any(), any());

        doReturn(List.of(itemElastic1, itemElastic3))
                .when(itemElasticRepository)
                .findAllNotStrong(any(), any());

        assertThat(searchServiceImpl.getAllCatalogues(query))
                .isEqualTo(List.of(new CatalogueElastic(
                        itemElastic1.getCatalogue(),
                        itemElastic1.getCatalogueId(),
                        List.of(itemElastic1, itemElastic3),
                        null
                )));

        verify(itemElasticRepository, atLeast(1))
                .findAllByBrand(
                        any(String.class),
                        any(Pageable.class));

        verify(itemElasticRepository, atLeast(1))
                .findAllByType(
                        any(String.class),
                        any(Pageable.class));

        verify(itemElasticRepository, atLeast(1))
                .findByCatalogue(
                        any(String.class),
                        any(Pageable.class));

        verify(itemElasticRepository, atLeast(1))
                .findAllByType(
                        any(String.class),
                        any(String.class),
                        any(Pageable.class));

        verify(itemElasticRepository, atLeast(1))
                .findAllNotStrong(
                        argThat(arg -> arg.equals(query + "?")),
                        any(Pageable.class));
    }

    @Test
    public void should_return_result_with_numeric_query() {
        String query = "111";

        doReturn(List.of(1, 2, 3))
                .when(itemJpaRepository)
                .findBySku(any());

        doReturn(List.of(itemElastic1))
                .when(itemElasticRepository)
                .findByItemId(any(), any());

        List<Object[]> items = Collections.singletonList(new Object[]{
                new BigInteger(item1.getItemId().toString()),
                item1.getName(),
                new BigInteger(item1.getPrice().toString()),
                item1.getUrl(),
                item1.getImage(),
                item1.getCat()
        });

        doReturn(items)
                .when(itemJpaRepository)
                .findByIds(any(), any());

        List<Object[]> cats = Collections.singletonList(new Object[]{
                category1.getName(),
                category1.getParentName(),
                "url1",
                "parent_url",
                category1.getImage(),
        });

        doReturn(cats)
                .when(itemJpaRepository)
                .findCatsByIds(any());

        List<Item> expectedItems = List.of(item1);
        List<Category> expectedCats = List.of(category1);


        SearchResult expected = new SearchResult(
                expectedItems,
                expectedCats,
                List.of(new TypeHelpText(
                        TypeOfQuery.SEE_ALSO,
                        itemElastic1.getType() + " " + itemElastic1.getBrand()
                ))
        );

        assertThat(searchServiceImpl.getSearchResult(1, query))
                .isEqualTo(expected);

        verify(itemJpaRepository, atLeast(1))
                .findBySku("111");

        verify(itemElasticRepository)
                .findByItemId(
                        argThat(arg -> arg.equals(itemElastic1.getItemId())),
                        any()
                );

        verify(itemJpaRepository, atLeast(1))
                .findByIds(
                        1,
                        List.of(itemElastic1.getItemId()));

        verify(itemJpaRepository)
                .findCatsByIds(
                        List.of(Math.toIntExact(itemElastic1.getItemId()))
                );
    }

    @Test
    public void should_return_result_with_non_numeric_query() {
        String query = itemElastic1.getBrand() + " "
                + itemElastic1.getName();

        doReturn(List.of(itemElastic1))
                .when(itemElasticRepository)
                .findAllByBrand(argThat(arg -> arg.equals(itemElastic1.getBrand())), any());

        doReturn(Collections.emptyList())
                .when(itemElasticRepository)
                .findAllByType(any(), any());

        doReturn(List.of(itemElastic1, itemElastic2))
                .when(itemElasticRepository)
                .findAllByBrand(any(), any(), any());

        List<Object[]> items = Collections.singletonList(new Object[]{
                new BigInteger(item1.getItemId().toString()),
                item1.getName(),
                new BigInteger(item1.getPrice().toString()),
                item1.getUrl(),
                item1.getImage(),
                item1.getCat()
        });

        doReturn(items)
                .when(itemJpaRepository)
                .findByIds(any(), any());

        List<Object[]> cats = Collections.singletonList(new Object[]{
                category1.getName(),
                category1.getParentName(),
                "url1",
                "parent_url",
                category1.getImage(),
        });

        doReturn(cats)
                .when(itemJpaRepository)
                .findCatsByIds(any());


        List<Item> expectedItems = List.of(item1);
        List<Category> expectedCats = List.of(category1);


        SearchResult expected = new SearchResult(
                expectedItems,
                expectedCats,
                List.of(new TypeHelpText(
                        TypeOfQuery.SEE_ALSO,
                        itemElastic1.getType() + " " + itemElastic1.getBrand()
                ))
        );

        assertThat(searchServiceImpl.getSearchResult(1, query))
                .isEqualTo(expected);

        verify(itemJpaRepository, atLeast(1))
                .findByIds(
                        1,
                        List.of(itemElastic1.getItemId()));

        verify(itemJpaRepository)
                .findCatsByIds(
                        List.of(Math.toIntExact(itemElastic1.getItemId()))
                );

        verify(itemElasticRepository, atLeast(1))
                .findAllByBrand(
                        argThat(arg -> arg.equals(itemElastic1.getBrand())),
                        any(Pageable.class));

        verify(itemElasticRepository, atLeast(1))
                .findAllByType(
                        argThat(arg -> arg.equals(itemElastic1.getName())),
                        any(Pageable.class));

        verify(itemElasticRepository, atLeast(1))
                .findAllByBrand(
                        any(String.class),
                        argThat(arg -> arg.equals(itemElastic1.getBrand())),
                        any(Pageable.class));
    }


    @Test
    public void should_return_result_if_text_non_numeric() {
        String query =  " " + itemElastic1.getCatalogue();

        doReturn(Collections.emptyList())
                .when(itemElasticRepository)
                .findAllByBrand(any(), any());

        doReturn(Collections.emptyList())
                .when(itemElasticRepository)
                .findAllByType(any(), any());

        doReturn(List.of(itemElastic1))
                .when(itemElasticRepository)
                .findByCatalogue(any(), any());

        doReturn(List.of(itemElastic1))
                .when(itemElasticRepository)
                .findByCatalogueId(any(), any(), any());

        assertThat(searchServiceImpl.prepareResult(query))
                .isEqualTo(new SearchResultElastic(
                        List.of(new CatalogueElastic(
                                itemElastic1.getCatalogue(),
                                itemElastic1.getCatalogueId(),
                                List.of(itemElastic1),
                                null))
                ));

        verify(itemElasticRepository)
                .findAllByBrand(
                        argThat(arg -> arg.equals(itemElastic1.getCatalogue())),
                        any());
        verify(itemElasticRepository, atLeast(1))
                .findAllByType(
                        any(),
                        any());
        verify(itemElasticRepository)
                .findByCatalogue(
                        any(String.class),
                        any());
        verify(itemElasticRepository)
                .findByCatalogueId(
                        any(),
                        any(),
                        any());
    }

    @Test
    public void should_return_result_if_text_numeric() {
        String query = "111";

        doReturn(List.of(1))
                .when(itemJpaRepository)
                .findBySku(any());

        doReturn(List.of(itemElastic1))
                .when(itemElasticRepository)
                .findByItemId(any(), any());

        assertThat(searchServiceImpl.prepareResult(query))
                .isEqualTo(new SearchResultElastic(
                        List.of(new CatalogueElastic(
                                itemElastic1.getCatalogue(),
                                itemElastic1.getCatalogueId(),
                                List.of(itemElastic1),
                                itemElastic1.getBrand()))
                ));

        verify(itemJpaRepository)
                .findBySku(
                        query);

        verify(itemElasticRepository)
                .findByItemId(
                        argThat(arg -> arg.equals(itemElastic1.getItemId())),
                        any());
    }

    @Test
    public void should_return_result_if_text_numeric_and_item_id_not_exists() {
        ItemElastic itemElastic1 = new ItemElastic(
                "111",
                "full",
                1L,
                1L,
                "cat",
                "brand",
                "type",
                "descr"
        );
        String query = itemElastic1.getName();

        doReturn(Collections.emptyList())
                .when(itemJpaRepository)
                .findBySku(any());

        doReturn(List.of(itemElastic1))
                .when(itemElasticRepository)
                .findAllByName(any(), any());

        assertThat(searchServiceImpl.prepareResult(query))
                .isEqualTo(new SearchResultElastic(
                        List.of(new CatalogueElastic(
                                itemElastic1.getCatalogue(),
                                itemElastic1.getCatalogueId(),
                                List.of(itemElastic1),
                                null))
                ));

        verify(itemJpaRepository)
                .findBySku(
                        query);

        verify(itemElasticRepository)
                .findAllByName(
                        argThat(arg -> arg.equals(query + ".*")),
                        any()
                );
    }


    private final ItemElastic itemElastic1 = new ItemElastic(
            "name",
            "full",
            1L,
            1L,
            "cat",
            "brand",
            "type",
            "descr"
    );

    private final ItemElastic itemElastic2 = new ItemElastic(
            "name2",
            "full2",
            2L,
            2L,
            "cat2",
            "brand2",
            "type2",
            "descr2"
    );

    private final ItemElastic itemElastic3 = new ItemElastic(
            itemElastic1.getName(),
            "full3",
            3L,
            3L,
            itemElastic1.getCatalogue(),
            "brand3",
            itemElastic1.getType(),
            "descr3"
    );

    private final Category category1 = new Category(
            "name1",
            "parent_name",
            "/cat/url1/brands/brand",
            "/cat/parent_url",
            "image1"
    );

    private final Item item1 = new Item(
            15,
            "name1",
            "url1",
            "image1",
            1,
            category1.getName()
    );

}