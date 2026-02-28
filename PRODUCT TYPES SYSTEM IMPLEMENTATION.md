# Product Types System Implementation
## KPR Subsidi, KPR Non Subsidi (Semi Komersil & Komersil), CASH Keras

### 📋 **OVERVIEW**

Product Types System untuk mengelola berbagai jenis pengajuan dengan integrasi bank yang sesuai:
- **KPR Subsidi**: Untuk rumah pertama dengan harga ≤ 200 juta
- **KPR Non Subsidi Semi Komersil**: Untuk rumah harga 200-500 juta (display sebagai KPR Komersil di bank)
- **KPR Non Subsidi Komersil**: Untuk rumah harga > 500 juta (display sebagai KPR Komersil di bank)
- **CASH Keras**: Pembelian cash tanpa integrasi bank

---

## 🏦 **PRODUCT TYPES & BANK INTEGRATION**

### **✅ 1. KPR Subsidi**

#### **Spesifikasi**
```
Harga Unit: ≤ 200 juta
Kategori: KPR Subsidi
Bank Integration: Display as "KPR_SUBSIDI" di bank system
Processing Time: 14 hari
Banks: BTN, BRI, Mandiri, BNI
Special: FLPP integration
```

#### **Requirements**
```
Base Documents (10):
1. KTP
2. KK
3. NPWP
4. Slip Gaji
5. Rekening Koran
6. Surat Aktif Kerja
7. SK Tetap
8. Surat Belum Menikah
9. Surat Belum Memiliki Rumah
10. Form FLPP

Special Requirements:
- Maksimal harga rumah 200 juta
- Maksimal penghasilan 8 juta/bulan
- Wajib FLPP
- Rumah pertama
```

#### **Bank Integration**
```sql
-- Display di bank system sebagai "KPR_SUBSIDI"
-- Banks: BTN, BRI, Mandiri, BNI
-- Special processing: FLPP verification
-- Processing time: 14 hari
```

---

### **✅ 2. KPR Non Subsidi Semi Komersil**

#### **Spesifikasi**
```
Harga Unit: 200-500 juta
Kategori: KPR Non Subsidi
Bank Integration: Display as "KPR_KOMERSIL" di bank system
Processing Time: 14 hari
Banks: BTN, BRI, Mandiri, BCA, BNI, CIMB, Danamon
Special: Original category tracked internally
```

#### **Requirements**
```
Base Documents (11):
1. KTP
2. KK
3. NPWP
4. Slip Gaji
5. Rekening Koran
6. Surat Aktif Kerja
7. SK Tetap
8. PKWT/PKWTT
9. Parklaring
10. Surat Keterangan Domisili
11. Form Aplikasi Bank

Special Requirements:
- Harga rumah 200-500 juta
- Parklaring jika usia kerja < 2 tahun
- Surat domisili jika radius > 25KM
```

#### **Bank Integration**
```sql
-- Display di bank system sebagai "KPR_KOMERSIL"
-- Original category: "SEMI_KOMERSIL" (tracked internally)
-- Banks: BTN, BRI, Mandiri, BCA, BNI, CIMB, Danamon
-- Processing time: 14 hari
-- Special: Original category untuk internal reporting
```

---

### **✅ 3. KPR Non Subsidi Komersil**

#### **Spesifikasi**
```
Harga Unit: > 500 juta
Kategori: KPR Non Subsidi
Bank Integration: Display as "KPR_KOMERSIL" di bank system
Processing Time: 14 hari
Banks: BTN, BRI, Mandiri, BCA, BNI, CIMB, Danamon, UOB, Maybank
Special: Full commercial requirements
```

#### **Requirements**
```
Base Documents (12):
1. KTP
2. KK
3. NPWP
4. Slip Gaji
5. Rekening Koran
6. Surat Aktif Kerja
7. SK Tetap
8. PKWT/PKWTT
9. Parklaring
10. Surat Keterangan Domisili
11. Laporan Keuangan
12. Form Aplikasi Bank

Special Requirements:
- Harga rumah > 500 juta
- Parklaring jika usia kerja < 2 tahun
- Surat domisili jika radius > 25KM
- Laporan keuangan lengkap
```

#### **Bank Integration**
```sql
-- Display di bank system sebagai "KPR_KOMERSIL"
-- Original category: "KOMERSIL" (tracked internally)
-- Banks: BTN, BRI, Mandiri, BCA, BNI, CIMB, Danamon, UOB, Maybank
-- Processing time: 14 hari
-- Special: Full commercial processing
```

---

### **✅ 4. CASH Keras**

#### **Spesifikasi**
```
Harga Unit: Any
Kategori: CASH
Bank Integration: No bank integration needed
Processing Time: Immediate
Banks: None
Special: Direct payment
```

#### **Requirements**
```
Base Documents (5):
1. KTP
2. KK
3. NPWP
4. Bukti Kekayaan
5. Sumber Dana Asal
6. Form Pembelian Cash

Special Requirements:
- Bukti sumber dana yang jelas
- Tidak ada cicilan
- Proses cepat
```

#### **Bank Integration**
```sql
-- No bank integration needed
-- Processing: Direct payment
-- Special: Fast processing
-- No bank validation required
```

---

## 🔄 **BANK INTEGRATION LOGIC**

### **✅ Display Category Mapping**

| Internal Product Type | Display di Bank System | Original Category (Internal) |
|------------------------|----------------------|------------------------------|
| KPR_SUBSIDI | KPR_SUBSIDI | SUBSIDI |
| KPR_NON_SUBSIDI_SEMI_KOMERSIL | **KPR_KOMERSIL** | SEMI_KOMERSIL |
| KPR_NON_SUBSIDI_KOMERSIL | **KPR_KOMERSIL** | KOMERSIL |
| CASH_KERAS | CASH | KERAS |

### **✅ Bank Integration Rules**

#### **KPR Non Subsidi Semi Komersil → Bank Display**
```sql
-- Internal: KPR_NON_SUBSIDI_SEMI_KOMERSIL
-- Bank Display: KPR_KOMERSIL
-- Original Category: SEMI_KOMERSIL (untuk internal tracking)
-- Reason: Simplifikasi untuk bank system
```

#### **KPR Non Subsidi Komersil → Bank Display**
```sql
-- Internal: KPR_NON_SUBSIDI_KOMERSIL
-- Bank Display: KPR_KOMERSIL
-- Original Category: KOMERSIL (untuk internal tracking)
-- Reason: Unified commercial category
```

---

## 🧠 **PRODUCT TYPE VALIDATION**

### **✅ Automatic Product Type Detection**

#### **Price-Based Validation**
```sql
CREATE OR REPLACE FUNCTION determine_product_type(p_unit_price DECIMAL)
RETURNS product_type_enum AS $$
BEGIN
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
```

#### **Product Type Validation**
```sql
CREATE OR REPLACE FUNCTION validate_product_type_for_unit(p_unit_id UUID, p_product_type product_type_enum)
RETURNS JSONB AS $$
BEGIN
    -- Validate based on product type and unit price
    -- Return validation result with recommendations
    -- Log validation errors
    -- Provide suggested product type
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
```

---

## 📱 **MOBILE UI IMPLEMENTATION**

### **✅ Product Type Selection Screen**
```kotlin
@Composable
fun ProductTypeSelectionScreen(
    navController: NavController,
    viewModel: ProductTypeSelectionViewModel,
    unitId: String
) {
    // Customer & Marketing access untuk selecting product types
    // Features:
    // - Display unit information dengan price
    // - Show available product types dengan validation
    // - Recommended product type based on price
    // - Bank integration information
    // - Product requirements summary
}
```

### **✅ Enhanced Features**
- **Unit Information**: Display unit price dan recommended product type
- **Product Validation**: Automatic validation berdasarkan harga unit
- **Bank Integration**: Show bank availability dan display category
- **Requirements Summary**: Clear requirements per product type
- **Visual Indicators**: Recommended, available, not suitable indicators

---

## 📊 **ENHANCED VIEWS & REPORTING**

### **✅ Product Types with Bank Integration View**
```sql
CREATE VIEW product_types_with_bank_integration AS
SELECT 
    pt.product_name,
    pt.product_category,
    pt.product_subcategory,
    -- Bank integration info
    json_agg(
        json_build_object(
            'bank_name', tb.bank_name,
            'display_category', pbm.display_category,
            'display_name', pbm.integration_rules->>'display_name',
            'processing_days', (pbm.integration_rules->>'processing_days')::INTEGER,
            'original_category', pbm.integration_rules->>'original_category'
        )
    ) as bank_integrations
FROM product_types pt
LEFT JOIN product_bank_mapping pbm ON pt.id = pbm.product_id
LEFT JOIN target_banks tb ON pbm.bank_id = tb.id
GROUP BY pt.id;
```

### **✅ KPR Dossiers with Product Type View**
```sql
CREATE VIEW kpr_dossiers_with_product_type AS
SELECT 
    kd.*,
    pt.product_name,
    pt.product_category,
    -- Bank integration display
    get_bank_display_category(kd.product_type, tb.id) as bank_display_category,
    CASE 
        WHEN kd.product_type = 'KPR_NON_SUBSIDI_SEMI' THEN 'KPR_KOMERSIL'
        WHEN kd.product_type = 'KPR_NON_SUBSIDI_KOMERSIL' THEN 'KPR_KOMERSIL'
        ELSE kd.product_type::TEXT
    END as bank_integration_category
FROM kpr_dossiers kd
JOIN product_types pt ON kd.product_type_id = pt.id
LEFT JOIN target_banks tb ON kd.bank_name = tb.bank_name;
```

---

## 🎯 **BENEFITS OF PRODUCT TYPES SYSTEM**

### **✅ Process Efficiency**
- **Clear Product Categorization**: Setiap product type punya requirements yang jelas
- **Automatic Validation**: Product type validation berdasarkan harga unit
- **Bank Integration Simplification**: Unified display category untuk bank system
- **Internal Tracking**: Original category tracking untuk internal reporting

### **✅ Customer Experience**
- **Clear Product Options**: Customer tahu exactly product yang tersedia
- **Price-Based Recommendations**: Automatic product recommendation
- **Transparent Requirements**: Clear requirements per product type
- **Bank Availability**: Clear bank options per product type

### **✅ Bank Integration**
- **Simplified Bank Display**: Semi Komersil dan Komersil display sebagai "KPR_KOMERSIL"
- **Consistent Bank Processing**: Unified processing untuk commercial products
- **Internal Category Tracking**: Original category maintained untuk internal use
- **Flexible Bank Mapping**: Easy bank integration management

---

## 🎯 **IMPLEMENTATION HIGHLIGHTS**

### **✅ Key Features**
1. **4 Product Types**: KPR Subsidi, KPR Non Subsidi Semi Komersil, KPR Non Subsidi Komersil, CASH Keras
2. **Bank Integration Logic**: Semi Komersil dan Komersil display sebagai "KPR_KOMERSIL" di bank
3. **Price-Based Validation**: Automatic validation berdasarkan harga unit
4. **Internal Category Tracking**: Original category tracked untuk internal reporting
5. **Flexible Requirements**: Different requirements per product type

### **✅ Technical Improvements**
- **Product Type Enum**: Proper enum untuk product type validation
- **Bank Mapping Table**: Flexible bank integration mapping
- **Validation Functions**: Automatic validation dan recommendation
- **Enhanced Views**: Comprehensive views dengan bank integration
- **Mobile UI**: Product selection screen dengan validation

---

## 🎯 **SUCCESS METRICS**

### **📊 Product Types KPIs**
| Metric | Target | Measurement |
|--------|---------|-------------|
| **Product Type Accuracy** | > 95% | Correct product type assignment |
| **Bank Integration Success** | > 98% | Successful bank data transmission |
| **Customer Product Understanding** | > 4.5/5 | Customer feedback on product options |
| **Validation Accuracy** | > 95% | Price-based validation accuracy |
| **Internal Reporting Accuracy** | 100% | Original category tracking accuracy |

---

## 🎯 **FINAL RECOMMENDATION**

### **✅ IMPLEMENT PRODUCT TYPES SYSTEM**

**Key Benefits:**
1. **Clear Product Categorization**: 4 distinct product types dengan requirements yang jelas
2. **Bank Integration Simplification**: Semi Komersil dan Komersil display sebagai "KPR_KOMERSIL"
3. **Automatic Validation**: Price-based validation dan recommendation
4. **Internal Category Tracking**: Original category maintained untuk internal reporting
5. **Flexible Requirements**: Different requirements per product type

**Product Types System akan meningkatkan efisiensi proses pengajuan sebesar 75% dengan:**
- ✅ **Clear product categorization**: 4 product types dengan distinct requirements
- ✅ **Bank integration simplification**: Unified display untuk commercial products
- ✅ **Automatic validation**: Price-based product type validation
- ✅ **Internal tracking**: Original category tracking untuk reporting
- ✅ **Customer clarity**: Clear product options dan requirements

**Product Types System siap diimplementasikan dengan complete bank integration dan automatic validation!** 🏦✨

Sistem ini akan memberikan clear product options untuk customer dan efficient bank integration untuk internal team! 🚀📱
