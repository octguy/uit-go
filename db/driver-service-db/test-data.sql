-- Insert test data for driver service

-- Insert test drivers
INSERT INTO drivers.drivers (id, user_id, license_number, vehicle_model, vehicle_plate, rating, total_completed_trips, status, vehicle_capacity, created_at, updated_at) VALUES
('550e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440011', 'LICENSE-001', 'Toyota Vios', '51A-12345', 4.5, 150, 'AVAILABLE', 4, extract(epoch from now()) * 1000, extract(epoch from now()) * 1000),
('550e8400-e29b-41d4-a716-446655440002', '550e8400-e29b-41d4-a716-446655440012', 'LICENSE-002', 'Honda City', '51B-67890', 4.2, 120, 'AVAILABLE', 4, extract(epoch from now()) * 1000, extract(epoch from now()) * 1000),
('550e8400-e29b-41d4-a716-446655440003', '550e8400-e29b-41d4-a716-446655440013', 'LICENSE-003', 'Toyota Camry', '51C-11111', 4.8, 200, 'BUSY', 4, extract(epoch from now()) * 1000, extract(epoch from now()) * 1000);

-- Insert test driver locations near Ben Thanh Market, Ho Chi Minh City (10.762622, 106.660172)
INSERT INTO drivers.driver_locations (id, driver_id, latitude, longitude, timestamp, geohash) VALUES
('650e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440001', 10.762622, 106.660172, extract(epoch from now()) * 1000, 'w3gvk1ysj'),
('650e8400-e29b-41d4-a716-446655440002', '550e8400-e29b-41d4-a716-446655440002', 10.765000, 106.665000, extract(epoch from now()) * 1000, 'w3gvk2abc'),
('650e8400-e29b-41d4-a716-446655440003', '550e8400-e29b-41d4-a716-446655440003', 10.770000, 106.670000, extract(epoch from now()) * 1000, 'w3gvk3def');

-- Insert driver sessions
INSERT INTO drivers.driver_sessions (id, driver_id, online_at, offline_at, is_active, total_distance_km, total_earnings) VALUES
('750e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440001', extract(epoch from now()) * 1000 - 3600000, NULL, true, 45.5, 125.75),
('750e8400-e29b-41d4-a716-446655440002', '550e8400-e29b-41d4-a716-446655440002', extract(epoch from now()) * 1000 - 7200000, NULL, true, 32.1, 89.50),
('750e8400-e29b-41d4-a716-446655440003', '550e8400-e29b-41d4-a716-446655440003', extract(epoch from now()) * 1000 - 1800000, NULL, true, 28.7, 67.25);