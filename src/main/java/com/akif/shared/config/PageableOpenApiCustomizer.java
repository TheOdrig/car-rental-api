package com.akif.shared.config;

import com.akif.shared.web.AllowedSort;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;

import java.math.BigDecimal;
import java.util.Arrays;

@Component
public class PageableOpenApiCustomizer implements OperationCustomizer {

    private final int maxPageSize;

    public PageableOpenApiCustomizer(@Value("${spring.data.web.pageable.max-page-size:100}") int maxPageSize) {
        this.maxPageSize = maxPageSize;
    }

    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {
        if (operation.getParameters() == null || !acceptsPageable(handlerMethod)) {
            return operation;
        }
        AllowedSort allowedSort = handlerMethod.getMethodAnnotation(AllowedSort.class);
        if (allowedSort == null || allowedSort.value().length == 0) {
            operation.getParameters().removeIf(parameter -> isQuery(parameter, "sort"));
        }
        operation.getParameters().stream()
                .filter(parameter -> isQuery(parameter, "size"))
                .filter(parameter -> parameter.getSchema() != null)
                .forEach(parameter -> {
                    parameter.getSchema().setMinimum(BigDecimal.ONE);
                    parameter.getSchema().setMaximum(BigDecimal.valueOf(maxPageSize));
                });
        return operation;
    }

    private static boolean acceptsPageable(HandlerMethod method) {
        return Arrays.stream(method.getMethodParameters())
                .anyMatch(parameter -> Pageable.class.isAssignableFrom(parameter.getParameterType()));
    }

    private static boolean isQuery(Parameter parameter, String name) {
        return "query".equals(parameter.getIn()) && name.equals(parameter.getName());
    }
}
