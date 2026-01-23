-- =============================================================================
-- V7: Seed Users (Admin + Customers)
-- =============================================================================
-- Admin Password: Admin123!
-- Customer Password (all): password
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Admin User
-- -----------------------------------------------------------------------------
INSERT INTO gallery.users (username, email, password, first_name, last_name, enabled, is_deleted, create_time, update_time, version)
VALUES (
    'admin',
    'admin@carrental.com',
    '$2a$10$KP5Ol/J0r51UDFsS7kyy0OrgTG/B1grMmOv3T6ObcdwORqdBLQGP2',
    'Admin',
    'User',
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

-- -----------------------------------------------------------------------------
-- Sample Customers (22 active + 3 banned = 25 total)
-- BCrypt hash for 'password': $2a$10$AQ/N5rC6y2xDgV2LXCf43OHLtH3Ygimn9cc9OI255u9JgSmzYTSzy
-- -----------------------------------------------------------------------------

-- Active Customers (22 users - spread across different registration dates)
INSERT INTO gallery.users (username, email, password, first_name, last_name, phone, enabled, is_deleted, is_banned, create_time, update_time, version)
VALUES ('ahmet_yilmaz', 'ahmet.yilmaz@gmail.com', '$2a$10$AQ/N5rC6y2xDgV2LXCf43OHLtH3Ygimn9cc9OI255u9JgSmzYTSzy', 'Ahmet', 'Yılmaz', '+905551234567', TRUE, FALSE, FALSE, NOW() - INTERVAL '365 days', NOW(), 0)
ON CONFLICT (username) DO NOTHING;
INSERT INTO gallery.user_roles (user_id, role) SELECT id, 'USER' FROM gallery.users WHERE username = 'ahmet_yilmaz' ON CONFLICT (user_id, role) DO NOTHING;

INSERT INTO gallery.users (username, email, password, first_name, last_name, phone, enabled, is_deleted, is_banned, create_time, update_time, version)
VALUES ('zeynep_kaya', 'zeynep.kaya@gmail.com', '$2a$10$AQ/N5rC6y2xDgV2LXCf43OHLtH3Ygimn9cc9OI255u9JgSmzYTSzy', 'Zeynep', 'Kaya', '+905559876543', TRUE, FALSE, FALSE, NOW() - INTERVAL '350 days', NOW(), 0)
ON CONFLICT (username) DO NOTHING;
INSERT INTO gallery.user_roles (user_id, role) SELECT id, 'USER' FROM gallery.users WHERE username = 'zeynep_kaya' ON CONFLICT (user_id, role) DO NOTHING;

INSERT INTO gallery.users (username, email, password, first_name, last_name, phone, enabled, is_deleted, is_banned, create_time, update_time, version)
VALUES ('mehmet_demir', 'mehmet.demir@gmail.com', '$2a$10$AQ/N5rC6y2xDgV2LXCf43OHLtH3Ygimn9cc9OI255u9JgSmzYTSzy', 'Mehmet', 'Demir', '+905321234567', TRUE, FALSE, FALSE, NOW() - INTERVAL '330 days', NOW(), 0)
ON CONFLICT (username) DO NOTHING;
INSERT INTO gallery.user_roles (user_id, role) SELECT id, 'USER' FROM gallery.users WHERE username = 'mehmet_demir' ON CONFLICT (user_id, role) DO NOTHING;

INSERT INTO gallery.users (username, email, password, first_name, last_name, phone, enabled, is_deleted, is_banned, create_time, update_time, version)
VALUES ('ayse_ozturk', 'ayse.ozturk@hotmail.com', '$2a$10$AQ/N5rC6y2xDgV2LXCf43OHLtH3Ygimn9cc9OI255u9JgSmzYTSzy', 'Ayşe', 'Öztürk', '+905331112233', TRUE, FALSE, FALSE, NOW() - INTERVAL '300 days', NOW(), 0)
ON CONFLICT (username) DO NOTHING;
INSERT INTO gallery.user_roles (user_id, role) SELECT id, 'USER' FROM gallery.users WHERE username = 'ayse_ozturk' ON CONFLICT (user_id, role) DO NOTHING;

INSERT INTO gallery.users (username, email, password, first_name, last_name, phone, enabled, is_deleted, is_banned, create_time, update_time, version)
VALUES ('can_arslan', 'can.arslan@outlook.com', '$2a$10$AQ/N5rC6y2xDgV2LXCf43OHLtH3Ygimn9cc9OI255u9JgSmzYTSzy', 'Can', 'Arslan', '+905342223344', TRUE, FALSE, FALSE, NOW() - INTERVAL '280 days', NOW(), 0)
ON CONFLICT (username) DO NOTHING;
INSERT INTO gallery.user_roles (user_id, role) SELECT id, 'USER' FROM gallery.users WHERE username = 'can_arslan' ON CONFLICT (user_id, role) DO NOTHING;

INSERT INTO gallery.users (username, email, password, first_name, last_name, phone, enabled, is_deleted, is_banned, create_time, update_time, version)
VALUES ('elif_sahin', 'elif.sahin@yahoo.com', '$2a$10$AQ/N5rC6y2xDgV2LXCf43OHLtH3Ygimn9cc9OI255u9JgSmzYTSzy', 'Elif', 'Şahin', '+905353334455', TRUE, FALSE, FALSE, NOW() - INTERVAL '260 days', NOW(), 0)
ON CONFLICT (username) DO NOTHING;
INSERT INTO gallery.user_roles (user_id, role) SELECT id, 'USER' FROM gallery.users WHERE username = 'elif_sahin' ON CONFLICT (user_id, role) DO NOTHING;

INSERT INTO gallery.users (username, email, password, first_name, last_name, phone, enabled, is_deleted, is_banned, create_time, update_time, version)
VALUES ('burak_celik', 'burak.celik@gmail.com', '$2a$10$AQ/N5rC6y2xDgV2LXCf43OHLtH3Ygimn9cc9OI255u9JgSmzYTSzy', 'Burak', 'Çelik', '+905364445566', TRUE, FALSE, FALSE, NOW() - INTERVAL '240 days', NOW(), 0)
ON CONFLICT (username) DO NOTHING;
INSERT INTO gallery.user_roles (user_id, role) SELECT id, 'USER' FROM gallery.users WHERE username = 'burak_celik' ON CONFLICT (user_id, role) DO NOTHING;

INSERT INTO gallery.users (username, email, password, first_name, last_name, phone, enabled, is_deleted, is_banned, create_time, update_time, version)
VALUES ('selin_yildiz', 'selin.yildiz@gmail.com', '$2a$10$AQ/N5rC6y2xDgV2LXCf43OHLtH3Ygimn9cc9OI255u9JgSmzYTSzy', 'Selin', 'Yıldız', '+905375556677', TRUE, FALSE, FALSE, NOW() - INTERVAL '220 days', NOW(), 0)
ON CONFLICT (username) DO NOTHING;
INSERT INTO gallery.user_roles (user_id, role) SELECT id, 'USER' FROM gallery.users WHERE username = 'selin_yildiz' ON CONFLICT (user_id, role) DO NOTHING;

INSERT INTO gallery.users (username, email, password, first_name, last_name, phone, enabled, is_deleted, is_banned, create_time, update_time, version)
VALUES ('emre_koc', 'emre.koc@hotmail.com', '$2a$10$AQ/N5rC6y2xDgV2LXCf43OHLtH3Ygimn9cc9OI255u9JgSmzYTSzy', 'Emre', 'Koç', '+905386667788', TRUE, FALSE, FALSE, NOW() - INTERVAL '200 days', NOW(), 0)
ON CONFLICT (username) DO NOTHING;
INSERT INTO gallery.user_roles (user_id, role) SELECT id, 'USER' FROM gallery.users WHERE username = 'emre_koc' ON CONFLICT (user_id, role) DO NOTHING;

INSERT INTO gallery.users (username, email, password, first_name, last_name, phone, enabled, is_deleted, is_banned, create_time, update_time, version)
VALUES ('deniz_aksoy', 'deniz.aksoy@outlook.com', '$2a$10$AQ/N5rC6y2xDgV2LXCf43OHLtH3Ygimn9cc9OI255u9JgSmzYTSzy', 'Deniz', 'Aksoy', '+905397778899', TRUE, FALSE, FALSE, NOW() - INTERVAL '180 days', NOW(), 0)
ON CONFLICT (username) DO NOTHING;
INSERT INTO gallery.user_roles (user_id, role) SELECT id, 'USER' FROM gallery.users WHERE username = 'deniz_aksoy' ON CONFLICT (user_id, role) DO NOTHING;

INSERT INTO gallery.users (username, email, password, first_name, last_name, phone, enabled, is_deleted, is_banned, create_time, update_time, version)
VALUES ('gokhan_erdogan', 'gokhan.erdogan@gmail.com', '$2a$10$AQ/N5rC6y2xDgV2LXCf43OHLtH3Ygimn9cc9OI255u9JgSmzYTSzy', 'Gökhan', 'Erdoğan', '+905508889900', TRUE, FALSE, FALSE, NOW() - INTERVAL '160 days', NOW(), 0)
ON CONFLICT (username) DO NOTHING;
INSERT INTO gallery.user_roles (user_id, role) SELECT id, 'USER' FROM gallery.users WHERE username = 'gokhan_erdogan' ON CONFLICT (user_id, role) DO NOTHING;

INSERT INTO gallery.users (username, email, password, first_name, last_name, phone, enabled, is_deleted, is_banned, create_time, update_time, version)
VALUES ('merve_kilic', 'merve.kilic@yahoo.com', '$2a$10$AQ/N5rC6y2xDgV2LXCf43OHLtH3Ygimn9cc9OI255u9JgSmzYTSzy', 'Merve', 'Kılıç', '+905519990011', TRUE, FALSE, FALSE, NOW() - INTERVAL '140 days', NOW(), 0)
ON CONFLICT (username) DO NOTHING;
INSERT INTO gallery.user_roles (user_id, role) SELECT id, 'USER' FROM gallery.users WHERE username = 'merve_kilic' ON CONFLICT (user_id, role) DO NOTHING;

INSERT INTO gallery.users (username, email, password, first_name, last_name, phone, enabled, is_deleted, is_banned, create_time, update_time, version)
VALUES ('ali_polat', 'ali.polat@gmail.com', '$2a$10$AQ/N5rC6y2xDgV2LXCf43OHLtH3Ygimn9cc9OI255u9JgSmzYTSzy', 'Ali', 'Polat', '+905520001122', TRUE, FALSE, FALSE, NOW() - INTERVAL '120 days', NOW(), 0)
ON CONFLICT (username) DO NOTHING;
INSERT INTO gallery.user_roles (user_id, role) SELECT id, 'USER' FROM gallery.users WHERE username = 'ali_polat' ON CONFLICT (user_id, role) DO NOTHING;

INSERT INTO gallery.users (username, email, password, first_name, last_name, phone, enabled, is_deleted, is_banned, create_time, update_time, version)
VALUES ('ipek_ozdemir', 'ipek.ozdemir@hotmail.com', '$2a$10$AQ/N5rC6y2xDgV2LXCf43OHLtH3Ygimn9cc9OI255u9JgSmzYTSzy', 'İpek', 'Özdemir', '+905531112233', TRUE, FALSE, FALSE, NOW() - INTERVAL '100 days', NOW(), 0)
ON CONFLICT (username) DO NOTHING;
INSERT INTO gallery.user_roles (user_id, role) SELECT id, 'USER' FROM gallery.users WHERE username = 'ipek_ozdemir' ON CONFLICT (user_id, role) DO NOTHING;

INSERT INTO gallery.users (username, email, password, first_name, last_name, phone, enabled, is_deleted, is_banned, create_time, update_time, version)
VALUES ('murat_aydin', 'murat.aydin@outlook.com', '$2a$10$AQ/N5rC6y2xDgV2LXCf43OHLtH3Ygimn9cc9OI255u9JgSmzYTSzy', 'Murat', 'Aydın', '+905542223344', TRUE, FALSE, FALSE, NOW() - INTERVAL '80 days', NOW(), 0)
ON CONFLICT (username) DO NOTHING;
INSERT INTO gallery.user_roles (user_id, role) SELECT id, 'USER' FROM gallery.users WHERE username = 'murat_aydin' ON CONFLICT (user_id, role) DO NOTHING;

INSERT INTO gallery.users (username, email, password, first_name, last_name, phone, enabled, is_deleted, is_banned, create_time, update_time, version)
VALUES ('ceren_ozer', 'ceren.ozer@gmail.com', '$2a$10$AQ/N5rC6y2xDgV2LXCf43OHLtH3Ygimn9cc9OI255u9JgSmzYTSzy', 'Ceren', 'Özer', '+905553334455', TRUE, FALSE, FALSE, NOW() - INTERVAL '60 days', NOW(), 0)
ON CONFLICT (username) DO NOTHING;
INSERT INTO gallery.user_roles (user_id, role) SELECT id, 'USER' FROM gallery.users WHERE username = 'ceren_ozer' ON CONFLICT (user_id, role) DO NOTHING;

INSERT INTO gallery.users (username, email, password, first_name, last_name, phone, enabled, is_deleted, is_banned, create_time, update_time, version)
VALUES ('yusuf_karaca', 'yusuf.karaca@gmail.com', '$2a$10$AQ/N5rC6y2xDgV2LXCf43OHLtH3Ygimn9cc9OI255u9JgSmzYTSzy', 'Yusuf', 'Karaca', '+905564445566', TRUE, FALSE, FALSE, NOW() - INTERVAL '40 days', NOW(), 0)
ON CONFLICT (username) DO NOTHING;
INSERT INTO gallery.user_roles (user_id, role) SELECT id, 'USER' FROM gallery.users WHERE username = 'yusuf_karaca' ON CONFLICT (user_id, role) DO NOTHING;

INSERT INTO gallery.users (username, email, password, first_name, last_name, phone, enabled, is_deleted, is_banned, create_time, update_time, version)
VALUES ('esra_dogan', 'esra.dogan@hotmail.com', '$2a$10$AQ/N5rC6y2xDgV2LXCf43OHLtH3Ygimn9cc9OI255u9JgSmzYTSzy', 'Esra', 'Doğan', '+905575556677', TRUE, FALSE, FALSE, NOW() - INTERVAL '20 days', NOW(), 0)
ON CONFLICT (username) DO NOTHING;
INSERT INTO gallery.user_roles (user_id, role) SELECT id, 'USER' FROM gallery.users WHERE username = 'esra_dogan' ON CONFLICT (user_id, role) DO NOTHING;

INSERT INTO gallery.users (username, email, password, first_name, last_name, phone, enabled, is_deleted, is_banned, create_time, update_time, version)
VALUES ('serkan_yilmaz', 'serkan.yilmaz@yahoo.com', '$2a$10$AQ/N5rC6y2xDgV2LXCf43OHLtH3Ygimn9cc9OI255u9JgSmzYTSzy', 'Serkan', 'Yılmaz', '+905586667788', TRUE, FALSE, FALSE, NOW() - INTERVAL '10 days', NOW(), 0)
ON CONFLICT (username) DO NOTHING;
INSERT INTO gallery.user_roles (user_id, role) SELECT id, 'USER' FROM gallery.users WHERE username = 'serkan_yilmaz' ON CONFLICT (user_id, role) DO NOTHING;

INSERT INTO gallery.users (username, email, password, first_name, last_name, phone, enabled, is_deleted, is_banned, create_time, update_time, version)
VALUES ('pinar_sen', 'pinar.sen@gmail.com', '$2a$10$AQ/N5rC6y2xDgV2LXCf43OHLtH3Ygimn9cc9OI255u9JgSmzYTSzy', 'Pınar', 'Şen', '+905597778899', TRUE, FALSE, FALSE, NOW() - INTERVAL '5 days', NOW(), 0)
ON CONFLICT (username) DO NOTHING;
INSERT INTO gallery.user_roles (user_id, role) SELECT id, 'USER' FROM gallery.users WHERE username = 'pinar_sen' ON CONFLICT (user_id, role) DO NOTHING;

INSERT INTO gallery.users (username, email, password, first_name, last_name, phone, enabled, is_deleted, is_banned, create_time, update_time, version)
VALUES ('okan_tekin', 'okan.tekin@outlook.com', '$2a$10$AQ/N5rC6y2xDgV2LXCf43OHLtH3Ygimn9cc9OI255u9JgSmzYTSzy', 'Okan', 'Tekin', NULL, TRUE, FALSE, FALSE, NOW(), NOW(), 0)
ON CONFLICT (username) DO NOTHING;
INSERT INTO gallery.user_roles (user_id, role) SELECT id, 'USER' FROM gallery.users WHERE username = 'okan_tekin' ON CONFLICT (user_id, role) DO NOTHING;

-- -----------------------------------------------------------------------------
-- Banned Customers (3 users)
-- -----------------------------------------------------------------------------
INSERT INTO gallery.users (username, email, password, first_name, last_name, phone, enabled, is_deleted, is_banned, banned_at, ban_reason, create_time, update_time, version)
VALUES ('hasan_korkmaz', 'hasan.korkmaz@gmail.com', '$2a$10$AQ/N5rC6y2xDgV2LXCf43OHLtH3Ygimn9cc9OI255u9JgSmzYTSzy', 'Hasan', 'Korkmaz', '+905608889900', TRUE, FALSE, TRUE, NOW() - INTERVAL '30 days', 'Repeated late returns and vehicle damage', NOW() - INTERVAL '200 days', NOW() - INTERVAL '30 days', 0)
ON CONFLICT (username) DO NOTHING;
INSERT INTO gallery.user_roles (user_id, role) SELECT id, 'USER' FROM gallery.users WHERE username = 'hasan_korkmaz' ON CONFLICT (user_id, role) DO NOTHING;

INSERT INTO gallery.users (username, email, password, first_name, last_name, phone, enabled, is_deleted, is_banned, banned_at, ban_reason, create_time, update_time, version)
VALUES ('fatma_aslan', 'fatma.aslan@hotmail.com', '$2a$10$AQ/N5rC6y2xDgV2LXCf43OHLtH3Ygimn9cc9OI255u9JgSmzYTSzy', 'Fatma', 'Aslan', '+905619990011', TRUE, FALSE, TRUE, NOW() - INTERVAL '15 days', 'Payment fraud detected', NOW() - INTERVAL '150 days', NOW() - INTERVAL '15 days', 0)
ON CONFLICT (username) DO NOTHING;
INSERT INTO gallery.user_roles (user_id, role) SELECT id, 'USER' FROM gallery.users WHERE username = 'fatma_aslan' ON CONFLICT (user_id, role) DO NOTHING;

INSERT INTO gallery.users (username, email, password, first_name, last_name, phone, enabled, is_deleted, is_banned, banned_at, ban_reason, create_time, update_time, version)
VALUES ('kerem_tas', 'kerem.tas@yahoo.com', '$2a$10$AQ/N5rC6y2xDgV2LXCf43OHLtH3Ygimn9cc9OI255u9JgSmzYTSzy', 'Kerem', 'Taş', '+905620001122', TRUE, FALSE, TRUE, NOW() - INTERVAL '7 days', 'Terms of service violation', NOW() - INTERVAL '100 days', NOW() - INTERVAL '7 days', 0)
ON CONFLICT (username) DO NOTHING;
INSERT INTO gallery.user_roles (user_id, role) SELECT id, 'USER' FROM gallery.users WHERE username = 'kerem_tas' ON CONFLICT (user_id, role) DO NOTHING;
