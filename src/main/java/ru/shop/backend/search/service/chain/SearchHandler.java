package ru.shop.backend.search.service.chain;

import ru.shop.backend.search.service.model.SearchRequest;

public interface SearchHandler {
    void handle(SearchRequest searchRequest);

}
