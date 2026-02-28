-- KPRFlow Enterprise Database Schema
-- PostgreSQL with Supabase

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create custom types
CREATE TYPE user_role AS ENUM (
    'CUSTOMER', 'MARKETING', 'LEGAL', 'FINANCE', 
    'BANK', 'TEKNIK', 'ESTATE', 'BOD'
);

CREATE TYPE kpr_status AS ENUM (
    'LEAD', 'PEMBERKASAN', 'PROSES_BANK', 'PUTUSAN_KREDIT_ACC', 
    'SP3K_TERBIT', 'PRA_AKAD', 'AKAD_BELUM_CAIR', 'FUNDS_DISBURSED', 
    'BAST_READY', 'BAST_COMPLETED', 'FLOATING_DOSSIER', 'CANCELLED_BY_SYSTEM'
);

CREATE TYPE unit_status AS ENUM ('AVAILABLE', 'BOOKED', 'LOCKED', 'SOLD');

CREATE TYPE document_type AS ENUM (
    'KTP', 'KK', 'NPWP', 'MARRIAGE_CERTIFICATE', 
    'PAYSLIP', 'BANK_STATEMENT', 'WORKPLACE_PHOTO', 'SPR_FORM'
);

-- User Profiles Table
CREATE TABLE user_profiles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    nik VARCHAR(16) UNIQUE NOT NULL,
    phone_number VARCHAR(20),
    marital_status VARCHAR(50),
    role user_role NOT NULL DEFAULT 'CUSTOMER',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    is_active BOOLEAN DEFAULT true
);

-- Unit Properties Table
CREATE TABLE unit_properties (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    block VARCHAR(10) NOT NULL,
    unit_number VARCHAR(10) NOT NULL,
    type VARCHAR(50) NOT NULL,
    price DECIMAL(15,2) NOT NULL,
    status unit_status NOT NULL DEFAULT 'AVAILABLE',
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(block, unit_number)
);

-- KPR Dossiers Table
CREATE TABLE kpr_dossiers (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES user_profiles(id) ON DELETE CASCADE,
    unit_id UUID REFERENCES unit_properties(id) ON DELETE SET NULL,
    status kpr_status NOT NULL DEFAULT 'LEAD',
    booking_date DATE NOT NULL DEFAULT CURRENT_DATE,
    kpr_amount DECIMAL(15,2),
    dp_amount DECIMAL(15,2),
    bank_name VARCHAR(100),
    sp3k_issued_date DATE,
    akad_date DATE,
    disbursed_date DATE,
    bast_date DATE,
    cancellation_reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    notes TEXT
);

-- Documents Table
CREATE TABLE documents (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    dossier_id UUID NOT NULL REFERENCES kpr_dossiers(id) ON DELETE CASCADE,
    type document_type NOT NULL,
    url TEXT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    is_verified BOOLEAN DEFAULT false,
    verified_by UUID REFERENCES user_profiles(id),
    verified_at TIMESTAMP WITH TIME ZONE,
    rejection_reason TEXT,
    uploaded_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(dossier_id, type)
);

-- Financial Transactions Table (for Phase 12)
CREATE TABLE financial_transactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    dossier_id UUID NOT NULL REFERENCES kpr_dossiers(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL, -- 'BOOKING', 'DP', 'DISBURSEMENT', 'REFUND'
    nominal DECIMAL(15,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- 'PENDING', 'VERIFIED', 'REJECTED'
    transaction_date DATE,
    verified_by UUID REFERENCES user_profiles(id),
    verified_at TIMESTAMP WITH TIME ZONE,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Unit Swap Requests Table (for Phase 17)
CREATE TABLE unit_swap_requests (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    dossier_id UUID NOT NULL REFERENCES kpr_dossiers(id) ON DELETE CASCADE,
    old_unit_id UUID NOT NULL REFERENCES unit_properties(id),
    new_unit_id UUID NOT NULL REFERENCES unit_properties(id),
    reason TEXT NOT NULL,
    finance_vote BOOLEAN DEFAULT NULL,
    marketing_vote BOOLEAN DEFAULT NULL,
    legal_vote BOOLEAN DEFAULT NULL,
    status VARCHAR(20) DEFAULT 'PENDING', -- 'PENDING', 'APPROVED', 'REJECTED'
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create indexes for performance
CREATE INDEX idx_user_profiles_email ON user_profiles(email);
CREATE INDEX idx_user_profiles_nik ON user_profiles(nik);
CREATE INDEX idx_user_profiles_role ON user_profiles(role);
CREATE INDEX idx_unit_properties_status ON unit_properties(status);
CREATE INDEX idx_kpr_dossiers_user_id ON kpr_dossiers(user_id);
CREATE INDEX idx_kpr_dossiers_unit_id ON kpr_dossiers(unit_id);
CREATE INDEX idx_kpr_dossiers_status ON kpr_dossiers(status);
CREATE INDEX idx_documents_dossier_id ON documents(dossier_id);
CREATE INDEX idx_documents_type ON documents(type);
CREATE INDEX idx_financial_transactions_dossier_id ON financial_transactions(dossier_id);
CREATE INDEX idx_unit_swap_requests_dossier_id ON unit_swap_requests(dossier_id);

-- Create updated_at trigger function
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create triggers for updated_at
CREATE TRIGGER update_user_profiles_updated_at BEFORE UPDATE ON user_profiles FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_unit_properties_updated_at BEFORE UPDATE ON unit_properties FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_kpr_dossiers_updated_at BEFORE UPDATE ON kpr_dossiers FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_documents_updated_at BEFORE UPDATE ON documents FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_financial_transactions_updated_at BEFORE UPDATE ON financial_transactions FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_unit_swap_requests_updated_at BEFORE UPDATE ON unit_swap_requests FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
