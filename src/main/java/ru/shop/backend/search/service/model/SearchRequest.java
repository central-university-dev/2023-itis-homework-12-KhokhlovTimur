package ru.shop.backend.search.service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Pageable;
import ru.shop.backend.search.model.ItemElastic;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SearchRequest {
    private String query = "";
    private String baseQuery;
    private List<ItemElastic> items;
    private String brand = "";
    private String type = "";
    private Long catalogueId;
    private Pageable pageable;
    private boolean needConvert;
}
