package ru.shop.backend.search.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.shop.backend.search.model.*;
import ru.shop.backend.search.repository.ItemElasticRepository;
import ru.shop.backend.search.repository.ItemJpaRepository;
import ru.shop.backend.search.service.chain.SearchChainProcessor;
import ru.shop.backend.search.service.model.SearchRequest;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static ru.shop.backend.search.service.util.SearchStringUtils.convert;
import static ru.shop.backend.search.service.util.SearchStringUtils.isNumeric;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {
    private final ItemElasticRepository itemElasticRepository;
    private final ItemJpaRepository itemJpaRepository;

    private final Pageable pageable = PageRequest.of(0, 150);
    private final Pageable pageableSmall = PageRequest.of(0, 10);
    private final SearchChainProcessor searchChainProcessor;

    @Override
    public SearchResultElastic prepareResult(String text) {
        if (isNumeric(text)) {
            Integer itemId = itemJpaRepository.findBySku(text)
                    .stream()
                    .findFirst()
                    .orElse(null);
            if (itemId == null) {
                var catalogue = getByItemName(text);
                if (!catalogue.isEmpty()) {
                    return new SearchResultElastic(catalogue);
                }
                return new SearchResultElastic(getAllFull(text));
            }
            try {
                return new SearchResultElastic(getByItemId(Long.valueOf(itemId)));
            } catch (Exception ignored) {
            }
        }

        return new SearchResultElastic(getAllFull(text));
    }

    @Override
    public List<CatalogueElastic> getByItemName(String name) {
        List<ItemElastic> items = itemElasticRepository.findAllByName(name + ".*", pageable);
        return getCatalogues(items, name, "");
    }

    @Override
    public List<CatalogueElastic> getByItemId(Long itemId) {
        var items = itemElasticRepository.findByItemId(itemId, PageRequest.of(0, 1));

        return Collections.singletonList(
                new CatalogueElastic(
                        items.get(0).getCatalogue(),
                        items.get(0).getCatalogueId(),
                        items,
                        items.get(0).getBrand()));
    }

    @Override
    public synchronized List<CatalogueElastic> getAllCatalogues(String text) {
        return getAllCatalogues(text, pageableSmall);
    }

    @Override
    public synchronized SearchResult getSearchResult(Integer regionId, String text) {
        List<CatalogueElastic> result = findResultFromJpaRepository(text);

        if (result == null) {
            result = getAllCatalogues(text);
        }

        String brand = null;
        if (!result.isEmpty()) {
            brand = result.get(0).getBrand();
        }
        if (brand == null) {
            brand = "";
        }

        brand = brand.toLowerCase(Locale.ROOT);
        String finalBrand = brand;

        List<Item> items = findItemsForResult(result, regionId);
        List<Category> categories = findCategoriesForResult(items, finalBrand);

        return new SearchResult(
                items,
                categories,
                getTypeHelpTexts(result)
        );
    }

    private List<TypeHelpText> getTypeHelpTexts(List<CatalogueElastic> result) {
        List<TypeHelpText> typeQueries = new ArrayList<>();

        if (!result.isEmpty()) {
            String resultType = result.get(0)
                    .getItems()
                    .get(0)
                    .getType();
            String resultBrand = result.get(0)
                    .getBrand();

            String text = (resultType != null ? resultType : "") +
                    " " + (resultBrand != null ? resultBrand : "")
                    .trim();

            typeQueries = List.of(new TypeHelpText(
                    TypeOfQuery.SEE_ALSO,
                    text));
        }

        return typeQueries;
    }

    private List<CatalogueElastic> findResultFromJpaRepository(String text) {
        List<CatalogueElastic> result = null;

        if (isNumeric(text)) {
            Integer itemId = itemJpaRepository.findBySku(text)
                    .stream()
                    .findFirst()
                    .orElse(null);

            if (itemId == null) {
                var catalogue = getByItemName(text);
                if (!catalogue.isEmpty()) {
                    result = catalogue;
                }
            } else {
                result = getByItemId(Long.valueOf(itemId));
            }
        }

        return result;
    }

    private List<Item> findItemsForResult(List<CatalogueElastic> result, Integer regionId) {
        List<Object[]> rawItems = itemJpaRepository.findByIds(regionId,
                result.stream()
                        .flatMap(category -> category.getItems().stream())
                        .map(ItemElastic::getItemId)
                        .collect(Collectors.toList()));

        return rawItems.stream()
                .map(arr -> new Item(
                        ((BigInteger) arr[2]).intValue(),
                        arr[1].toString(),
                        arr[3].toString(),
                        arr[4].toString(),
                        ((BigInteger) arr[0]).intValue(),
                        arr[5].toString()))
                .collect(Collectors.toList());
    }

    private List<Category> findCategoriesForResult(List<Item> items, String finalBrand) {
        Set<String> catUrls = new HashSet<>();
        List<Object[]> rawCategories = itemJpaRepository.findCatsByIds(
                items.stream()
                        .map(Item::getItemId)
                        .collect(Collectors.toList()));

        return rawCategories.stream()
                .map(raw -> mapToCategory(catUrls, raw, finalBrand))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private Category mapToCategory(Set<String> catUrls, Object[] arr, String finalBrand) {
        if (catUrls.contains(arr[2].toString()))
            return null;
        catUrls.add(arr[2].toString());
        return new Category(
                arr[0].toString(),
                arr[1].toString(),
                String.format("/cat/%s%s", arr[2].toString(),
                        finalBrand.isEmpty() ? "" : "/brands/" + finalBrand),
                String.format("/cat/%s", arr[3].toString()),
                (arr[4] == null) ? null : arr[4].toString());
    }

    private List<CatalogueElastic> getAllCatalogues(String text, Pageable pageable) {
        String baseText = text;
        boolean needConvert = true;

        if (isContainErrorChar(text)) {
            text = convert(text);
            needConvert = false;
        }
        if (needConvert && isContainErrorChar(convert(text))) {
            needConvert = false;
        }

        SearchRequest searchRequest = createSearchRequest(text, pageable, baseText);

        findBrand(searchRequest, needConvert);
        findType(searchRequest, needConvert);
        findCatalogue(searchRequest, needConvert);

        if (searchRequest.getQuery().isEmpty()
                && !searchRequest.getBrand().isEmpty()) {
            return Collections.singletonList(new CatalogueElastic(
                    searchRequest.getItems().get(0).getCatalogue(),
                    searchRequest.getItems().get(0).getCatalogueId(),
                    null,
                    searchRequest.getBrand()));
        }
        searchRequest.setQuery(searchRequest.getQuery() + "?");

        searchChainProcessor.processRequest(searchRequest);

        return getCatalogues(
                searchRequest.getItems(),
                searchRequest.getQuery(),
                searchRequest.getBrand());
    }

    private void findCatalogue(SearchRequest searchRequest, boolean needConvert) {
        String text = searchRequest.getQuery();
        Pageable pageable = searchRequest.getPageable();

        if (searchRequest.getBrand().isEmpty()) {
            List<ItemElastic> items = itemElasticRepository.findByCatalogue(text, pageable);
            if (items.isEmpty() && needConvert) {
                items = itemElasticRepository.findByCatalogue(convert(text), pageable);
            }
            if (!items.isEmpty()) {
                searchRequest.setCatalogueId(items.get(0).getCatalogueId());
            }

            searchRequest.setItems(items);
        }

    }

    private void findType(SearchRequest searchRequest, boolean needConvert) {
        List<ItemElastic> items = searchRequest.getItems();
        String text = searchRequest.getQuery();
        String type = searchRequest.getType();
        Pageable pageable = searchRequest.getPageable();

        items = itemElasticRepository.findAllByType(text, pageable);
        if (items.isEmpty() && needConvert) {
            items = itemElasticRepository.findAllByType(convert(text), pageable);
        }
        if (!items.isEmpty()) {
            type = findTypeInItems(items);
            text = text.replace(type, "").trim();
            searchRequest.setType(type);
            searchRequest.setQuery(text);
        } else {
            for (String queryWord : text.split("\\s")) {
                items = itemElasticRepository.findAllByType(queryWord, pageable);
                if (items.isEmpty() && needConvert) {
                    items = itemElasticRepository.findAllByType(convert(queryWord), pageable);
                }

                if (!items.isEmpty()) {
                    text = text.replace(queryWord, "").trim();
                    type = findTypeInItems(items);
                    searchRequest.setType(type);
                    searchRequest.setQuery(text);
                    break;
                }
            }
        }

        searchRequest.setItems(items);
    }

    private String findTypeInItems(List<ItemElastic> items) {
        return items.stream()
                .map(ItemElastic::getType)
                .min(Comparator.comparingInt(String::length))
                .orElse("");
    }

    private void findBrand(SearchRequest searchRequest, boolean needConvert) {
        List<ItemElastic> items = searchRequest.getItems();
        String brand = searchRequest.getBrand();
        String text = searchRequest.getQuery();

        if (text.contains(" ")) {
            for (String queryWord : text.split("\\s")) {
                items = itemElasticRepository.findAllByBrand(queryWord, searchRequest.getPageable());

                if (items.isEmpty() && needConvert) {
                    items = itemElasticRepository.findAllByBrand(convert(queryWord), searchRequest.getPageable());
                }

                if (!items.isEmpty()) {
                    text = text.replace(queryWord, "")
                            .trim()
                            .replace("\\s+", " ");
                    brand = items.get(0)
                            .getBrand();
                    searchRequest.setBrand(brand);
                    searchRequest.setQuery(text);
                    break;
                }
            }
        }

        searchRequest.setItems(items);
    }

    private List<CatalogueElastic> getCatalogues(List<ItemElastic> items, String text, String brand) {
        Map<String, List<ItemElastic>> catalogues = new HashMap<>();
        AtomicReference<ItemElastic> searchedItem = new AtomicReference<>();
        String cleanText = text.replace("?", "");
        items.forEach(
                i ->
                {
                    if (cleanText.equals(i.getName()) ||
                            (cleanText.endsWith(i.getName()) && cleanText.startsWith(i.getType()))) {
                        searchedItem.set(i);
                    }

                    catalogues.computeIfAbsent(i.getCatalogue(), k -> new ArrayList<>())
                            .add(i);
                }
        );

        if (brand.isEmpty()) {
            brand = null;
        }

        if (searchedItem.get() != null) {
            ItemElastic item = searchedItem.get();
            return Collections.singletonList(new CatalogueElastic(
                    item.getCatalogue(),
                    item.getCatalogueId(),
                    Collections.singletonList(item),
                    brand));
        }

        String finalBrand = brand;

        return catalogues.entrySet()
                .stream()
                .map(entry ->
                        new CatalogueElastic(
                                entry.getKey(),
                                entry.getValue().get(0).getCatalogueId(),
                                entry.getValue(),
                                finalBrand))
                .collect(Collectors.toList());
    }

    private List<CatalogueElastic> getAllFull(String text) {
        return getAllCatalogues(text, pageable);
    }

    private Boolean isContainErrorChar(String text) {
        return text.contains("[")
                || text.contains("]")
                || text.contains("\"")
                || text.contains("/")
                || text.contains(";");
    }

    private SearchRequest createSearchRequest(String text, Pageable pageable, String baseQuery) {
        return SearchRequest.builder()
                .query(text)
                .items(new ArrayList<>())
                .brand("")
                .type("")
                .catalogueId(null)
                .baseQuery(baseQuery)
                .pageable(pageable)
                .build();
    }

}
