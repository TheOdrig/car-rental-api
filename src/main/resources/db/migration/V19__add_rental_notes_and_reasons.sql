ALTER TABLE gallery.rentals ADD COLUMN IF NOT EXISTS approval_notes TEXT;
ALTER TABLE gallery.rentals ADD COLUMN IF NOT EXISTS cancellation_reason TEXT;
