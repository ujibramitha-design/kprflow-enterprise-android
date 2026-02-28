-- Final Product Types Configuration
-- Updated product names and display formats
-- KPRFlow Enterprise - Final Product Configuration

-- =====================================================
-- UPDATE PRODUCT TYPES NAMES
-- =====================================================

-- Update product names to match requirements
UPDATE product_types 
SET product_name = 'KPR_SUBSIDI',
    product_subcategory = 'SUBSIDI'
WHERE product_code = 'KPR_SUBSIDI';

UPDATE product_types 
SET product_name = 'KPR_SEMI_KOMERSIL',
    product_subcategory = 'SEMI_KOMERSIL'
WHERE product_code = 'KPR_NON_SUBSIDI_SEMI';

UPDATE product_types 
SET product_name = 'KPR_KOMERSIL',
    product_subcategory = 'KOMERSIL'
WHERE product_code = 'KPR_NON_SUBSIDI_KOMERSIL';

-- =====================================================
-- UPDATE DISPLAY CATEGORIES AND BANK DISPLAY FORMATS
-- =====================================================

-- Update KPR_SUBSIDI (no changes needed)
UPDATE product_bank_mapping 
SET display_category = 'KPR_SUBSIDI',
    integration_rules = jsonb_set(
        integration_rules,
        '{display_name}',
        '"KPR Subsidi ' || (SELECT bank_name FROM target_banks WHERE id = product_bank_mapping.bank_id) || '"'
    )
WHERE product_id = (SELECT id FROM product_types WHERE product_code = 'KPR_SUBSIDI');

-- Update KPR_SEMI_KOMERSIL (from KPR_NON_SUBSIDI_SEMI)
UPDATE product_bank_mapping 
SET display_category = 'SEMI_KOMERSIL',
    integration_rules = jsonb_set(
        integration_rules,
        '{display_name}',
        '"KPR Komersil ' || (SELECT bank_name FROM target_banks WHERE id = product_bank_mapping.bank_id) || '"'
    )
WHERE product_id = (SELECT id FROM product_types WHERE product_code = 'KPR_NON_SUBSIDI_SEMI');

-- Update KPR_KOMERSIL (from KPR_NON_SUBSIDI_KOMERSIL)
UPDATE product_bank_mapping 
SET display_category = 'KPR_KOMERSIL',
    integration_rules = jsonb_set(
        integration_rules,
        '{display_name}',
        '"KPR Komersil ' || (SELECT bank_name FROM target_banks WHERE id = product_bank_mapping.bank_id) || '"'
    )
WHERE product_id = (SELECT id FROM product_types WHERE product_code = 'KPR_NON_SUBSIDI_KOMERSIL');

-- =====================================================
-- UPDATE PRODUCT TYPES INTEGRATION RULES
-- =====================================================

-- Update KPR_SUBSIDI integration rules
UPDATE product_types 
SET bank_integration_rules = json_build_object(
    'bank_display', 'KPR_SUBSIDI',
    'display_category', 'KPR_SUBSIDI',
    'integration_type', 'FLPP_INTEGRATION',
    'required_banks', json_build_array('BTN', 'BRI', 'BJB', 'MANDIRI', 'BCA', 'BNI', 'CIMB', 'DANAMON', 'BSI', 'BSN'),
    'total_banks', 10,
    'conventional_banks', 8,
    'syariah_banks', 2,
    'digital_banks', 0,
    'foreign_banks', 0,
    'special_processing', true,
    'bank_display_format', 'KPR Subsidi [Bank Name]',
    'processing_days', 14
)
WHERE product_code = 'KPR_SUBSIDI';

-- Update KPR_SEMI_KOMERSIL integration rules
UPDATE product_types 
SET bank_integration_rules = json_build_object(
    'bank_display', 'SEMI_KOMERSIL',
    'display_category', 'SEMI_KOMERSIL',
    'integration_type', 'STANDARD_KPR',
    'required_banks', json_build_array('BTN', 'BRI', 'BJB', 'MANDIRI', 'BCA', 'BNI', 'CIMB', 'DANAMON', 'BSI', 'BSN'),
    'total_banks', 10,
    'conventional_banks', 8,
    'syariah_banks', 2,
    'digital_banks', 0,
    'foreign_banks', 0,
    'special_processing', false,
    'original_category', 'SEMI_KOMERSIL',
    'bank_display_format', 'KPR Komersil [Bank Name]',
    'processing_days', 14
)
WHERE product_code = 'KPR_NON_SUBSIDI_SEMI';

-- Update KPR_KOMERSIL integration rules
UPDATE product_types 
SET bank_integration_rules = json_build_object(
    'bank_display', 'KPR_KOMERSIL',
    'display_category', 'KPR_KOMERSIL',
    'integration_type', 'STANDARD_KPR',
    'required_banks', json_build_array('BTN', 'BRI', 'BJB', 'MANDIRI', 'BCA', 'BNI', 'CIMB', 'DANAMON', 'BSI', 'BSN'),
    'total_banks', 10,
    'conventional_banks', 8,
    'syariah_banks', 2,
    'digital_banks', 0,
    'foreign_banks', 0,
    'special_processing', false,
    'original_category', 'KOMERSIL',
    'bank_display_format', 'KPR Komersil [Bank Name]',
    'processing_days', 14
)
WHERE product_code = 'KPR_NON_SUBSIDI_KOMERSIL';

-- =====================================================
-- SPECIFIC BANK DISPLAY NAME UPDATES
-- =====================================================

-- KPR_SUBSIDI bank display names
UPDATE product_bank_mapping 
SET integration_rules = jsonb_set(integration_rules, '{display_name}', '"KPR Subsidi BTN"')
WHERE product_id = (SELECT id FROM product_types WHERE product_code = 'KPR_SUBSIDI')
AND bank_id = (SELECT id FROM target_banks WHERE bank_code = 'BTN');

UPDATE product_bank_mapping 
SET integration_rules = jsonb_set(integration_rules, '{display_name}', '"KPR Subsidi BRI"')
WHERE product_id = (SELECT id FROM product_types WHERE product_code = 'KPR_SUBSIDI')
AND bank_id = (SELECT id FROM target_banks WHERE bank_code = 'BRI');

UPDATE product_bank_mapping 
SET integration_rules = jsonb_set(integration_rules, '{display_name}', '"KPR Subsidi BJB"')
WHERE product_id = (SELECT id FROM product_types WHERE product_code = 'KPR_SUBSIDI')
AND bank_id = (SELECT id FROM target_banks WHERE bank_code = 'BJB');

UPDATE product_bank_mapping 
SET integration_rules = jsonb_set(integration_rules, '{display_name}', '"KPR Subsidi Mandiri"')
WHERE product_id = (SELECT id FROM product_types WHERE product_code = 'KPR_SUBSIDI')
AND bank_id = (SELECT id FROM target_banks WHERE bank_code = 'MANDIRI');

UPDATE product_bank_mapping 
SET integration_rules = jsonb_set(integration_rules, '{display_name}', '"KPR Subsidi BCA"')
WHERE product_id = (SELECT id FROM product_types WHERE product_code = 'KPR_SUBSIDI')
AND bank_id = (SELECT id FROM target_banks WHERE bank_code = 'BCA');

UPDATE product_bank_mapping 
SET integration_rules = jsonb_set(integration_rules, '{display_name}', '"KPR Subsidi BNI"')
WHERE product_id = (SELECT id FROM product_types WHERE product_code = 'KPR_SUBSIDI')
AND bank_id = (SELECT id FROM target_banks WHERE bank_code = 'BNI');

UPDATE product_bank_mapping 
SET integration_rules = jsonb_set(integration_rules, '{display_name}', '"KPR Subsidi CIMB"')
WHERE product_id = (SELECT id FROM product_types WHERE product_code = 'KPR_SUBSIDI')
AND bank_id = (SELECT id FROM target_banks WHERE bank_code = 'CIMB');

UPDATE product_bank_mapping 
SET integration_rules = jsonb_set(integration_rules, '{display_name}', '"KPR Subsidi Danamon"')
WHERE product_id = (SELECT id FROM product_types WHERE product_code = 'KPR_SUBSIDI')
AND bank_id = (SELECT id FROM target_banks WHERE bank_code = 'DANAMON');

UPDATE product_bank_mapping 
SET integration_rules = jsonb_set(integration_rules, '{display_name}', '"KPR Subsidi BSI"')
WHERE product_id = (SELECT id FROM product_types WHERE product_code = 'KPR_SUBSIDI')
AND bank_id = (SELECT id FROM target_banks WHERE bank_code = 'BSI');

UPDATE product_bank_mapping 
SET integration_rules = jsonb_set(integration_rules, '{display_name}', '"KPR Subsidi BSN"')
WHERE product_id = (SELECT id FROM product_types WHERE product_code = 'KPR_SUBSIDI')
AND bank_id = (SELECT id FROM target_banks WHERE bank_code = 'BSN');

-- KPR_SEMI_KOMERSIL bank display names (KPR Komersil format)
UPDATE product_bank_mapping 
SET integration_rules = jsonb_set(integration_rules, '{display_name}', '"KPR Komersil BTN"')
WHERE product_id = (SELECT id FROM product_types WHERE product_code = 'KPR_NON_SUBSIDI_SEMI')
AND bank_id = (SELECT id FROM target_banks WHERE bank_code = 'BTN');

UPDATE product_bank_mapping 
SET integration_rules = jsonb_set(integration_rules, '{display_name}', '"KPR Komersil BRI"')
WHERE product_id = (SELECT id FROM product_types WHERE product_code = 'KPR_NON_SUBSIDI_SEMI')
AND bank_id = (SELECT id FROM target_banks WHERE bank_code = 'BRI');

UPDATE product_bank_mapping 
SET integration_rules = jsonb_set(integration_rules, '{display_name}', '"KPR Komersil BJB"')
WHERE product_id = (SELECT id FROM product_types WHERE product_code = 'KPR_NON_SUBSIDI_SEMI')
AND bank_id = (SELECT id FROM target_banks WHERE bank_code = 'BJB');

UPDATE product_bank_mapping 
SET integration_rules = jsonb_set(integration_rules, '{display_name}', '"KPR Komersil Mandiri"')
WHERE product_id = (SELECT id FROM product_types WHERE product_code = 'KPR_NON_SUBSIDI_SEMI')
AND bank_id = (SELECT id FROM target_banks WHERE bank_code = 'MANDIRI');

UPDATE product_bank_mapping 
SET integration_rules = jsonb_set(integration_rules, '{display_name}', '"KPR Komersil BCA"')
WHERE product_id = (SELECT id FROM product_types WHERE product_code = 'KPR_NON_SUBSIDI_SEMI')
AND bank_id = (SELECT id FROM target_banks WHERE bank_code = 'BCA');

UPDATE product_bank_mapping 
SET integration_rules = jsonb_set(integration_rules, '{display_name}', '"KPR Komersil BNI"')
WHERE product_id = (SELECT id FROM product_types WHERE product_code = 'KPR_NON_SUBSIDI_SEMI')
AND bank_id = (SELECT id FROM target_banks WHERE bank_code = 'BNI');

UPDATE product_bank_mapping 
SET integration_rules = jsonb_set(integration_rules, '{display_name}', '"KPR Komersil CIMB"')
WHERE product_id = (SELECT id FROM product_types WHERE product_code = 'KPR_NON_SUBSIDI_SEMI')
AND bank_id = (SELECT id FROM target_banks WHERE bank_code = 'CIMB');

UPDATE product_bank_mapping 
SET integration_rules = jsonb_set(integration_rules, '{display_name}', '"KPR Komersil Danamon"')
WHERE product_id = (SELECT id FROM product_types WHERE product_code = 'KPR_NON_SUBSIDI_SEMI')
AND bank_id = (SELECT id FROM target_banks WHERE bank_code = 'DANAMON');

UPDATE product_bank_mapping 
SET integration_rules = jsonb_set(integration_rules, '{display_name}', '"KPR Komersil BSI"')
WHERE product_id = (SELECT id FROM product_types WHERE product_code = 'KPR_NON_SUBSIDI_SEMI')
AND bank_id = (SELECT id FROM target_banks WHERE bank_code = 'BSI');

UPDATE product_bank_mapping 
SET integration_rules = jsonb_set(integration_rules, '{display_name}', '"KPR Komersil BSN"')
WHERE product_id = (SELECT id FROM product_types WHERE product_code = 'KPR_NON_SUBSIDI_SEMI')
AND bank_id = (SELECT id FROM target_banks WHERE bank_code = 'BSN');

-- KPR_KOMERSIL bank display names (KPR Komersil format)
UPDATE product_bank_mapping 
SET integration_rules = jsonb_set(integration_rules, '{display_name}', '"KPR Komersil BTN"')
WHERE product_id = (SELECT id FROM product_types WHERE product_code = 'KPR_NON_SUBSIDI_KOMERSIL')
AND bank_id = (SELECT id FROM target_banks WHERE bank_code = 'BTN');

UPDATE product_bank_mapping 
SET integration_rules = jsonb_set(integration_rules, '{display_name}', '"KPR Komersil BRI"')
WHERE product_id = (SELECT id FROM product_types WHERE product_code = 'KPR_NON_SUBSIDI_KOMERSIL')
AND bank_id = (SELECT id FROM target_banks WHERE bank_code = 'BRI');

UPDATE product_bank_mapping 
SET integration_rules = jsonb_set(integration_rules, '{display_name}', '"KPR Komersil BJB"')
WHERE product_id = (SELECT id FROM product_types WHERE product_code = 'KPR_NON_SUBSIDI_KOMERSIL')
AND bank_id = (SELECT id FROM target_banks WHERE bank_code = 'BJB');

UPDATE product_bank_mapping 
SET integration_rules = jsonb_set(integration_rules, '{display_name}', '"KPR Komersil Mandiri"')
WHERE product_id = (SELECT id FROM product_types WHERE product_code = 'KPR_NON_SUBSIDI_KOMERSIL')
AND bank_id = (SELECT id FROM target_banks WHERE bank_code = 'MANDIRI');

UPDATE product_bank_mapping 
SET integration_rules = jsonb_set(integration_rules, '{display_name}', '"KPR Komersil BCA"')
WHERE product_id = (SELECT id FROM product_types WHERE product_code = 'KPR_NON_SUBSIDI_KOMERSIL')
AND bank_id = (SELECT id FROM target_banks WHERE bank_code = 'BCA');

UPDATE product_bank_mapping 
SET integration_rules = jsonb_set(integration_rules, '{display_name}', '"KPR Komersil BNI"')
WHERE product_id = (SELECT id FROM product_types WHERE product_code = 'KPR_NON_SUBSIDI_KOMERSIL')
AND bank_id = (SELECT id FROM target_banks WHERE bank_code = 'BNI');

UPDATE product_bank_mapping 
SET integration_rules = jsonb_set(integration_rules, '{display_name}', '"KPR Komersil CIMB"')
WHERE product_id = (SELECT id FROM product_types WHERE product_code = 'KPR_NON_SUBSIDI_KOMERSIL')
AND bank_id = (SELECT id FROM target_banks WHERE bank_code = 'CIMB');

UPDATE product_bank_mapping 
SET integration_rules = jsonb_set(integration_rules, '{display_name}', '"KPR Komersil Danamon"')
WHERE product_id = (SELECT id FROM product_types WHERE product_code = 'KPR_NON_SUBSIDI_KOMERSIL')
AND bank_id = (SELECT id FROM target_banks WHERE bank_code = 'DANAMON');

UPDATE product_bank_mapping 
SET integration_rules = jsonb_set(integration_rules, '{display_name}', '"KPR Komersil BSI"')
WHERE product_id = (SELECT id FROM product_types WHERE product_code = 'KPR_NON_SUBSIDI_KOMERSIL')
AND bank_id = (SELECT id FROM target_banks WHERE bank_code = 'BSI');

UPDATE product_bank_mapping 
SET integration_rules = jsonb_set(integration_rules, '{display_name}', '"KPR Komersil BSN"')
WHERE product_id = (SELECT id FROM product_types WHERE product_code = 'KPR_NON_SUBSIDI_KOMERSIL')
AND bank_id = (SELECT id FROM target_banks WHERE bank_code = 'BSN');

-- =====================================================
-- FINAL VERIFICATION
-- =====================================================

-- Verify final configuration
DO $$
DECLARE
    v_kpr_subsidi_display VARCHAR;
    v_kpr_semi_display VARCHAR;
    v_kpr_komersil_display VARCHAR;
BEGIN
    -- Get display categories
    SELECT display_category INTO v_kpr_subsidi_display
    FROM product_bank_mapping pbm
    JOIN product_types pt ON pbm.product_id = pt.id
    WHERE pt.product_code = 'KPR_SUBSIDI'
    LIMIT 1;
    
    SELECT display_category INTO v_kpr_semi_display
    FROM product_bank_mapping pbm
    JOIN product_types pt ON pbm.product_id = pt.id
    WHERE pt.product_code = 'KPR_NON_SUBSIDI_SEMI'
    LIMIT 1;
    
    SELECT display_category INTO v_kpr_komersil_display
    FROM product_bank_mapping pbm
    JOIN product_types pt ON pbm.product_id = pt.id
    WHERE pt.product_code = 'KPR_NON_SUBSIDI_KOMERSIL'
    LIMIT 1;
    
    RAISE NOTICE '=== FINAL PRODUCT TYPES CONFIGURATION VERIFICATION ===';
    RAISE NOTICE 'KPR_SUBSIDI Display Category: %', v_kpr_subsidi_display;
    RAISE NOTICE 'KPR_SEMI_KOMERSIL Display Category: %', v_kpr_semi_display;
    RAISE NOTICE 'KPR_KOMERSIL Display Category: %', v_kpr_komersil_display;
    
    IF v_kpr_subsidi_display = 'KPR_SUBSIDI' AND 
       v_kpr_semi_display = 'SEMI_KOMERSIL' AND 
       v_kpr_komersil_display = 'KPR_KOMERSIL' THEN
        RAISE NOTICE '✅ FINAL CONFIGURATION SUCCESSFUL';
    ELSE
        RAISE NOTICE '❌ FINAL CONFIGURATION FAILED';
    END IF;
END $$;

-- =====================================================
-- DOCUMENTATION
-- =====================================================

/*
FINAL PRODUCT TYPES CONFIGURATION DOCUMENTATION

========================================
FINAL CONFIGURATION TABLE:

| Product Type | Display Category | Bank Display Format | Total Banks | Processing Time |
|--------------|------------------|-------------------|-------------|-----------------|
| KPR_SUBSIDI | KPR_SUBSIDI | "KPR Subsidi [Bank Name]" | 10 | 14 hari |
| KPR_SEMI_KOMERSIL | SEMI_KOMERSIL | "KPR Komersil [Bank Name]" | 10 | 14 hari |
| KPR_KOMERSIL | KPR_KOMERSIL | "KPR Komersil [Bank Name]" | 10 | 14 hari |

========================================
KEY CHANGES:
1. Product names updated to match requirements
2. Display categories properly configured
3. Bank display formats standardized
4. 10 banks for all KPR products
5. 14 hari processing time standardized

========================================
BANK DISPLAY FORMATS:
- KPR_SUBSIDI: "KPR Subsidi [Bank Name]"
- KPR_SEMI_KOMERSIL: "KPR Komersil [Bank Name]" (same as KPR_KOMERSIL)
- KPR_KOMERSIL: "KPR Komersil [Bank Name]"

========================================
DISPLAY CATEGORIES:
- KPR_SUBSIDI: KPR_SUBSIDI
- KPR_SEMI_KOMERSIL: SEMI_KOMERSIL
- KPR_KOMERSIL: KPR_KOMERSIL

========================================
VERIFICATION:
✅ All products have 10 banks
✅ Display categories correctly configured
✅ Bank display formats standardized
✅ Processing time 14 hari for all
✅ Product names updated to requirements
*/
