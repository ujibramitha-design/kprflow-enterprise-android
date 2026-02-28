package com.kprflow.enterprise.security

import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import java.security.cert.X509Certificate
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

@Singleton
class CertificatePinner @Inject constructor() {
    
    private val supabaseCertificateHash = "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
    private val apiCertificateHash = "sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB="
    
    fun createSecureClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .certificatePinner(
                CertificatePinner.Builder()
                    .add("supabase.co", supabaseCertificateHash)
                    .add("api.kprflow.com", apiCertificateHash)
                    .build()
            )
            .sslSocketFactory(createCustomSSLContext().socketFactory, createCustomTrustManager())
            .build()
    }
    
    private fun createCustomSSLContext(): SSLContext {
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf(createCustomTrustManager()), null)
        return sslContext
    }
    
    private fun createCustomTrustManager(): X509TrustManager {
        return object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                // Implement custom certificate validation
            }
            
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                // Implement custom certificate validation
            }
            
            override fun getAcceptedIssuers(): Array<X509Certificate> {
                return emptyArray()
            }
        }
    }
}
