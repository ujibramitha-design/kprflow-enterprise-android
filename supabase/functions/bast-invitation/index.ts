import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2"

// =====================================================
// AUTOMATED BAST INVITATION SYSTEM
// Edge Function for WhatsApp integration and BAST scheduling
// =====================================================

interface InspectionData {
  id: string
  unit_id: string
  dossier_id: string
  customer_name: string
  customer_email: string
  customer_phone: string
  block: string
  unit_number: string
  inspector_name: string
  status: string
  bast_invitation_sent: boolean
}

interface BASTInvitation {
  id: string
  inspection_id: string
  dossier_id: string
  customer_id: string
  invitation_token: string
  invitation_url: string
  expires_at: string
  scheduled_date?: string
  scheduled_time?: string
}

interface WhatsAppMessage {
  to: string
  message: string
  templateName?: string
  templateData?: Record<string, string>
}

// Supabase client
const supabaseUrl = Deno.env.get('SUPABASE_URL')!
const supabaseKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!
const supabase = createClient(supabaseUrl, supabaseKey)

// WhatsApp API configuration
const WHATSAPP_CONFIG = {
  apiKey: Deno.env.get('WHATSAPP_API_KEY'),
  baseUrl: Deno.env.get('WHATSAPP_BASE_URL'),
  phoneNumber: Deno.env.get('WHATSAPP_PHONE_NUMBER')
}

// =====================================================
// MAIN EDGE FUNCTION
// =====================================================

serve(async (req) => {
  try {
    // Handle CORS
    if (req.method === 'OPTIONS') {
      return new Response(null, { 
        headers: corsHeaders 
      })
    }

    // Parse request
    const { method } = req
    const url = new URL(req.url)
    const action = url.searchParams.get('action') || 'trigger'

    console.log(`BAST invitation started: ${action}`)

    let result: any

    switch (action) {
      case 'trigger':
        result = await triggerBastInvitation()
        break
      case 'schedule':
        const body = await req.json()
        result = await scheduleBast(body)
        break
      case 'respond':
        const response = await req.json()
        result = await handleCustomerResponse(response)
        break
      case 'complete':
        const completion = await req.json()
        result = await completeBast(completion)
        break
      default:
        result = { success: false, message: 'Invalid action' }
    }

    return new Response(JSON.stringify(result), {
      headers: { ...corsHeaders, 'Content-Type': 'application/json' },
      status: result.success ? 200 : 400
    })

  } catch (error) {
    console.error('BAST invitation error:', error)
    
    return new Response(JSON.stringify({
      success: false,
      message: error.message
    }), {
      headers: { ...corsHeaders, 'Content-Type': 'application/json' },
      status: 500
    })
  }
})

// =====================================================
// CORE BAST INVITATION LOGIC
// =====================================================

async function triggerBastInvitation(): Promise<any> {
  try {
    // Get inspections that passed but haven't sent BAST invitation
    const { data: inspections, error: inspectionError } = await supabase
      .from('v_estate_inspection_status')
      .select('*')
      .eq('status', 'PASS')
      .eq('bast_invitation_sent', false)
      .eq('ready_for_bast', true)

    if (inspectionError) {
      throw new Error(`Failed to fetch inspections: ${inspectionError.message}`)
    }

    console.log(`Found ${inspections.length} inspections ready for BAST`)

    const results = []

    for (const inspection of inspections) {
      try {
        const result = await processBastInvitation(inspection)
        results.push(result)
      } catch (error) {
        console.error(`Error processing inspection ${inspection.id}:`, error)
        results.push({
          inspectionId: inspection.id,
          success: false,
          error: error.message
        })
      }
    }

    return {
      success: true,
      message: `Processed ${results.length} BAST invitations`,
      results
    }

  } catch (error) {
    return {
      success: false,
      message: `Failed to trigger BAST invitations: ${error.message}`
    }
  }
}

async function processBastInvitation(inspection: InspectionData): Promise<any> {
  // 1. Generate unique invitation link
  const invitationToken = generateInvitationToken()
  const invitationUrl = generateInvitationUrl(invitationToken)
  const expiresAt = new Date(Date.now() + 7 * 24 * 60 * 60 * 1000) // 7 days

  // 2. Create BAST invitation record
  const { data: invitation, error: invitationError } = await supabase
    .from('BASTInvitationLinks')
    .insert({
      inspection_id: inspection.id,
      dossier_id: inspection.dossier_id,
      customer_id: inspection.customer_email, // Will need to get actual customer ID
      invitation_token: invitationToken,
      invitation_url: invitationUrl,
      expires_at: expiresAt.toISOString()
    })
    .select()
    .single()

  if (invitationError) {
    throw new Error(`Failed to create invitation: ${invitationError.message}`)
  }

  // 3. Send WhatsApp message
  const whatsappResult = await sendWhatsAppInvitation(inspection, invitationUrl)

  // 4. Update inspection record
  await supabase
    .from('EstateInspection')
    .update({
      bast_invitation_sent: true,
      bast_invitation_sent_at: new Date().toISOString()
    })
    .eq('id', inspection.id)

  // 5. Update invitation record
  await supabase
    .from('BASTInvitationLinks')
    .update({
      whatsapp_sent: whatsappResult.success,
      whatsapp_sent_at: new Date().toISOString()
    })
    .eq('id', invitation.id)

  return {
    inspectionId: inspection.id,
    success: true,
    invitationId: invitation.id,
    whatsappSent: whatsappResult.success,
    invitationUrl
  }
}

// =====================================================
// WHATSAPP INTEGRATION
// =====================================================

async function sendWhatsAppInvitation(
  inspection: InspectionData, 
  invitationUrl: string
): Promise<{ success: boolean, message?: string }> {
  try {
    const message = buildWhatsAppMessage(inspection, invitationUrl)
    
    // Send WhatsApp message
    const response = await fetch(`${WHATSAPP_CONFIG.baseUrl}/messages`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${WHATSAPP_CONFIG.apiKey}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        messaging_product: 'whatsapp',
        to: inspection.customer_phone,
        type: 'text',
        text: {
          body: message
        }
      })
    })

    if (!response.ok) {
      const error = await response.json()
      throw new Error(`WhatsApp API error: ${error.error?.message || 'Unknown error'}`)
    }

    const result = await response.json()
    
    return {
      success: true,
      message: `WhatsApp message sent successfully. Message ID: ${result.messages[0].id}`
    }

  } catch (error) {
    console.error('WhatsApp sending error:', error)
    return {
      success: false,
      message: error.message
    }
  }
}

function buildWhatsAppMessage(
  inspection: InspectionData, 
  invitationUrl: string
): string {
  return `🎉 *Selamat! Unit ${inspection.block} ${inspection.unit_number} Lolos QC*

Hormat ${inspection.customer_name},

Kami senang menginformasikan bahwa unit Anda telah melewati uji kualitas oleh tim Estate kami dan dinyatakan *LULUS*.

📋 *Detail Inspeksi:*
• Unit: ${inspection.block} ${inspection.unit_number}
• Inspector: ${inspection.inspector_name}
• Status: LULAS ✅

🗓️ *Serah Terima Bangunan (BAST)*

Silakan pilih jadwal Serah Terima Bangunan (BAST) melalui link berikut:
${invitationUrl}

⏰ Link berlaku selama 7 hari

Jika Anda memiliki pertanyaan, jangan ragu menghubungi kami.

Terima kasih,
Tim KPRFlow Enterprise

---
*Ini adalah pesan otomatis. Mohon tidak membalas pesan ini.*`
}

// =====================================================
// BAST SCHEDULING
// =====================================================

async function scheduleBast(body: {
  invitationToken: string
  scheduledDate: string
  scheduledTime: string
  notes?: string
}): Promise<any> {
  try {
    const { invitationToken, scheduledDate, scheduledTime, notes } = body

    // Validate invitation token
    const { data: invitation, error: invitationError } = await supabase
      .from('BASTInvitationLinks')
      .select('*')
      .eq('invitation_token', invitationToken)
      .eq('is_used', false)
      .single()

    if (invitationError || !invitation) {
      throw new Error('Invalid or expired invitation token')
    }

    // Check if expired
    if (new Date(invitation.expires_at) < new Date()) {
      throw new Error('Invitation has expired')
    }

    // Update invitation with customer response
    await supabase
      .from('BASTInvitationLinks')
      .update({
        customer_response: 'ACCEPTED',
        response_at: new Date().toISOString(),
        scheduled_date: scheduledDate,
        scheduled_time: scheduledTime,
        response_notes: notes
      })
      .eq('id', invitation.id)

    // Update inspection record
    await supabase
      .from('EstateInspection')
      .update({
        bast_status: 'SCHEDULED',
        bast_scheduled_date: scheduledDate,
        bast_scheduled_time: scheduledTime,
        bast_location: 'KPRFlow Office' // Default location
      })
      .eq('id', invitation.inspection_id)

    // Send confirmation WhatsApp
    await sendBastConfirmation(invitation, scheduledDate, scheduledTime)

    return {
      success: true,
      message: 'BAST scheduled successfully',
      scheduledDate,
      scheduledTime
    }

  } catch (error) {
    return {
      success: false,
      message: error.message
    }
  }
}

async function sendBastConfirmation(
  invitation: BASTInvitation,
  scheduledDate: string,
  scheduledTime: string
): Promise<void> {
  try {
    // Get customer details
    const { data: customer } = await supabase
      .from('UserProfile')
      .select('name, phone')
      .eq('id', invitation.customer_id)
      .single()

    if (!customer) return

    const message = `✅ *BAST Scheduled Successfully*

Hormat ${customer.name},

Jadwal Serah Terima Bangunan (BAST) Anda telah dikonfirmasi:

📅 *Tanggal:* ${formatDate(scheduledDate)}
⏰ *Waktu:* ${scheduledTime}
📍 *Lokasi:* KPRFlow Office

Mohon datang tepat waktu dengan membawa:
• KTP asli
• Dokumen pendukung lainnya

Jika perlu mengubah jadwal, silakan hubungi kami.

Terima kasih!`

    // Send WhatsApp (implementation similar to sendWhatsAppInvitation)
    console.log('BAST confirmation message prepared for:', customer.phone)

  } catch (error) {
    console.error('Error sending BAST confirmation:', error)
  }
}

// =====================================================
// BAST COMPLETION
// =====================================================

async function completeBast(body: {
  inspectionId: string
  signedDocumentUrl: string
  notes?: string
}): Promise<any> {
  try {
    const { inspectionId, signedDocumentUrl, notes } = body

    // Update inspection record
    const { error: updateError } = await supabase
      .from('EstateInspection')
      .update({
        bast_status: 'COMPLETED',
        bast_completed_at: new Date().toISOString(),
        bast_signed_document_url: signedDocumentUrl,
        bast_notes: notes
      })
      .eq('id', inspectionId)

    if (updateError) {
      throw new Error(`Failed to update BAST status: ${updateError.message}`)
    }

    // Get dossier information
    const { data: inspection } = await supabase
      .from('EstateInspection')
      .select('dossier_id')
      .eq('id', inspectionId)
      .single()

    if (inspection) {
      // Update dossier status to COMPLETED
      await supabase
        .from('KprDossier')
        .update({
          status: 'COMPLETED',
          updated_at: new Date().toISOString()
        })
        .eq('id', inspection.dossier_id)
    }

    return {
      success: true,
      message: 'BAST completed successfully'
    }

  } catch (error) {
    return {
      success: false,
      message: error.message
    }
  }
}

// =====================================================
// UTILITY FUNCTIONS
// =====================================================

function generateInvitationToken(): string {
  const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789'
  let token = ''
  for (let i = 0; i < 32; i++) {
    token += chars.charAt(Math.floor(Math.random() * chars.length))
  }
  return token
}

function generateInvitationUrl(token: string): string {
  const baseUrl = Deno.env.get('BAST_INVITATION_BASE_URL') || 'https://kprflow.app/bast'
  return `${baseUrl}?token=${token}`
}

function formatDate(dateString: string): string {
  const date = new Date(dateString)
  const options: Intl.DateTimeFormatOptions = {
    weekday: 'long',
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  }
  return date.toLocaleDateString('id-ID', options)
}

async function handleCustomerResponse(response: {
  invitationToken: string
  customerResponse: 'ACCEPTED' | 'DECLINED' | 'RESCHEDULED'
  notes?: string
}): Promise<any> {
  try {
    const { invitationToken, customerResponse, notes } = response

    // Update invitation record
    await supabase
      .from('BASTInvitationLinks')
      .update({
        customer_response,
        response_at: new Date().toISOString(),
        response_notes: notes
      })
      .eq('invitation_token', invitationToken)

    return {
      success: true,
      message: `Customer response recorded: ${customerResponse}`
    }

  } catch (error) {
    return {
      success: false,
      message: error.message
    }
  }
}

// =====================================================
// CORS HEADERS
// =====================================================

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
  'Access-Control-Allow-Methods': 'GET, POST, PUT, DELETE, OPTIONS'
}
