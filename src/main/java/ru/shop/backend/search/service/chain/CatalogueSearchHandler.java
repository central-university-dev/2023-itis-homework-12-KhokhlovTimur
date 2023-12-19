package ru.shop.backend.search.service.chain;

import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import ru.shop.backend.search.model.ItemElastic;
import ru.shop.backend.search.repository.ItemElasticRepository;
import ru.shop.backend.search.service.model.SearchRequest;

import java.util.List;

import static ru.shop.backend.search.service.util.SearchStringUtils.convert;

@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CatalogueSearchHandler implements SearchHandler {
    private final ItemElasticRepository itemElasticRepository;

    @Override
    public void handle(SearchRequest searchRequest) {
        List<ItemElastic> items = searchRequest.getItems();
        Long catalogueId = searchRequest.getCatalogueId();
        String text = searchRequest.getQuery();
        String type = searchRequest.getType();
        Pageable pageable = searchRequest.getPageable();
        boolean needConvert = searchRequest.isNeedConvert();

        if (searchRequest.getBrand().isEmpty()) {
            if (catalogueId == null) {
                type += "?";
                searchRequest.setType(type);
                items = itemElasticRepository.findAllByType(text, type, pageable);
                if (items.isEmpty() && needConvert) {
                    items = itemElasticRepository.findAllByType(convert(text), type, pageable);
                }
            } else {
                items = itemElasticRepository.findByCatalogueId(text, catalogueId, pageable);
                if (items.isEmpty() && needConvert) {
                    items = itemElasticRepository.findByCatalogueId(convert(text), catalogueId, pageable);
                }
            }
        }

        searchRequest.setItems(items);
    }

}
