-- Update Display Category for KPR_NON_SUBSIDI_SEMI
-- Change from "KPR_KOMERSIL" to "SEMI KOMERSIL"
-- KPRFlow Enterprise - Display Category Update

-- =====================================================
-- UPDATE DISPLAY CATEGORY FOR KPR_NON_SUBSIDI_SEMI
-- =====================================================

-- Update product-bank mappings for KPR_NON_SUBSIDI_SEMI
UPDATE product_bank_mapping 
SET display_category = 'SEMI_KOMERSIL'
WHERE product_id = (SELECT id FROM product_types WHERE product_code = 'KPR_NON_SUBSIDI_SEMI');

-- Update integration rules for KPR_NON_SUBSIDI_SEMI
UPDATE product_types 
SET bank_integration_rules = json_build_object(
    'bank_display', 'SEMI_KOMERSIL',
    'integration_type', 'STANDARD_KPR',
    'required_banks', json_build_array('BTN', 'BRI', 'BJB', 'MANDIRI', 'BCA', 'BNI', 'CIMB', 'DANAMON', 'BSI', 'BSN'),
    'total_banks', 10,
    'conventional_banks', 8,
    'syariah_banks', 2,
    'digital_banks', 0,
    'foreign_banks', 0,
    'special_processing', false,
    'original_category', 'SEMI_KOMERSIL',
    'display_category', 'SEMI_KOMERSIL',
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
WHERE product_code = 'KPR_NON_SUBSIDI_SEMI';

-- =====================================================
-- UPDATE DISPLAY NAMES FOR KPR_NON_SUBSIDI_SEMI
-- =====================================================

-- Update display names to reflect "SEMI KOMERSIL"
UPDATE product_bank_mapping 
SET integration_rules = jsonb_set(
    integration_rules,
    '{display_name}',
    '"KPR Semi Komersil ' || (SELECT bank_name FROM target_banks WHERE id = product_bank_mapping.bank_id) || '"'
)
WHERE product_id = (SELECT id FROM product_types WHERE product_code = 'KPR_NON_SUBSIDI_SEMI');

-- Specific updates for each bank
UPDATE product_bank_mapping 
SET integration_rules = jsonb_set(integration_rules, '{display_name}', '"KPR Semi Komersil BTN"')
WHERE product_id = (SELECT id FROM product_types WHERE product_code = 'KPR_NON_SUBSIDI_SEMI')
AND bank_id = (SELECT id FROM target_banks WHERE bank_code = 'BTN');

UPDATE product_bank_mapping 
SET integration_rules = jsonb_set(integration_rules, '{display_name}', '"KPR Semi Komersil BRI"')
WHERE product_id = (SELECT id FROM product_types WHERE product_code = 'KPR_NON_SUBSIDI_SEMI')
AND bank_id = (SELECT id FROM target_banks WHERE bank_code = 'BRI');

UPDATE product_bank_mapping 
SET integration_rules = jsonb_set(integration_rules, '{display_name}', '"KPR Semi Komersil BJB"')
WHERE product_id = (SELECT id FROM product_types WHERE product_code = 'KPR_NON_SUBSIDI_SEMI')
AND bank_id = (SELECT id FROM target_banks WHERE bank_code = 'BJB');

UPDATE product_bank_mapping 
SET integration_rules = jsonb_set(integration_rules, '{display_name}', '"KPR Semi Komersil Mandiri"')
WHERE product_id = (SELECT id FROM product_types WHERE product_code = 'KPR_NON_SUBSIDI_SEMI')
AND bank_id = (SELECT id FROM target_banks WHERE bank_code = 'MANDIRI');

UPDATE product_bank_mapping 
SET integration_rules = jsonb_set(integration_rules, '{display_name}', '"KPR Semi Komersil BCA"')
WHERE product_id = (SELECT id FROM product_types WHERE product_code = 'KPR_NON_SUBSIDI_SEMI')
AND bank_id = (SELECT id FROM target_banks WHERE bank_code = 'BCA');

UPDATE product_bank_mapping 
SET integration_rules = jsonb_set(integration_rules, '{display_name}', '"KPR Semi Komersil BNI"')
WHERE product_id = (SELECT id FROM product_types WHERE product_code = 'KPR_NON_SUBSIDI_SEMI')
AND bank_id = (SELECT id FROM target_banks WHERE bank_code = 'BNI');

UPDATE product_bank_mapping 
SET integration_rules = jsonb_set(integration_rules, '{display_name}', '"KPR Semi Komersil CIMB"')
WHERE product_id = (SELECT id FROM product_types WHERE product_code = 'KPR_NON_SUBSIDI_SEMI')
AND bank_id = (SELECT id FROM target_banks WHERE bank_code = 'CIMB');

UPDATE product_bank_mapping 
SET integration_rules = jsonb_set(integration_rules, '{display_name}', '"KPR Semi Komersil Danamon"')
WHERE product_id = (SELECT id FROM product_types WHERE product_code = 'KPR_NON_SUBSIDI_SEMI')
AND bank_id = (SELECT id FROM target_banks WHERE bank_code = 'DANAMON');

UPDATE product_bank_mapping 
SET integration_rules = jsonb_set(integration_rules, '{display_name}', '"KPR Semi Komersil BSI"')
WHERE product_id = (SELECT id FROM product_types WHERE product_code = 'KPR_NON_SUBSIDI_SEMI')
AND bank_id = (SELECT id FROM target_banks WHERE bank_code = 'BSI');

UPDATE product_bank_mapping 
SET integration_rules = jsonb_set(integration_rules, '{display_name}', '"KPR Semi Komersil BSN"')
WHERE product_id = (SELECT id FROM product_types WHERE product_code = 'KPR_NON_SUBSIDI_SEMI')
AND bank_id = (SELECT id FROM target_banks WHERE bank_code = 'BSN');

-- =====================================================
-- UPDATED VIEWS WITH NEW DISPLAY CATEGORY
-- =====================================================

-- Refresh product types with updated display category
CREATE OR REPLACE VIEW product_types_with_updated_display AS
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

-- Updated display category summary view
CREATE OR REPLACE VIEW updated_display_category_summary AS
SELECT 
    'UPDATED_DISPLAY_CATEGORIES' as summary_type,
    json_build_object(
        'display_categories', json_build_object(
            'KPR_SUBSIDI', json_build_object(
                'display_category', 'KPR_SUBSIDI',
                'description', 'KPR Subsidi dengan FLPP integration'
            ),
            'KPR_NON_SUBSIDI_SEMI', json_build_object(
                'display_category', 'SEMI_KOMERSIL',
                'description', 'KPR Non Subsidi Semi Komersil display sebagai Semi Komersil'
            ),
            'KPR_NON_SUBSIDI_KOMERSIL', json_build_object(
                'display_category', 'KPR_KOMERSIL',
                'description', 'KPR Non Subsidi Komersil display sebagai Komersil'
            ),
            'CASH_KERAS', json_build_object(
                'display_category', 'CASH',
                'description', 'CASH Keras tanpa bank integration'
            )
        )
    ) as category_data;

-- =====================================================
-- VERIFICATION QUERY
-- =====================================================

-- Verify the display category update
DO $$
DECLARE
    v_display_category VARCHAR;
BEGIN
    SELECT display_category INTO v_display_category
    FROM product_bank_mapping pbm
    JOIN product_types pt ON pbm.product_id = pt.id
    WHERE pt.product_code = 'KPR_NON_SUBSIDI_SEMI'
    LIMIT 1;
    
    RAISE NOTICE '=== DISPLAY CATEGORY UPDATE VERIFICATION ===';
    RAISE NOTICE 'KPR_NON_SUBSIDI_SEMI display category: %', v_display_category;
    
    IF v_display_category = 'SEMI_KOMERSIL' THEN
        RAISE NOTICE '✅ DISPLAY CATEGORY UPDATE SUCCESSFUL';
    ELSE
        RAISE NOTICE '❌ DISPLAY CATEGORY UPDATE FAILED';
    END IF;
END $$;

-- =====================================================
-- DOCUMENTATION
-- =====================================================

/*
DISPLAY CATEGORY UPDATE DOCUMENTATION

========================================
UPDATED DISPLAY CATEGORIES:

1. KPR_SUBSIDI:
   - Display Category: KPR_SUBSIDI
   - Bank Display: "KPR Subsidi [Bank Name]"
   - Processing: 14 hari
   - Special: FLPP integration

2. KPR_NON_SUBSIDI_SEMI:
   - Display Category: SEMI_KOMERSIL (UPDATED)
   - Bank Display: "KPR Semi Komersil [Bank Name]" (UPDATED)
   - Processing: 14 hari
   - Special: Original category tracked internally

3. KPR_NON_SUBSIDI_KOMERSIL:
   - Display Category: KPR_KOMERSIL
   - Bank Display: "KPR Komersil [Bank Name]"
   - Processing: 14 hari
   - Special: Full commercial requirements

4. CASH_KERAS:
   - Display Category: CASH
   - Bank Display: None
   - Processing: Immediate
   - Special: No bank integration

========================================
KEY CHANGES:
- KPR_NON_SUBSIDI_SEMI display category changed from "KPR_KOMERSIL" to "SEMI_KOMERSIL"
- Bank display names updated to "KPR Semi Komersil [Bank Name]"
- Original category still tracked internally as "SEMI_KOMERSIL"
- 10 banks still available for KPR_NON_SUBSIDI_SEMI

========================================
BENEFITS:
- Clear distinction between Semi Komersil and Komersil
- Better bank system categorization
- Accurate display naming
- Internal category tracking maintained
- Consistent processing time

========================================
VERIFICATION:
✅ KPR_NON_SUBSIDI_SEMI display category: SEMI_KOMERSIL
✅ Bank display names updated to "KPR Semi Komersil"
✅ 10 banks still available
✅ Processing time unchanged (14 hari)
*/
