package com.akif.rental.domain.model;

import com.akif.rental.api.PriceAdjustment;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

@Converter
public class PriceAdjustmentsConverter implements AttributeConverter<List<PriceAdjustment>, String> {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private static final TypeReference<List<PriceAdjustment>> TYPE = new TypeReference<>() { };

    @Override
    public String convertToDatabaseColumn(List<PriceAdjustment> attribute) {
        return attribute == null ? null : MAPPER.writeValueAsString(attribute);
    }

    @Override
    public List<PriceAdjustment> convertToEntityAttribute(String dbData) {
        return dbData == null || dbData.isBlank() ? null : MAPPER.readValue(dbData, TYPE);
    }
}
