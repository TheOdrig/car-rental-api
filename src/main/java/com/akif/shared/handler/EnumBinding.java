package com.akif.shared.handler;

import org.springframework.beans.TypeMismatchException;
import org.springframework.core.convert.ConversionFailedException;
import tools.jackson.databind.exc.InvalidFormatException;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

final class EnumBinding {

    private EnumBinding() {
    }

    static Optional<Class<?>> enumTypeOf(Throwable failure) {
        int guard = 0;
        for (Throwable cause = failure; cause != null && guard < 16; cause = cause.getCause(), guard++) {
            Class<?> candidate = null;
            if (cause instanceof TypeMismatchException mismatch) {
                candidate = mismatch.getRequiredType();
            } else if (cause instanceof ConversionFailedException conversion) {
                candidate = conversion.getTargetType().getType();
            } else if (cause instanceof InvalidFormatException format) {
                candidate = format.getTargetType();
            }
            if (candidate != null && candidate.isEnum()) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    static String allowedValues(Class<?> enumType) {
        return Arrays.stream(enumType.getEnumConstants())
                .map(constant -> ((Enum<?>) constant).name())
                .collect(Collectors.joining(", "));
    }
}
