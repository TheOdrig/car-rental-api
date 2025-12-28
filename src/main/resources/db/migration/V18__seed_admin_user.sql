-- Create admin user
-- Password: Admin123!
INSERT INTO gallery.users (username, email, password, enabled, is_deleted, create_time, update_time, version)
VALUES (
           'admin',
           'admin@carrental.com',
           '$2a$10$KP5Ol/J0r51UDFsS7kyy0OrgTG/B1grMmOv3T6ObcdwORqdBLQGP2',
           TRUE,
           FALSE,
           NOW(),
           NOW(),
           0
       ) ON CONFLICT (username) DO NOTHING;

INSERT INTO gallery.user_roles (user_id, role)
SELECT id, 'ADMIN' FROM gallery.users WHERE username = 'admin'
ON CONFLICT (user_id, role) DO NOTHING;

INSERT INTO gallery.user_roles (user_id, role)
SELECT id, 'USER' FROM gallery.users WHERE username = 'admin'
ON CONFLICT (user_id, role) DO NOTHING;