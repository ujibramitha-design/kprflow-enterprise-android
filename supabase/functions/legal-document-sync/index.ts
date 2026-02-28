import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2"

// =====================================================
// GOOGLE DRIVE API INTEGRATION
// Automated Legal Document Sync Worker
// =====================================================

interface GoogleDriveFile {
  id: string
  name: string
  mimeType: string
  size: string
  webViewLink: string
  webContentLink: string
  parents: string[]
}

interface UnitProperty {
  id: string
  block: string
  unit_number: string
  shgb_url?: string
  pbg_url?: string
  imb_url?: string
  legal_sync_status: string
}

interface SyncResult {
  success: boolean
  message: string
  processed: number
  errors: string[]
}

// Document type patterns
const DOCUMENT_PATTERNS = {
  SHGB: /^SHGB_([A-Z0-9]+)\.pdf$/i,
  PBG: /^PBG_([A-Z0-9]+)\.pdf$/i,
  IMB: /^IMB_([A-Z0-9]+)\.pdf$/i
}

// Google Drive API configuration
const GDRIVE_CONFIG = {
  folderId: Deno.env.get('GDRIVE_LEGAL_FOLDER_ID'),
  apiKey: Deno.env.get('GDRIVE_API_KEY'),
  scopes: ['https://www.googleapis.com/auth/drive.readonly']
}

// Supabase client
const supabaseUrl = Deno.env.get('SUPABASE_URL')!
const supabaseKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!
const supabase = createClient(supabaseUrl, supabaseKey)

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
    const action = url.searchParams.get('action') || 'sync'

    console.log(`Legal document sync started: ${action}`)

    let result: SyncResult

    switch (action) {
      case 'sync':
        result = await performDocumentSync()
        break
      case 'status':
        result = await getSyncStatus()
        break
      case 'force':
        result = await forceSync()
        break
      default:
        result = { success: false, message: 'Invalid action', processed: 0, errors: [] }
    }

    return new Response(JSON.stringify(result), {
      headers: { ...corsHeaders, 'Content-Type': 'application/json' },
      status: result.success ? 200 : 400
    })

  } catch (error) {
    console.error('Legal document sync error:', error)
    
    return new Response(JSON.stringify({
      success: false,
      message: error.message,
      processed: 0,
      errors: [error.message]
    }), {
      headers: { ...corsHeaders, 'Content-Type': 'application/json' },
      status: 500
    })
  }
})

// =====================================================
// CORE SYNC LOGIC
// =====================================================

async function performDocumentSync(): Promise<SyncResult> {
  const errors: string[] = []
  let processed = 0

  try {
    // 1. Get all units from database
    const { data: units, error: unitsError } = await supabase
      .from('UnitProperty')
      .select('id, block, unit_number, shgb_url, pbg_url, imb_url, legal_sync_status')

    if (unitsError) {
      throw new Error(`Failed to fetch units: ${unitsError.message}`)
    }

    console.log(`Found ${units.length} units to process`)

    // 2. Scan Google Drive for legal documents
    const driveFiles = await scanGoogleDrive()
    console.log(`Found ${driveFiles.length} files in Google Drive`)

    // 3. Process each file and match with units
    for (const file of driveFiles) {
      try {
        const matchResult = matchFileWithUnit(file, units)
        
        if (matchResult) {
          await processDocumentFile(file, matchResult.unit, matchResult.documentType)
          processed++
          console.log(`Processed ${file.name} for unit ${matchResult.unit.block}`)
        } else {
          errors.push(`No matching unit found for file: ${file.name}`)
        }
      } catch (fileError) {
        errors.push(`Error processing ${file.name}: ${fileError.message}`)
      }
    }

    // 4. Update sync status for all units
    await updateSyncStatus(units)

    return {
      success: true,
      message: `Sync completed. Processed ${processed} files.`,
      processed,
      errors
    }

  } catch (error) {
    return {
      success: false,
      message: `Sync failed: ${error.message}`,
      processed,
      errors: [error.message]
    }
  }
}

// =====================================================
// GOOGLE DRIVE SCANNING
// =====================================================

async function scanGoogleDrive(): Promise<GoogleDriveFile[]> {
  if (!GDRIVE_CONFIG.folderId || !GDRIVE_CONFIG.apiKey) {
    throw new Error('Google Drive configuration missing')
  }

  const query = `'${GDRIVE_CONFIG.folderId}' in parents and trashed=false`
  const url = `https://www.googleapis.com/drive/v3/files?q=${encodeURIComponent(query)}&key=${GDRIVE_CONFIG.apiKey}`

  const response = await fetch(url)
  if (!response.ok) {
    throw new Error(`Google Drive API error: ${response.statusText}`)
  }

  const data = await response.json()
  return data.files || []
}

// =====================================================
// PATTERN MATCHING
// =====================================================

function matchFileWithUnit(file: GoogleDriveFile, units: UnitProperty[]): {
  unit: UnitProperty
  documentType: 'SHGB' | 'PBG' | 'IMB'
} | null {
  const fileName = file.name.toUpperCase()

  // Try each document type pattern
  for (const [docType, pattern] of Object.entries(DOCUMENT_PATTERNS)) {
    const match = fileName.match(pattern)
    if (match) {
      const blockIdentifier = match[1]
      
      // Find unit by block
      const unit = units.find(u => 
        u.block.toUpperCase() === blockIdentifier.toUpperCase()
      )

      if (unit) {
        return {
          unit,
          documentType: docType as 'SHGB' | 'PBG' | 'IMB'
        }
      }
    }
  }

  return null
}

// =====================================================
// DOCUMENT PROCESSING
// =====================================================

async function processDocumentFile(
  file: GoogleDriveFile, 
  unit: UnitProperty, 
  documentType: 'SHGB' | 'PBG' | 'IMB'
): Promise<void> {
  try {
    // 1. Download file from Google Drive
    const fileBuffer = await downloadFromGoogleDrive(file)
    
    // 2. Upload to Supabase Storage
    const supabaseUrl = await uploadToSupabase(file, fileBuffer, documentType)
    
    // 3. Update unit property
    await updateUnitDocument(unit.id, documentType, supabaseUrl, file)
    
    // 4. Log sync activity
    await logSyncActivity(unit.id, documentType, file, supabaseUrl)
    
    console.log(`Successfully processed ${documentType} for unit ${unit.block}`)

  } catch (error) {
    console.error(`Failed to process ${documentType} for unit ${unit.block}:`, error)
    throw error
  }
}

// =====================================================
// GOOGLE DRIVE DOWNLOAD
// =====================================================

async function downloadFromGoogleDrive(file: GoogleDriveFile): Promise<Uint8Array> {
  const downloadUrl = `https://www.googleapis.com/drive/v3/files/${file.id}?alt=media&key=${GDRIVE_CONFIG.apiKey}`
  
  const response = await fetch(downloadUrl)
  if (!response.ok) {
    throw new Error(`Failed to download ${file.name}: ${response.statusText}`)
  }

  return new Uint8Array(await response.arrayBuffer())
}

// =====================================================
// SUPABASE STORAGE UPLOAD
// =====================================================

async function uploadToSupabase(
  file: GoogleDriveFile, 
  fileBuffer: Uint8Array, 
  documentType: string
): Promise<string> {
  const bucketName = 'legal_documents'
  const filePath = `${documentType}/${file.name}`
  
  const { data, error } = await supabase.storage
    .from(bucketName)
    .upload(filePath, fileBuffer, {
      contentType: 'application/pdf',
      upsert: true
    })

  if (error) {
    throw new Error(`Failed to upload to Supabase: ${error.message}`)
  }

  // Get public URL
  const { data: { publicUrl } } = supabase.storage
    .from(bucketName)
    .getPublicUrl(filePath)

  return publicUrl
}

// =====================================================
// DATABASE UPDATES
// =====================================================

async function updateUnitDocument(
  unitId: string, 
  documentType: 'SHGB' | 'PBG' | 'IMB', 
  supabaseUrl: string, 
  file: GoogleDriveFile
): Promise<void> {
  const updateData: any = {
    legal_sync_at: new Date().toISOString(),
    legal_sync_status: 'SYNCED',
    legal_sync_error: null
  }

  // Set appropriate URL field
  updateData[`${documentType.toLowerCase()}_url`] = supabaseUrl

  const { error } = await supabase
    .from('UnitProperty')
    .update(updateData)
    .eq('id', unitId)

  if (error) {
    throw new Error(`Failed to update unit: ${error.message}`)
  }
}

async function updateSyncStatus(units: UnitProperty[]): Promise<void> {
  for (const unit of units) {
    const hasDocuments = unit.shgb_url || unit.pbg_url || unit.imb_url
    const status = hasDocuments ? 'SYNCED' : 'PENDING'

    await supabase
      .from('UnitProperty')
      .update({ 
        legal_sync_status: status,
        legal_sync_at: new Date().toISOString()
      })
      .eq('id', unit.id)
  }
}

async function logSyncActivity(
  unitId: string, 
  documentType: string, 
  file: GoogleDriveFile, 
  supabaseUrl: string
): Promise<void> {
  const { error } = await supabase
    .from('LegalDocumentSync')
    .insert({
      unit_id: unitId,
      document_type: documentType,
      file_name: file.name,
      gdrive_file_id: file.id,
      gdrive_url: file.webViewLink,
      supabase_url: supabaseUrl,
      sync_status: 'SYNCED',
      file_size: parseInt(file.size),
      mime_type: file.mimeType,
      synced_at: new Date().toISOString()
    })

  if (error) {
    console.error(`Failed to log sync activity: ${error.message}`)
  }
}

// =====================================================
// UTILITY FUNCTIONS
// =====================================================

async function getSyncStatus(): Promise<SyncResult> {
  try {
    const { data, error } = await supabase
      .from('v_legal_dashboard_summary')
      .select('*')
      .single()

    if (error) {
      throw new Error(`Failed to get sync status: ${error.message}`)
    }

    return {
      success: true,
      message: `Sync status retrieved. Ready: ${data.ready_units}, Partial: ${data.partial_units}, Incomplete: ${data.incomplete_units}`,
      processed: data.ready_units + data.partial_units,
      errors: []
    }

  } catch (error) {
    return {
      success: false,
      message: `Failed to get sync status: ${error.message}`,
      processed: 0,
      errors: [error.message]
    }
  }
}

async function forceSync(): Promise<SyncResult> {
  // Clear existing sync status
  await supabase
    .from('UnitProperty')
    .update({ 
      legal_sync_status: 'PENDING',
      legal_sync_at: null,
      legal_sync_error: null
    })

  // Perform full sync
  return await performDocumentSync()
}

// =====================================================
// CORS HEADERS
// =====================================================

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
  'Access-Control-Allow-Methods': 'GET, POST, PUT, DELETE, OPTIONS'
}
