-- Localization tables for Phase 19: Multi-language Support

-- Translations table
CREATE TABLE IF NOT EXISTS translations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    key VARCHAR(255) NOT NULL,
    language VARCHAR(10) NOT NULL, -- 'en', 'id'
    value TEXT NOT NULL,
    category VARCHAR(50), -- 'common', 'dashboard', 'dossier', 'document', etc.
    description TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(key, language)
);

-- Language preferences table (could be part of user_profiles)
ALTER TABLE user_profiles ADD COLUMN IF NOT EXISTS preferred_language VARCHAR(10) DEFAULT 'id';

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_translations_key ON translations(key);
CREATE INDEX IF NOT EXISTS idx_translations_language ON translations(language);
CREATE INDEX IF NOT EXISTS idx_translations_category ON translations(category);
CREATE INDEX IF NOT EXISTS idx_translations_is_active ON translations(is_active);
CREATE INDEX IF NOT EXISTS idx_translations_composite ON translations(key, language);

-- RLS Policies for translations
ALTER TABLE translations ENABLE ROW LEVEL SECURITY;

-- System can manage translations
CREATE POLICY "System can manage translations" ON translations
    FOR ALL USING (auth.jwt() ->> 'role' = 'SYSTEM');

-- Admin can manage translations
CREATE POLICY "Admin can manage translations" ON translations
    FOR ALL USING (auth.jwt() ->> 'role' = 'ADMIN');

-- BOD can read translations
CREATE POLICY "BOD can read translations" ON translations
    FOR SELECT USING (auth.jwt() ->> 'role' = 'BOD');

-- Users can read active translations
CREATE POLICY "Users can read active translations" ON translations
    FOR SELECT USING (is_active = TRUE);

-- Insert default translations
INSERT INTO translations (key, language, value, category, description) VALUES
-- Common translations
('app_name', 'en', 'KPRFlow Enterprise', 'common', 'Application name'),
('app_name', 'id', 'KPRFlow Enterprise', 'common', 'Application name'),
('welcome', 'en', 'Welcome', 'common', 'Welcome message'),
('welcome', 'id', 'Selamat Datang', 'common', 'Welcome message'),
('login', 'en', 'Login', 'common', 'Login action'),
('login', 'id', 'Masuk', 'common', 'Login action'),
('logout', 'en', 'Logout', 'common', 'Logout action'),
('logout', 'id', 'Keluar', 'common', 'Logout action'),
('dashboard', 'en', 'Dashboard', 'common', 'Dashboard page'),
('dashboard', 'id', 'Dasbor', 'common', 'Dashboard page'),
('profile', 'en', 'Profile', 'common', 'User profile'),
('profile', 'id', 'Profil', 'common', 'User profile'),
('settings', 'en', 'Settings', 'common', 'Settings page'),
('settings', 'id', 'Pengaturan', 'common', 'Settings page'),
('save', 'en', 'Save', 'common', 'Save action'),
('save', 'id', 'Simpan', 'common', 'Save action'),
('cancel', 'en', 'Cancel', 'common', 'Cancel action'),
('cancel', 'id', 'Batal', 'common', 'Cancel action'),
('delete', 'en', 'Delete', 'common', 'Delete action'),
('delete', 'id', 'Hapus', 'common', 'Delete action'),
('edit', 'en', 'Edit', 'common', 'Edit action'),
('edit', 'id', 'Edit', 'common', 'Edit action'),
('search', 'en', 'Search', 'common', 'Search action'),
('search', 'id', 'Cari', 'common', 'Search action'),
('loading', 'en', 'Loading...', 'common', 'Loading message'),
('loading', 'id', 'Memuat...', 'common', 'Loading message'),
('error', 'en', 'Error', 'common', 'Error message'),
('error', 'id', 'Kesalahan', 'common', 'Error message'),
('success', 'en', 'Success', 'common', 'Success message'),
('success', 'id', 'Berhasil', 'common', 'Success message'),
('warning', 'en', 'Warning', 'common', 'Warning message'),
('warning', 'id', 'Peringatan', 'common', 'Warning message'),
('info', 'en', 'Information', 'common', 'Information message'),
('info', 'id', 'Informasi', 'common', 'Information message'),
('yes', 'en', 'Yes', 'common', 'Yes option'),
('yes', 'id', 'Ya', 'common', 'Yes option'),
('no', 'en', 'No', 'common', 'No option'),
('no', 'id', 'Tidak', 'common', 'No option'),
('ok', 'en', 'OK', 'common', 'OK button'),
('ok', 'id', 'OK', 'common', 'OK button'),
('retry', 'en', 'Retry', 'common', 'Retry action'),
('retry', 'id', 'Coba Lagi', 'common', 'Retry action'),
('refresh', 'en', 'Refresh', 'common', 'Refresh action'),
('refresh', 'id', 'Segarkan', 'common', 'Refresh action'),
('submit', 'en', 'Submit', 'common', 'Submit action'),
('submit', 'id', 'Kirim', 'common', 'Submit action'),
('upload', 'en', 'Upload', 'common', 'Upload action'),
('upload', 'id', 'Unggah', 'common', 'Upload action'),
('download', 'en', 'Download', 'common', 'Download action'),
('download', 'id', 'Unduh', 'common', 'Download action'),
('view', 'en', 'View', 'common', 'View action'),
('view', 'id', 'Lihat', 'common', 'View action'),
('close', 'en', 'Close', 'common', 'Close action'),
('close', 'id', 'Tutup', 'common', 'Close action'),
('back', 'en', 'Back', 'common', 'Back action'),
('back', 'id', 'Kembali', 'common', 'Back action'),
('next', 'en', 'Next', 'common', 'Next action'),
('next', 'id', 'Selanjutnya', 'common', 'Next action'),
('previous', 'en', 'Previous', 'common', 'Previous action'),
('previous', 'id', 'Sebelumnya', 'common', 'Previous action'),

-- Dashboard translations
('kpr_application', 'en', 'KPR Application', 'dashboard', 'KPR Application title'),
('kpr_application', 'id', 'Aplikasi KPR', 'dashboard', 'KPR Application title'),
('application_status', 'en', 'Application Status', 'dashboard', 'Application status label'),
('application_status', 'id', 'Status Aplikasi', 'dashboard', 'Application status label'),
('documents', 'en', 'Documents', 'dashboard', 'Documents section'),
('documents', 'id', 'Dokumen', 'dashboard', 'Documents section'),
('payments', 'en', 'Payments', 'dashboard', 'Payments section'),
('payments', 'id', 'Pembayaran', 'dashboard', 'Payments section'),
('notifications', 'en', 'Notifications', 'dashboard', 'Notifications section'),
('notifications', 'id', 'Notifikasi', 'dashboard', 'Notifications section'),

-- Dossier translations
('dossier_id', 'en', 'Dossier ID', 'dossier', 'Dossier ID label'),
('dossier_id', 'id', 'ID Dossier', 'dossier', 'Dossier ID label'),
('customer_name', 'en', 'Customer Name', 'dossier', 'Customer name label'),
('customer_name', 'id', 'Nama Pelanggan', 'dossier', 'Customer name label'),
('property_type', 'en', 'Property Type', 'dossier', 'Property type label'),
('property_type', 'id', 'Tipe Properti', 'dossier', 'Property type label'),
('loan_amount', 'en', 'Loan Amount', 'dossier', 'Loan amount label'),
('loan_amount', 'id', 'Jumlah Pinjaman', 'dossier', 'Loan amount label'),
('down_payment', 'en', 'Down Payment', 'dossier', 'Down payment label'),
('down_payment', 'id', 'Uang Muka', 'dossier', 'Down payment label'),
('bank_name', 'en', 'Bank Name', 'dossier', 'Bank name label'),
('bank_name', 'id', 'Nama Bank', 'dossier', 'Bank name label'),

-- Document translations
('upload_document', 'en', 'Upload Document', 'document', 'Upload document action'),
('upload_document', 'id', 'Unggah Dokumen', 'document', 'Upload document action'),
('document_type', 'en', 'Document Type', 'document', 'Document type label'),
('document_type', 'id', 'Jenis Dokumen', 'document', 'Document type label'),
('verification_status', 'en', 'Verification Status', 'document', 'Verification status label'),
('verification_status', 'id', 'Status Verifikasi', 'document', 'Verification status label'),
('verified', 'en', 'Verified', 'document', 'Verified status'),
('verified', 'id', 'Terverifikasi', 'document', 'Verified status'),
('pending', 'en', 'Pending', 'document', 'Pending status'),
('pending', 'id', 'Menunggu', 'document', 'Pending status'),
('rejected', 'en', 'Rejected', 'document', 'Rejected status'),
('rejected', 'id', 'Ditolak', 'document', 'Rejected status'),

-- Payment translations
('payment_schedule', 'en', 'Payment Schedule', 'payment', 'Payment schedule title'),
('payment_schedule', 'id', 'Jadwal Pembayaran', 'payment', 'Payment schedule title'),
('installment', 'en', 'Installment', 'payment', 'Installment label'),
('installment', 'id', 'Cicilan', 'payment', 'Installment label'),
('due_date', 'en', 'Due Date', 'payment', 'Due date label'),
('due_date', 'id', 'Tanggal Jatuh Tempo', 'payment', 'Due date label'),
('paid_amount', 'en', 'Paid Amount', 'payment', 'Paid amount label'),
('paid_amount', 'id', 'Jumlah Dibayar', 'payment', 'Paid amount label'),
('remaining_amount', 'en', 'Remaining Amount', 'payment', 'Remaining amount label'),
('remaining_amount', 'id', 'Sisa Jumlah', 'payment', 'Remaining amount label'),
('overdue', 'en', 'Overdue', 'payment', 'Overdue status'),
('overdue', 'id', 'Terlambat', 'payment', 'Overdue status'),

-- Error messages
('error_network', 'en', 'Network error. Please check your connection.', 'error', 'Network error message'),
('error_network', 'id', 'Kesalahan jaringan. Silakan periksa koneksi Anda.', 'error', 'Network error message'),
('error_server', 'en', 'Server error. Please try again later.', 'error', 'Server error message'),
('error_server', 'id', 'Kesalahan server. Silakan coba lagi nanti.', 'error', 'Server error message'),
('error_validation', 'en', 'Validation error. Please check your input.', 'error', 'Validation error message'),
('error_validation', 'id', 'Kesalahan validasi. Silakan periksa input Anda.', 'error', 'Validation error message'),
('error_unauthorized', 'en', 'Unauthorized access. Please login again.', 'error', 'Unauthorized error message'),
('error_unauthorized', 'id', 'Akses tidak sah. Silakan masuk kembali.', 'error', 'Unauthorized error message'),
('error_not_found', 'en', 'Data not found.', 'error', 'Not found error message'),
('error_not_found', 'id', 'Data tidak ditemukan.', 'error', 'Not found error message'),

-- Validation messages
('validation_required', 'en', 'This field is required.', 'validation', 'Required field validation'),
('validation_required', 'id', 'Field ini wajib diisi.', 'validation', 'Required field validation'),
('validation_email', 'en', 'Please enter a valid email address.', 'validation', 'Email validation'),
('validation_email', 'id', 'Silakan masukkan alamat email yang valid.', 'validation', 'Email validation'),
('validation_phone', 'en', 'Please enter a valid phone number.', 'validation', 'Phone validation'),
('validation_phone', 'id', 'Silakan masukkan nomor telepon yang valid.', 'validation', 'Phone validation'),
('validation_min_length', 'en', 'Minimum length is {0} characters.', 'validation', 'Minimum length validation'),
('validation_min_length', 'id', 'Panjang minimum adalah {0} karakter.', 'validation', 'Minimum length validation'),
('validation_max_length', 'en', 'Maximum length is {0} characters.', 'validation', 'Maximum length validation'),
('validation_max_length', 'id', 'Panjang maksimum adalah {0} karakter.', 'validation', 'Maximum length validation')
ON CONFLICT (key, language) DO NOTHING;

-- Function to get translation
CREATE OR REPLACE FUNCTION get_translation(
    p_key VARCHAR(255),
    p_language VARCHAR(10) DEFAULT 'id',
    p_default_value TEXT DEFAULT NULL
)
RETURNS TEXT AS $$
DECLARE
    translation_value TEXT;
BEGIN
    SELECT value INTO translation_value
    FROM translations
    WHERE key = p_key
    AND language = p_language
    AND is_active = TRUE;
    
    -- Return translation or default value
    RETURN COALESCE(translation_value, p_default_value, p_key);
END;
$$ LANGUAGE plpgsql;

-- Function to get translations for a language
CREATE OR REPLACE FUNCTION get_translations(
    p_language VARCHAR(10) DEFAULT 'id',
    p_category VARCHAR(50) DEFAULT NULL
)
RETURNS TABLE (
    key VARCHAR(255),
    value TEXT
) AS $$
BEGIN
    RETURN QUERY
    SELECT key, value
    FROM translations
    WHERE language = p_language
    AND is_active = TRUE
    AND (p_category IS NULL OR category = p_category)
    ORDER BY key;
END;
$$ LANGUAGE plpgsql;

-- Function to add or update translation
CREATE OR REPLACE FUNCTION upsert_translation(
    p_key VARCHAR(255),
    p_language VARCHAR(10),
    p_value TEXT,
    p_category VARCHAR(50) DEFAULT NULL,
    p_description TEXT DEFAULT NULL
)
RETURNS BOOLEAN AS $$
BEGIN
    INSERT INTO translations (key, language, value, category, description)
    VALUES (p_key, p_language, p_value, p_category, p_description)
    ON CONFLICT (key, language) 
    DO UPDATE SET 
        value = p_value,
        category = p_category,
        description = p_description,
        updated_at = NOW();
    
    RETURN TRUE;
END;
$$ LANGUAGE plpgsql;

-- Function to get user preferred language
CREATE OR REPLACE FUNCTION get_user_language(
    p_user_id UUID
)
RETURNS VARCHAR(10) AS $$
DECLARE
    user_language VARCHAR(10);
BEGIN
    SELECT preferred_language INTO user_language
    FROM user_profiles
    WHERE id = p_user_id;
    
    RETURN COALESCE(user_language, 'id');
END;
$$ LANGUAGE plpgsql;

-- Function to set user preferred language
CREATE OR REPLACE FUNCTION set_user_language(
    p_user_id UUID,
    p_language VARCHAR(10)
)
RETURNS BOOLEAN AS $$
BEGIN
    UPDATE user_profiles
    SET preferred_language = p_language,
        updated_at = NOW()
    WHERE id = p_user_id;
    
    RETURN FOUND;
END;
$$ LANGUAGE plpgsql;

-- Function to get translation statistics
CREATE OR REPLACE FUNCTION get_translation_statistics()
RETURNS TABLE (
    total_translations BIGINT,
    language_stats JSONB,
    supported_languages INTEGER
) AS $$
BEGIN
    RETURN QUERY
    WITH lang_stats AS (
        SELECT 
            language,
            COUNT(*) as count,
            ROUND((COUNT(*)::DECIMAL / (SELECT COUNT(*) FROM translations WHERE is_active = TRUE)::DECIMAL) * 100, 2) as percentage
        FROM translations
        WHERE is_active = TRUE
        GROUP BY language
        ORDER BY count DESC
    )
    SELECT 
        (SELECT COUNT(*) FROM translations WHERE is_active = TRUE) as total_translations,
        jsonb_agg(
            jsonb_build_object(
                'language', language,
                'count', count,
                'percentage', percentage
            )
        ) as language_stats,
        (SELECT COUNT(DISTINCT language) FROM translations WHERE is_active = TRUE) as supported_languages
    FROM lang_stats;
END;
$$ LANGUAGE plpgsql;

-- Trigger for updated_at timestamp
CREATE OR REPLACE FUNCTION update_translations_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_translations_updated_at BEFORE UPDATE ON translations FOR EACH ROW EXECUTE FUNCTION update_translations_updated_at();
