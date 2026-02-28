import { serve } from 'https://deno.land/std@0.168.0/http/server.ts'
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'
import { Twilio } from 'https://esm.sh/twilio@4.19.0'

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
}

interface WhatsAppMessage {
  to: string
  templateName: string
  variables?: Record<string, string>
}

interface NotificationPayload {
  type: 'document_missing' | 'sp3k_issued' | 'sla_warning' | 'bast_invitation'
  userId: string
  dossierId?: string
  variables?: Record<string, string>
}

interface WhatsAppTemplate {
  name: string
  language: string
  components: Array<{
    type: string
    parameters: Array<{
      type: string
      text?: string
      currency?: {
        fallback_value: string
        code: string
        amount_1000: number
      }
    }>
  }>
}

serve(async (req) => {
  // Handle CORS preflight requests
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders })
  }

  try {
    // Initialize Supabase client
    const supabaseUrl = Deno.env.get('SUPABASE_URL')!
    const supabaseServiceKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!
    const supabase = createClient(supabaseUrl, supabaseServiceKey)

    // Initialize Twilio client
    const accountSid = Deno.env.get('TWILIO_ACCOUNT_SID')!
    const authToken = Deno.env.get('TWILIO_AUTH_TOKEN')!
    const twilio = new Twilio(accountSid, authToken)
    const whatsappFrom = Deno.env.get('TWILIO_WHATSAPP_FROM')!

    if (req.method === 'POST') {
      const payload: NotificationPayload = await req.json()

      // Get user phone number
      const { data: user, error: userError } = await supabase
        .from('user_profiles')
        .select('phone_number, name')
        .eq('id', payload.userId)
        .single()

      if (userError || !user) {
        return new Response(
          JSON.stringify({ error: 'User not found' }),
          { headers: { ...corsHeaders, 'Content-Type': 'application/json' }, status: 404 }
        )
      }

      const phoneNumber = user.phone_number
      if (!phoneNumber) {
        return new Response(
          JSON.stringify({ error: 'User phone number not found' }),
          { headers: { ...corsHeaders, 'Content-Type': 'application/json' }, status: 400 }
        )
      }

      // Format phone number for WhatsApp
      const formattedPhone = formatPhoneNumber(phoneNumber)

      // Create WhatsApp template based on notification type
      const template = createWhatsAppTemplate(payload.type, user.name, payload.variables)

      // Send WhatsApp message
      const message = await twilio.messages.create({
        from: `whatsapp:${whatsappFrom}`,
        to: `whatsapp:${formattedPhone}`,
        contentSid: template.sid || undefined,
        contentVariables: template.variables || undefined,
        body: template.body || undefined,
      })

      // Log notification
      await logNotification(supabase, {
        userId: payload.userId,
        dossierId: payload.dossierId,
        type: payload.type,
        channel: 'whatsapp',
        messageSid: message.sid,
        status: 'sent',
        sentAt: new Date().toISOString()
      })

      return new Response(
        JSON.stringify({
          success: true,
          messageSid: message.sid,
          status: 'sent'
        }),
        { headers: { ...corsHeaders, 'Content-Type': 'application/json' }, status: 200 }
      )
    }

    return new Response(
      JSON.stringify({ error: 'Method not allowed' }),
      { headers: { ...corsHeaders, 'Content-Type': 'application/json' }, status: 405 }
    )
  } catch (error) {
    console.error('WhatsApp Notifier Error:', error)
    return new Response(
      JSON.stringify({ error: 'Internal server error' }),
      { headers: { ...corsHeaders, 'Content-Type': 'application/json' }, status: 500 }
    )
  }
})

function formatPhoneNumber(phoneNumber: string): string {
  // Remove all non-digit characters
  const cleaned = phoneNumber.replace(/\D/g, '')
  
  // Add country code if not present (assuming Indonesia)
  if (!cleaned.startsWith('62')) {
    return `62${cleaned}`
  }
  
  return cleaned
}

function createWhatsAppTemplate(
  type: string,
  userName: string,
  variables?: Record<string, string>
): { sid?: string; body?: string; variables?: string } {
  const templates = {
    document_missing: {
      sid: 'HXxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx', // Replace with actual template SID
      variables: variables ? JSON.stringify(variables) : undefined,
      body: `Halo ${userName}, kami belum menerima dokumen yang diperlukan untuk pengajuan KPR Anda. Segera lengkapi dokumen untuk mempercepat proses.`
    },
    sp3k_issued: {
      sid: 'HXxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx', // Replace with actual template SID
      variables: variables ? JSON.stringify(variables) : undefined,
      body: `Selamat ${userName}! SP3K Anda telah terbit. Nomor SP3K: ${variables?.sp3kNumber || 'N/A'}. Silakan persiapkan dokumen untuk tahap selanjutnya.`
    },
    sla_warning: {
      sid: 'HXxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx', // Replace with actual template SID
      variables: variables ? JSON.stringify(variables) : undefined,
      body: `Halo ${userName}, pengajuan KPR Anda mendekati batas waktu SLA. Segera lengkapi dokumen yang diperlukan untuk menghindari penundaan.`
    },
    bast_invitation: {
      sid: 'HXxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx', // Replace with actual template SID
      variables: variables ? JSON.stringify(variables) : undefined,
      body: `Halo ${userName}, Anda diundang untuk serah terima bangunan (BAST) pada tanggal ${variables?.bastDate || 'N/A'} di lokasi properti.`
    }
  }

  return templates[type as keyof typeof templates] || {
    body: `Halo ${userName}, ada update terkait pengajuan KPR Anda.`
  }
}

async function logNotification(
  supabase: any,
  notification: {
    userId: string
    dossierId?: string
    type: string
    channel: string
    messageSid: string
    status: string
    sentAt: string
  }
): Promise<void> {
  try {
    await supabase
      .from('notification_logs')
      .insert({
        user_id: notification.userId,
        dossier_id: notification.dossierId,
        type: notification.type,
        channel: notification.channel,
        message_sid: notification.messageSid,
        status: notification.status,
        sent_at: notification.sentAt
      })
  } catch (error) {
    console.error('Error logging notification:', error)
    // Don't fail the entire process if logging fails
  }
}

// Webhook handler for database changes
async function handleDatabaseChange(payload: any, supabase: any, twilio: any): Promise<void> {
  const { record, type, table, schema } = payload

  if (table === 'kpr_dossiers') {
    const dossier = record
    const userId = dossier.user_id

    // Handle different status changes
    switch (dossier.status) {
      case 'SP3K_TERBIT':
        await sendWhatsAppNotification({
          type: 'sp3k_issued',
          userId,
          dossierId: dossier.id,
          variables: {
            sp3kNumber: dossier.sp3k_number || 'N/A'
          }
        }, supabase, twilio)
        break

      case 'BAST_READY':
        await sendWhatsAppNotification({
          type: 'bast_invitation',
          userId,
          dossierId: dossier.id,
          variables: {
            bastDate: dossier.bast_date || 'N/A'
          }
        }, supabase, twilio)
        break
    }
  }

  if (table === 'documents') {
    const document = record
    const userId = document.user_id

    // Check if document is rejected
    if (!document.is_verified && document.rejection_reason) {
      await sendWhatsAppNotification({
        type: 'document_missing',
        userId,
        dossierId: document.dossier_id,
        variables: {
          documentType: document.type,
          rejectionReason: document.rejection_reason
        }
      }, supabase, twilio)
    }
  }
}

async function sendWhatsAppNotification(
  notification: NotificationPayload,
  supabase: any,
  twilio: any
): Promise<void> {
  try {
    // Get user phone number
    const { data: user } = await supabase
      .from('user_profiles')
      .select('phone_number, name')
      .eq('id', notification.userId)
      .single()

    if (!user?.phone_number) return

    const phoneNumber = formatPhoneNumber(user.phone_number)
    const whatsappFrom = Deno.env.get('TWILIO_WHATSAPP_FROM')!

    const template = createWhatsAppTemplate(
      notification.type,
      user.name,
      notification.variables
    )

    await twilio.messages.create({
      from: `whatsapp:${whatsappFrom}`,
      to: `whatsapp:${phoneNumber}`,
      contentSid: template.sid || undefined,
      contentVariables: template.variables || undefined,
      body: template.body || undefined,
    })

    await logNotification(supabase, {
      userId: notification.userId,
      dossierId: notification.dossierId,
      type: notification.type,
      channel: 'whatsapp',
      messageSid: 'auto-generated',
      status: 'sent',
      sentAt: new Date().toISOString()
    })
  } catch (error) {
    console.error('Error sending WhatsApp notification:', error)
  }
}
