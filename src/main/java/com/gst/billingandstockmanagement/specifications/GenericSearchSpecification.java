package com.gst.billingandstockmanagement.specifications;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Expression;

import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class GenericSearchSpecification<T> implements Specification<T> {
    private final String searchText;
    private final List<String> fields;

    public GenericSearchSpecification(String searchText, List<String> fields) {
        this.searchText = (searchText == null || searchText.trim().isEmpty()) ? null : searchText.trim();
        this.fields = fields == null ? new ArrayList<>() : fields;
    }

    @Override
    public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        if (searchText == null || fields.isEmpty()) return cb.conjunction();

        List<Predicate> preds = new ArrayList<>();
        String likePattern = "%" + searchText.toUpperCase() + "%";

        for (String f : fields) {
            try {
                // support nested properties with dot notation (e.g., product.name or billItems.snapshotProductName)
                Path<?> path = root;
                boolean ok = true;
                for (String part : f.split("\\.")) {
                    try {
                        path = path.get(part);
                    } catch (IllegalArgumentException iae) {
                        // maybe collection/association - try join if possible
                        if (path instanceof From) {
                            try {
                                path = ((From<?, ?>) path).join(part);
                            } catch (Exception je) {
                                ok = false; // can't resolve this path
                                break;
                            }
                        } else {
                            ok = false;
                            break;
                        }
                    }
                }
                if (!ok) continue;

                // Check if the field is a Date/DateTime type
                Class<?> javaType = path.getJavaType();
                if (Date.class.isAssignableFrom(javaType) ||
                        LocalDate.class.isAssignableFrom(javaType) ||
                        LocalDateTime.class.isAssignableFrom(javaType)) {

                    // Convert Date to String format for searching
                    // For MySQL use DATE_FORMAT, for PostgreSQL use TO_CHAR
                    Expression<String> dateAsString;

                    // Try MySQL first (more common)
                    try {
                        dateAsString = cb.function(
                                "DATE_FORMAT",
                                String.class,
                                path,
                                cb.literal("%Y-%m-%d")
                        );
                    } catch (Exception e) {
                        // Fallback to PostgreSQL format
                        dateAsString = cb.function(
                                "TO_CHAR",
                                String.class,
                                path,
                                cb.literal("YYYY-MM-DD")
                        );
                    }

                    preds.add(cb.like(cb.upper(dateAsString), likePattern));
                } else {
                    // Regular string field
                    preds.add(cb.like(cb.upper(path.as(String.class)), likePattern));
                }
            } catch (Exception e) {
                // if field path not resolvable, skip it
            }
        }

        if (preds.isEmpty()) return cb.conjunction();
        return cb.or(preds.toArray(new Predicate[0]));
    }
}