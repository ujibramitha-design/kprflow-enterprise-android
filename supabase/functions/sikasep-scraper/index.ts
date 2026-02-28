import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2"

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
}

interface SiKasepRequest {
  userId: string
  nik: string
  monthlyIncome?: number
  isFirstHome?: boolean
}

interface SiKasepResponse {
  success: boolean
  status: 'ELIGIBLE' | 'NOT_ELIGIBLE' | 'ERROR' | 'DATA_NOT_FOUND'
  idSikasep?: string
  rejectionReason?: string
  processingTime?: number
  screenshotUrl?: string
}

serve(async (req) => {
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders })
  }

  try {
    const supabaseUrl = Deno.env.get('SUPABASE_URL')!
    const supabaseKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!
    const sikasepUsername = Deno.env.get('SIKASEP_USERNAME')!
    const sikasepPassword = Deno.env.get('SIKASEP_PASSWORD')!

    const supabase = createClient(supabaseUrl, supabaseKey)

    if (req.method === 'POST') {
      const { action, data }: { action: string; data: any } = await req.json()

      switch (action) {
        case 'check_eligibility':
          const result = await checkSiKasepEligibility(
            data as SiKasepRequest,
            supabase,
            sikasepUsername,
            sikasepPassword
          )
          return new Response(
            JSON.stringify(result),
            { headers: { ...corsHeaders, 'Content-Type': 'application/json' }, status: 200 }
          )

        case 'bulk_check':
          const bulkResult = await bulkCheckSiKasepEligibility(
            data.userIds as string[],
            supabase,
            sikasepUsername,
            sikasepPassword
          )
          return new Response(
            JSON.stringify(bulkResult),
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
    console.error('Error in sikasep-scraper:', error)
    return new Response(
      JSON.stringify({ error: error.message || 'Internal server error' }),
      { headers: { ...corsHeaders, 'Content-Type': 'application/json' }, status: 500 }
    )
  }
})

async function checkSiKasepEligibility(
  request: SiKasepRequest,
  supabase: any,
  username: string,
  password: string
): Promise<SiKasepResponse> {
  const startTime = Date.now()

  try {
    // Validate NIK format (16 digits)
    if (!request.nik || !/^\d{16}$/.test(request.nik)) {
      return {
        success: false,
        status: 'ERROR',
        rejectionReason: 'Invalid NIK format'
      }
    }

    // Basic eligibility check first
    const basicEligibility = checkBasicEligibility(request.monthlyIncome, request.isFirstHome)
    if (!basicEligibility.eligible) {
      return {
        success: true,
        status: 'NOT_ELIGIBLE',
        rejectionReason: basicEligibility.reason
      }
    }

    // Simulate SiKasep API check (in real implementation, this would use Puppeteer/Playwright)
    const sikasepResult = await simulateSiKasepCheck(request.nik, username, password)
    
    const processingTime = Date.now() - startTime

    // Log the result
    await logSiKasepCheck(request.userId, request.nik, sikasepResult, processingTime, supabase)

    // Update user profile
    await updateUserSiKasepStatus(request.userId, sikasepResult, supabase)

    return {
      success: true,
      status: sikasepResult.status,
      idSikasep: sikasepResult.idSikasep,
      rejectionReason: sikasepResult.rejectionReason,
      processingTime
    }

  } catch (error: any) {
    console.error('Error checking SiKasep eligibility:', error)
    return {
      success: false,
      status: 'ERROR',
      rejectionReason: error.message || 'Failed to check eligibility'
    }
  }
}

async function bulkCheckSiKasepEligibility(
  userIds: string[],
  supabase: any,
  username: string,
  password: string
) {
  const results = []
  
  for (const userId of userIds) {
    try {
      // Get user profile
      const { data: user, error } = await supabase
        .from('user_profiles')
        .select('nik, monthly_income, is_first_home')
        .eq('id', userId)
        .single()
      
      if (error || !user) {
        results.push({
          userId,
          success: false,
          error: 'User not found'
        })
        continue
      }

      const result = await checkSiKasepEligibility(
        {
          userId,
          nik: user.nik,
          monthlyIncome: user.monthly_income,
          isFirstHome: user.is_first_home
        },
        supabase,
        username,
        password
      )
      
      results.push({
        userId,
        ...result
      })
      
      // Add delay to avoid rate limiting
      await new Promise(resolve => setTimeout(resolve, 2000))
      
    } catch (error: any) {
      results.push({
        userId,
        success: false,
        error: error.message
      })
    }
  }
  
  return {
    success: true,
    total: userIds.length,
    results
  }
}

function checkBasicEligibility(monthlyIncome?: number, isFirstHome?: boolean) {
  if (monthlyIncome && monthlyIncome > 8000000) {
    return {
      eligible: false,
      reason: 'Monthly income exceeds FLPP limit (Rp 8.000.000)'
    }
  }
  
  if (isFirstHome === false) {
    return {
      eligible: false,
      reason: 'Not first home purchase'
    }
  }
  
  return {
    eligible: true
  }
}

async function simulateSiKasepCheck(
  nik: string,
  username: string,
  password: string
): Promise<{ status: string; idSikasep?: string; rejectionReason?: string }> {
  // Simulate API delay
  await new Promise(resolve => setTimeout(resolve, 1000 + Math.random() * 2000))
  
  // Simulate different responses based on NIK patterns (for demo purposes)
  if (nik.endsWith('0001') || nik.endsWith('0002')) {
    return {
      status: 'ELIGIBLE',
      idSikasep: `FLPP-${Date.now()}-${nik.slice(-4)}`
    }
  } else if (nik.endsWith('0003') || nik.endsWith('0004')) {
    return {
      status: 'NOT_ELIGIBLE',
      rejectionReason: 'Monthly income exceeds FLPP limit'
    }
  } else if (nik.endsWith('0005')) {
    return {
      status: 'NOT_ELIGIBLE',
      rejectionReason: 'Not first home purchase'
    }
  } else {
    // Random eligibility for other NIKs
    const isEligible = Math.random() > 0.3
    return {
      status: isEligible ? 'ELIGIBLE' : 'NOT_ELIGIBLE',
      idSikasep: isEligible ? `FLPP-${Date.now()}-${nik.slice(-4)}` : undefined,
      rejectionReason: isEligible ? undefined : 'Does not meet FLPP criteria'
    }
  }
}

async function logSiKasepCheck(
  userId: string,
  nik: string,
  result: any,
  processingTime: number,
  supabase: any
) {
  try {
    await supabase
      .from('sikasep_logs')
      .insert({
        user_id: userId,
        nik: nik,
        status: result.status,
        id_sikasep: result.idSikasep,
        rejection_reason: result.rejectionReason,
        raw_response: {
          checked_at: new Date().toISOString(),
          processing_time: processingTime,
          ...result
        },
        processing_time_ms: processingTime
      })
  } catch (error: any) {
    console.error('Error logging SiKasep check:', error)
  }
}

async function updateUserSiKasepStatus(
  userId: string,
  result: any,
  supabase: any
) {
  try {
    await supabase
      .from('user_profiles')
      .update({
        status_sikasep: result.status,
        id_sikasep: result.idSikasep,
        sikasep_checked_at: new Date().toISOString(),
        sikasep_rejection_reason: result.rejectionReason,
        updated_at: new Date().toISOString()
      })
      .eq('id', userId)
  } catch (error: any) {
    console.error('Error updating user SiKasep status:', error)
  }
}
