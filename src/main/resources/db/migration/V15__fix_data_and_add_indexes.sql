UPDATE gallery.car
SET transmission_type = 'Manual' 
WHERE LOWER(transmission_type) = 'manuel';


CREATE INDEX IF NOT EXISTS idx_car_transmission_type ON gallery.car(transmission_type);
CREATE INDEX IF NOT EXISTS idx_car_body_type ON gallery.car(body_type);
CREATE INDEX IF NOT EXISTS idx_car_fuel_type ON gallery.car(fuel_type);
CREATE INDEX IF NOT EXISTS idx_car_seats ON gallery.car(seats);
