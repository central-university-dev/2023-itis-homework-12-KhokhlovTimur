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
@Order(Ordered.LOWEST_PRECEDENCE)
public class NotStrongSearchHandler implements SearchHandler {
    private final ItemElasticRepository itemElasticRepository;

    @Override
    public void handle(SearchRequest searchRequest) {
        List<ItemElastic> items = searchRequest.getItems();
        String text = searchRequest.getQuery();
        String brand = searchRequest.getBrand();
        String type = searchRequest.getType();
        Pageable pageable = searchRequest.getPageable();
        String baseText = searchRequest.getBaseQuery();
        boolean needConvert = searchRequest.isNeedConvert();

        if (items.isEmpty()) {
            if (baseText.contains(" ")) {
                text = String.join(" ", text.split("\\s+"));
                baseText = String.join(" ", baseText.split("\\s+"));
                searchRequest.setQuery(text);
            }
            baseText += "?";
            items = itemElasticRepository.findAllNotStrong(baseText, pageable);
            if (items.isEmpty() && needConvert) {
                items = itemElasticRepository.findAllByTypeAndBrand(convert(baseText), brand, type, pageable);
            }
        }
        searchRequest.setItems(items);
    }
}
