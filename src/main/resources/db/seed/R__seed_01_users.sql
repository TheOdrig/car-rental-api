INSERT INTO car_rental.users (username, email, password, first_name, last_name, enabled, is_deleted, create_time, update_time, version)
VALUES (
    'admin',
    'admin@example.com',
    '$2a$10$KP5Ol/J0r51UDFsS7kyy0OrgTG/B1grMmOv3T6ObcdwORqdBLQGP2',
    'Admin',
    'User',
    TRUE,
    FALSE,
    NOW(),
    NOW(),
    0
) ON CONFLICT (username) DO NOTHING;

INSERT INTO car_rental.user_roles (user_id, role)
SELECT id, 'ADMIN' FROM car_rental.users WHERE username = 'admin'
ON CONFLICT (user_id, role) DO NOTHING;

INSERT INTO car_rental.user_roles (user_id, role)
SELECT id, 'USER' FROM car_rental.users WHERE username = 'admin'
ON CONFLICT (user_id, role) DO NOTHING;

INSERT INTO car_rental.users (username, email, password, first_name, last_name, phone, enabled, is_deleted, is_banned, create_time, update_time, version)
VALUES ('ahmet_yilmaz', 'ahmet.yilmaz@example.com', '$2a$10$AQ/N5rC6y2xDgV2LXCf43OHLtH3Ygimn9cc9OI255u9JgSmzYTSzy', 'Ahmet', 'Yılmaz', '+1555010014', TRUE, FALSE, FALSE, NOW() - INTERVAL '365 days', NOW(), 0)
ON CONFLICT (username) DO NOTHING;
INSERT INTO car_rental.user_roles (user_id, role) SELECT id, 'USER' FROM car_rental.users WHERE username = 'ahmet_yilmaz' ON CONFLICT (user_id, role) DO NOTHING;

INSERT INTO car_rental.users (username, email, password, first_name, last_name, phone, enabled, is_deleted, is_banned, create_time, update_time, version)
VALUES ('zeynep_kaya', 'zeynep.kaya@example.com', '$2a$10$AQ/N5rC6y2xDgV2LXCf43OHLtH3Ygimn9cc9OI255u9JgSmzYTSzy', 'Zeynep', 'Kaya', '+1555010016', TRUE, FALSE, FALSE, NOW() - INTERVAL '350 days', NOW(), 0)
ON CONFLICT (username) DO NOTHING;
INSERT INTO car_rental.user_roles (user_id, role) SELECT id, 'USER' FROM car_rental.users WHERE username = 'zeynep_kaya' ON CONFLICT (user_id, role) DO NOTHING;

INSERT INTO car_rental.users (username, email, password, first_name, last_name, phone, enabled, is_deleted, is_banned, create_time, update_time, version)
VALUES ('mehmet_demir', 'mehmet.demir@example.com', '$2a$10$AQ/N5rC6y2xDgV2LXCf43OHLtH3Ygimn9cc9OI255u9JgSmzYTSzy', 'Mehmet', 'Demir', '+1555010001', TRUE, FALSE, FALSE, NOW() - INTERVAL '330 days', NOW(), 0)
ON CONFLICT (username) DO NOTHING;
INSERT INTO car_rental.user_roles (user_id, role) SELECT id, 'USER' FROM car_rental.users WHERE username = 'mehmet_demir' ON CONFLICT (user_id, role) DO NOTHING;

INSERT INTO car_rental.users (username, email, password, first_name, last_name, phone, enabled, is_deleted, is_banned, create_time, update_time, version)
VALUES ('ayse_ozturk', 'ayse.ozturk@example.com', '$2a$10$AQ/N5rC6y2xDgV2LXCf43OHLtH3Ygimn9cc9OI255u9JgSmzYTSzy', 'Ayşe', 'Öztürk', '+1555010002', TRUE, FALSE, FALSE, NOW() - INTERVAL '300 days', NOW(), 0)
ON CONFLICT (username) DO NOTHING;
INSERT INTO car_rental.user_roles (user_id, role) SELECT id, 'USER' FROM car_rental.users WHERE username = 'ayse_ozturk' ON CONFLICT (user_id, role) DO NOTHING;

INSERT INTO car_rental.users (username, email, password, first_name, last_name, phone, enabled, is_deleted, is_banned, create_time, update_time, version)
VALUES ('can_arslan', 'can.arslan@example.com', '$2a$10$AQ/N5rC6y2xDgV2LXCf43OHLtH3Ygimn9cc9OI255u9JgSmzYTSzy', 'Can', 'Arslan', '+1555010003', TRUE, FALSE, FALSE, NOW() - INTERVAL '280 days', NOW(), 0)
ON CONFLICT (username) DO NOTHING;
INSERT INTO car_rental.user_roles (user_id, role) SELECT id, 'USER' FROM car_rental.users WHERE username = 'can_arslan' ON CONFLICT (user_id, role) DO NOTHING;

INSERT INTO car_rental.users (username, email, password, first_name, last_name, phone, enabled, is_deleted, is_banned, create_time, update_time, version)
VALUES ('elif_sahin', 'elif.sahin@example.com', '$2a$10$AQ/N5rC6y2xDgV2LXCf43OHLtH3Ygimn9cc9OI255u9JgSmzYTSzy', 'Elif', 'Şahin', '+1555010004', TRUE, FALSE, FALSE, NOW() - INTERVAL '260 days', NOW(), 0)
ON CONFLICT (username) DO NOTHING;
INSERT INTO car_rental.user_roles (user_id, role) SELECT id, 'USER' FROM car_rental.users WHERE username = 'elif_sahin' ON CONFLICT (user_id, role) DO NOTHING;

INSERT INTO car_rental.users (username, email, password, first_name, last_name, phone, enabled, is_deleted, is_banned, create_time, update_time, version)
VALUES ('burak_celik', 'burak.celik@example.com', '$2a$10$AQ/N5rC6y2xDgV2LXCf43OHLtH3Ygimn9cc9OI255u9JgSmzYTSzy', 'Burak', 'Çelik', '+1555010005', TRUE, FALSE, FALSE, NOW() - INTERVAL '240 days', NOW(), 0)
ON CONFLICT (username) DO NOTHING;
INSERT INTO car_rental.user_roles (user_id, role) SELECT id, 'USER' FROM car_rental.users WHERE username = 'burak_celik' ON CONFLICT (user_id, role) DO NOTHING;

INSERT INTO car_rental.users (username, email, password, first_name, last_name, phone, enabled, is_deleted, is_banned, create_time, update_time, version)
VALUES ('selin_yildiz', 'selin.yildiz@example.com', '$2a$10$AQ/N5rC6y2xDgV2LXCf43OHLtH3Ygimn9cc9OI255u9JgSmzYTSzy', 'Selin', 'Yıldız', '+1555010006', TRUE, FALSE, FALSE, NOW() - INTERVAL '220 days', NOW(), 0)
ON CONFLICT (username) DO NOTHING;
INSERT INTO car_rental.user_roles (user_id, role) SELECT id, 'USER' FROM car_rental.users WHERE username = 'selin_yildiz' ON CONFLICT (user_id, role) DO NOTHING;

INSERT INTO car_rental.users (username, email, password, first_name, last_name, phone, enabled, is_deleted, is_banned, create_time, update_time, version)
VALUES ('emre_koc', 'emre.koc@example.com', '$2a$10$AQ/N5rC6y2xDgV2LXCf43OHLtH3Ygimn9cc9OI255u9JgSmzYTSzy', 'Emre', 'Koç', '+1555010007', TRUE, FALSE, FALSE, NOW() - INTERVAL '200 days', NOW(), 0)
ON CONFLICT (username) DO NOTHING;
INSERT INTO car_rental.user_roles (user_id, role) SELECT id, 'USER' FROM car_rental.users WHERE username = 'emre_koc' ON CONFLICT (user_id, role) DO NOTHING;

INSERT INTO car_rental.users (username, email, password, first_name, last_name, phone, enabled, is_deleted, is_banned, create_time, update_time, version)
VALUES ('deniz_aksoy', 'deniz.aksoy@example.com', '$2a$10$AQ/N5rC6y2xDgV2LXCf43OHLtH3Ygimn9cc9OI255u9JgSmzYTSzy', 'Deniz', 'Aksoy', '+1555010008', TRUE, FALSE, FALSE, NOW() - INTERVAL '180 days', NOW(), 0)
ON CONFLICT (username) DO NOTHING;
INSERT INTO car_rental.user_roles (user_id, role) SELECT id, 'USER' FROM car_rental.users WHERE username = 'deniz_aksoy' ON CONFLICT (user_id, role) DO NOTHING;

INSERT INTO car_rental.users (username, email, password, first_name, last_name, phone, enabled, is_deleted, is_banned, create_time, update_time, version)
VALUES ('gokhan_erdogan', 'gokhan.erdogan@example.com', '$2a$10$AQ/N5rC6y2xDgV2LXCf43OHLtH3Ygimn9cc9OI255u9JgSmzYTSzy', 'Gökhan', 'Erdoğan', '+1555010009', TRUE, FALSE, FALSE, NOW() - INTERVAL '160 days', NOW(), 0)
ON CONFLICT (username) DO NOTHING;
INSERT INTO car_rental.user_roles (user_id, role) SELECT id, 'USER' FROM car_rental.users WHERE username = 'gokhan_erdogan' ON CONFLICT (user_id, role) DO NOTHING;

INSERT INTO car_rental.users (username, email, password, first_name, last_name, phone, enabled, is_deleted, is_banned, create_time, update_time, version)
VALUES ('merve_kilic', 'merve.kilic@example.com', '$2a$10$AQ/N5rC6y2xDgV2LXCf43OHLtH3Ygimn9cc9OI255u9JgSmzYTSzy', 'Merve', 'Kılıç', '+1555010010', TRUE, FALSE, FALSE, NOW() - INTERVAL '140 days', NOW(), 0)
ON CONFLICT (username) DO NOTHING;
INSERT INTO car_rental.user_roles (user_id, role) SELECT id, 'USER' FROM car_rental.users WHERE username = 'merve_kilic' ON CONFLICT (user_id, role) DO NOTHING;

INSERT INTO car_rental.users (username, email, password, first_name, last_name, phone, enabled, is_deleted, is_banned, create_time, update_time, version)
VALUES ('ali_polat', 'ali.polat@example.com', '$2a$10$AQ/N5rC6y2xDgV2LXCf43OHLtH3Ygimn9cc9OI255u9JgSmzYTSzy', 'Ali', 'Polat', '+1555010011', TRUE, FALSE, FALSE, NOW() - INTERVAL '120 days', NOW(), 0)
ON CONFLICT (username) DO NOTHING;
INSERT INTO car_rental.user_roles (user_id, role) SELECT id, 'USER' FROM car_rental.users WHERE username = 'ali_polat' ON CONFLICT (user_id, role) DO NOTHING;

INSERT INTO car_rental.users (username, email, password, first_name, last_name, phone, enabled, is_deleted, is_banned, create_time, update_time, version)
VALUES ('ipek_ozdemir', 'ipek.ozdemir@example.com', '$2a$10$AQ/N5rC6y2xDgV2LXCf43OHLtH3Ygimn9cc9OI255u9JgSmzYTSzy', 'İpek', 'Özdemir', '+1555010012', TRUE, FALSE, FALSE, NOW() - INTERVAL '100 days', NOW(), 0)
ON CONFLICT (username) DO NOTHING;
INSERT INTO car_rental.user_roles (user_id, role) SELECT id, 'USER' FROM car_rental.users WHERE username = 'ipek_ozdemir' ON CONFLICT (user_id, role) DO NOTHING;

INSERT INTO car_rental.users (username, email, password, first_name, last_name, phone, enabled, is_deleted, is_banned, create_time, update_time, version)
VALUES ('murat_aydin', 'murat.aydin@example.com', '$2a$10$AQ/N5rC6y2xDgV2LXCf43OHLtH3Ygimn9cc9OI255u9JgSmzYTSzy', 'Murat', 'Aydın', '+1555010013', TRUE, FALSE, FALSE, NOW() - INTERVAL '80 days', NOW(), 0)
ON CONFLICT (username) DO NOTHING;
INSERT INTO car_rental.user_roles (user_id, role) SELECT id, 'USER' FROM car_rental.users WHERE username = 'murat_aydin' ON CONFLICT (user_id, role) DO NOTHING;

INSERT INTO car_rental.users (username, email, password, first_name, last_name, phone, enabled, is_deleted, is_banned, create_time, update_time, version)
VALUES ('ceren_ozer', 'ceren.ozer@example.com', '$2a$10$AQ/N5rC6y2xDgV2LXCf43OHLtH3Ygimn9cc9OI255u9JgSmzYTSzy', 'Ceren', 'Özer', '+1555010015', TRUE, FALSE, FALSE, NOW() - INTERVAL '60 days', NOW(), 0)
ON CONFLICT (username) DO NOTHING;
INSERT INTO car_rental.user_roles (user_id, role) SELECT id, 'USER' FROM car_rental.users WHERE username = 'ceren_ozer' ON CONFLICT (user_id, role) DO NOTHING;

INSERT INTO car_rental.users (username, email, password, first_name, last_name, phone, enabled, is_deleted, is_banned, create_time, update_time, version)
VALUES ('yusuf_karaca', 'yusuf.karaca@example.com', '$2a$10$AQ/N5rC6y2xDgV2LXCf43OHLtH3Ygimn9cc9OI255u9JgSmzYTSzy', 'Yusuf', 'Karaca', '+1555010017', TRUE, FALSE, FALSE, NOW() - INTERVAL '40 days', NOW(), 0)
ON CONFLICT (username) DO NOTHING;
INSERT INTO car_rental.user_roles (user_id, role) SELECT id, 'USER' FROM car_rental.users WHERE username = 'yusuf_karaca' ON CONFLICT (user_id, role) DO NOTHING;

INSERT INTO car_rental.users (username, email, password, first_name, last_name, phone, enabled, is_deleted, is_banned, create_time, update_time, version)
VALUES ('esra_dogan', 'esra.dogan@example.com', '$2a$10$AQ/N5rC6y2xDgV2LXCf43OHLtH3Ygimn9cc9OI255u9JgSmzYTSzy', 'Esra', 'Doğan', '+1555010018', TRUE, FALSE, FALSE, NOW() - INTERVAL '20 days', NOW(), 0)
ON CONFLICT (username) DO NOTHING;
INSERT INTO car_rental.user_roles (user_id, role) SELECT id, 'USER' FROM car_rental.users WHERE username = 'esra_dogan' ON CONFLICT (user_id, role) DO NOTHING;

INSERT INTO car_rental.users (username, email, password, first_name, last_name, phone, enabled, is_deleted, is_banned, create_time, update_time, version)
VALUES ('serkan_yilmaz', 'serkan.yilmaz@example.com', '$2a$10$AQ/N5rC6y2xDgV2LXCf43OHLtH3Ygimn9cc9OI255u9JgSmzYTSzy', 'Serkan', 'Yılmaz', '+1555010019', TRUE, FALSE, FALSE, NOW() - INTERVAL '10 days', NOW(), 0)
ON CONFLICT (username) DO NOTHING;
INSERT INTO car_rental.user_roles (user_id, role) SELECT id, 'USER' FROM car_rental.users WHERE username = 'serkan_yilmaz' ON CONFLICT (user_id, role) DO NOTHING;

INSERT INTO car_rental.users (username, email, password, first_name, last_name, phone, enabled, is_deleted, is_banned, create_time, update_time, version)
VALUES ('pinar_sen', 'pinar.sen@example.com', '$2a$10$AQ/N5rC6y2xDgV2LXCf43OHLtH3Ygimn9cc9OI255u9JgSmzYTSzy', 'Pınar', 'Şen', '+1555010020', TRUE, FALSE, FALSE, NOW() - INTERVAL '5 days', NOW(), 0)
ON CONFLICT (username) DO NOTHING;
INSERT INTO car_rental.user_roles (user_id, role) SELECT id, 'USER' FROM car_rental.users WHERE username = 'pinar_sen' ON CONFLICT (user_id, role) DO NOTHING;

INSERT INTO car_rental.users (username, email, password, first_name, last_name, phone, enabled, is_deleted, is_banned, create_time, update_time, version)
VALUES ('okan_tekin', 'okan.tekin@example.com', '$2a$10$AQ/N5rC6y2xDgV2LXCf43OHLtH3Ygimn9cc9OI255u9JgSmzYTSzy', 'Okan', 'Tekin', NULL, TRUE, FALSE, FALSE, NOW(), NOW(), 0)
ON CONFLICT (username) DO NOTHING;
INSERT INTO car_rental.user_roles (user_id, role) SELECT id, 'USER' FROM car_rental.users WHERE username = 'okan_tekin' ON CONFLICT (user_id, role) DO NOTHING;

INSERT INTO car_rental.users (username, email, password, first_name, last_name, phone, enabled, is_deleted, is_banned, banned_at, ban_reason, create_time, update_time, version)
VALUES ('hasan_korkmaz', 'hasan.korkmaz@example.com', '$2a$10$AQ/N5rC6y2xDgV2LXCf43OHLtH3Ygimn9cc9OI255u9JgSmzYTSzy', 'Hasan', 'Korkmaz', '+1555010021', TRUE, FALSE, TRUE, NOW() - INTERVAL '30 days', 'Repeated late returns and vehicle damage', NOW() - INTERVAL '200 days', NOW() - INTERVAL '30 days', 0)
ON CONFLICT (username) DO NOTHING;
INSERT INTO car_rental.user_roles (user_id, role) SELECT id, 'USER' FROM car_rental.users WHERE username = 'hasan_korkmaz' ON CONFLICT (user_id, role) DO NOTHING;

INSERT INTO car_rental.users (username, email, password, first_name, last_name, phone, enabled, is_deleted, is_banned, banned_at, ban_reason, create_time, update_time, version)
VALUES ('fatma_aslan', 'fatma.aslan@example.com', '$2a$10$AQ/N5rC6y2xDgV2LXCf43OHLtH3Ygimn9cc9OI255u9JgSmzYTSzy', 'Fatma', 'Aslan', '+1555010022', TRUE, FALSE, TRUE, NOW() - INTERVAL '15 days', 'Payment fraud detected', NOW() - INTERVAL '150 days', NOW() - INTERVAL '15 days', 0)
ON CONFLICT (username) DO NOTHING;
INSERT INTO car_rental.user_roles (user_id, role) SELECT id, 'USER' FROM car_rental.users WHERE username = 'fatma_aslan' ON CONFLICT (user_id, role) DO NOTHING;

INSERT INTO car_rental.users (username, email, password, first_name, last_name, phone, enabled, is_deleted, is_banned, banned_at, ban_reason, create_time, update_time, version)
VALUES ('kerem_tas', 'kerem.tas@example.com', '$2a$10$AQ/N5rC6y2xDgV2LXCf43OHLtH3Ygimn9cc9OI255u9JgSmzYTSzy', 'Kerem', 'Taş', '+1555010023', TRUE, FALSE, TRUE, NOW() - INTERVAL '7 days', 'Terms of service violation', NOW() - INTERVAL '100 days', NOW() - INTERVAL '7 days', 0)
ON CONFLICT (username) DO NOTHING;
INSERT INTO car_rental.user_roles (user_id, role) SELECT id, 'USER' FROM car_rental.users WHERE username = 'kerem_tas' ON CONFLICT (user_id, role) DO NOTHING;
