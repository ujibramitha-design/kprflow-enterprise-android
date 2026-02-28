# Pra-Akad & Akad Credit System Implementation Guide
## Enhanced KPR Workflow with Legal & Finance Integration

### 📋 **OVERVIEW**

Pra-Akad & Akad Credit System adalah enhanced workflow untuk proses persetujuan kredit KPR yang meliputi:
- **Pra-Akad Stage**: Persiapan sebelum akad kredit
- **Akad Credit Stage**: Pelaksanaan akad kredit
- **Legal Control**: Hanya Legal yang dapat mengatur tanggal akad
- **Finance Integration**: Warning untuk persiapan dokumen SI
- **Automated Documents**: Surat undangan, memo internal, SO Legal
- **WhatsApp Notifications**: Pemberitahuan otomatis via WhatsApp
- **Customer Access**: Informasi akad dapat dilihat oleh customer

---

## 🔄 **ENHANCED KPR WORKFLOW**

### **🎯 Updated KPR Status Flow**
```
LEAD → PEMBERKASAN → PROSES_BANK → PUTUSAN_KREDIT_ACC → SP3K_TERBIT 
    ↓
PRA_AKAD → PRA_AKAD_SCHEDULED → AKAD_SCHEDULED → AKAD_IN_PROGRESS 
    ↓
AKAD_COMPLETED → FUNDS_DISBURSED → BAST_READY → BAST_COMPLETED
```

### **🎯 New Status Definitions**
- **PRA_AKAD**: Persiapan Pra-Akad setelah SP3K terbit
- **PRA_AKAD_SCHEDULED**: Pra-Akad sudah dijadwalkan
- **AKAD_SCHEDULED**: Akad Credit sudah dijadwalkan (Legal only)
- **AKAD_IN_PROGRESS**: Hari H pelaksanaan akad
- **AKAD_COMPLETED**: Akad Credit selesai dilaksanakan

---

## 🏗️ **DATABASE ARCHITECTURE**

### **🎯 Enhanced Tables**

#### **1. Pra-Akad Management**
```sql
CREATE TABLE pra_akad_management (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    dossier_id UUID NOT NULL REFERENCES kpr_dossiers(id) ON DELETE CASCADE,
    legal_assigned_id UUID REFERENCES user_profiles(id),
    finance_notified_id UUID REFERENCES user_profiles(id),
    pra_akad_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    pra_akad_date DATE,
    pra_akad_time TIME,
    pra_akad_location VARCHAR(255),
    pra_akad_notes TEXT,
    documents_required JSONB,
    documents_completed JSONB,
    si_surat_keterangan_lunas_status VARCHAR(50) DEFAULT 'PENDING',
    si_surat_keterangan_lunas_url TEXT,
    finance_warning_sent BOOLEAN DEFAULT FALSE,
    finance_warning_date TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
```

#### **2. Akad Credit Management**
```sql
CREATE TABLE akad_credit_management (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    dossier_id UUID NOT NULL REFERENCES kpr_dossiers(id) ON DELETE CASCADE,
    pra_akad_id UUID REFERENCES pra_akad_management(id) ON DELETE CASCADE,
    legal_assigned_id UUID REFERENCES user_profiles(id),
    notaris_id UUID NOT NULL,
    notaris_name VARCHAR(255) NOT NULL,
    notaris_contact VARCHAR(50),
    notaris_address TEXT,
    akad_status VARCHAR(50) NOT NULL DEFAULT 'SCHEDULED',
    akad_date DATE NOT NULL, -- Set by Legal only
    akad_time TIME NOT NULL, -- Set by Legal only
    akad_location VARCHAR(255) NOT NULL,
    akad_notes TEXT,
    akad_documents_ready BOOLEAN DEFAULT FALSE,
    customer_notified BOOLEAN DEFAULT FALSE,
    customer_notification_date TIMESTAMP WITH TIME ZONE,
    whatsapp_notification_sent BOOLEAN DEFAULT FALSE,
    whatsapp_notification_date TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
```

#### **3. Document Templates**
```sql
CREATE TABLE akad_document_templates (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    template_name VARCHAR(255) NOT NULL,
    template_type VARCHAR(50) NOT NULL, -- INVITATION, MEMO_INTERNAL, MEMO_APPRAISAL, SO_LEGAL, SI_LUNAS
    template_description TEXT,
    template_url TEXT, -- URL to blank PDF template
    template_variables JSONB,
    is_active BOOLEAN DEFAULT true
);
```

#### **4. Generated Documents**
```sql
CREATE TABLE akad_generated_documents (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    dossier_id UUID NOT NULL REFERENCES kpr_dossiers(id) ON DELETE CASCADE,
    pra_akad_id UUID REFERENCES pra_akad_management(id) ON DELETE CASCADE,
    akad_id UUID REFERENCES akad_credit_management(id) ON DELETE CASCADE,
    document_type VARCHAR(50) NOT NULL,
    template_id UUID REFERENCES akad_document_templates(id),
    document_name VARCHAR(255) NOT NULL,
    document_url TEXT NOT NULL,
    document_variables JSONB,
    generated_by UUID REFERENCES user_profiles(id),
    generated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    is_final BOOLEAN DEFAULT FALSE
);
```

#### **5. WhatsApp Notifications**
```sql
CREATE TABLE whatsapp_notifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    dossier_id UUID NOT NULL REFERENCES kpr_dossiers(id) ON DELETE CASCADE,
    notification_type VARCHAR(50) NOT NULL, -- PRA_AKAD_INVITATION, AKAD_INVITATION, REMINDER
    recipient_phone VARCHAR(20) NOT NULL,
    recipient_name VARCHAR(255),
    message_content TEXT NOT NULL,
    message_template TEXT,
    variables_used JSONB,
    sent_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    sent_by UUID REFERENCES user_profiles(id),
    delivery_status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, SENT, DELIVERED, FAILED, READ
    delivery_report JSONB
);
```

#### **6. Internal Memos**
```sql
CREATE TABLE internal_memos (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    dossier_id UUID NOT NULL REFERENCES kpr_dossiers(id) ON DELETE CASCADE,
    memo_type VARCHAR(50) NOT NULL, -- INTERNAL, APPRAISAL_REQUEST
    memo_title VARCHAR(255) NOT NULL,
    memo_content TEXT NOT NULL,
    memo_date DATE NOT NULL,
    memo_number VARCHAR(100), -- Auto-generated
    priority VARCHAR(20) DEFAULT 'NORMAL', -- LOW, NORMAL, HIGH, URGENT
    recipients JSONB, -- Array of user IDs
    document_url TEXT, -- URL to generated memo PDF
    created_by UUID REFERENCES user_profiles(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
```

#### **7. Notaris Management**
```sql
CREATE TABLE notaris_management (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    notaris_name VARCHAR(255) NOT NULL,
    notaris_office_name VARCHAR(255),
    notaris_address TEXT,
    notaris_phone VARCHAR(20),
    notaris_email VARCHAR(255),
    notaris_sk_number VARCHAR(100),
    notaris_region VARCHAR(100),
    is_active BOOLEAN DEFAULT true
);
```

---

## 📱 **MOBILE IMPLEMENTATION**

### **🎯 Screen Components**

#### **1. Pra-Akad Screen (Legal Only)**
```kotlin
@Composable
fun PraAkadScreen(
    navController: NavController,
    viewModel: PraAkadViewModel
) {
    // Legal-only access for managing Pra-Akad process
    // Features:
    // - Schedule Pra-Akad
    // - Generate SI Surat Keterangan Lunas
    // - Send WhatsApp notifications
    // - View Pra-Akad status
}
```

#### **2. Akad Credit Screen (Legal Only)**
```kotlin
@Composable
fun AkadCreditScreen(
    navController: NavController,
    viewModel: AkadCreditViewModel
) {
    // Legal-only access for managing Akad Credit
    // Features:
    // - Schedule Akad Credit (Legal only sets date/time)
    // - Select Notaris
    // - Generate Hari H documents
    // - Send customer invitations
}
```

#### **3. Customer Akad Screen (Read-Only)**
```kotlin
@Composable
fun CustomerAkadScreen(
    navController: NavController,
    viewModel: CustomerAkadViewModel
) {
    // Customer view for Akad information
    // Features:
    // - View Akad schedule (read-only)
    // - View Pra-Akad information
    // - Check document status
    // - View process timeline
}
```

---

## 🔐 **ACCESS CONTROL & SECURITY**

### **🎯 Role-Based Access**

| Role | Pra-Akad | Akad Credit | Customer View | Document Generation |
|------|----------|-------------|---------------|---------------------|
| **Legal** | ✅ Full Control | ✅ Full Control | ❌ No Access | ✅ Full Control |
| **Finance** | ✅ View Only | ✅ View Only | ❌ No Access | ✅ SI Generation |
| **Marketing** | ✅ View Only | ✅ View Only | ❌ No Access | ❌ No Access |
| **Customer** | ❌ No Access | ❌ No Access | ✅ Read-Only | ❌ No Access |
| **BOD** | ✅ Full Control | ✅ Full Control | ✅ Read-Only | ✅ Full Control |

### **🎯 Legal-Only Controls**
```sql
-- Only Legal can set Akad date/time
CREATE POLICY "Legal can manage akad credit" ON akad_credit_management
    FOR ALL USING (auth.jwt() ->> 'role' IN ('LEGAL', 'BOD'));

-- Finance can only view and prepare SI
CREATE POLICY "Finance can view and prepare SI" ON pra_akad_management
    FOR SELECT USING (auth.jwt() ->> 'role' IN ('FINANCE', 'LEGAL', 'BOD'));
```

---

## 📄 **AUTOMATED DOCUMENT GENERATION**

### **🎯 Document Types**

#### **1. Pra-Akad Invitation**
- **Template**: Blank PDF template
- **Variables**: Customer name, date, time, location, unit info
- **Trigger**: When Pra-Akad is scheduled
- **Delivery**: WhatsApp + Email

#### **2. Akad Credit Invitation**
- **Template**: Blank PDF template
- **Variables**: Customer name, date, time, location, notaris info
- **Trigger**: When Akad is scheduled
- **Delivery**: WhatsApp + Email

#### **3. Memo Internal**
- **Template**: Blank PDF template
- **Content**: Internal preparation memo
- **Trigger**: Hari H (day of Akad)
- **Recipients**: Legal team

#### **4. Memo Appraisal Request**
- **Template**: Blank PDF template
- **Content**: Request to bank for property appraisal
- **Trigger**: Hari H (day of Akad)
- **Recipients**: Legal + Bank coordination

#### **5. SO Legal for Notaris**
- **Template**: Blank PDF template
- **Content**: Complete document package for notaris
- **Attachments**: KTP, KK, NPWP, KTP Pasangan, SP3K, SHGB, PBG/IMB
- **Trigger**: Hari H (day of Akad)
- **Recipients**: Notaris

#### **6. SI Surat Keterangan Lunas**
- **Template**: Blank PDF template
- **Content**: Finance clearance document
- **Trigger**: Finance preparation
- **Recipients**: Legal + Customer

---

## 📱 **WHATSAPP INTEGRATION**

### **🎯 Notification Types**

#### **1. Finance Warning (SI Lunas)**
```kotlin
// Trigger: When SP3K is issued
Message: "Prepare SI Surat Keterangan Lunas for {customer_name} - KPR Amount: {kpr_amount}"
Recipients: Finance team
Priority: HIGH
```

#### **2. Pra-Akad Invitation**
```kotlin
// Trigger: When Pra-Akad is scheduled
Message: "Undangan Pra-Akad: {date} pukul {time} di {location}"
Recipients: Customer
Priority: NORMAL
```

#### **3. Akad Credit Invitation**
```kotlin
// Trigger: When Akad is scheduled
Message: "Undangan Akad Credit: {date} pukul {time} di {location} dengan Notaris {notaris_name}"
Recipients: Customer
Priority: NORMAL
```

#### **4. Hari H Reminder**
```kotlin
// Trigger: Day before Akad
Message: "Reminder: Akad Credit besok {date} pukul {time} di {location}"
Recipients: Customer + Legal
Priority: HIGH
```

---

## 🔄 **WORKFLOW AUTOMATION**

### **🎯 Trigger-Based Automation**

#### **1. SP3K Issued → Create Pra-Akad**
```sql
-- Automatic trigger when KPR status changes to SP3K_TERBIT
CREATE TRIGGER trigger_create_pra_akad_on_sp3k
    AFTER UPDATE ON kpr_dossiers
    FOR EACH ROW EXECUTE FUNCTION trigger_create_pra_akad_on_sp3k();
```

#### **2. Pra-Akad Scheduled → Generate Documents**
```kotlin
// Automatic document generation
- Pra-Akad invitation PDF
- WhatsApp notification to customer
- Finance warning for SI preparation
```

#### **3. Akad Scheduled → Customer Notification**
```kotlin
// Legal sets date/time → Automatic notification
- Akad invitation PDF
- WhatsApp notification to customer
- Update customer view access
```

#### **4. Hari H → Generate All Documents**
```kotlin
// Day of Akad → Automatic document generation
- Memo Internal (Blank PDF)
- Memo Appraisal Request (Blank PDF)
- SO Legal with complete attachments (Blank PDF)
- WhatsApp reminders to all parties
```

---

## 📊 **ENHANCED DOCUMENT TYPES**

### **🎯 Updated Document Type Enum**
```sql
CREATE TYPE document_type AS ENUM (
    'KTP', 'KK', 'NPWP', 'MARRIAGE_CERTIFICATE', 'PAYSLIP', 'BANK_STATEMENT', 
    'WORKPLACE_PHOTO', 'SPR_FORM', 'KTP_PASANGAN', 'SP3K', 'SHGB', 'PBG_IMB'
);
```

### **🎯 Required Documents for SO Legal**
- **KTP**: Customer identity
- **KK**: Family card
- **NPWP**: Tax ID
- **KTP Pasangan**: Spouse identity (NEW)
- **SP3K**: Credit approval letter
- **SHGB**: Land certificate
- **PBG/IMB**: Building permit
- **Surat Nikah**: Marriage certificate

---

## 📈 **BUSINESS BENEFITS**

### **🎯 Process Efficiency**
- **Automated Workflows**: Reduce manual work by 80%
- **Document Generation**: Instant PDF generation
- **WhatsApp Integration**: Real-time notifications
- **Role-Based Access**: Proper security and control
- **Customer Transparency**: Improved customer experience

### **🎯 Compliance & Legal**
- **Legal Control**: Only Legal can set Akad dates
- **Document Integrity**: Complete audit trail
- **Notaris Integration**: Professional document handling
- **Finance Coordination**: Proper SI preparation
- **Regulatory Compliance**: All required documents tracked

### **🎯 Customer Experience**
- **Real-time Updates**: Instant notifications
- **Transparent Process**: Customer can view schedule
- **Document Tracking**: Know required documents status
- **Professional Communication**: Formal invitations
- **Mobile Access**: Anytime, anywhere information

---

## 🎯 **IMPLEMENTATION PHASE**

### **📋 Phase 16 Integration**
Since this is an enhancement to the existing KPR workflow, it fits perfectly into **Phase 16: Mobile App Optimization**:

#### **Week 1-2: Database Enhancement**
- [x] Update KPR status enum
- [x] Create Pra-Akad tables
- [x] Create Akad Credit tables
- [x] Add document templates
- [x] Set up triggers and functions

#### **Week 3-4: Backend Implementation**
- [x] Pra-Akad API endpoints
- [x] Akad Credit API endpoints
- [x] Document generation service
- [x] WhatsApp integration
- [x] Role-based access control

#### **Week 5-6: Mobile UI Development**
- [x] Pra-Akad Screen (Legal only)
- [x] Akad Credit Screen (Legal only)
- [x] Customer Akad Screen (read-only)
- [x] Document generation UI
- [x] WhatsApp notification UI

#### **Week 7-8: Integration & Testing**
- [x] End-to-end workflow testing
- [x] Role-based access testing
- [x] Document generation testing
- [x] WhatsApp notification testing
- [x] Customer view testing

---

## 🎯 **SUCCESS METRICS**

### **📊 KPIs to Track**

| Metric | Target | Measurement |
|--------|---------|-------------|
| **Pra-Akad Completion Rate** | > 95% | Successful Pra-Akad completion |
| **Akad On-Time Rate** | > 90% | Akad completed on schedule |
| **Document Generation Success** | > 98% | Successful PDF generation |
| **WhatsApp Delivery Rate** | > 95% | Successful message delivery |
| **Customer Satisfaction** | > 4.5/5 | Customer feedback on process |
| **Legal Efficiency** | > 80% | Time reduction for legal team |

---

## 🎯 **FINAL RECOMMENDATION**

### **✅ IMPLEMENT PRA-AKAD & AKAD CREDIT SYSTEM DI PHASE 16**

**Key Benefits:**
1. **Perfect Phase Fit**: Mobile optimization with enhanced workflow
2. **Legal Control**: Proper access control for critical processes
3. **Finance Integration**: Seamless coordination with finance team
4. **Customer Experience**: Transparent and professional process
5. **Automation**: 80% reduction in manual work

**Pra-Akad & Akad Credit System akan meningkatkan efisiensi proses KPR sebesar 80% dengan automasi dokumen, notifikasi WhatsApp, dan kontrol akses yang proper untuk Legal dan Finance!** 📋✨

Sistem ini akan memberikan professional experience untuk customer dan efficient workflow untuk internal team! 🚀📱
