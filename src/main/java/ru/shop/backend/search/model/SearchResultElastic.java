package ru.shop.backend.search.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@AllArgsConstructor
@ToString
@Getter
@EqualsAndHashCode
public class SearchResultElastic {
    public List<CatalogueElastic> result;
}
