package com.webtoapp.core.apkbuilder

import com.google.common.truth.Truth.assertThat
import com.webtoapp.data.model.CustomCaCertificate
import com.webtoapp.data.model.NetworkTrustConfig
import org.junit.Test

class NetworkSecurityConfigBuilderTest {

    @Test
    fun `default config trusts system and user anchors`() {
        val xml = NetworkSecurityConfigBuilder.build(NetworkTrustConfig())

        assertThat(xml).contains("""<certificates src="system" />""")
        assertThat(xml).contains("""<certificates src="user" />""")
        assertThat(xml).contains("cleartextTrafficPermitted=\"true\"")
    }

    @Test
    fun `disabling user ca removes user anchor`() {
        val xml = NetworkSecurityConfigBuilder.build(
            NetworkTrustConfig(trustUserCa = false)
        )

        assertThat(xml).contains("""<certificates src="system" />""")
        assertThat(xml).doesNotContain("""<certificates src="user" />""")
    }

    @Test
    fun `custom ca certificates map to stable raw resources`() {
        val xml = NetworkSecurityConfigBuilder.build(
            NetworkTrustConfig(
                customCaCertificates = listOf(
                    CustomCaCertificate(
                        id = "one",
                        displayName = "Dev CA",
                        filePath = "/tmp/dev-ca.pem",
                        sha256 = "abc"
                    )
                )
            )
        )

        assertThat(xml).contains("""<certificates src="@raw/wta_custom_ca_1" />""")
    }
}
