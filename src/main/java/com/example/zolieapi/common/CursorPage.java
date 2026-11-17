package com.example.zolieapi.common;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Limit;

public record CursorPage<T>(List<T> items, String nextCursor) {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 50;

    public static int limit(Integer requested) {
        if (requested == null) {
            return DEFAULT_LIMIT;
        }
        return Math.max(1, Math.min(requested, MAX_LIMIT));
    }

    public static Limit fetch(int limit) {
        return Limit.of(limit + 1);
    }

    public static <R, T> CursorPage<T> of(List<R> rows, int limit, Function<R, Cursor> cursorOf,
                                           Function<List<R>, List<T>> mapper) {
        boolean hasMore = rows.size() > limit;
        List<R> page = hasMore ? rows.subList(0, limit) : rows;
        String next = hasMore ? cursorOf.apply(page.get(page.size() - 1)).encode() : null;
        return new CursorPage<>(mapper.apply(page), next);
    }

    public static <T> CursorPage<T> empty() {
        return new CursorPage<>(List.of(), null);
    }
}
