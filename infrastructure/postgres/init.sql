CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE events (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    event_date DATE NOT NULL,
    public_token VARCHAR(255) NOT NULL UNIQUE,
    viewer_token VARCHAR(255) NOT NULL UNIQUE,
    cover_image_key VARCHAR(255),
    active BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_events_owner FOREIGN KEY (owner_id) REFERENCES users (id)
);

CREATE TABLE media (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL,
    storage_key VARCHAR(255) NOT NULL UNIQUE,
    original_filename VARCHAR(255) NOT NULL,
    mime_type VARCHAR(255) NOT NULL,
    size_bytes BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_media_event FOREIGN KEY (event_id) REFERENCES events (id)
);
