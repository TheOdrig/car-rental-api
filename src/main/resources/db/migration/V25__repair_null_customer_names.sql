UPDATE car_rental.rentals r
SET user_full_name = COALESCE(
    NULLIF(btrim(concat_ws(' ', NULLIF(btrim(u.first_name), ''), NULLIF(btrim(u.last_name), ''))), ''),
    u.username,
    r.user_email)
FROM car_rental.users u
WHERE u.id = r.user_id
  AND r.user_full_name ~* '(^|\s)null(\s|$)';

UPDATE car_rental.damage_reports d
SET customer_full_name = COALESCE(
    NULLIF(btrim(concat_ws(' ', NULLIF(btrim(u.first_name), ''), NULLIF(btrim(u.last_name), ''))), ''),
    u.username,
    d.customer_email)
FROM car_rental.users u
WHERE u.id = d.customer_user_id
  AND d.customer_full_name ~* '(^|\s)null(\s|$)';
