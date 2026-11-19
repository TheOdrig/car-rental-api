CREATE INDEX idx_damage_photos_file_name
    ON car_rental.damage_photos ((substring(file_path from '[^/]+$')))
    WHERE is_deleted = false;

CREATE INDEX idx_users_avatar_file_name
    ON car_rental.users ((substring(avatar_url from '[^/]+$')))
    WHERE avatar_url IS NOT NULL;
