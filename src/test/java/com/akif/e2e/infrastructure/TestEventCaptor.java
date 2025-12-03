package com.akif.e2e.infrastructure;

import lombok.Getter;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@ActiveProfiles("test")
@Getter
public class TestEventCaptor {
    
    private final List<Object> capturedEvents = new ArrayList<>();

    @EventListener
    public void captureEvent(Object event) {
        capturedEvents.add(event);
    }

    public <T> List<T> getEventsOfType(Class<T> eventType) {
        return capturedEvents.stream()
                .filter(eventType::isInstance)
                .map(eventType::cast)
                .collect(Collectors.toList());
    }

    public void clear() {
        capturedEvents.clear();
    }

    public List<Object> getAllEvents() {
        return new ArrayList<>(capturedEvents);
    }
}
