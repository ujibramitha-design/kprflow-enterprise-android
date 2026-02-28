-- Updated Product Types System
-- Enhanced Bank Integration for KPR_SUBSIDI and KPR_NON_SUBSIDI_SEMI
-- KPRFlow Enterprise - Updated Bank Integration

-- =====================================================
-- DELETE EXISTING PRODUCT-BANK MAPPINGS
-- =====================================================

-- Delete existing mappings for KPR_SUBSIDI and KPR_NON_SUBSIDI_SEMI
DELETE FROM product_bank_mapping 
WHERE product_id IN (
    SELECT id FROM product_types 
    WHERE product_code IN ('KPR_SUBSIDI', 'KPR_NON_SUBSIDI_SEMI')
);

-- =====================================================
-- UPDATED PRODUCT-BANK MAPPING
-- =====================================================

-- Insert updated KPR Subsidi mappings (7 banks)
INSERT INTO product_bank_mapping (product_id, bank_id, display_category, integration_rules) VALUES
-- KPR Subsidi with all 7 banks
((SELECT id FROM product_types WHERE product_code = 'KPR_SUBSIDI'), (SELECT id FROM target_banks WHERE bank_code = 'BTN'), 'KPR_SUBSIDI', 
 json_build_object('display_name', 'KPR FLPP BTN', 'processing_days', 14, 'special_requirements', json_build_array('FLPP verification'))),

((SELECT id FROM product_types WHERE product_code = 'KPR_SUBSIDI'), (SELECT id FROM target_banks WHERE bank_code = 'BRI'), 'KPR_SUBSIDI', 
 json_build_object('display_name', 'KPR Subsidi BRI', 'processing_days', 14, 'special_requirements', json_build_array('BRI Subsidi program'))),

((SELECT id FROM product_types WHERE product_code = 'KPR_SUBSIDI'), (SELECT id FROM target_banks WHERE bank_code = 'BJB'), 'KPR_SUBSIDI', 
 json_build_object('display_name', 'KPR Subsidi BJB', 'processing_days', 14, 'special_requirements', json_build_array('BJB Subsidi program'))),

((SELECT id FROM product_types WHERE product_code = 'KPR_SUBSIDI'), (SELECT id FROM target_banks WHERE bank_code = 'MANDIRI'), 'KPR_SUBSIDI', 
 json_build_object('display_name', 'KPR Subsidi Mandiri', 'processing_days', 14, 'special_requirements', json_build_array('Mandiri Subsidi program'))),

((SELECT id FROM product_types WHERE product_code = 'KPR_SUBSIDI'), (SELECT id FROM target_banks WHERE bank_code = 'BNI'), 'KPR_SUBSIDI', 
 json_build_object('display_name', 'KPR Subsidi BNI', 'processing_days', 14, 'special_requirements', json_build_array('BNI Subsidi program'))),

((SELECT id FROM product_types WHERE product_code = 'KPR_SUBSIDI'), (SELECT id FROM target_banks WHERE bank_code = 'BSI'), 'KPR_SUBSIDI', 
 json_build_object('display_name', 'KPR Subsidi BSI', 'processing_days', 14, 'special_requirements', json_build_array('BSI Subsidi program'))),

((SELECT id FROM product_types WHERE product_code = 'KPR_SUBSIDI'), (SELECT id FROM target_banks WHERE bank_code = 'BSN'), 'KPR_SUBSIDI', 
 json_build_object('display_name', 'KPR Subsidi BSN', 'processing_days', 14, 'special_requirements', json_build_array('BSN Subsidi program'))),

-- KPR Non Subsidi Semi Komersil with ALL banks
((SELECT id FROM product_types WHERE product_code = 'KPR_NON_SUBSIDI_SEMI'), (SELECT id FROM target_banks WHERE bank_code = 'BTN'), 'KPR_KOMERSIL', 
 json_build_object('display_name', 'KPR Komersil BTN', 'processing_days', 14, 'original_category', 'SEMI_KOMERSIL')),

((SELECT id FROM product_types WHERE product_code = 'KPR_NON_SUBSIDI_SEMI'), (SELECT id FROM target_banks WHERE bank_code = 'BRI'), 'KPR_KOMERSIL', 
 json_build_object('display_name', 'KPR Komersil BRI', 'processing_days', 14, 'original_category', 'SEMI_KOMERSIL')),

((SELECT id FROM product_types WHERE product_code = 'KPR_NON_SUBSIDI_SEMI'), (SELECT id FROM target_banks WHERE bank_code = 'BJB'), 'KPR_KOMERSIL', 
 json_build_object('display_name', 'KPR Komersil BJB', 'processing_days', 14, 'original_category', 'SEMI_KOMERSIL')),

((SELECT id FROM product_types WHERE product_code = 'KPR_NON_SUBSIDI_SEMI'), (SELECT id FROM target_banks WHERE bank_code = 'MANDIRI'), 'KPR_KOMERSIL', 
 json_build_object('display_name', 'KPR Komersil Mandiri', 'processing_days', 14, 'original_category', 'SEMI_KOMERSIL')),

((SELECT id FROM product_types WHERE product_code = 'KPR_NON_SUBSIDI_SEMI'), (SELECT id FROM target_banks WHERE bank_code = 'BCA'), 'KPR_KOMERSIL', 
 json_build_object('display_name', 'KPR Komersil BCA', 'processing_days', 14, 'original_category', 'SEMI_KOMERSIL')),

((SELECT id FROM product_types WHERE product_code = 'KPR_NON_SUBSIDI_SEMI'), (SELECT id FROM target_banks WHERE bank_code = 'BNI'), 'KPR_KOMERSIL', 
 json_build_object('display_name', 'KPR Komersil BNI', 'processing_days', 14, 'original_category', 'SEMI_KOMERSIL')),

((SELECT id FROM product_types WHERE product_code = 'KPR_NON_SUBSIDI_SEMI'), (SELECT id FROM target_banks WHERE bank_code = 'BSI'), 'KPR_KOMERSIL', 
 json_build_object('display_name', 'KPR Komersil BSI', 'processing_days', 14, 'original_category', 'SEMI_KOMERSIL')),

((SELECT id FROM product_types WHERE product_code = 'KPR_NON_SUBSIDI_SEMI'), (SELECT id FROM target_banks WHERE bank_code = 'BSN'), 'KPR_KOMERSIL', 
 json_build_object('display_name', 'KPR Komersil BSN', 'processing_days', 14, 'original_category', 'SEMI_KOMERSIL')),

((SELECT id FROM product_types WHERE product_code = 'KPR_NON_SUBSIDI_SEMI'), (SELECT id FROM target_banks WHERE bank_code = 'NOBU'), 'KPR_KOMERSIL', 
 json_build_object('display_name', 'KPR Komersil NOBU', 'processing_days', 14, 'original_category', 'SEMI_KOMERSIL')),

((SELECT id FROM product_types WHERE product_code = 'KPR_NON_SUBSIDI_SEMI'), (SELECT id FROM target_banks WHERE bank_code = 'CIMB'), 'KPR_KOMERSIL', 
 json_build_object('display_name', 'KPR Komersil CIMB', 'processing_days', 14, 'original_category', 'SEMI_KOMERSIL')),

((SELECT id FROM product_types WHERE product_code = 'KPR_NON_SUBSIDI_SEMI'), (SELECT id FROM target_banks WHERE bank_code = 'DANAMON'), 'KPR_KOMERSIL', 
 json_build_object('display_name', 'KPR Komersil Danamon', 'processing_days', 14, 'original_category', 'SEMI_KOMERSIL'));

-- =====================================================
-- UPDATE PRODUCT TYPES WITH NEW BANK INTEGRATION
-- =====================================================

-- Update KPR_SUBSIDI bank integration rules
UPDATE product_types 
SET bank_integration_rules = json_build_object(
    'bank_display', 'KPR_SUBSIDI',
    'integration_type', 'FLPP_INTEGRATION',
    'required_banks', json_build_array('BTN', 'BRI', 'BJB', 'MANDIRI', 'BNI', 'BSI', 'BSN'),
    'total_banks', 7,
    'special_processing', true,
    'bank_list', json_build_array(
        json_build_object('bank_code', 'BTN', 'bank_name', 'Bank BTN', 'display_name', 'KPR FLPP BTN'),
        json_build_object('bank_code', 'BRI', 'bank_name', 'Bank BRI', 'display_name', 'KPR Subsidi BRI'),
        json_build_object('bank_code', 'BJB', 'bank_name', 'Bank BJB', 'display_name', 'KPR Subsidi BJB'),
        json_build_object('bank_code', 'MANDIRI', 'bank_name', 'Bank Mandiri', 'display_name', 'KPR Subsidi Mandiri'),
        json_build_object('bank_code', 'BNI', 'bank_name', 'Bank BNI', 'display_name', 'KPR Subsidi BNI'),
        json_build_object('bank_code', 'BSI', 'bank_name', 'Bank Syariah Indonesia', 'display_name', 'KPR Subsidi BSI'),
        json_build_object('bank_code', 'BSN', 'bank_name', 'Bank Syariah Nasional', 'display_name', 'KPR Subsidi BSN')
    )
)
WHERE product_code = 'KPR_SUBSIDI';

-- Update KPR_NON_SUBSIDI_SEMI bank integration rules
UPDATE product_types 
SET bank_integration_rules = json_build_object(
    'bank_display', 'KPR_KOMERSIL',
    'integration_type', 'STANDARD_KPR',
    'required_banks', json_build_array('BTN', 'BRI', 'BJB', 'MANDIRI', 'BCA', 'BNI', 'BSI', 'BSN', 'NOBU', 'CIMB', 'DANAMON'),
    'total_banks', 11,
    'special_processing', false,
    'original_category', 'SEMI_KOMERSIL',
    'bank_list', json_build_array(
        json_build_object('bank_code', 'BTN', 'bank_name', 'Bank BTN', 'display_name', 'KPR Komersil BTN'),
        json_build_object('bank_code', 'BRI', 'bank_name', 'Bank BRI', 'display_name', 'KPR Komersil BRI'),
        json_build_object('bank_code', 'BJB', 'bank_name', 'Bank BJB', 'display_name', 'KPR Komersil BJB'),
        json_build_object('bank_code', 'MANDIRI', 'bank_name', 'Bank Mandiri', 'display_name', 'KPR Komersil Mandiri'),
        json_build_object('bank_code', 'BCA', 'bank_name', 'Bank BCA', 'display_name', 'KPR Komersil BCA'),
        json_build_object('bank_code', 'BNI', 'bank_name', 'Bank BNI', 'display_name', 'KPR Komersil BNI'),
        json_build_object('bank_code', 'BSI', 'bank_name', 'Bank Syariah Indonesia', 'display_name', 'KPR Komersil BSI'),
        json_build_object('bank_code', 'BSN', 'bank_name', 'Bank Syariah Nasional', 'display_name', 'KPR Komersil BSN'),
        json_build_object('bank_code', 'NOBU', 'bank_name', 'Bank NOBU', 'display_name', 'KPR Komersil NOBU'),
        json_build_object('bank_code', 'CIMB', 'bank_name', 'Bank CIMB', 'display_name', 'KPR Komersil CIMB'),
        json_build_object('bank_code', 'DANAMON', 'bank_name', 'Bank Danamon', 'display_name', 'KPR Komersil Danamon')
    )
)
WHERE product_code = 'KPR_NON_SUBSIDI_SEMI';

-- =====================================================
-- UPDATED VIEWS WITH NEW BANK INTEGRATION
-- =====================================================

-- Refresh product types with bank integration view
CREATE OR REPLACE VIEW product_types_with_bank_integration_updated AS
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
            'bank_code', tb.bank_code,
            'bank_name', tb.bank_name,
            'bank_type', tb.bank_type,
            'display_category', pbm.display_category,
            'display_name', pbm.integration_rules->>'display_name',
            'processing_days', (pbm.integration_rules->>'processing_days')::INTEGER,
            'original_category', pbm.integration_rules->>'original_category'
        ) ORDER BY tb.bank_name
    ) FILTER (WHERE pbm.bank_id IS NOT NULL) as bank_integrations,
    -- Bank count
    COUNT(pbm.bank_id) as total_banks,
    pt.is_active,
    pt.created_at,
    pt.updated_at
FROM product_types pt
LEFT JOIN product_bank_mapping pbm ON pt.id = pbm.product_id AND pbm.is_active = true
LEFT JOIN target_banks tb ON pbm.bank_id = tb.id AND tb.is_active = true
WHERE pt.is_active = true
GROUP BY pt.id, pt.product_code, pt.product_name, pt.product_category, pt.product_subcategory, pt.description, pt.requirements, pt.bank_integration_rules, pt.is_active, pt.created_at, pt.updated_at
ORDER BY pt.product_category, pt.product_subcategory;

-- =====================================================
-- UPDATED FUNCTIONS FOR NEW BANK INTEGRATION
-- =====================================================

-- Updated function to get available banks for product
CREATE OR REPLACE FUNCTION get_available_banks_for_product_updated(p_product_type product_type_enum)
RETURNS JSONB AS $$
BEGIN
    RETURN json_agg(
        json_build_object(
            'bank_id', pbm.bank_id,
            'bank_code', tb.bank_code,
            'bank_name', tb.bank_name,
            'bank_type', tb.bank_type,
            'display_category', pbm.display_category,
            'display_name', pbm.integration_rules->>'display_name',
            'processing_days', (pbm.integration_rules->>'processing_days')::INTEGER,
            'integration_rules', pbm.integration_rules,
            'supports_appraisal_lpa', tb.supports_appraisal_lpa,
            'supports_bpn_clearance', tb.supports_bpn_clearance
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

-- Function to validate bank availability for product type
CREATE OR REPLACE FUNCTION validate_bank_for_product_type(p_product_type product_type_enum, p_bank_code VARCHAR)
RETURNS BOOLEAN AS $$
BEGIN
    -- Check if bank is available for product type
    RETURN EXISTS(
        SELECT 1 
        FROM product_bank_mapping pbm
        JOIN product_types pt ON pbm.product_id = pt.id
        JOIN target_banks tb ON pbm.bank_id = tb.id
        WHERE pt.product_type = p_product_type 
        AND tb.bank_code = p_bank_code
        AND pbm.is_active = true 
        AND tb.is_active = true
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- =====================================================
-- SAMPLE DATA VERIFICATION
-- =====================================================

-- Verify KPR_SUBSIDI has 7 banks
DO $$
DECLARE
    v_bank_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_bank_count
    FROM product_bank_mapping pbm
    JOIN product_types pt ON pbm.product_id = pt.id
    WHERE pt.product_code = 'KPR_SUBSIDI' AND pbm.is_active = true;
    
    RAISE NOTICE 'KPR_SUBSIDI has % banks (expected: 7)', v_bank_count;
END $$;

-- Verify KPR_NON_SUBSIDI_SEMI has 11 banks
DO $$
DECLARE
    v_bank_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_bank_count
    FROM product_bank_mapping pbm
    JOIN product_types pt ON pbm.product_id = pt.id
    WHERE pt.product_code = 'KPR_NON_SUBSIDI_SEMI' AND pbm.is_active = true;
    
    RAISE NOTICE 'KPR_NON_SUBSIDI_SEMI has % banks (expected: 11)', v_bank_count;
END $$;

-- =====================================================
-- DOCUMENTATION
-- =====================================================

/*
UPDATED PRODUCT TYPES SYSTEM DOCUMENTATION

========================================
UPDATED BANK INTEGRATION:

1. KPR Subsidi:
   - Banks: BTN, BRI, BJB, Mandiri, BNI, BSI, BSN (7 banks)
   - Display di bank: "KPR_SUBSIDI"
   - Processing: 14 hari
   - Special: FLPP integration

2. KPR Non Subsidi Semi Komersil:
   - Banks: ALL 11 banks (BTN, BRI, BJB, Mandiri, BCA, BNI, BSI, BSN, NOBU, CIMB, Danamon)
   - Display di bank: "KPR_KOMERSIL"
   - Processing: 14 hari
   - Special: Original category tracked internally

3. KPR Non Subsidi Komersil:
   - Banks: 9 banks (BTN, BRI, Mandiri, BCA, BNI, CIMB, Danamon, UOB, Maybank)
   - Display di bank: "KPR_KOMERSIL"
   - Processing: 14 hari
   - Special: Full commercial requirements

4. CASH Keras:
   - Banks: None
   - Display: CASH
   - Processing: Immediate
   - Special: No bank integration

========================================
BANK INTEGRATION SUMMARY:

KPR_SUBSIDI: 7 banks (BTN, BRI, BJB, Mandiri, BNI, BSI, BSN)
KPR_NON_SUBSIDI_SEMI: 11 banks (ALL BANKS)
KPR_NON_SUBSIDI_KOMERSIL: 9 banks (selected commercial banks)
CASH_KERAS: 0 banks (no integration)

========================================
VALIDATION:
- Automatic bank availability validation
- Product type validation for bank selection
- Complete bank integration mapping
- Updated views and functions

========================================
BENEFITS:
- Maximum bank options for customers
- Proper bank integration per product type
- Simplified bank display for commercial products
- Complete tracking of original categories
*/
