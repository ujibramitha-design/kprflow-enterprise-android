import { serve } from 'https://deno.land/std@0.168.0/http/server.ts'
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'
import { PDFDocument } from 'https://cdn.skypack.dev/pdf-lib@1.17.1'
import { parse } from 'https://deno.land/std@0.168.0/flags/mod.ts'

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
}

interface SPRData {
  nik: string
  name: string
  unitBlock: string
  email?: string
  phoneNumber?: string
}

interface ParsedSPR {
  success: boolean
  data?: SPRData
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

    if (req.method === 'POST') {
      const formData = await req.formData()
      const file = formData.get('pdf') as File

      if (!file) {
        return new Response(
          JSON.stringify({ error: 'No PDF file provided' }),
          { headers: { ...corsHeaders, 'Content-Type': 'application/json' }, status: 400 }
        )
      }

      // Parse PDF and extract SPR data
      const parsedSPR = await parseSPRPDF(file)
      
      if (!parsedSPR.success || !parsedSPR.data) {
        return new Response(
          JSON.stringify({ error: parsedSPR.error || 'Failed to parse SPR PDF' }),
          { headers: { ...corsHeaders, 'Content-Type': 'application/json' }, status: 400 }
        )
      }

      const sprData = parsedSPR.data

      // Check if user with this NIK already exists
      const { data: existingUser, error: userError } = await supabase
        .from('user_profiles')
        .select('id')
        .eq('nik', sprData.nik)
        .single()

      let userId: string

      if (userError && userError.code === 'PGRST116') {
        // User doesn't exist, create new user profile
        const { data: newUser, error: createError } = await supabase
          .from('user_profiles')
          .insert({
            name: sprData.name,
            email: sprData.email || `${sprData.nik}@kprflow.com`,
            nik: sprData.nik,
            phone_number: sprData.phoneNumber || '',
            marital_status: 'Unknown',
            role: 'CUSTOMER',
            is_active: true
          })
          .select()
          .single()

        if (createError) {
          console.error('Error creating user profile:', createError)
          return new Response(
            JSON.stringify({ error: 'Failed to create user profile' }),
            { headers: { ...corsHeaders, 'Content-Type': 'application/json' }, status: 500 }
          )
        }

        userId = newUser.id
      } else if (existingUser) {
        userId = existingUser.id
      } else {
        return new Response(
          JSON.stringify({ error: 'Database error checking user existence' }),
          { headers: { ...corsHeaders, 'Content-Type': 'application/json' }, status: 500 }
        )
      }

      // Find unit by block
      const { data: unit, error: unitError } = await supabase
        .from('unit_properties')
        .select('id, status')
        .eq('block', sprData.unitBlock.toUpperCase())
        .eq('status', 'AVAILABLE')
        .single()

      let unitId: string | null = null

      if (!unitError && unit) {
        unitId = unit.id
        
        // Update unit status to BOOKED
        await supabase
          .from('unit_properties')
          .update({ status: 'BOOKED' })
          .eq('id', unitId)
      }

      // Create KPR dossier
      const { data: dossier, error: dossierError } = await supabase
        .from('kpr_dossiers')
        .insert({
          user_id: userId,
          unit_id: unitId,
          status: 'LEAD',
          booking_date: new Date().toISOString().split('T')[0],
          notes: `Auto-created from SPR form - Unit Block: ${sprData.unitBlock}`
        })
        .select()
        .single()

      if (dossierError) {
        console.error('Error creating dossier:', dossierError)
        
        // Rollback unit status if it was updated
        if (unitId) {
          await supabase
            .from('unit_properties')
            .update({ status: 'AVAILABLE' })
            .eq('id', unitId)
        }

        return new Response(
          JSON.stringify({ error: 'Failed to create KPR dossier' }),
          { headers: { ...corsHeaders, 'Content-Type': 'application/json' }, status: 500 }
        )
      }

      // Send notification to marketing team
      await sendNotificationToMarketing(supabase, dossier.id, sprData)

      return new Response(
        JSON.stringify({
          success: true,
          message: 'SPR processed successfully',
          data: {
            dossierId: dossier.id,
            userId: userId,
            unitId: unitId,
            sprData: sprData
          }
        }),
        { headers: { ...corsHeaders, 'Content-Type': 'application/json' }, status: 200 }
      )
    }

    return new Response(
      JSON.stringify({ error: 'Method not allowed' }),
      { headers: { ...corsHeaders, 'Content-Type': 'application/json' }, status: 405 }
    )
  } catch (error) {
    console.error('SPR Parser Error:', error)
    return new Response(
      JSON.stringify({ error: 'Internal server error' }),
      { headers: { ...corsHeaders, 'Content-Type': 'application/json' }, status: 500 }
    )
  }
})

async function parseSPRPDF(file: File): Promise<ParsedSPR> {
  try {
    const arrayBuffer = await file.arrayBuffer()
    const pdfDoc = await PDFDocument.load(arrayBuffer)
    const pages = pdfDoc.getPages()
    
    if (pages.length === 0) {
      return { success: false, error: 'PDF has no pages' }
    }

    // Extract text from first page (SPR forms are typically single page)
    const firstPage = pages[0]
    const text = await firstPage.getText()
    
    // Parse SPR data using regex patterns
    const sprData = extractSPRData(text)
    
    if (!sprData.nik || !sprData.name || !sprData.unitBlock) {
      return { success: false, error: 'Required fields (NIK, Name, Unit Block) not found in PDF' }
    }

    return { success: true, data: sprData }
  } catch (error) {
    console.error('PDF parsing error:', error)
    return { success: false, error: 'Failed to parse PDF file' }
  }
}

function extractSPRData(text: string): SPRData {
  const sprData: SPRData = {
    nik: '',
    name: '',
    unitBlock: '',
    email: '',
    phoneNumber: ''
  }

  // Common patterns for Indonesian SPR forms
  const patterns = {
    // NIK patterns (16 digits)
    nik: [
      /NIK\s*[:\-]?\s*(\d{16})/i,
      /No\.?\s*KTP\s*[:\-]?\s*(\d{16})/i,
      /(\d{16})/,
    ],
    // Name patterns
    name: [
      /Nama\s*[:\-]?\s*([A-Za-z\s\.]+)/i,
      /Nama\s*Lengkap\s*[:\-]?\s*([A-Za-z\s\.]+)/i,
      /Pemohon\s*[:\-]?\s*([A-Za-z\s\.]+)/i,
    ],
    // Unit block patterns
    unitBlock: [
      /Blok\s*[:\-]?\s*([A-Z0-9]+)/i,
      /Unit\s*[:\-]?\s*([A-Z0-9]+)/i,
      /Tipe\s*[:\-]?\s*([A-Z0-9]+)/i,
      /Kavling\s*[:\-]?\s*([A-Z0-9]+)/i,
    ],
    // Email patterns
    email: [
      /Email\s*[:\-]?\s*([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,})/i,
      /E-mail\s*[:\-]?\s*([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,})/i,
    ],
    // Phone patterns
    phoneNumber: [
      /No\.?\s*HP\s*[:\-]?\s*([0-9+\-\s]+)/i,
      /Telepon\s*[:\-]?\s*([0-9+\-\s]+)/i,
      /WhatsApp\s*[:\-]?\s*([0-9+\-\s]+)/i,
    ]
  }

  // Extract NIK
  for (const pattern of patterns.nik) {
    const match = text.match(pattern)
    if (match && match[1]) {
      sprData.nik = match[1].replace(/\s/g, '')
      break
    }
  }

  // Extract Name
  for (const pattern of patterns.name) {
    const match = text.match(pattern)
    if (match && match[1]) {
      sprData.name = match[1].trim()
      break
    }
  }

  // Extract Unit Block
  for (const pattern of patterns.unitBlock) {
    const match = text.match(pattern)
    if (match && match[1]) {
      sprData.unitBlock = match[1].trim()
      break
    }
  }

  // Extract Email (optional)
  for (const pattern of patterns.email) {
    const match = text.match(pattern)
    if (match && match[1]) {
      sprData.email = match[1].trim()
      break
    }
  }

  // Extract Phone Number (optional)
  for (const pattern of patterns.phoneNumber) {
    const match = text.match(pattern)
    if (match && match[1]) {
      sprData.phoneNumber = match[1].replace(/[\s\-\(\)]/g, '')
      break
    }
  }

  return sprData
}

async function sendNotificationToMarketing(
  supabase: any,
  dossierId: string,
  sprData: SPRData
): Promise<void> {
  try {
    // Get marketing users
    const { data: marketingUsers } = await supabase
      .from('user_profiles')
      .select('email')
      .eq('role', 'MARKETING')
      .eq('is_active', true)

    if (marketingUsers && marketingUsers.length > 0) {
      // Create notification records for marketing team
      const notifications = marketingUsers.map((user: any) => ({
        user_id: user.id,
        title: 'New SPR Application',
        message: `New SPR application received: ${sprData.name} - Unit ${sprData.unitBlock}`,
        type: 'new_spr',
        reference_id: dossierId,
        created_at: new Date().toISOString()
      }))

      await supabase
        .from('notifications')
        .insert(notifications)
    }

    // Log the SPR processing
    await supabase
      .from('spr_processing_log')
      .insert({
        dossier_id: dossierId,
        nik: sprData.nik,
        name: sprData.name,
        unit_block: sprData.unitBlock,
        processed_at: new Date().toISOString(),
        status: 'success'
      })
  } catch (error) {
    console.error('Error sending notification:', error)
    // Don't fail the entire process if notification fails
  }
}
