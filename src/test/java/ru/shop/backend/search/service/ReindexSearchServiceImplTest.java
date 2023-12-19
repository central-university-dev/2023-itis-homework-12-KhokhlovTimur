package ru.shop.backend.search.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.shop.backend.search.model.ItemElastic;
import ru.shop.backend.search.model.ItemEntity;
import ru.shop.backend.search.repository.ItemElasticRepository;
import ru.shop.backend.search.repository.ItemJpaRepository;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReindexSearchServiceImplTest {

    @Mock
    private ItemJpaRepository itemJpaRepository;
    @Mock
    private ItemElasticRepository itemElasticRepository;
    @InjectMocks
    private ReindexSearchService reindexSearchService;

    @Test
    public void should_invoke_methods_in_repos() {
        ItemEntity item1 = new ItemEntity(
                "Name1",
                "Brand1",
                "Catalogue1",
                "Type1",
                "Description1",
                1,
                1,
                1);
        ItemEntity item2 = new ItemEntity("Name2",
                "Brand2",
                "Catalogue2",
                "Type2",
                "Description2",
                2,
                2,
                2);

        Stream<ItemEntity> stream = Stream.of(item1, item2);

        doReturn(stream)
                .when(itemJpaRepository)
                .findAllInStream();

        reindexSearchService.reindex();

        verify(itemJpaRepository)
                .findAllInStream();

        verify(itemElasticRepository)
                .save(argThat(arg -> arg.getItemId().equals(item1.getItemId())));

        verify(itemElasticRepository)
                .save(argThat(arg -> arg.getItemId().equals(item2.getItemId())));
    }

}