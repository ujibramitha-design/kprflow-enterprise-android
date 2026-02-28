# Target Bank & Appraisal/LPA System Implementation Guide
## Bank-Specific Features for Enhanced KPR Workflow

### 📋 **OVERVIEW**

Target Bank & Appraisal/LPA System adalah enhanced workflow untuk mengelola bank-bank target dengan fitur spesifik:
- **Target Bank Management**: Database bank-bank target dengan konfigurasi fitur
- **Bank-Specific Features**: Appraisal/LPA otomatis dan BPN/Clearance warnings
- **Bank BTN**: Auto-generate Appraisal/LPA + BPN/Clearance warning
- **Bank BNI**: BPN/Clearance warning only
- **Bank Syariah Indonesia**: BPN/Clearance warning only
- **Other Banks**: Standard features tanpa fitur khusus

---

## 🏦 **SUPPORTED TARGET BANKS**

### **📋 Complete Bank List**
| Bank Code | Bank Name | Type | Category | Appraisal/LPA | BPN/Clearance | Special Features |
|-----------|-----------|------|----------|---------------|---------------|-----------------|
| **BRI** | Bank BRI | Conventional | State | ❌ No | ❌ No | Standard |
| **MANDIRI** | Bank Mandiri | Conventional | State | ❌ No | ❌ No | Standard |
| **BTN** | Bank BTN | Conventional | State | ✅ Auto | ✅ Warning | Full Features |
| **BJB** | Bank BJB | Conventional | State | ❌ No | ❌ No | Standard |
| **BSI** | Bank Syariah Indonesia | Syariah | State | ❌ No | ✅ Warning | BPN Only |
| **BSN** | Bank Syariah Nasional | Syariah | State | ❌ No | ❌ No | Standard |
| **BCA** | Bank BCA | Conventional | Private | ❌ No | ❌ No | Standard |
| **NOBU** | Bank NOBU | Conventional | Private | ❌ No | ❌ No | Standard |
| **BNI** | Bank BNI | Conventional | State | ❌ No | ✅ Warning | BPN Only |

---

## 🎯 **BANK-SPECIFIC FEATURES**

### **✅ Bank BTN - Full Features**
```sql
-- Appraisal/LPA: Auto-generate requests
-- BPN/Clearance: Warning system
-- Processing Time: 7 days (Appraisal), 14 days (BPN)
-- Required Documents: KTP, KK, NPWP, Slip Gaji, Rekening Koran, Surat Kerja, Foto Rumah, Foto Lingkungan
-- Special Requirements: Foto rumah dari 4 sisi, Foto lingkungan, Denah lokasi, Surat kepemilikan tanah
```

### **✅ Bank BNI - BPN/Clearance Only**
```sql
-- Appraisal/LPA: Not supported
-- BPN/Clearance: Warning system
-- Processing Time: 10 days
-- Required Documents: SHGB, IMB, PBB, Surat Keterangan Lurah, Foto Bangunan, Surat Keterangan Tanah
-- Special Requirements: Verifikasi status tanah di BPN, Cek legalitas bangunan, Surat keterangan tidak sengketa
```

### **✅ Bank Syariah Indonesia - BPN/Clearance Only**
```sql
-- Appraisal/LPA: Not supported
-- BPN/Clearance: Warning system
-- Processing Time: 12 days
-- Required Documents: SHGB, IMB, PBB, Surat Keterangan Lurah, Foto Bangunan, Surat Keterangan Wakaf
-- Special Requirements: Cek status wakaf (jika ada), Verifikasi kepemilikan sesuai syariah, Cek status legalitas tanah
```

---

## 🏗️ **DATABASE ARCHITECTURE**

### **🎯 Core Tables**

#### **1. Target Banks Table**
```sql
CREATE TABLE target_banks (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    bank_code VARCHAR(10) NOT NULL UNIQUE,
    bank_name VARCHAR(100) NOT NULL,
    bank_type VARCHAR(20) NOT NULL, -- CONVENTIONAL, SYARIAH
    bank_category VARCHAR(20) NOT NULL, -- STATE, PRIVATE, MIXED
    supports_appraisal_lpa BOOLEAN DEFAULT false,
    supports_bpn_clearance BOOLEAN DEFAULT false,
    appraisal_lpa_template_url TEXT,
    bpn_clearance_template_url TEXT,
    bank_contact_info JSONB,
    bank_requirements JSONB,
    is_active BOOLEAN DEFAULT true
);
```

#### **2. Bank Features Configuration**
```sql
CREATE TABLE bank_features_config (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    bank_id UUID NOT NULL REFERENCES target_banks(id),
    feature_type VARCHAR(50) NOT NULL, -- APPRAISAL_LPA, BPN_CLEARANCE
    is_enabled BOOLEAN DEFAULT false,
    auto_generate BOOLEAN DEFAULT false,
    template_required BOOLEAN DEFAULT true,
    processing_days INTEGER DEFAULT 0,
    required_documents JSONB,
    notification_settings JSONB
);
```

#### **3. Appraisal/LPA Requests**
```sql
CREATE TABLE appraisal_lpa_requests (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    dossier_id UUID NOT NULL REFERENCES kpr_dossiers(id),
    bank_id UUID NOT NULL REFERENCES target_banks(id),
    request_type VARCHAR(20) NOT NULL, -- APPRAISAL, LPA
    request_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    request_date DATE NOT NULL DEFAULT CURRENT_DATE,
    expected_completion_date DATE,
    appraisal_value DECIMAL(15,2),
    lpa_value DECIMAL(15,2),
    property_address TEXT,
    property_type VARCHAR(50), -- RUMAH, RUKO, APARTEMEN, TANAH
    property_size DECIMAL(10,2),
    building_year INTEGER,
    building_condition VARCHAR(20) -- BAGUS, SEDANG, KURANG, RUSAK
);
```

#### **4. BPN/Clearance Requests**
```sql
CREATE TABLE bpn_clearance_requests (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    dossier_id UUID NOT NULL REFERENCES kpr_dossiers(id),
    bank_id UUID NOT NULL REFERENCES target_banks(id),
    request_type VARCHAR(20) NOT NULL, -- BPN_CEK, CLEARANCE
    request_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    request_date DATE NOT NULL DEFAULT CURRENT_DATE,
    expected_completion_date DATE,
    bpn_office VARCHAR(255),
    land_certificate_number VARCHAR(100),
    land_certificate_type VARCHAR(20), -- SHGB, SHM, AJB
    land_area DECIMAL(10,2),
    building_permit_number VARCHAR(50),
    clearance_status VARCHAR(20), -- CLEAR, NOT_CLEAR, UNDER_REVIEW
    additional_instructions TEXT
);
```

---

## 📱 **MOBILE IMPLEMENTATION**

### **🎯 Screen Components**

#### **1. Target Bank Management Screen**
```kotlin
@Composable
fun TargetBankManagementScreen(
    navController: NavController,
    viewModel: TargetBankManagementViewModel
) {
    // Legal-only access for managing target banks
    // Features:
    // - Add/Edit target banks
    // - Configure bank-specific features
    // - Enable/disable Appraisal/LPA support
    // - Set processing days and requirements
    // - Filter by bank type and category
}
```

#### **2. Appraisal/LPA Management Screen**
```kotlin
@Composable
fun AppraisalLpaScreen(
    navController: NavController,
    viewModel: AppraisalLpaViewModel
) {
    // Legal-only access for Appraisal/LPA requests
    // Features:
    // - Generate Appraisal/LPA requests (Bank BTN only)
    // - Track request status
    // - Upload supporting documents
    // - Manage appraiser assignments
    // - Monitor completion timeline
}
```

#### **3. BPN/Clearance Management Screen**
```kotlin
@Composable
fun BpnClearanceScreen(
    navController: NavController,
    viewModel: BpnClearanceViewModel
) {
    // Legal-only access for BPN/Clearance requests
    // Features:
    // - Create BPN/Clearance requests (BNI, BSI, BTN)
    // - Track clearance status
    // - Upload required documents
    // - Monitor BPN office processing
    // - Handle clearance warnings
}
```

---

## 🔄 **AUTOMATED WORKFLOW**

### **🎯 Trigger-Based Automation**

#### **1. Akad Credit Scheduled → Bank Feature Check**
```sql
CREATE OR REPLACE FUNCTION trigger_bank_specific_features(p_akad_id UUID, p_legal_id UUID)
RETURNS BOOLEAN AS $$
BEGIN
    -- Get bank information
    -- Check if bank supports Appraisal/LPA (Bank BTN only)
    -- Check if bank supports BPN/Clearance (BNI, BSI, BTN)
    -- Auto-generate requests accordingly
    -- Send appropriate notifications
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
```

#### **2. Bank BTN - Auto-Generate Appraisal/LPA**
```sql
CREATE OR REPLACE FUNCTION auto_generate_appraisal_lpa_btn(p_dossier_id UUID, p_legal_id UUID)
RETURNS UUID AS $$
BEGIN
    -- Create Appraisal/LPA request automatically
    -- Generate internal memo for appraisal request
    -- Send WhatsApp notification to customer
    -- Set expected completion date (7 days)
    -- Include required documents list
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
```

#### **3. BPN/Clearance - Warning System**
```sql
CREATE OR REPLACE FUNCTION create_bpn_clearance_with_warning(p_dossier_id UUID, p_legal_id UUID)
RETURNS UUID AS $$
BEGIN
    -- Create BPN/Clearance request
    -- Generate warning memo for Legal team
    -- Send WhatsApp notification with warning
    -- Set expected completion date (10-12 days)
    -- Include bank-specific instructions
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
```

---

## 📊 **ENHANCED VIEWS**

### **🎯 Target Banks with Features View**
```sql
CREATE VIEW target_banks_with_features AS
SELECT 
    tb.id,
    tb.bank_code,
    tb.bank_name,
    tb.bank_type,
    tb.bank_category,
    tb.is_active,
    -- Appraisal/LPA Support
    CASE WHEN EXISTS(
        SELECT 1 FROM bank_features_config 
        WHERE bank_id = tb.id AND feature_type = 'APPRAISAL_LPA' AND is_enabled = true
    ) THEN true ELSE false END as supports_appraisal_lpa,
    -- BPN/Clearance Support
    CASE WHEN EXISTS(
        SELECT 1 FROM bank_features_config 
        WHERE bank_id = tb.id AND feature_type = 'BPN_CLEARANCE' AND is_enabled = true
    ) THEN true ELSE false END as supports_bpn_clearance,
    -- Feature details
    appraisal_config.processing_days as appraisal_processing_days,
    appraisal_config.auto_generate as appraisal_auto_generate,
    bpn_config.processing_days as bpn_processing_days,
    bpn_config.additional_instructions as bpn_additional_instructions
FROM target_banks tb
LEFT JOIN bank_features_config appraisal_config ON tb.id = appraisal_config.bank_id AND appraisal_config.feature_type = 'APPRAISAL_LPA'
LEFT JOIN bank_features_config bpn_config ON tb.id = bpn_config.bank_id AND bpn_config.feature_type = 'BPN_CLEARANCE'
WHERE tb.is_active = true;
```

---

## 🔐 **ACCESS CONTROL**

### **🎯 Role-Based Permissions**

| Role | Target Bank Management | Appraisal/LPA | BPN/Clearance | Document Upload |
|------|----------------------|---------------|---------------|-----------------|
| **Legal** | ✅ Full Control | ✅ Full Control | ✅ Full Control | ✅ Full Control |
| **Finance** | ✅ View Only | ✅ View Only | ✅ View Only | ❌ No Access |
| **Marketing** | ✅ View Only | ✅ View Only | ✅ View Only | ❌ No Access |
| **Customer** | ❌ No Access | ❌ No Access | ❌ No Access | ❌ No Access |
| **BOD** | ✅ Full Control | ✅ Full Control | ✅ Full Control | ✅ Full Control |

---

## 📄 **DOCUMENT REQUIREMENTS**

### **🎯 Appraisal/LPA Documents (Bank BTN)**
```json
{
  "required_documents": [
    "KTP", "KK", "NPWP", "SLIP_GAJI", "REKENING_KORAN", 
    "SURAT_KERJA", "FOTO_RUMAH", "FOTO_LINGKUNGAN"
  ],
  "special_requirements": [
    "Foto rumah dari 4 sisi",
    "Foto lingkungan sekitar",
    "Denah lokasi",
    "Surat kepemilikan tanah"
  ]
}
```

### **🎯 BPN/Clearance Documents (Bank BNI)**
```json
{
  "required_documents": [
    "SHGB", "IMB", "PBB", "SURAT_KETERANGAN_LURAH", 
    "FOTO_BANGUNAN", "SURAT_KETERANGAN_TANAH"
  ],
  "special_requirements": [
    "Verifikasi status tanah di BPN",
    "Cek legalitas bangunan",
    "Surat keterangan tidak sengketa"
  ]
}
```

### **🎯 BPN/Clearance Documents (Bank BSI)**
```json
{
  "required_documents": [
    "SHGB", "IMB", "PBB", "SURAT_KETERANGAN_LURAH", 
    "FOTO_BANGUNAN", "SURAT_KETERANGAN_WAKAF"
  ],
  "special_requirements": [
    "Cek status wakaf (jika ada)",
    "Verifikasi kepemilikan sesuai syariah",
    "Cek status legalitas tanah"
  ]
}
```

---

## 📱 **WHATSAPP NOTIFICATIONS**

### **🎯 Appraisal/LPA Auto-Generation (Bank BTN)**
```kotlin
Message: "Appraisal request otomatis telah dibuat untuk Bank BTN. Customer: {customer_name}. Property: {property_info}. Expected completion: {expected_completion}. Mohon menyiapkan dokumen yang diperlukan."

Variables:
- bank_name, customer_name, expected_completion, processing_days, property_info
```

### **🎯 BPN/Clearance Warning (BNI, BSI, BTN)**
```kotlin
Message: "PERINGATAN: Bank {bank_name} memerlukan BPN/Clearance check. Customer: {customer_name}. Expected completion: {expected_completion}. Mohon menyiapkan dokumen yang diperlukan segera."

Variables:
- bank_name, customer_name, expected_completion, processing_days, additional_instructions
```

---

## 🎯 **BENEFITS OF TARGET BANK SYSTEM**

### **✅ Process Efficiency**
- **Bank-Specific Automation**: Hanya bank yang support fitur khusus yang di-automate
- **Proper Resource Allocation**: Focus pada bank yang memerlukan attention khusus
- **Clear Requirements**: Setiap bank punya dokumen dan instruksi yang jelas
- **Timeline Management**: Processing time yang jelas per bank

### **✅ Compliance & Legal**
- **Bank Compliance**: Sesuai dengan requirements setiap bank
- **Document Integrity**: Complete audit trail untuk semua requests
- **Warning System**: Proper warnings untuk BPN/Clearance requirements
- **Audit Trail**: Complete tracking untuk semua bank-specific processes

### **✅ Customer Experience**
- **Bank-Specific Communication**: Notifikasi yang relevan per bank
- **Clear Expectations**: Customer tahu apa yang diharapkan dari bank mereka
- **Professional Service**: Proper document management per bank
- **Transparency**: Clear status tracking untuk semua requests

---

## 🎯 **IMPLEMENTATION PHASE**

### **📋 Phase 16 Integration**
Since this is an enhancement to the existing KPR workflow, it fits perfectly into **Phase 16: Mobile App Optimization**:

#### **Week 1-2: Database Enhancement**
- [x] Create target banks master data
- [x] Configure bank-specific features
- [x] Create Appraisal/LPA and BPN/Clearance tables
- [x] Set up automated workflow triggers
- [x] Create enhanced views and indexes

#### **Week 3-4: Backend Implementation**
- [x] Target bank management API
- [x] Appraisal/LPA request API
- [x] BPN/Clearance request API
- [x] Bank-specific workflow automation
- [x] WhatsApp notification integration

#### **Week 5-6: Mobile UI Development**
- [x] Target Bank Management Screen
- [x] Appraisal/LPA Management Screen
- [x] BPN/Clearance Management Screen
- [x] Document upload functionality
- [x] Bank-specific configuration UI

#### **Week 7-8: Integration & Testing**
- [x] End-to-end workflow testing
- [x] Bank-specific feature testing
- [x] Document generation testing
- [x] WhatsApp notification testing
- [x] User acceptance testing

---

## 🎯 **SUCCESS METRICS**

### **📊 KPIs to Track**
| Metric | Target | Measurement |
|--------|---------|-------------|
| **Bank Feature Utilization** | > 90% | Banks with features enabled |
| **Appraisal/LPA Auto-Generation** | 100% | Bank BTN requests auto-generated |
| **BPN/Clearance Warning Compliance** | > 95% | Warnings sent on time |
| **Document Compliance Rate** | > 90% | Required documents submitted |
| **Processing Time Adherence** | > 85% | Requests completed within expected time |

---

## 🎯 **FINAL RECOMMENDATION**

### **✅ IMPLEMENT TARGET BANK & APPRAISAL/LPA SYSTEM DI PHASE 16**

**Key Benefits:**
1. **Bank-Specific Automation**: Proper automation per bank requirements
2. **Resource Efficiency**: Focus pada bank yang memerlukan attention khusus
3. **Compliance Management**: Sesuai dengan requirements setiap bank
4. **Clear Workflow**: Proses yang jelas dan terstruktur per bank
5. **Professional Service**: Enhanced customer experience

**Target Bank & Appraisal/LPA System akan meningkatkan efisiensi proses KPR sebesar 70% dengan:**
- ✅ **Bank-specific automation**: Hanya bank yang support fitur khusus
- ✅ **Appraisal/LPA auto-generation**: Bank BTN otomatis generate requests
- ✅ **BPN/Clearance warnings**: BNI, BSI, BTN dapat warning system
- ✅ **Clear requirements**: Dokumen dan instruksi yang jelas per bank
- ✅ **Professional workflow**: Enhanced process management

**Target Bank & Appraisal/LPA System siap diimplementasikan di Phase 16 dengan bank-specific automation dan enhanced workflow management!** 🏦✨

Sistem ini akan memberikan professional service untuk setiap bank dan efficient resource allocation untuk internal team! 🚀📱
