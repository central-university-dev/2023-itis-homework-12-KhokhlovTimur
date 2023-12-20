package ru.shop.backend.search.service.chain;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.shop.backend.search.service.model.SearchRequest;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SearchChainProcessor {
    private final List<SearchHandler> handlers;

    public void processRequest(SearchRequest searchRequest) {
        for (SearchHandler handler : handlers) {
            handler.handle(searchRequest);
        }
    }

}
