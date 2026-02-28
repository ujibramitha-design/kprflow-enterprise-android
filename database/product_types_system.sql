-- Product Types System
-- KPR Subsidi, KPR Non Subsidi (Semi Komersil & Komersil), CASH Keras
-- KPRFlow Enterprise - Product Types & Bank Integration

-- =====================================================
-- PRODUCT TYPES ENUM & TABLES
-- =====================================================

-- Create product type enum
CREATE TYPE product_type_enum AS ENUM (
    'KPR_SUBSIDI',
    'KPR_NON_SUBSIDI_SEMI_KOMERSIL',
    'KPR_NON_SUBSIDI_KOMERSIL',
    'CASH_KERAS'
);

-- Create product types table
CREATE TABLE product_types (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    product_code VARCHAR(20) NOT NULL UNIQUE,
    product_name VARCHAR(100) NOT NULL,
    product_category VARCHAR(50) NOT NULL, -- KPR, CASH
    product_subcategory VARCHAR(50) NOT NULL, -- SUBSIDI, NON_SUBSIDI, KERAS
    is_active BOOLEAN DEFAULT true,
    description TEXT,
    requirements JSONB, -- Specific requirements per product type
    bank_integration_rules JSONB, -- Rules for bank integration
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create product-bank mapping table
CREATE TABLE product_bank_mapping (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    product_id UUID NOT NULL REFERENCES product_types(id) ON DELETE CASCADE,
    bank_id UUID NOT NULL REFERENCES target_banks(id) ON DELETE CASCADE,
    display_category VARCHAR(50) NOT NULL, -- How to display in bank system
    is_active BOOLEAN DEFAULT true,
    integration_rules JSONB, -- Specific integration rules per bank
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(product_id, bank_id)
);

-- =====================================================
-- INSERT PRODUCT TYPES
-- =====================================================

-- Insert product types
INSERT INTO product_types (product_code, product_name, product_category, product_subcategory, description, requirements, bank_integration_rules) VALUES
('KPR_SUBSIDI', 'KPR Subsidi', 'KPR', 'SUBSIDI', 
 'KPR dengan subsidi pemerintah untuk rumah pertama',
 json_build_object(
     'max_price', 200000000,
     'max_income', 8000000,
     'requirements', json_build_array(
         'KTP', 'KK', 'NPWP', 'Slip Gaji', 'Rekening Koran', 
         'Surat Aktif Kerja', 'SK Tetap', 'Surat Belum Menikah', 
         'Surat Belum Memiliki Rumah', 'Form FLPP'
     ),
     'special_requirements', json_build_array(
         'Maksimal harga rumah 200 juta',
         'Maksimal penghasilan 8 juta/bulan',
         'Wajib FLPP',
         'Rumah pertama'
     )
 ),
 json_build_object(
     'bank_display', 'KPR_SUBSIDI',
     'integration_type', 'FLPP_INTEGRATION',
     'required_banks', json_build_array('BTN', 'BRI', 'Mandiri', 'BNI'),
     'special_processing', true
 )),

('KPR_NON_SUBSIDI_SEMI', 'KPR Non Subsidi Semi Komersil', 'KPR', 'NON_SUBSIDI',
 'KPR non subsidi dengan kategori semi komersil',
 json_build_object(
     'min_price', 200000001,
     'max_price', 500000000,
     'requirements', json_build_array(
         'KTP', 'KK', 'NPWP', 'Slip Gaji', 'Rekening Koran', 
         'Surat Aktif Kerja', 'SK Tetap', 'PKWT/PKWTT', 'Parklaring',
         'Surat Keterangan Domisili', 'Form Aplikasi Bank'
     ),
     'special_requirements', json_build_array(
         'Harga rumah 200-500 juta',
         'Parklaring jika usia kerja < 2 tahun',
         'Surat domisili jika radius > 25KM'
     )
 ),
 json_build_object(
     'bank_display', 'KPR_KOMERSIL', -- Display as Komersil in bank system
     'integration_type', 'STANDARD_KPR',
     'required_banks', json_build_array('BTN', 'BRI', 'Mandiri', 'BCA', 'BNI', 'CIMB', 'Danamon'),
     'special_processing', false
 )),

('KPR_NON_SUBSIDI_KOMERSIL', 'KPR Non Subsidi Komersil', 'KPR', 'NON_SUBSIDI',
 'KPR non subsidi dengan kategori komersil',
 json_build_object(
     'min_price', 500000001,
     'requirements', json_build_array(
         'KTP', 'KK', 'NPWP', 'Slip Gaji', 'Rekening Koran', 
         'Surat Aktif Kerja', 'SK Tetap', 'PKWT/PKWTT', 'Parklaring',
         'Surat Keterangan Domisili', 'Laporan Keuangan', 'Form Aplikasi Bank'
     ),
     'special_requirements', json_build_array(
         'Harga rumah > 500 juta',
         'Parklaring jika usia kerja < 2 tahun',
         'Surat domisili jika radius > 25KM',
         'Laporan keuangan lengkap'
     )
 ),
 json_build_object(
     'bank_display', 'KPR_KOMERSIL', -- Display as Komersil in bank system
     'integration_type', 'STANDARD_KPR',
     'required_banks', json_build_array('BTN', 'BRI', 'Mandiri', 'BCA', 'BNI', 'CIMB', 'Danamon', 'UOB', 'Maybank'),
     'special_processing', false
 )),

('CASH_KERAS', 'CASH Keras', 'CASH', 'KERAS',
 'Pembelian rumah dengan cash keras',
 json_build_object(
     'requirements', json_build_array(
         'KTP', 'KK', 'NPWP', 'Bukti Kekayaan', 
         'Sumber Dana Asal', 'Form Pembelian Cash'
     ),
     'special_requirements', json_build_array(
         'Bukti sumber dana yang jelas',
         'Tidak ada cicilan',
         'Proses cepat'
     )
 ),
 json_build_object(
     'bank_display', 'CASH',
     'integration_type', 'DIRECT_PAYMENT',
     'required_banks', json_build_array(), -- No bank integration needed
     'special_processing', false
 ));

-- =====================================================
-- PRODUCT-BANK MAPPING
-- =====================================================

-- Insert product-bank mappings
INSERT INTO product_bank_mapping (product_id, bank_id, display_category, integration_rules) VALUES
-- KPR Subsidi mappings
((SELECT id FROM product_types WHERE product_code = 'KPR_SUBSIDI'), (SELECT id FROM target_banks WHERE bank_code = 'BTN'), 'KPR_SUBSIDI', 
 json_build_object('display_name', 'KPR FLPP', 'processing_days', 14, 'special_requirements', json_build_array('FLPP verification'))),

((SELECT id FROM product_types WHERE product_code = 'KPR_SUBSIDI'), (SELECT id FROM target_banks WHERE bank_code = 'BRI'), 'KPR_SUBSIDI', 
 json_build_object('display_name', 'KPR Subsidi BRI', 'processing_days', 14, 'special_requirements', json_build_array('BRI Subsidi program'))),

((SELECT id FROM product_types WHERE product_code = 'KPR_SUBSIDI'), (SELECT id FROM target_banks WHERE bank_code = 'MANDIRI'), 'KPR_SUBSIDI', 
 json_build_object('display_name', 'KPR Subsidi Mandiri', 'processing_days', 14, 'special_requirements', json_build_array('Mandiri Subsidi program'))),

((SELECT id FROM product_types WHERE product_code = 'KPR_SUBSIDI'), (SELECT id FROM target_banks WHERE bank_code = 'BNI'), 'KPR_SUBSIDI', 
 json_build_object('display_name', 'KPR Subsidi BNI', 'processing_days', 14, 'special_requirements', json_build_array('BNI Subsidi program'))),

-- KPR Non Subsidi Semi Komersil (display as KPR Komersil in bank)
((SELECT id FROM product_types WHERE product_code = 'KPR_NON_SUBSIDI_SEMI'), (SELECT id FROM target_banks WHERE bank_code = 'BTN'), 'KPR_KOMERSIL', 
 json_build_object('display_name', 'KPR Komersil BTN', 'processing_days', 14, 'original_category', 'SEMI_KOMERSIL')),

((SELECT id FROM product_types WHERE product_code = 'KPR_NON_SUBSIDI_SEMI'), (SELECT id FROM target_banks WHERE bank_code = 'BRI'), 'KPR_KOMERSIL', 
 json_build_object('display_name', 'KPR Komersil BRI', 'processing_days', 14, 'original_category', 'SEMI_KOMERSIL')),

((SELECT id FROM product_types WHERE product_code = 'KPR_NON_SUBSIDI_SEMI'), (SELECT id FROM target_banks WHERE bank_code = 'MANDIRI'), 'KPR_KOMERSIL', 
 json_build_object('display_name', 'KPR Komersil Mandiri', 'processing_days', 14, 'original_category', 'SEMI_KOMERSIL')),

((SELECT id FROM product_types WHERE product_code = 'KPR_NON_SUBSIDI_SEMI'), (SELECT id FROM target_banks WHERE bank_code = 'BCA'), 'KPR_KOMERSIL', 
 json_build_object('display_name', 'KPR Komersil BCA', 'processing_days', 14, 'original_category', 'SEMI_KOMERSIL')),

((SELECT id FROM product_types WHERE product_code = 'KPR_NON_SUBSIDI_SEMI'), (SELECT id FROM target_banks WHERE bank_code = 'BNI'), 'KPR_KOMERSIL', 
 json_build_object('display_name', 'KPR Komersil BNI', 'processing_days', 14, 'original_category', 'SEMI_KOMERSIL')),

-- KPR Non Subsidi Komersil
((SELECT id FROM product_types WHERE product_code = 'KPR_NON_SUBSIDI_KOMERSIL'), (SELECT id FROM target_banks WHERE bank_code = 'BTN'), 'KPR_KOMERSIL', 
 json_build_object('display_name', 'KPR Komersil BTN', 'processing_days', 14, 'original_category', 'KOMERSIL')),

((SELECT id FROM product_types WHERE product_code = 'KPR_NON_SUBSIDI_KOMERSIL'), (SELECT id FROM target_banks WHERE bank_code = 'BRI'), 'KPR_KOMERSIL', 
 json_build_object('display_name', 'KPR Komersil BRI', 'processing_days', 14, 'original_category', 'KOMERSIL')),

((SELECT id FROM product_types WHERE product_code = 'KPR_NON_SUBSIDI_KOMERSIL'), (SELECT id FROM target_banks WHERE bank_code = 'MANDIRI'), 'KPR_KOMERSIL', 
 json_build_object('display_name', 'KPR Komersil Mandiri', 'processing_days', 14, 'original_category', 'KOMERSIL')),

((SELECT id FROM product_types WHERE product_code = 'KPR_NON_SUBSIDI_KOMERSIL'), (SELECT id FROM target_banks WHERE bank_code = 'BCA'), 'KPR_KOMERSIL', 
 json_build_object('display_name', 'KPR Komersil BCA', 'processing_days', 14, 'original_category', 'KOMERSIL')),

((SELECT id FROM product_types WHERE product_code = 'KPR_NON_SUBSIDI_KOMERSIL'), (SELECT id FROM target_banks WHERE bank_code = 'BNI'), 'KPR_KOMERSIL', 
 json_build_object('display_name', 'KPR Komersil BNI', 'processing_days', 14, 'original_category', 'KOMERSIL')),

((SELECT id FROM product_types WHERE product_code = 'KPR_NON_SUBSIDI_KOMERSIL'), (SELECT id FROM target_banks WHERE bank_code = 'CIMB'), 'KPR_KOMERSIL', 
 json_build_object('display_name', 'KPR Komersil CIMB', 'processing_days', 14, 'original_category', 'KOMERSIL')),

((SELECT id FROM product_types WHERE product_code = 'KPR_NON_SUBSIDI_KOMERSIL'), (SELECT id FROM target_banks WHERE bank_code = 'DANAMON'), 'KPR_KOMERSIL', 
 json_build_object('display_name', 'KPR Komersil Danamon', 'processing_days', 14, 'original_category', 'KOMERSIL')),

((SELECT id FROM product_types WHERE product_code = 'KPR_NON_SUBSIDI_KOMERSIL'), (SELECT id FROM target_banks WHERE bank_code = 'UOB'), 'KPR_KOMERSIL', 
 json_build_object('display_name', 'KPR Komersil UOB', 'processing_days', 14, 'original_category', 'KOMERSIL')),

((SELECT id FROM product_types WHERE product_code = 'KPR_NON_SUBSIDI_KOMERSIL'), (SELECT id FROM target_banks WHERE bank_code = 'MAYBANK'), 'KPR_KOMERSIL', 
 json_build_object('display_name', 'KPR Komersil Maybank', 'processing_days', 14, 'original_category', 'KOMERSIL'));

-- =====================================================
-- UPDATE KPR DOSSIERS WITH PRODUCT TYPE
-- =====================================================

-- Add product type to kpr_dossiers
ALTER TABLE kpr_dossiers ADD COLUMN product_type_id UUID REFERENCES product_types(id);
ALTER TABLE kpr_dossiers ADD COLUMN product_type product_type_enum;

-- Create function to determine product type based on unit price
CREATE OR REPLACE FUNCTION determine_product_type(p_unit_price DECIMAL)
RETURNS product_type_enum AS $$
BEGIN
    -- CASH Keras (manual selection, not based on price)
    -- KPR Subsidi: <= 200 juta
    IF p_unit_price <= 200000000 THEN
        RETURN 'KPR_SUBSIDI';
    -- KPR Non Subsidi Semi Komersil: 200-500 juta
    ELSIF p_unit_price <= 500000000 THEN
        RETURN 'KPR_NON_SUBSIDI_SEMI_KOMERSIL';
    -- KPR Non Subsidi Komersil: > 500 juta
    ELSE
        RETURN 'KPR_NON_SUBSIDI_KOMERSIL';
    END IF;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Function to get bank display category for product
CREATE OR REPLACE FUNCTION get_bank_display_category(p_product_type product_type_enum, p_bank_id UUID)
RETURNS VARCHAR AS $$
DECLARE
    v_display_category VARCHAR;
BEGIN
    -- Get display category from product-bank mapping
    SELECT pbm.display_category INTO v_display_category
    FROM product_bank_mapping pbm
    JOIN product_types pt ON pbm.product_id = pt.id
    WHERE pt.product_type = p_product_type AND pbm.bank_id = p_bank_id;
    
    RETURN COALESCE(v_display_category, p_product_type::TEXT);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- =====================================================
-- VIEWS FOR PRODUCT TYPES
-- =====================================================

-- Product types with bank integration view
CREATE VIEW product_types_with_bank_integration AS
SELECT 
    pt.id,
    pt.product_code,
    pt.product_name,
    pt.product_category,
    pt.product_subcategory,
    pt.description,
    pt.requirements,
    pt.bank_integration_rules,
    -- Bank integration info
    json_agg(
        json_build_object(
            'bank_id', pbm.bank_id,
            'bank_name', tb.bank_name,
            'display_category', pbm.display_category,
            'display_name', pbm.integration_rules->>'display_name',
            'processing_days', (pbm.integration_rules->>'processing_days')::INTEGER,
            'original_category', pbm.integration_rules->>'original_category'
        ) ORDER BY tb.bank_name
    ) as bank_integrations,
    pt.is_active,
    pt.created_at,
    pt.updated_at
FROM product_types pt
LEFT JOIN product_bank_mapping pbm ON pt.id = pbm.product_id AND pbm.is_active = true
LEFT JOIN target_banks tb ON pbm.bank_id = tb.id AND tb.is_active = true
WHERE pt.is_active = true
GROUP BY pt.id, pt.product_code, pt.product_name, pt.product_category, pt.product_subcategory, pt.description, pt.requirements, pt.bank_integration_rules, pt.is_active, pt.created_at, pt.updated_at
ORDER BY pt.product_category, pt.product_subcategory;

-- KPR dossiers with product type and bank integration view
CREATE VIEW kpr_dossiers_with_product_type AS
SELECT 
    kd.*,
    pt.product_code,
    pt.product_name,
    pt.product_category,
    pt.product_subcategory,
    pt.requirements as product_requirements,
    -- Bank integration display
    get_bank_display_category(kd.product_type, tb.id) as bank_display_category,
    CASE 
        WHEN kd.product_type = 'KPR_NON_SUBSIDI_SEMI' THEN 'KPR_KOMERSIL'
        WHEN kd.product_type = 'KPR_NON_SUBSIDI_KOMERSIL' THEN 'KPR_KOMERSIL'
        ELSE kd.product_type::TEXT
    END as bank_integration_category,
    -- Product validation
    CASE 
        WHEN kd.product_type = 'KPR_SUBSIDI' THEN 
            CASE WHEN u.price <= 200000000 THEN true ELSE false END
        WHEN kd.product_type = 'KPR_NON_SUBSIDI_SEMI' THEN 
            CASE WHEN u.price > 200000000 AND u.price <= 500000000 THEN true ELSE false END
        WHEN kd.product_type = 'KPR_NON_SUBSIDI_KOMERSIL' THEN 
            CASE WHEN u.price > 500000000 THEN true ELSE false END
        ELSE true
    END as product_type_valid
FROM kpr_dossiers kd
JOIN product_types pt ON kd.product_type_id = pt.id
JOIN units u ON kd.unit_id = u.id
LEFT JOIN target_banks tb ON kd.bank_name = tb.bank_name;

-- =====================================================
-- FUNCTIONS FOR PRODUCT TYPE VALIDATION
-- =====================================================

-- Function to validate product type for unit
CREATE OR REPLACE FUNCTION validate_product_type_for_unit(p_unit_id UUID, p_product_type product_type_enum)
RETURNS JSONB AS $$
DECLARE
    v_unit_data RECORD;
    v_validation_result JSONB;
    v_is_valid BOOLEAN := false;
    v_reasons TEXT[];
BEGIN
    -- Get unit data
    SELECT * INTO v_unit_data FROM units WHERE id = p_unit_id;
    
    -- Validate based on product type
    CASE p_product_type
        WHEN 'KPR_SUBSIDI' THEN
            v_is_valid := v_unit_data.price <= 200000000;
            IF NOT v_is_valid THEN
                v_reasons := ARRAY_APPEND(v_reasons, 'Harga unit > 200 juta (maksimal KPR Subsidi)');
            END IF;
            
        WHEN 'KPR_NON_SUBSIDI_SEMI_KOMERSIL' THEN
            v_is_valid := v_unit_data.price > 200000000 AND v_unit_data.price <= 500000000;
            IF NOT v_is_valid THEN
                IF v_unit_data.price <= 200000000 THEN
                    v_reasons := ARRAY_APPEND(v_reasons, 'Harga unit <= 200 juta (seharusnya KPR Subsidi)');
                ELSIF v_unit_data.price > 500000000 THEN
                    v_reasons := ARRAY_APPEND(v_reasons, 'Harga unit > 500 juta (seharusnya KPR Komersil)');
                END IF;
            END IF;
            
        WHEN 'KPR_NON_SUBSIDI_KOMERSIL' THEN
            v_is_valid := v_unit_data.price > 500000000;
            IF NOT v_is_valid THEN
                v_reasons := ARRAY_APPEND(v_reasons, 'Harga unit <= 500 juta (seharusnya KPR Semi Komersil atau Subsidi)');
            END IF;
            
        WHEN 'CASH_KERAS' THEN
            v_is_valid := true; -- Cash keras always valid
            
    END CASE;
    
    -- Build validation result
    v_validation_result := json_build_object(
        'unit_id', p_unit_id,
        'product_type', p_product_type,
        'unit_price', v_unit_data.price,
        'is_valid', v_is_valid,
        'validation_reasons', v_reasons,
        'recommendations', CASE 
            WHEN v_is_valid THEN json_build_array('Product type valid untuk unit ini')
            ELSE json_build_array(
                'Product type tidak valid untuk unit ini',
                'Harga unit: ' || v_unit_data.price,
                'Product type yang sesuai: ' || determine_product_type(v_unit_data.price)::TEXT
            )
        END
    );
    
    RETURN v_validation_result;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Function to get available banks for product type
CREATE OR REPLACE FUNCTION get_available_banks_for_product(p_product_type product_type_enum)
RETURNS JSONB AS $$
BEGIN
    RETURN json_agg(
        json_build_object(
            'bank_id', pbm.bank_id,
            'bank_name', tb.bank_name,
            'display_category', pbm.display_category,
            'display_name', pbm.integration_rules->>'display_name',
            'processing_days', (pbm.integration_rules->>'processing_days')::INTEGER,
            'integration_rules', pbm.integration_rules
        ) ORDER BY tb.bank_name
    )
    FROM product_bank_mapping pbm
    JOIN product_types pt ON pbm.product_id = pt.id
    JOIN target_banks tb ON pbm.bank_id = tb.id
    WHERE pt.product_type = p_product_type 
    AND pbm.is_active = true 
    AND tb.is_active = true;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- =====================================================
-- TRIGGERS FOR PRODUCT TYPE VALIDATION
-- =====================================================

-- Trigger to validate product type when dossier is created
CREATE OR REPLACE FUNCTION validate_product_type_trigger()
RETURNS TRIGGER AS $$
DECLARE
    v_validation_result JSONB;
    v_recommended_type product_type_enum;
BEGIN
    -- Validate product type for unit
    v_validation_result := validate_product_type_for_unit(NEW.unit_id, NEW.product_type);
    
    -- If not valid, set recommended type
    IF NOT (v_validation_result->>'is_valid')::BOOLEAN THEN
        v_recommended_type := determine_product_type((SELECT price FROM units WHERE id = NEW.unit_id));
        
        -- Log validation error
        INSERT INTO system_logs (
            log_type, log_message, log_data, created_at
        ) VALUES (
            'PRODUCT_TYPE_VALIDATION_ERROR',
            'Product type ' || NEW.product_type::TEXT || ' not valid for unit ' || NEW.unit_id::TEXT,
            v_validation_result,
            NOW()
        );
        
        -- Optionally auto-correct (commented out for manual review)
        -- NEW.product_type := v_recommended_type;
        -- NEW.product_type_id := (SELECT id FROM product_types WHERE product_type = v_recommended_type);
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Create trigger
CREATE TRIGGER trigger_validate_product_type
    BEFORE INSERT OR UPDATE ON kpr_dossiers
    FOR EACH ROW EXECUTE FUNCTION validate_product_type_trigger();

-- =====================================================
-- RLS POLICIES
-- =====================================================

-- RLS for product types
ALTER TABLE product_types ENABLE ROW LEVEL SECURITY;
ALTER TABLE product_bank_mapping ENABLE ROW LEVEL SECURITY;

CREATE POLICY "All users can view active product types" ON product_types
    FOR SELECT USING (is_active = true);

CREATE POLICY "Legal can manage product types" ON product_types
    FOR ALL USING (
        EXISTS (
            SELECT 1 FROM user_profiles 
            WHERE id = auth.uid() AND role = 'LEGAL'
        )
    );

CREATE POLICY "All users can view product bank mappings" ON product_bank_mapping
    FOR SELECT USING (is_active = true);

CREATE POLICY "Legal can manage product bank mappings" ON product_bank_mapping
    FOR ALL USING (
        EXISTS (
            SELECT 1 FROM user_profiles 
            WHERE id = auth.uid() AND role = 'LEGAL'
        )
    );

-- =====================================================
-- DOCUMENTATION
-- =====================================================

/*
PRODUCT TYPES SYSTEM DOCUMENTATION

========================================
PRODUCT TYPES:

1. KPR Subsidi:
   - Harga: <= 200 juta
   - Bank Integration: Display as "KPR_SUBSIDI" in bank system
   - Banks: BTN, BRI, Mandiri, BNI
   - Special: FLPP integration

2. KPR Non Subsidi Semi Komersil:
   - Harga: 200-500 juta
   - Bank Integration: Display as "KPR_KOMERSIL" in bank system
   - Banks: BTN, BRI, Mandiri, BCA, BNI, CIMB, Danamon
   - Special: Original category tracked internally

3. KPR Non Subsidi Komersil:
   - Harga: > 500 juta
   - Bank Integration: Display as "KPR_KOMERSIL" in bank system
   - Banks: BTN, BRI, Mandiri, BCA, BNI, CIMB, Danamon, UOB, Maybank
   - Special: Full commercial requirements

4. CASH Keras:
   - Harga: Any
   - Bank Integration: No bank integration needed
   - Processing: Direct payment
   - Special: Fast processing

========================================
BANK INTEGRATION RULES:

- KPR Non Subsidi Semi Komersil → Display as "KPR_KOMERSIL" in bank
- KPR Non Subsidi Komersil → Display as "KPR_KOMERSIL" in bank
- Original category tracked internally for reporting
- Bank sees unified "KPR_KOMERSIL" category

========================================
VALIDATION:
- Automatic product type validation based on unit price
- Recommended product type suggestions
- Validation error logging
- Manual review for invalid combinations

========================================
BENEFITS:
- Clear product type categorization
- Proper bank integration mapping
- Simplified bank system display
- Internal tracking of original categories
- Automated validation and recommendations
*/
