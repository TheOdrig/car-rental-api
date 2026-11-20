UPDATE car_rental.rentals
SET price_adjustments = replace(replace(replace(replace(replace(price_adjustments,
        '"code":"Season Pricing"', '"code":"SEASON"'),
        '"code":"Early Booking"', '"code":"EARLY_BOOKING"'),
        '"code":"Duration Discount"', '"code":"DURATION_DISCOUNT"'),
        '"code":"Weekend Pricing"', '"code":"WEEKEND"'),
        '"code":"Demand Pricing"', '"code":"DEMAND"')
WHERE price_adjustments IS NOT NULL;
