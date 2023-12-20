package ru.shop.backend.search.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.shop.backend.search.model.ItemElastic;
import ru.shop.backend.search.repository.ItemJpaRepository;
import ru.shop.backend.search.repository.ItemElasticRepository;

import javax.transaction.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReindexSearchService {
    private final ItemJpaRepository itemJpaRepository;
    private final ItemElasticRepository itemElasticRepository;

    @Scheduled(fixedDelay = 43200000)
    @Transactional
    public void reindex() {
        log.info("генерация индексов по товарам запущена");

        itemJpaRepository
                .findAllInStream()
                .parallel()
                .map(ItemElastic::new)
                .forEach(itemElasticRepository::save);

        log.info("генерация индексов по товарам закончилась");

    }
}
