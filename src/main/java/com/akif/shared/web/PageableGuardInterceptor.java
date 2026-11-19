package com.akif.shared.web;

import com.akif.shared.exception.InvalidParameterException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class PageableGuardInterceptor implements HandlerInterceptor {

    private final int maxPageSize;

    public PageableGuardInterceptor(int maxPageSize) {
        this.maxPageSize = maxPageSize;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod method) || !acceptsPageable(method)) {
            return true;
        }
        validateInteger(request.getParameter("page"), "page", 0, Integer.MAX_VALUE);
        validateInteger(request.getParameter("size"), "size", 1, maxPageSize);
        String[] sorts = request.getParameterValues("sort");
        if (sorts != null && sorts.length > 0) {
            validateSort(sorts, method);
        }
        return true;
    }

    private static boolean acceptsPageable(HandlerMethod method) {
        return Arrays.stream(method.getMethodParameters())
                .anyMatch(parameter -> Pageable.class.isAssignableFrom(parameter.getParameterType()));
    }

    private void validateInteger(String raw, String name, int min, int max) {
        if (raw == null) {
            return;
        }
        int value;
        try {
            value = Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new InvalidParameterException("Parameter '" + name + "' must be an integer");
        }
        if (value < min || value > max) {
            String bound = max == Integer.MAX_VALUE ? "at least " + min : "between " + min + " and " + max;
            throw new InvalidParameterException("Parameter '" + name + "' must be " + bound);
        }
    }

    private static void validateSort(String[] sorts, HandlerMethod method) {
        AllowedSort annotation = method.getMethodAnnotation(AllowedSort.class);
        Set<String> allowed = annotation == null ? Set.of() : Set.copyOf(List.of(annotation.value()));
        for (String sort : sorts) {
            for (String token : sort.split(",")) {
                String trimmed = token.trim();
                if (trimmed.isEmpty() || isDirection(trimmed)) {
                    continue;
                }
                if (!allowed.contains(trimmed)) {
                    String supported = allowed.isEmpty() ? "none" : String.join(", ", allowed.stream().sorted().toList());
                    throw new InvalidParameterException(
                            "Cannot sort by '" + trimmed + "'. Supported sort fields: " + supported);
                }
            }
        }
    }

    private static boolean isDirection(String token) {
        String lower = token.toLowerCase(Locale.ROOT);
        return lower.equals("asc") || lower.equals("desc");
    }
}
