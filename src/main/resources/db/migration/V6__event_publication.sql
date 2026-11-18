CREATE TABLE car_rental.event_publication (
    id UUID NOT NULL,
    listener_id VARCHAR(512) NOT NULL,
    event_type VARCHAR(512) NOT NULL,
    serialized_event TEXT NOT NULL,
    publication_date TIMESTAMP WITH TIME ZONE NOT NULL,
    completion_date TIMESTAMP WITH TIME ZONE,
    status VARCHAR(20),
    completion_attempts INTEGER,
    last_resubmission_date TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (id)
);

CREATE INDEX event_publication_by_completion_date_idx
    ON car_rental.event_publication (completion_date);
CREATE INDEX event_publication_incomplete_idx
    ON car_rental.event_publication (publication_date)
    WHERE completion_date IS NULL;

CREATE INDEX event_publication_serialized_event_hash_idx
    ON car_rental.event_publication USING hash (serialized_event);
