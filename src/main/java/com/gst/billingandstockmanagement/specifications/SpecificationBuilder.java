package com.gst.billingandstockmanagement.specifications;

import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.From;

public class SpecificationBuilder<T> {

    public Specification<T> build(String searchText, List<String> searchFields, Map<String, String> filters) {
        List<Specification<T>> specs = new ArrayList<>();
        // search spec
        if (searchText != null && !searchText.trim().isEmpty()) {
            specs.add(new GenericSearchSpecification<>(searchText, searchFields));
        }
        // filters: equality checks (support nested keys like 'user.id')
        if (filters != null && !filters.isEmpty()) {
            for (Map.Entry<String, String> e : filters.entrySet()) {
                String key = e.getKey();
                String val = e.getValue();
                specs.add((root, query, cb) -> {
                    if (val == null) return cb.conjunction();
                    try {
                        Path<?> path = root;
                        String[] parts = key.split("\\.");
                        for (String p : parts) {
                            try {
                                path = path.get(p);
                            } catch (IllegalArgumentException iae) {
                                if (path instanceof From) {
                                    path = ((From<?, ?>) path).join(p);
                                } else {
                                    // can't resolve path
                                    return cb.conjunction();
                                }
                            }
                        }
                        return cb.equal(path.as(String.class), val);
                    } catch (Exception ex) {
                        return cb.conjunction();
                    }
                });
            }
        }

        if (specs.isEmpty()) return (root, query, cb) -> cb.conjunction();

        Specification<T> result = specs.get(0);
        for (int i = 1; i < specs.size(); i++) result = result.and(specs.get(i));
        return result;
    }
}
