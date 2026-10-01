package org.rhythmeta.chunithmd.account

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import org.rhythmeta.chunithmd.shared.account.RhythmetaSecretStore
import org.rhythmeta.chunithmd.shared.account.StoredSecret

class AndroidSecretStore(context: Context) : RhythmetaSecretStore {
    private val preferences = context.getSharedPreferences("rhythmeta_session", Context.MODE_PRIVATE)
    private fun secretKey(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey("chunithmd.rhythmeta", null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder("chunithmd.rhythmeta", KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    override fun read(key: String): StoredSecret = StoredSecret(preferences.getString(key, null)?.let { value ->
        runCatching {
            val parts = value.split(':', limit=2)
            Cipher.getInstance("AES/GCM/NoPadding").run {
                init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, Base64.decode(parts[0], Base64.NO_WRAP)))
                doFinal(Base64.decode(parts[1], Base64.NO_WRAP)).decodeToString()
            }
        }.getOrNull()
    })
    override fun write(key: String, value: String?) {
        val editor = preferences.edit()
        if (value == null) editor.remove(key)
        else {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, secretKey()) }
            val encrypted = cipher.doFinal(value.encodeToByteArray())
            editor.putString(key, Base64.encodeToString(cipher.iv, Base64.NO_WRAP)+":"+Base64.encodeToString(encrypted, Base64.NO_WRAP))
        }
        check(editor.commit()) { "Could not save account credentials." }
    }
}
