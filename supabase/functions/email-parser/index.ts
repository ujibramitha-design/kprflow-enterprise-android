import { serve } from 'https://deno.land/std@0.168.0/http/server.ts'
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
}

interface EmailData {
  subject: string
  from_email: string
  text_body?: string
  html_body?: string
  attachments?: Array<{
    filename: string
    content_type: string
    data: string
  }>
}

interface ParsedEmail {
  success: boolean
  data?: {
    customerName: string
    customerEmail: string
    customerPhone?: string
    unitType?: string
    sprNumber?: string
    attachments?: Array<{
      filename: string
      url: string
    }>
  }
  error?: string
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

    // Verify webhook authorization (optional but recommended)
    const authHeader = req.headers.get('authorization')
    if (authHeader !== `Bearer ${Deno.env.get('WEBHOOK_SECRET')}`) {
      return new Response(
        JSON.stringify({ error: 'Unauthorized webhook request' }),
        { headers: { ...corsHeaders, 'Content-Type': 'application/json' }, status: 401 }
      )
    }

    if (req.method === 'POST') {
      const emailData: EmailData = await req.json()

      // Validate email data
      if (!emailData.subject || !emailData.from_email) {
        return new Response(
          JSON.stringify({ error: 'Missing required email fields' }),
          { headers: { ...corsHeaders, 'Content-Type': 'application/json' }, status: 400 }
        )
      }

      // Filter for SPR-related emails only
      const subject = emailData.subject.toLowerCase()
      if (!subject.includes('surat pesanan') && !subject.includes('spr') && !subject.includes('po')) {
        return new Response(
          JSON.stringify({ message: 'Email not relevant to SPR processing' }),
          { headers: { ...corsHeaders, 'Content-Type': 'application/json' }, status: 200 }
        )
      }

      // Parse email content
      const parsedEmail = await parseEmailContent(emailData)
      
      if (!parsedEmail.success || !parsedEmail.data) {
        return new Response(
          JSON.stringify({ error: parsedEmail.error || 'Failed to parse email' }),
          { headers: { ...corsHeaders, 'Content-Type': 'application/json' }, status: 400 }
        )
      }

      // Create lead in database
      const { data: leadData, error: leadError } = await supabase
        .from('kpr_dossiers')
        .insert({
          user_id: null, // Will be updated when customer registers
          status: 'LEAD',
          booking_date: new Date().toISOString().split('T')[0],
          notes: `Auto-generated from email: ${emailData.subject}`,
          created_at: new Date().toISOString(),
          updated_at: new Date().toISOString()
        })
        .select()
        .single()

      if (leadError) {
        console.error('Error creating lead:', leadError)
        return new Response(
          JSON.stringify({ error: 'Failed to create lead in database' }),
          { headers: { ...corsHeaders, 'Content-Type': 'application/json' }, status: 500 }
        )
      }

      // Process attachments if any
      if (parsedEmail.data.attachments && parsedEmail.data.attachments.length > 0) {
        for (const attachment of parsedEmail.data.attachments) {
          // Store attachment in Supabase Storage
          const { data: uploadData, error: uploadError } = await supabase.storage
            .from('documents')
            .upload(`${leadData.id}/${attachment.filename}`, decodeBase64(attachment.data))

          if (uploadError) {
            console.error('Error uploading attachment:', uploadError)
            continue
          }

          // Create document record
          await supabase
            .from('documents')
            .insert({
              dossier_id: leadData.id,
              file_name: attachment.filename,
              url: uploadData.path,
              type: attachment.filename.split('.').pop()?.toLowerCase(),
              uploaded_by: 'system',
              uploaded_at: new Date().toISOString(),
              updated_at: new Date().toISOString()
            })
        }
      }

      // Send notification to marketing team
      await supabase
        .from('notifications')
        .insert({
          user_id: null, // Will be filtered by role in RLS
          title: 'New Lead Generated',
          message: `New lead from ${parsedEmail.data.customerName} (${parsedEmail.data.customerEmail})`,
          type: 'lead_generated',
          data: {
            dossier_id: leadData.id,
            customer_name: parsedEmail.data.customerName,
            customer_email: parsedEmail.data.customerEmail
          },
          created_at: new Date().toISOString(),
          read_at: null
        })

      return new Response(
        JSON.stringify({ 
          success: true, 
          message: 'Lead created successfully',
          lead_id: leadData.id 
        }),
        { headers: { ...corsHeaders, 'Content-Type': 'application/json' }, status: 200 }
      )
    }

    return new Response(
      JSON.stringify({ error: 'Method not allowed' }),
      { headers: { ...corsHeaders, 'Content-Type': 'application/json' }, status: 405 }
    )

  } catch (error: any) {
    console.error('Error in email-parser:', error)
    return new Response(
      JSON.stringify({ error: error.message || 'Internal server error' }),
      { headers: { ...corsHeaders, 'Content-Type': 'application/json' }, status: 500 }
    )
  }
})

async function parseEmailContent(emailData: EmailData): Promise<ParsedEmail> {
  try {
    const subject = emailData.subject
    const body = emailData.text_body || emailData.html_body || ''
    
    // Extract customer information using regex patterns
    const namePattern = /(?:nama|name)[:\s]+([A-Za-z\s]+)/i
    const emailPattern = /([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,})/
    const phonePattern = /(?:tel|phone|no\.?hp|whatsapp)[:\s]*([0-9\-\s\+]+)/i
    const sprPattern = /(?:spr|po|nomor)[:\s]*([A-Z0-9\-\/]+)/i
    
    const nameMatch = subject.match(namePattern) || body.match(namePattern)
    const emailMatch = body.match(emailPattern)
    const phoneMatch = body.match(phonePattern)
    const sprMatch = subject.match(sprPattern) || body.match(sprPattern)
    
    const customerName = nameMatch ? nameMatch[1].trim() : extractNameFromEmail(emailData.from_email)
    const customerEmail = emailMatch ? emailMatch[1] : emailData.from_email
    const customerPhone = phoneMatch ? phoneMatch[1].replace(/[\s\-\(\)]/g, '') : undefined
    const sprNumber = sprMatch ? sprMatch[1] : undefined
    
    // Process attachments
    const attachments = emailData.attachments?.map(att => ({
      filename: att.filename,
      data: att.data,
      url: '' // Will be populated after upload
    }))
    
    return {
      success: true,
      data: {
        customerName,
        customerEmail,
        customerPhone,
        sprNumber,
        attachments
      }
    }
  } catch (error) {
    console.error('Error parsing email:', error)
    return {
      success: false,
      error: 'Failed to parse email content'
    }
  }
}

function extractNameFromEmail(email: string): string {
  const localPart = email.split('@')[0]
  return localPart
    .replace(/[._-]/g, ' ')
    .split(' ')
    .map(word => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ')
}

function decodeBase64(base64: string): Uint8Array {
  const binaryString = atob(base64)
  const bytes = new Uint8Array(binaryString.length)
  for (let i = 0; i < binaryString.length; i++) {
    bytes[i] = binaryString.charCodeAt(i)
  }
  return bytes
}
