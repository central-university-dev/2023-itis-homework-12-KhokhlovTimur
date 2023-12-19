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
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class BrandTypeSearchHandler implements SearchHandler {
    private final ItemElasticRepository itemElasticRepository;

    @Override
    public void handle(SearchRequest searchRequest) {
        String text = searchRequest.getQuery();
        String brand = searchRequest.getBrand();
        String type = searchRequest.getType();
        List<ItemElastic> items = searchRequest.getItems();
        Pageable pageable = searchRequest.getPageable();

        if (!brand.isEmpty()) {
            if (type.isEmpty()) {
                items = itemElasticRepository.findAllByBrand(text, brand, pageable);
                if (items.isEmpty()) {
                    items = itemElasticRepository.findAllByBrand(convert(text), brand, pageable);
                }
            } else {
//                type += "?";
                searchRequest.setType(type);
                items = itemElasticRepository.findAllByTypeAndBrand(text, brand, type, pageable);
                if (items.isEmpty() && searchRequest.isNeedConvert()) {
                    items = itemElasticRepository.findAllByTypeAndBrand(convert(text), brand, type, pageable);
                }
            }
        }

        searchRequest.setItems(items);

    }
}
