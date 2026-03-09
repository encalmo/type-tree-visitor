package org.encalmo.utils;

import java.util.List;
import java.math.BigDecimal;

public record Order(
        String id,
        String customerId,
        List<Integer> items,
        int[] codes,
        BigDecimal total,
        java.util.Map<String, Integer> delivery) {
}
