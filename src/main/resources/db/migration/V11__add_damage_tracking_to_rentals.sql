ALTER TABLE gallery.rentals ADD COLUMN IF NOT EXISTS has_damage_reports BOOLEAN DEFAULT FALSE;
ALTER TABLE gallery.rentals ADD COLUMN IF NOT EXISTS damage_reports_count INTEGER DEFAULT 0;
