import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2"

interface WhatsAppWebhookPayload {
  object: string
  entry: Array<{
    id: string
    changes: Array<{
      field: string
      value: {
        messaging_product: string
        metadata: {
          display_phone_number: string
          phone_number_id: string
        }
        messages?: Array<{
          from: string
          id: string
          timestamp: string
          text?: {
            body: string
          }
          type: string
        }>
      }
    }>
  }>
}

interface SPRData {
  customer_name: string
  customer_phone: string
  unit_type: string
  block: string
  unit_number: string
  price: number
  dp_amount?: number
  kpr_amount?: number
  bank_name?: string
  notes?: string
}

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
}

serve(async (req: Request) => {
  // Handle CORS preflight requests
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders })
  }

  try {
    // Initialize Supabase client
    const supabaseUrl = Deno.env.get('SUPABASE_URL') || ''
    const supabaseKey = Deno.env.get('SUPABASE_ANON_KEY') || ''
    
    const supabaseClient = createClient(supabaseUrl, supabaseKey, {
      global: {
        headers: { Authorization: req.headers.get('Authorization') || '' },
      },
    })

    if (req.method === 'POST') {
      const body: WhatsAppWebhookPayload = await req.json()
      
      // Process WhatsApp webhook
      for (const entry of body.entry) {
        for (const change of entry.changes) {
          if (change.field === 'messages' && change.value.messages) {
            for (const message of change.value.messages) {
              if (message.type === 'text' && message.text) {
                await processWhatsAppMessage(
                  supabaseClient,
                  message.from,
                  message.id,
                  message.text.body,
                  message.timestamp
                )
              }
            }
          }
        }
      }

      return new Response(
        JSON.stringify({ status: 'success', message: 'Webhook processed' }),
        { 
          headers: { ...corsHeaders, 'Content-Type': 'application/json' },
          status: 200 
        }
      )
    }

    // Handle GET request for webhook verification
    if (req.method === 'GET') {
      const url = new URL(req.url)
      const mode = url.searchParams.get('hub.mode')
      const token = url.searchParams.get('hub.verify_token')
      const challenge = url.searchParams.get('hub.challenge')

      if (mode === 'subscribe' && token === Deno.env.get('WHATSAPP_VERIFY_TOKEN')) {
        return new Response(challenge || '', { status: 200 })
      } else {
        return new Response('Verification failed', { status: 403 })
      }
    }

    return new Response(
      JSON.stringify({ error: 'Method not allowed' }),
      { 
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
        status: 405 
      }
    )
  } catch (error: any) {
    console.error('Error processing webhook:', error)
    return new Response(
      JSON.stringify({ error: error.message || 'Unknown error' }),
      { 
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
        status: 500 
      }
    )
  }
})

async function processWhatsAppMessage(
  supabaseClient: any,
  from: string,
  messageId: string,
  content: string,
  timestamp: string
) {
  try {
    // Store WhatsApp message
    const { data: messageData, error: messageError } = await supabaseClient
      .from('whatsapp_messages')
      .insert({
        message_id: messageId,
        sender_phone: from,
        sender_name: extractNameFromContent(content),
        content: content,
        timestamp: new Date(parseInt(timestamp) * 1000).toISOString(),
        group_id: 'KPRFlow_Group_001', // Default group
        processed: false
      })
      .select()

    if (messageError) {
      console.error('Error storing message:', messageError)
      return
    }

    // Check if message contains SPR keywords
    if (containsSPRKeywords(content)) {
      // Extract SPR data
      const sprData = extractSPRData(content, from)
      
      if (sprData) {
        // Create SPR record
        const { data: sprRecord, error: sprError } = await supabaseClient
          .from('spr_data')
          .insert({
            ...sprData,
            whatsapp_message_id: messageId,
            status: 'INACTIVE'
          })
          .select()

        if (sprError) {
          console.error('Error creating SPR:', sprError)
        } else {
          // Mark message as processed
          await supabaseClient
            .from('whatsapp_messages')
            .update({ processed: true })
            .eq('message_id', messageId)

          // Send confirmation message (in real implementation)
          await sendConfirmationMessage(from, sprRecord[0].id)
        }
      }
    }
  } catch (error) {
    console.error('Error processing message:', error)
  }
}

function containsSPRKeywords(content: string): boolean {
  const keywords = [
    'pesan rumah', 'mau pesan', 'beli rumah', 'interested', 
    'type', 'blok', 'harga', 'dp', 'kpr', 'cash'
  ]
  
  return keywords.some(keyword => 
    content.toLowerCase().includes(keyword.toLowerCase())
  )
}

function extractSPRData(content: string, from: string): SPRData | null {
  try {
    // Extract information using regex patterns
    const nameMatch = content.match(/(?:saya|nama|aku)\s+([A-Za-z\s]+)/i)
    const typeMatch = content.match(/type\s+(\d+\/\d+)/i)
    const blockMatch = content.match(/blok\s+([A-Z])\s*(?:no\s*\.?\s*(\d+))?/i)
    const priceMatch = content.match(/(\d+(?:\.\d+)?)\s*(?:jt|juta|miliar|m)/i)
    const dpMatch = content.match(/dp\s+(\d+(?:\.\d+)?)\s*%?/i)
    const bankMatch = content.match(/(?:kpr|bank)\s+([A-Z]{3})/i)

    const customerName = nameMatch ? nameMatch[1].trim() : extractNameFromContent(content)
    const unitType = typeMatch ? typeMatch[1] : '36/72'
    const block = blockMatch ? blockMatch[1] : 'A'
    const unitNumber = blockMatch && blockMatch[2] ? blockMatch[2] : '1'
    
    let price = 450000000 // Default price
    if (priceMatch) {
      const priceValue = parseFloat(priceMatch[1].replace('.', ''))
      const multiplier = content.toLowerCase().includes('miliar') ? 1_000_000_000 :
                         content.toLowerCase().includes('juta') || content.toLowerCase().includes('jt') ? 1_000_000 : 1_000_000
      price = Math.round(priceValue * multiplier)
    }

    const dpAmount = dpMatch ? Math.round(price * parseFloat(dpMatch[1].replace('.', '')) / 100) : undefined
    const kprAmount = dpAmount ? price - dpAmount : price
    const bankName = bankMatch ? bankMatch[1] : undefined

    return {
      customer_name: customerName,
      customer_phone: from,
      unit_type: unitType,
      block: block,
      unit_number: unitNumber,
      price: price,
      dp_amount: dpAmount,
      kpr_amount: kprAmount,
      bank_name: bankName,
      notes: content
    }
  } catch (error) {
    console.error('Error extracting SPR data:', error)
    return null
  }
}

function extractNameFromContent(content: string): string {
  // Try to extract name from message content
  const namePatterns = [
    /(?:saya|nama|aku)\s+([A-Za-z\s]+)/i,
    /^([A-Za-z\s]+),/i,
    /saya\s+([A-Za-z\s]+)/i
  ]

  for (const pattern of namePatterns) {
    const match = content.match(pattern)
    if (match && match[1]) {
      return match[1].trim()
    }
  }

  return 'Unknown'
}

async function sendConfirmationMessage(phoneNumber: string, sprId: string) {
  // In real implementation, this would use WhatsApp Business API
  console.log(`Sending confirmation to ${phoneNumber} for SPR ${sprId}`)
  
  // Mock implementation
  const confirmationMessage = `Terima kasih telah menghubungi KPRFlow! SPR Anda telah dibuat dengan ID: ${sprId}. Status: INACTIVE. Tim marketing kami akan segera menghubungi Anda untuk verifikasi.`
  
  console.log(`Message: ${confirmationMessage}`)
}
