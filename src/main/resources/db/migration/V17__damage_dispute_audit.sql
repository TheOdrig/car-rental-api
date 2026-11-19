ALTER TABLE car_rental.damage_reports
    ADD COLUMN original_liability NUMERIC(12,2),
    ADD COLUMN dispute_refund_amount NUMERIC(12,2);

UPDATE car_rental.dashboard_alerts SET action_url = '/admin/fleet' WHERE action_url = '/admin/dashboard/fleet';
UPDATE car_rental.dashboard_alerts SET action_url = '/admin/damages' WHERE action_url = '/admin/damage/disputes';
UPDATE car_rental.dashboard_alerts SET action_url = '/admin/rentals' WHERE action_url = '/admin/payments?status=FAILED';
