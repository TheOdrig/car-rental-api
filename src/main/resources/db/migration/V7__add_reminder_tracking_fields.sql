ALTER TABLE gallery.rentals
    ADD COLUMN pickup_reminder_sent BOOLEAN DEFAULT FALSE,
    ADD COLUMN return_reminder_sent BOOLEAN DEFAULT FALSE;

UPDATE gallery.rentals
SET pickup_reminder_sent = FALSE,
    return_reminder_sent = FALSE
WHERE pickup_reminder_sent IS NULL
   OR return_reminder_sent IS NULL;
