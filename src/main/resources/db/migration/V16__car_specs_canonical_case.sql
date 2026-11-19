CREATE FUNCTION car_rental.canonical_spec(value TEXT) RETURNS TEXT AS $$
    SELECT NULLIF(
        CASE
            WHEN lower(btrim(value)) IN ('suv', 'mpv', 'cvt', 'lpg', 'ev') THEN upper(btrim(value))
            ELSE initcap(lower(btrim(value)))
        END, '')
$$ LANGUAGE SQL IMMUTABLE;

UPDATE car_rental.car
SET fuel_type = car_rental.canonical_spec(fuel_type),
    transmission_type = car_rental.canonical_spec(transmission_type),
    body_type = car_rental.canonical_spec(body_type)
WHERE fuel_type IS DISTINCT FROM car_rental.canonical_spec(fuel_type)
   OR transmission_type IS DISTINCT FROM car_rental.canonical_spec(transmission_type)
   OR body_type IS DISTINCT FROM car_rental.canonical_spec(body_type);

DROP FUNCTION car_rental.canonical_spec(TEXT);
