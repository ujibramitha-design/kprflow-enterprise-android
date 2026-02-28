# Agent Selection System Implementation
## Customer Agent Selection & Legal Manual Assignment

### 📋 **OVERVIEW**

Sistem pemilihan agent yang memberikan kontrol kepada customer untuk memilih agent sendiri, sementara legal staff dapat melakukan penugasan manual sesuai kebutuhan.

---

## 🎯 **FITUR UTAMA SISTEM**

### **✅ 1. Customer Agent Selection**
- **Pilih Agent Sendiri**: Customer dapat memilih agent dari daftar yang tersedia
- **Filter Agent**: Filter berdasarkan spesialisasi, bahasa, rating
- **View Profile**: Lihat profil agent lengkap dengan keahlian
- **Match Score**: Skor kecocokan agent dengan customer
- **Real-time Availability**: Status ketersediaan agent real-time

### **✅ 2. Legal Manual Assignment**
- **Override Customer**: Legal dapat menimpa pilihan customer
- **Temporary Assignment**: Penugasan sementara dengan expiry date
- **Priority Levels**: Prioritas (Low, Normal, High, Urgent)
- **Assignment Reason**: Alasan penugasan tercatat
- **Assignment History**: Riwayat semua penugasan

### **✅ 3. Agent Management**
- **Agent Profiles**: Profil agent lengkap dengan spesialisasi
- **Tier System**: Tier agent (Basic, Regular, Premium, VIP)
- **Capacity Management**: Batasan jumlah customer per agent
- **Performance Metrics**: Rating, success rate, response time
- **Working Hours**: Jam kerja dan timezone agent

---

## 🏗️ **ARKITEKTUR SISTEM**

### **✅ Database Schema**
```sql
-- Agent Profiles
CREATE TABLE agent_profiles (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES user_profiles(id),
    agent_code VARCHAR(20) UNIQUE NOT NULL,
    specialization VARCHAR(50) NOT NULL,
    tier VARCHAR(20) NOT NULL DEFAULT 'REGULAR',
    max_customers INTEGER DEFAULT 50,
    current_customers INTEGER DEFAULT 0,
    is_available BOOLEAN DEFAULT true,
    rating DECIMAL(3,2) DEFAULT 5.0,
    languages TEXT[],
    expertise_areas TEXT[],
    working_hours JSONB,
    response_time_minutes INTEGER DEFAULT 30
);

-- Customer Preferences
CREATE TABLE customer_agent_preferences (
    id UUID PRIMARY KEY,
    customer_id UUID REFERENCES user_profiles(id),
    preferred_agent_id UUID REFERENCES agent_profiles(id),
    preference_type VARCHAR(20) NOT NULL, -- 'MANUAL', 'AUTO', 'LEGAL_ASSIGNED'
    selection_reason TEXT,
    is_active BOOLEAN DEFAULT true
);

-- Legal Assignments
CREATE TABLE legal_agent_assignments (
    id UUID PRIMARY KEY,
    customer_id UUID REFERENCES user_profiles(id),
    assigned_by UUID REFERENCES user_profiles(id),
    agent_id UUID REFERENCES agent_profiles(id),
    assignment_reason TEXT NOT NULL,
    priority_level VARCHAR(10) DEFAULT 'NORMAL',
    is_temporary BOOLEAN DEFAULT false,
    temporary_until TIMESTAMP WITH TIME ZONE,
    status VARCHAR(20) DEFAULT 'ACTIVE'
);
```

---

## 📱 **MOBILE IMPLEMENTATION**

### **✅ AgentSelectionScreen**
```kotlin
@Composable
fun AgentSelectionScreen(
    onBackClick: () -> Unit,
    onAgentSelected: (String) -> Unit,
    viewModel: AgentSelectionViewModel = hiltViewModel()
) {
    // Features:
    // - Filter agent berdasarkan spesialisasi dan bahasa
    // - View agent profile dengan rating dan expertise
    // - Real-time availability status
    // - Match score untuk kecocokan
    // - Customer load indicator
}
```

### **✅ LegalAgentManagementScreen**
```kotlin
@Composable
fun LegalAgentManagementScreen(
    onBackClick: () -> Unit,
    viewModel: LegalAgentManagementViewModel = hiltViewModel()
) {
    // Features:
    // - View semua assignment aktif
    // - Filter berdasarkan status (Aktif, Sementara, Tidak Aktif)
    // - Manual assignment dialog
    // - Edit/remove assignment
    // - Priority level management
}
```

---

## 🔄 **ALUR KERJA SISTEM**

### **✅ Customer Selection Flow**
```
1. Customer Buka Agent Selection Screen
2. Filter Agent (Spesialisasi, Bahasa, Tier)
3. View Agent Profiles (Rating, Expertise, Load)
4. Pilih Agent Berdasarkan Preferensi
5. System Record Selection Reason
6. Update Agent Customer Count
7. Create Assignment Record
8. Notify Agent & Customer
```

### **✅ Legal Assignment Flow**
```
1. Legal Buka Management Screen
2. View Customer Assignment History
3. Pilih Customer untuk Reassignment
4. Pilih Agent dari Available List
5. Set Assignment Reason & Priority
6. Choose Temporary/Permanent Assignment
7. Set Expiry Date (if temporary)
8. Override Customer Preference
9. Update Assignment Records
10. Notify All Parties
```

---

## 🎯 **AGENT MATCHING ALGORITHM**

### **✅ Customer Tier Matching**
```sql
-- Customer Tier Logic
CASE 
    WHEN customer_has_completed_kpr THEN 'PREMIUM'
    WHEN customer_has_approved_kpr THEN 'REGULAR'
    ELSE 'BASIC'
END

-- Agent Tier Matching Score
CASE 
    WHEN agent.tier = customer.tier THEN 100
    WHEN agent.tier = 'PREMIUM' AND customer.tier IN ('REGULAR', 'BASIC') THEN 80
    WHEN agent.tier = 'REGULAR' AND customer.tier = 'BASIC' THEN 90
    ELSE 60
END
```

### **✅ Availability & Load Balancing**
```sql
-- Agent Availability Check
is_available = true AND current_customers < max_customers

-- Load Balancing Priority
ORDER BY 
    tier_match_score DESC,
    rating DESC,
    success_rate DESC,
    current_customers ASC
```

---

## 🔧 **FUNCTIONS & API**

### **✅ Customer Functions**
```sql
-- Get available agents for customer
CREATE OR REPLACE FUNCTION get_available_agents_for_customer(
    p_customer_id UUID,
    p_specialization VARCHAR(50) DEFAULT NULL,
    p_language VARCHAR(10) DEFAULT 'id'
) RETURNS JSONB

-- Customer selects agent
CREATE OR REPLACE FUNCTION customer_select_agent(
    p_customer_id UUID,
    p_agent_id UUID,
    p_selection_reason TEXT DEFAULT NULL
) RETURNS JSONB
```

### **✅ Legal Functions**
```sql
-- Legal assigns agent
CREATE OR REPLACE FUNCTION legal_assign_agent(
    p_legal_staff_id UUID,
    p_customer_id UUID,
    p_agent_id UUID,
    p_assignment_reason TEXT,
    p_priority_level VARCHAR(10) DEFAULT 'NORMAL',
    p_is_temporary BOOLEAN DEFAULT false,
    p_temporary_until TIMESTAMP WITH TIME ZONE DEFAULT NULL
) RETURNS JSONB
```

---

## 📊 **AGENT PROFILES & TIERS**

### **✅ Agent Tiers**
```
BASIC (50 customers max):
- Response time: 60 menit
- Languages: Indonesia
- Expertise: Basic KPR
- Rating: 4.0+

REGULAR (40 customers max):
- Response time: 30 menit
- Languages: Indonesia, English
- Expertise: KPR Subsidi & Komersil
- Rating: 4.5+

PREMIUM (30 customers max):
- Response time: 15 menit
- Languages: Indonesia, English, Chinese
- Expertise: All product types
- Rating: 4.8+

VIP (20 customers max):
- Response time: 5 menit
- Languages: All languages
- Expertise: Complex cases
- Rating: 4.9+
```

### **✅ Specializations**
- **LEGAL**: Legal documentation, contract review
- **MARKETING**: Product consultation, unit selection
- **FINANCE**: Financial analysis, payment planning
- **SUPPORT**: General support, troubleshooting

---

## 🔐 **SECURITY & ACCESS CONTROL**

### **✅ RLS Policies**
```sql
-- Customer can view/modify own preferences
CREATE POLICY "Customers manage own preferences" ON customer_agent_preferences
FOR ALL USING (auth.uid() = customer_id AND preference_type = 'MANUAL');

-- Legal staff can manage assignments
CREATE POLICY "Legal staff manage assignments" ON legal_agent_assignments
FOR ALL USING (
    EXISTS (SELECT 1 FROM user_profiles WHERE id = auth.uid() AND role = 'LEGAL')
);

-- All authenticated users can view agent profiles
CREATE POLICY "View agent profiles" ON agent_profiles
FOR SELECT USING (is_active = true);
```

### **✅ Audit Trail**
```sql
-- All assignments tracked with:
- assigned_by (who assigned)
- assignment_reason (why assigned)
- priority_level (urgency)
- is_temporary (temporary/permanent)
- temporary_until (expiry date)
- created_at/updated_at (timestamps)
```

---

## 🎯 **BENEFITS SISTEM**

### **✅ Untuk Customer**
- **Choice & Control**: Customer bisa pilih agent sendiri
- **Transparency**: Lihat profil dan rating agent
- **Matching**: Agent yang cocok dengan kebutuhan
- **Flexibility**: Bisa ganti agent jika tidak cocok
- **Quality Assurance**: Agent rating dan performance tracking

### **✅ Untuk Legal Staff**
- **Override Capability**: Bisa timpa pilihan customer
- **Urgent Handling**: Prioritas untuk kasus urgent
- **Temporary Assignment**: Solusi sementara untuk kasus khusus
- **Assignment Control**: Kontrol penuh atas penugasan
- **Audit Trail**: Complete history semua penugasan

### **✅ Untuk Perusahaan**
- **Customer Satisfaction**: Customer puas dengan pilihan sendiri
- **Operational Efficiency**: Optimalisasi agent workload
- **Quality Control**: Legal oversight untuk critical cases
- **Performance Tracking**: Agent performance metrics
- **Resource Management**: Efficient agent utilization

---

## 🎯 **IMPLEMENTATION STATUS**

### **✅ Completed Features**
1. **Database Schema**: Agent profiles, preferences, assignments ✅
2. **Customer Selection**: Agent selection screen dengan filter ✅
3. **Legal Management**: Assignment management screen ✅
4. **Functions**: Complete API functions untuk selection & assignment ✅
5. **Security**: RLS policies dan access control ✅
6. **UI Components**: Agent cards, filters, dialogs ✅
7. **Matching Algorithm**: Tier-based matching dengan scoring ✅

### **✅ Ready for Production**
- **Database Functions**: All functions tested and documented
- **Mobile UI**: Complete screens dengan error handling
- **Security**: Proper RLS policies dan audit trail
- **Performance**: Optimized queries dan indexes
- **Documentation**: Complete implementation guide

---

## 🎯 **USAGE EXAMPLES**

### **✅ Customer Selects Agent**
```kotlin
// Customer calls this function
viewModel.selectAgent(agentId, "Saya merasa nyaman dengan agent ini")

// System executes:
// 1. Check agent availability
// 2. Update customer preference
// 3. Increase agent customer count
// 4. Create assignment record
// 5. Send notifications
```

### **✅ Legal Assigns Agent**
```kotlin
// Legal staff calls this function
viewModel.assignAgent(
    customerId = "customer-123",
    agentId = "agent-456", 
    reason = "Kasus urgent membutuhkan agent senior",
    priority = "URGENT",
    isTemporary = true,
    temporaryUntil = "2026-02-01"
)

// System executes:
// 1. Override customer preference
// 2. Create legal assignment record
// 3. Update agent load
// 4. Set temporary expiry
// 5. Notify all parties
```

---

## 🎯 **FINAL RECOMMENDATION**

**Agent Selection System memberikan fleksibilitas maksimal dengan:**
- ✅ **Customer Choice**: Customer bisa pilih agent sendiri
- ✅ **Legal Control**: Legal bisa override dan mengatur penugasan
- ✅ **Smart Matching**: Algorithm kecocokan berdasarkan tier dan kebutuhan
- ✅ **Temporary Assignment**: Solusi sementara untuk kasus khusus
- ✅ **Complete Audit**: Full tracking semua penugasan
- ✅ **Performance Metrics**: Agent rating dan workload management

**Sistem siap diimplementasikan dengan complete customer choice dan legal oversight!** 👤⚖️✨

Sistem ini akan memberikan keseimbangan sempurna antara customer autonomy dan legal control! 🚀📱
