package ru.shop.backend.search.service;

import ru.shop.backend.search.model.CatalogueElastic;
import ru.shop.backend.search.model.SearchResult;
import ru.shop.backend.search.model.SearchResultElastic;

import java.util.List;

public interface SearchService {
    SearchResult getSearchResult(Integer regionId, String text);

    List<CatalogueElastic> getByItemName(String name);

    List<CatalogueElastic> getByItemId(Long itemId);

    SearchResultElastic prepareResult(String text);

    List<CatalogueElastic> getAllCatalogues(String text);
}
