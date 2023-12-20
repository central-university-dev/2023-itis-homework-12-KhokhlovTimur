package ru.shop.backend.search.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import ru.shop.backend.search.model.SearchResult;
import ru.shop.backend.search.model.SearchResultElastic;
import ru.shop.backend.search.service.SearchService;

@RestController
@RequiredArgsConstructor
public class SearchController implements SearchApi {

    private final SearchService searchService;

    public SearchResult find(String text, int regionId) {
        return searchService.getSearchResult(regionId, text);
    }

    public ResponseEntity<SearchResultElastic> findElastic(String text) {
        return ResponseEntity.ok()
                .body(searchService.prepareResult(text));
    }

}
