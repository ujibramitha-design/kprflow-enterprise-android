import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2"

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
}

interface WhatsAppMessage {
  to: string
  type: 'text' | 'document' | 'image'
  content: string
  fileName?: string
  fileUrl?: string
}

interface NotificationData {
  userId?: string
  title: string
  message: string
  type: 'lead_generated' | 'status_change' | 'document_uploaded' | 'unit_cancelled' | 'payment_reminder'
  data?: Record<string, any>
  referenceId?: string
}

serve(async (req) => {
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders })
  }

  try {
    const supabaseUrl = Deno.env.get('SUPABASE_URL')!
    const supabaseKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!
    const waApiKey = Deno.env.get('WA_API_KEY')!
    const waGatewayUrl = Deno.env.get('WA_GATEWAY_URL')!

    const supabase = createClient(supabaseUrl, supabaseKey)

    if (req.method === 'POST') {
      const { action, data }: { action: string; data: any } = await req.json()

      switch (action) {
        case 'send_notification':
          const result = await sendWhatsAppNotification(
            data as NotificationData,
            supabase,
            waApiKey,
            waGatewayUrl
          )
          return new Response(
            JSON.stringify(result),
            { headers: { ...corsHeaders, 'Content-Type': 'application/json' }, status: 200 }
          )

        case 'send_bulk_notifications':
          const bulkResult = await sendBulkNotifications(
            data.notifications as NotificationData[],
            supabase,
            waApiKey,
            waGatewayUrl
          )
          return new Response(
            JSON.stringify(bulkResult),
            { headers: { ...corsHeaders, 'Content-Type': 'application/json' }, status: 200 }
          )

        case 'webhook_status':
          await handleWebhookStatus(data, supabase)
          return new Response(
            JSON.stringify({ success: true }),
            { headers: { ...corsHeaders, 'Content-Type': 'application/json' }, status: 200 }
          )

        default:
          return new Response(
            JSON.stringify({ error: 'Invalid action' }),
            { headers: { ...corsHeaders, 'Content-Type': 'application/json' }, status: 400 }
          )
      }
    }

    return new Response(
      JSON.stringify({ error: 'Method not allowed' }),
      { headers: { ...corsHeaders, 'Content-Type': 'application/json' }, status: 405 }
    )
  } catch (error: any) {
    console.error('Error in whatsapp-engine:', error)
    return new Response(
      JSON.stringify({ error: error.message || 'Internal server error' }),
      { headers: { ...corsHeaders, 'Content-Type': 'application/json' }, status: 500 }
    )
  }
})

async function sendWhatsAppNotification(
  notification: NotificationData,
  supabase: any,
  waApiKey: string,
  waGatewayUrl: string
) {
  try {
    // Get user phone number
    let phoneNumber = ''
    
    if (notification.userId) {
      const { data: user, error } = await supabase
        .from('user_profiles')
        .select('phone_number, name')
        .eq('id', notification.userId)
        .single()
      
      if (error || !user) {
        return { success: false, error: 'User not found' }
      }
      
      phoneNumber = user.phone_number
    } else if (notification.data?.customer_phone) {
      phoneNumber = notification.data.customer_phone
    } else {
      return { success: false, error: 'No phone number available' }
    }

    // Format phone number (remove non-digits, add country code if needed)
    const formattedPhone = formatPhoneNumber(phoneNumber)
    
    // Craft message based on notification type
    const message = craftMessage(notification)
    
    // Send WhatsApp message
    const waMessage: WhatsAppMessage = {
      to: formattedPhone,
      type: 'text',
      content: message
    }

    const response = await sendWhatsAppMessage(waMessage, waApiKey, waGatewayUrl)
    
    if (response.success) {
      // Log the notification
      await logNotification(notification, supabase, 'whatsapp', response.messageId)
    }
    
    return response
  } catch (error: any) {
    console.error('Error sending WhatsApp notification:', error)
    return { success: false, error: error.message }
  }
}

async function sendBulkNotifications(
  notifications: NotificationData[],
  supabase: any,
  waApiKey: string,
  waGatewayUrl: string
) {
  const results = []
  
  for (const notification of notifications) {
    const result = await sendWhatsAppNotification(notification, supabase, waApiKey, waGatewayUrl)
    results.push({
      notificationId: notification.referenceId || notification.userId,
      ...result
    })
    
    // Add delay to avoid rate limiting
    await new Promise(resolve => setTimeout(resolve, 1000))
  }
  
  return {
    success: true,
    total: notifications.length,
    results
  }
}

async function handleWebhookStatus(data: any, supabase: any) {
  try {
    const { messageId, status, timestamp } = data
    
    // Update notification status in database
    await supabase
      .from('notification_logs')
      .update({
        delivery_status: status,
        delivered_at: timestamp,
        updated_at: new Date().toISOString()
      })
      .eq('message_id', messageId)
    
    console.log(`WhatsApp message ${messageId} status updated to ${status}`)
  } catch (error: any) {
    console.error('Error handling webhook status:', error)
  }
}

function formatPhoneNumber(phone: string): string {
  // Remove all non-digit characters
  let cleaned = phone.replace(/\D/g, '')
  
  // Add Indonesia country code if not present
  if (!cleaned.startsWith('62')) {
    if (cleaned.startsWith('0')) {
      cleaned = '62' + cleaned.substring(1)
    } else {
      cleaned = '62' + cleaned
    }
  }
  
  return cleaned + '@c.us' // WhatsApp format
}

function craftMessage(notification: NotificationData): string {
  const { title, message, type, data } = notification
  
  switch (type) {
    case 'lead_generated':
      return `🎉 *${title}*\n\n${message}\n\nUnit: ${data?.unit_block || '-'}\nCustomer: ${data?.customer_name || '-'}\n\nTerima kasih telah menggunakan KPRFlow Enterprise.\n\nHubungi kami untuk info lebih lanjut.`

    case 'status_change':
      return `📋 *${title}*\n\n${message}\n\nStatus: ${data?.new_status || '-'}\n${data?.previous_status ? `Previous: ${data.previous_status}` : ''}\n\nLogin ke dashboard untuk detail lengkap.\n\nKPRFlow Enterprise`

    case 'document_uploaded':
      return `📄 *${title}*\n\n${message}\n\nDocument: ${data?.document_type || '-'}\nUploaded: ${new Date().toLocaleString('id-ID')}\n\nDocument sedang diproses oleh tim kami.\n\nKPRFlow Enterprise`

    case 'unit_cancelled':
      return `⚠️ *${title}*\n\n${message}\n\nUnit: ${data?.unit_block || '-'}\nReason: ${data?.cancellation_reason || '-'}\n\nHubungi marketing untuk informasi lebih lanjut.\n\nKPRFlow Enterprise`

    case 'payment_reminder':
      return `💰 *${title}*\n\n${message}\n\nAmount: ${data?.amount || '-'}\nDue Date: ${data?.due_date || '-'}\n\nSegera lakukan pembayaran untuk menghindari keterlambatan.\n\nKPRFlow Enterprise`

    default:
      return `*${title}*\n\n${message}\n\nKPRFlow Enterprise`
  }
}

async function sendWhatsAppMessage(
  message: WhatsAppMessage,
  apiKey: string,
  gatewayUrl: string
) {
  try {
    const payload = {
      apikey: apiKey,
      target: message.to,
      message: message.content,
      type: message.type
    }

    if (message.type === 'document' && message.fileUrl) {
      payload.file_url = message.fileUrl
      payload.file_name = message.fileName
    }

    const response = await fetch(gatewayUrl, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(payload)
    })

    const result = await response.json()
    
    if (result.status) {
      return {
        success: true,
        messageId: result.id || result.message_id,
        response: result
      }
    } else {
      return {
        success: false,
        error: result.message || 'Failed to send WhatsApp message'
      }
    }
  } catch (error: any) {
    return {
      success: false,
      error: error.message
    }
  }
}

async function logNotification(
  notification: NotificationData,
  supabase: any,
  channel: string,
  messageId?: string
) {
  try {
    await supabase
      .from('notification_logs')
      .insert({
        user_id: notification.userId,
        title: notification.title,
        message: notification.message,
        type: notification.type,
        channel: channel,
        message_id: messageId,
        data: notification.data,
        reference_id: notification.referenceId,
        created_at: new Date().toISOString()
      })
  } catch (error: any) {
    console.error('Error logging notification:', error)
  }
}
