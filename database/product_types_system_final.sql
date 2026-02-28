-- Final Product Types System
-- 10 Banks for All Product Types (8 Conventional + 2 Syariah)
-- KPRFlow Enterprise - Final Bank Integration

-- =====================================================
-- DELETE EXISTING PRODUCT-BANK MAPPINGS
-- =====================================================

-- Delete all existing mappings to start fresh
DELETE FROM product_bank_mapping;

-- =====================================================
-- FINAL BANK INTEGRATION - 10 BANKS FOR ALL PRODUCTS
-- =====================================================

-- Insert KPR_SUBSIDI mappings (10 banks)
INSERT INTO product_bank_mapping (product_id, bank_id, display_category, integration_rules) VALUES
((SELECT id FROM product_types WHERE product_code = 'KPR_SUBSIDI'), (SELECT id FROM target_banks WHERE bank_code = 'BTN'), 'KPR_SUBSIDI', 
 json_build_object('display_name', 'KPR FLPP BTN', 'processing_days', 14, 'special_requirements', json_build_array('FLPP verification'))),

((SELECT id FROM product_types WHERE product_code = 'KPR_SUBSIDI'), (SELECT id FROM target_banks WHERE bank_code = 'BRI'), 'KPR_SUBSIDI', 
 json_build_object('display_name', 'KPR Subsidi BRI', 'processing_days', 14, 'special_requirements', json_build_array('BRI Subsidi program'))),

((SELECT id FROM product_types WHERE product_code = 'KPR_SUBSIDI'), (SELECT id FROM target_banks WHERE bank_code = 'BJB'), 'KPR_SUBSIDI', 
 json_build_object('display_name', 'KPR Subsidi BJB', 'processing_days', 14, 'special_requirements', json_build_array('BJB Subsidi program'))),

((SELECT id FROM product_types WHERE product_code = 'KPR_SUBSIDI'), (SELECT id FROM target_banks WHERE bank_code = 'MANDIRI'), 'KPR_SUBSIDI', 
 json_build_object('display_name', 'KPR Subsidi Mandiri', 'processing_days', 14, 'special_requirements', json_build_array('Mandiri Subsidi program'))),

((SELECT id FROM product_types WHERE product_code = 'KPR_SUBSIDI'), (SELECT id FROM target_banks WHERE bank_code = 'BCA'), 'KPR_SUBSIDI', 
 json_build_object('display_name', 'KPR Subsidi BCA', 'processing_days', 14, 'special_requirements', json_build_array('BCA Subsidi program'))),

((SELECT id FROM product_types WHERE product_code = 'KPR_SUBSIDI'), (SELECT id FROM target_banks WHERE bank_code = 'BNI'), 'KPR_SUBSIDI', 
 json_build_object('display_name', 'KPR Subsidi BNI', 'processing_days', 14, 'special_requirements', json_build_array('BNI Subsidi program'))),

((SELECT id FROM product_types WHERE product_code = 'KPR_SUBSIDI'), (SELECT id FROM target_banks WHERE bank_code = 'CIMB'), 'KPR_SUBSIDI', 
 json_build_object('display_name', 'KPR Subsidi CIMB', 'processing_days', 14, 'special_requirements', json_build_array('CIMB Subsidi program'))),

((SELECT id FROM product_types WHERE product_code = 'KPR_SUBSIDI'), (SELECT id FROM target_banks WHERE bank_code = 'DANAMON'), 'KPR_SUBSIDI', 
 json_build_object('display_name', 'KPR Subsidi Danamon', 'processing_days', 14, 'special_requirements', json_build_array('Danamon Subsidi program'))),

((SELECT id FROM product_types WHERE product_code = 'KPR_SUBSIDI'), (SELECT id FROM target_banks WHERE bank_code = 'BSI'), 'KPR_SUBSIDI', 
 json_build_object('display_name', 'KPR Subsidi BSI', 'processing_days', 14, 'special_requirements', json_build_array('BSI Subsidi program'))),

((SELECT id FROM product_types WHERE product_code = 'KPR_SUBSIDI'), (SELECT id FROM target_banks WHERE bank_code = 'BSN'), 'KPR_SUBSIDI', 
 json_build_object('display_name', 'KPR Subsidi BSN', 'processing_days', 14, 'special_requirements', json_build_array('BSN Subsidi program')));

-- Insert KPR_NON_SUBSIDI_SEMI mappings (10 banks)
INSERT INTO product_bank_mapping (product_id, bank_id, display_category, integration_rules) VALUES
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

((SELECT id FROM product_types WHERE product_code = 'KPR_NON_SUBSIDI_SEMI'), (SELECT id FROM target_banks WHERE bank_code = 'CIMB'), 'KPR_KOMERSIL', 
 json_build_object('display_name', 'KPR Komersil CIMB', 'processing_days', 14, 'original_category', 'SEMI_KOMERSIL')),

((SELECT id FROM product_types WHERE product_code = 'KPR_NON_SUBSIDI_SEMI'), (SELECT id FROM target_banks WHERE bank_code = 'DANAMON'), 'KPR_KOMERSIL', 
 json_build_object('display_name', 'KPR Komersil Danamon', 'processing_days', 14, 'original_category', 'SEMI_KOMERSIL')),

((SELECT id FROM product_types WHERE product_code = 'KPR_NON_SUBSIDI_SEMI'), (SELECT id FROM target_banks WHERE bank_code = 'BSI'), 'KPR_KOMERSIL', 
 json_build_object('display_name', 'KPR Komersil BSI', 'processing_days', 14, 'original_category', 'SEMI_KOMERSIL')),

((SELECT id FROM product_types WHERE product_code = 'KPR_NON_SUBSIDI_SEMI'), (SELECT id FROM target_banks WHERE bank_code = 'BSN'), 'KPR_KOMERSIL', 
 json_build_object('display_name', 'KPR Komersil BSN', 'processing_days', 14, 'original_category', 'SEMI_KOMERSIL'));

-- Insert KPR_NON_SUBSIDI_KOMERSIL mappings (10 banks)
INSERT INTO product_bank_mapping (product_id, bank_id, display_category, integration_rules) VALUES
((SELECT id FROM product_types WHERE product_code = 'KPR_NON_SUBSIDI_KOMERSIL'), (SELECT id FROM target_banks WHERE bank_code = 'BTN'), 'KPR_KOMERSIL', 
 json_build_object('display_name', 'KPR Komersil BTN', 'processing_days', 14, 'original_category', 'KOMERSIL')),

((SELECT id FROM product_types WHERE product_code = 'KPR_NON_SUBSIDI_KOMERSIL'), (SELECT id FROM target_banks WHERE bank_code = 'BRI'), 'KPR_KOMERSIL', 
 json_build_object('display_name', 'KPR Komersil BRI', 'processing_days', 14, 'original_category', 'KOMERSIL')),

((SELECT id FROM product_types WHERE product_code = 'KPR_NON_SUBSIDI_KOMERSIL'), (SELECT id FROM target_banks WHERE bank_code = 'BJB'), 'KPR_KOMERSIL', 
 json_build_object('display_name', 'KPR Komersil BJB', 'processing_days', 14, 'original_category', 'KOMERSIL')),

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

((SELECT id FROM product_types WHERE product_code = 'KPR_NON_SUBSIDI_KOMERSIL'), (SELECT id FROM target_banks WHERE bank_code = 'BSI'), 'KPR_KOMERSIL', 
 json_build_object('display_name', 'KPR Komersil BSI', 'processing_days', 14, 'original_category', 'KOMERSIL')),

((SELECT id FROM product_types WHERE product_code = 'KPR_NON_SUBSIDI_KOMERSIL'), (SELECT id FROM target_banks WHERE bank_code = 'BSN'), 'KPR_KOMERSIL', 
 json_build_object('display_name', 'KPR Komersil BSN', 'processing_days', 14, 'original_category', 'KOMERSIL'));

-- =====================================================
-- UPDATE PRODUCT TYPES WITH FINAL BANK INTEGRATION
-- =====================================================

-- Update all product types with 10 banks
UPDATE product_types 
SET bank_integration_rules = json_build_object(
    'bank_display', CASE 
        WHEN product_code = 'KPR_SUBSIDI' THEN 'KPR_SUBSIDI'
        ELSE 'KPR_KOMERSIL'
    END,
    'integration_type', CASE 
        WHEN product_code = 'KPR_SUBSIDI' THEN 'FLPP_INTEGRATION'
        ELSE 'STANDARD_KPR'
    END,
    'required_banks', json_build_array('BTN', 'BRI', 'BJB', 'MANDIRI', 'BCA', 'BNI', 'CIMB', 'DANAMON', 'BSI', 'BSN'),
    'total_banks', 10,
    'conventional_banks', 8,
    'syariah_banks', 2,
    'digital_banks', 0,
    'foreign_banks', 0,
    'special_processing', CASE 
        WHEN product_code = 'KPR_SUBSIDI' THEN true
        ELSE false
    END,
    'bank_list', json_build_array(
        json_build_object('bank_code', 'BTN', 'bank_name', 'Bank BTN', 'bank_type', 'CONVENTIONAL'),
        json_build_object('bank_code', 'BRI', 'bank_name', 'Bank BRI', 'bank_type', 'CONVENTIONAL'),
        json_build_object('bank_code', 'BJB', 'bank_name', 'Bank BJB', 'bank_type', 'CONVENTIONAL'),
        json_build_object('bank_code', 'MANDIRI', 'bank_name', 'Bank Mandiri', 'bank_type', 'CONVENTIONAL'),
        json_build_object('bank_code', 'BCA', 'bank_name', 'Bank BCA', 'bank_type', 'CONVENTIONAL'),
        json_build_object('bank_code', 'BNI', 'bank_name', 'Bank BNI', 'bank_type', 'CONVENTIONAL'),
        json_build_object('bank_code', 'CIMB', 'bank_name', 'Bank CIMB', 'bank_type', 'CONVENTIONAL'),
        json_build_object('bank_code', 'DANAMON', 'bank_name', 'Bank Danamon', 'bank_type', 'CONVENTIONAL'),
        json_build_object('bank_code', 'BSI', 'bank_name', 'Bank Syariah Indonesia', 'bank_type', 'SYARIAH'),
        json_build_object('bank_code', 'BSN', 'bank_name', 'Bank Syariah Nasional', 'bank_type', 'SYARIAH')
    )
)
WHERE product_code IN ('KPR_SUBSIDI', 'KPR_NON_SUBSIDI_SEMI', 'KPR_NON_SUBSIDI_KOMERSIL');

-- =====================================================
-- FINAL VIEWS WITH UNIFIED BANK INTEGRATION
-- =====================================================

-- Final product types with unified bank integration view
CREATE OR REPLACE VIEW product_types_final_unified AS
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

-- Unified bank coverage summary view
CREATE OR REPLACE VIEW bank_coverage_summary AS
SELECT 
    'UNIFIED_BANK_COVERAGE' as summary_type,
    json_build_object(
        'total_banks', 10,
        'conventional_banks', 8,
        'syariah_banks', 2,
        'digital_banks', 0,
        'foreign_banks', 0,
        'bank_list', json_build_array(
            'BTN', 'BRI', 'BJB', 'MANDIRI', 'BCA', 'BNI', 'CIMB', 'DANAMON', 'BSI', 'BSN'
        ),
        'product_coverage', json_build_object(
            'KPR_SUBSIDI', json_build_object(
                'total_banks', 10,
                'display_category', 'KPR_SUBSIDI',
                'conventional', 8,
                'syariah', 2
            ),
            'KPR_NON_SUBSIDI_SEMI', json_build_object(
                'total_banks', 10,
                'display_category', 'KPR_KOMERSIL',
                'conventional', 8,
                'syariah', 2
            ),
            'KPR_NON_SUBSIDI_KOMERSIL', json_build_object(
                'total_banks', 10,
                'display_category', 'KPR_KOMERSIL',
                'conventional', 8,
                'syariah', 2
            )
        )
    ) as coverage_data;

-- =====================================================
-- FINAL FUNCTIONS FOR UNIFIED BANK INTEGRATION
-- =====================================================

-- Final unified function to get available banks for any product
CREATE OR REPLACE FUNCTION get_unified_banks_for_product(p_product_type product_type_enum)
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

-- Function to validate unified bank availability
CREATE OR REPLACE FUNCTION validate_unified_bank_availability(p_product_type product_type_enum, p_bank_code VARCHAR)
RETURNS JSONB AS $$
DECLARE
    v_is_available BOOLEAN;
    v_bank_info JSONB;
BEGIN
    -- Check if bank is available for product type
    v_is_available := EXISTS(
        SELECT 1 
        FROM product_bank_mapping pbm
        JOIN product_types pt ON pbm.product_id = pt.id
        JOIN target_banks tb ON pbm.bank_id = tb.id
        WHERE pt.product_type = p_product_type 
        AND tb.bank_code = p_bank_code
        AND pbm.is_active = true 
        AND tb.is_active = true
    );
    
    -- Get bank info
    SELECT json_build_object(
        'bank_code', tb.bank_code,
        'bank_name', tb.bank_name,
        'bank_type', tb.bank_type,
        'is_available', v_is_available,
        'product_type', p_product_type
    ) INTO v_bank_info
    FROM target_banks tb
    WHERE tb.bank_code = p_bank_code;
    
    RETURN v_bank_info;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- =====================================================
-- VERIFICATION QUERIES
-- =====================================================

-- Verify all products have 10 banks
DO $$
DECLARE
    v_kpr_subsidi_count INTEGER;
    v_kpr_semi_count INTEGER;
    v_kpr_komersil_count INTEGER;
BEGIN
    -- KPR_SUBSIDI
    SELECT COUNT(*) INTO v_kpr_subsidi_count
    FROM product_bank_mapping pbm
    JOIN product_types pt ON pbm.product_id = pt.id
    WHERE pt.product_code = 'KPR_SUBSIDI' AND pbm.is_active = true;
    
    -- KPR_NON_SUBSIDI_SEMI
    SELECT COUNT(*) INTO v_kpr_semi_count
    FROM product_bank_mapping pbm
    JOIN product_types pt ON pbm.product_id = pt.id
    WHERE pt.product_code = 'KPR_NON_SUBSIDI_SEMI' AND pbm.is_active = true;
    
    -- KPR_NON_SUBSIDI_KOMERSIL
    SELECT COUNT(*) INTO v_kpr_komersil_count
    FROM product_bank_mapping pbm
    JOIN product_types pt ON pbm.product_id = pt.id
    WHERE pt.product_code = 'KPR_NON_SUBSIDI_KOMERSIL' AND pbm.is_active = true;
    
    RAISE NOTICE '=== FINAL BANK INTEGRATION VERIFICATION ===';
    RAISE NOTICE 'KPR_SUBSIDI: % banks (expected: 10)', v_kpr_subsidi_count;
    RAISE NOTICE 'KPR_NON_SUBSIDI_SEMI: % banks (expected: 10)', v_kpr_semi_count;
    RAISE NOTICE 'KPR_NON_SUBSIDI_KOMERSIL: % banks (expected: 10)', v_kpr_komersil_count;
    
    IF v_kpr_subsidi_count = 10 AND v_kpr_semi_count = 10 AND v_kpr_komersil_count = 10 THEN
        RAISE NOTICE '✅ ALL PRODUCTS HAVE 10 BANKS - VERIFICATION PASSED';
    ELSE
        RAISE NOTICE '❌ VERIFICATION FAILED - SOME PRODUCTS DO NOT HAVE 10 BANKS';
    END IF;
END $$;

-- =====================================================
-- DOCUMENTATION
-- =====================================================

/*
FINAL PRODUCT TYPES SYSTEM DOCUMENTATION

========================================
UNIFIED BANK INTEGRATION - 10 BANKS FOR ALL PRODUCTS:

All KPR products now have the same 10 banks:
- 8 Conventional Banks: BTN, BRI, BJB, Mandiri, BCA, BNI, CIMB, Danamon
- 2 Syariah Banks: BSI, BSN
- 0 Digital Banks
- 0 Foreign Banks

========================================
PRODUCT BANK COVERAGE:

1. KPR_SUBSIDI:
   - Total Banks: 10
   - Conventional: 8
   - Syariah: 2
   - Digital: 0
   - Foreign: 0
   - Display: KPR_SUBSIDI

2. KPR_NON_SUBSIDI_SEMI:
   - Total Banks: 10
   - Conventional: 8
   - Syariah: 2
   - Digital: 0
   - Foreign: 0
   - Display: KPR_KOMERSIL

3. KPR_NON_SUBSIDI_KOMERSIL:
   - Total Banks: 10
   - Conventional: 8
   - Syariah: 2
   - Digital: 0
   - Foreign: 0
   - Display: KPR_KOMERSIL

4. CASH_KERAS:
   - Total Banks: 0
   - No bank integration

========================================
BANK LIST (Unified for all KPR products):

Conventional Banks (8):
1. Bank BTN
2. Bank BRI
3. Bank BJB
4. Bank Mandiri
5. Bank BCA
6. Bank BNI
7. Bank CIMB
8. Bank Danamon

Syariah Banks (2):
9. Bank Syariah Indonesia (BSI)
10. Bank Syariah Nasional (BSN)

========================================
BENEFITS OF UNIFIED INTEGRATION:
- Consistent bank options across all products
- Simplified bank management
- Unified processing time (14 days)
- Equal opportunity for all banks
- Simplified customer choice
- Consistent display categories
- Streamlined bank validation

========================================
IMPLEMENTATION STATUS:
✅ All KPR products have 10 banks
✅ 8 Conventional + 2 Syariah banks
✅ No digital or foreign banks
✅ Unified processing time
✅ Consistent display categories
✅ Complete validation functions
*/
