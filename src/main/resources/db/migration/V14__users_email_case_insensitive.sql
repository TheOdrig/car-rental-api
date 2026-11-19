WITH ranked AS (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY LOWER(email) ORDER BY create_time, id) AS position
    FROM car_rental.users
)
UPDATE car_rental.users u
SET email = LEFT('dup' || u.id || '.' || u.email, 100)
FROM ranked r
WHERE r.id = u.id AND r.position > 1;

CREATE UNIQUE INDEX uk_users_email_lower ON car_rental.users (LOWER(email));
