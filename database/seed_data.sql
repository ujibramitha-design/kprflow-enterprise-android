-- Seed Data for KPRFlow Enterprise
-- Initial data for testing and development

-- Insert sample users with different roles
INSERT INTO user_profiles (id, name, email, nik, phone_number, marital_status, role) VALUES
('550e8400-e29b-41d4-a716-446655440001', 'John Customer', 'customer@kprflow.com', '1234567890123456', '081234567890', 'Menikah', 'CUSTOMER'),
('550e8400-e29b-41d4-a716-446655440002', 'Sarah Marketing', 'marketing@kprflow.com', '1234567890123457', '081234567891', 'Single', 'MARKETING'),
('550e8400-e29b-41d4-a716-446655440003', 'Michael Legal', 'legal@kprflow.com', '1234567890123458', '081234567892', 'Menikah', 'LEGAL'),
('550e8400-e29b-41d4-a716-446655440004', 'David Finance', 'finance@kprflow.com', '1234567890123459', '081234567893', 'Menikah', 'FINANCE'),
('550e8400-e29b-41d4-a716-446655440005', 'Robert Bank', 'bank@kprflow.com', '1234567890123460', '081234567894', 'Single', 'BANK'),
('550e8400-e29b-41d4-a716-446655440006', 'James Technical', 'technical@kprflow.com', '1234567890123461', '081234567895', 'Menikah', 'TEKNIK'),
('550e8400-e29b-41d4-a716-446655440007', 'William Estate', 'estate@kprflow.com', '1234567890123462', '081234567896', 'Single', 'ESTATE'),
('550e8400-e29b-41d4-a716-446655440008', 'Edward BOD', 'bod@kprflow.com', '1234567890123463', '081234567897', 'Menikah', 'BOD');

-- Insert sample units
INSERT INTO unit_properties (id, block, unit_number, type, price, status, description) VALUES
('660e8400-e29b-41d4-a716-446655440001', 'A', '01', 'Type 36/72', 450000000, 'AVAILABLE', 'Rumah Type 36/72 dengan 2 kamar tidur'),
('660e8400-e29b-41d4-a716-446655440002', 'A', '02', 'Type 36/72', 450000000, 'AVAILABLE', 'Rumah Type 36/72 dengan 2 kamar tidur'),
('660e8400-e29b-41d4-a716-446655440003', 'A', '03', 'Type 45/84', 550000000, 'BOOKED', 'Rumah Type 45/84 dengan 3 kamar tidur'),
('660e8400-e29b-41d4-a716-446655440004', 'B', '01', 'Type 54/96', 650000000, 'AVAILABLE', 'Rumah Type 54/96 dengan 3 kamar tidur'),
('660e8400-e29b-41d4-a716-446655440005', 'B', '02', 'Type 70/120', 850000000, 'LOCKED', 'Rumah Type 70/120 dengan 4 kamar tidur'),
('660e8400-e29b-41d4-a716-446655440006', 'C', '01', 'Type 36/72', 475000000, 'AVAILABLE', 'Rumah Type 36/72 dengan 2 kamar tidur'),
('660e8400-e29b-41d4-a716-446655440007', 'C', '02', 'Type 45/84', 575000000, 'SOLD', 'Rumah Type 45/84 dengan 3 kamar tidur'),
('660e8400-e29b-41d4-a716-446655440008', 'C', '03', 'Type 54/96', 675000000, 'AVAILABLE', 'Rumah Type 54/96 dengan 3 kamar tidur');

-- Insert sample KPR dossiers
INSERT INTO kpr_dossiers (id, user_id, unit_id, status, booking_date, kpr_amount, dp_amount, bank_name, notes) VALUES
('770e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440001', '660e8400-e29b-41d4-a716-446655440003', 'PEMBERKASAN', CURRENT_DATE - INTERVAL '10 days', 495000000, 49500000, 'BCA', 'Dokumen sedang diverifikasi'),
('770e8400-e29b-41d4-a716-446655440002', '550e8400-e29b-41d4-a716-446655440001', '660e8400-e29b-41d4-a716-446655440005', 'PROSES_BANK', CURRENT_DATE - INTERVAL '20 days', 765000000, 76500000, 'Mandiri', 'Menunggu putusan kredit dari bank'),
('770e8400-e29b-41d4-a716-446655440003', '550e8400-e29b-41d4-a716-446655440001', NULL, 'LEAD', CURRENT_DATE - INTERVAL '5 days', NULL, NULL, NULL, 'Lead baru dari SPR form');

-- Insert sample documents
INSERT INTO documents (id, dossier_id, type, url, file_name, file_size, is_verified, uploaded_at) VALUES
('880e8400-e29b-41d4-a716-446655440001', '770e8400-e29b-41d4-a716-446655440001', 'KTP', 'https://storage.supabase.co/kpr_documents/ktp_001.pdf', 'ktp_john_customer.pdf', 1024000, true, CURRENT_DATE - INTERVAL '8 days'),
('880e8400-e29b-41d4-a716-446655440002', '770e8400-e29b-41d4-a716-446655440001', 'KK', 'https://storage.supabase.co/kpr_documents/kk_001.pdf', 'kk_john_customer.pdf', 2048000, true, CURRENT_DATE - INTERVAL '7 days'),
('880e8400-e29b-41d4-a716-446655440003', '770e8400-e29b-41d4-a716-446655440001', 'NPWP', 'https://storage.supabase.co/kpr_documents/npwp_001.pdf', 'npwp_john_customer.pdf', 512000, true, CURRENT_DATE - INTERVAL '6 days'),
('880e8400-e29b-41d4-a716-446655440004', '770e8400-e29b-41d4-a716-446655440001', 'PAYSLIP', 'https://storage.supabase.co/kpr_documents/payslip_001.pdf', 'payslip_john_customer.pdf', 1536000, false, CURRENT_DATE - INTERVAL '5 days'),
('880e8400-e29b-41d4-a716-446655440005', '770e8400-e29b-41d4-a716-446655440002', 'KTP', 'https://storage.supabase.co/kpr_documents/ktp_002.pdf', 'ktp_john_customer_2.pdf', 1024000, true, CURRENT_DATE - INTERVAL '18 days'),
('880e8400-e29b-41d4-a716-446655440006', '770e8400-e29b-41d4-a716-446655440002', 'KK', 'https://storage.supabase.co/kpr_documents/kk_002.pdf', 'kk_john_customer_2.pdf', 2048000, true, CURRENT_DATE - INTERVAL '17 days'),
('880e8400-e29b-41d4-a716-446655440007', '770e8400-e29b-41d4-a716-446655440002', 'NPWP', 'https://storage.supabase.co/kpr_documents/npwp_002.pdf', 'npwp_john_customer_2.pdf', 512000, true, CURRENT_DATE - INTERVAL '16 days'),
('880e8400-e29b-41d4-a716-446655440008', '770e8400-e29b-41d4-a716-446655440002', 'MARRIAGE_CERTIFICATE', 'https://storage.supabase.co/kpr_documents/marriage_002.pdf', 'marriage_john_customer_2.pdf', 3072000, true, CURRENT_DATE - INTERVAL '15 days');

-- Insert sample financial transactions
INSERT INTO financial_transactions (id, dossier_id, type, nominal, status, transaction_date, notes) VALUES
('990e8400-e29b-41d4-a716-446655440001', '770e8400-e29b-41d4-a716-446655440001', 'BOOKING', 5000000, 'VERIFIED', CURRENT_DATE - INTERVAL '10 days', 'Booking fee for unit A-03'),
('990e8400-e29b-41d4-a716-446655440002', '770e8400-e29b-41d4-a716-446655440001', 'DP', 44500000, 'VERIFIED', CURRENT_DATE - INTERVAL '8 days', 'Down payment 10%'),
('990e8400-e29b-41d4-a716-446655440003', '770e8400-e29b-41d4-a716-446655440002', 'BOOKING', 5000000, 'VERIFIED', CURRENT_DATE - INTERVAL '20 days', 'Booking fee for unit B-02'),
('990e8400-e29b-41d4-a716-446655440004', '770e8400-e29b-41d4-a716-446655440002', 'DP', 71500000, 'VERIFIED', CURRENT_DATE - INTERVAL '18 days', 'Down payment 10%');

-- Insert sample unit swap requests
INSERT INTO unit_swap_requests (id, dossier_id, old_unit_id, new_unit_id, reason, finance_vote, marketing_vote, legal_vote, status) VALUES
('aa0e8400-e29b-41d4-a716-446655440001', '770e8400-e29b-41d4-a716-446655440001', '660e8400-e29b-41d4-a716-446655440003', '660e8400-e29b-41d4-a716-446655440004', 'Customer wants larger unit for growing family', true, true, NULL, 'PENDING');
