package com.example.tvvoiceprobe;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Encrypts Home Assistant credentials with a device-local, non-exportable Android Keystore key. */
public final class CredentialStore {
    private static final String ALIAS = "tv_voice_assist_credentials_v1";
    private static final String PREFS = "tv_voice_local_settings";
    private static final String SECRET = "encrypted_connection";
    private final Context context;

    public static final class Connection {
        public final String url;
        public final String token;
        Connection(String url, String token) { this.url = url; this.token = token; }
    }

    public CredentialStore(Context context) { this.context = context.getApplicationContext(); }

    private SharedPreferences prefs() {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private SecretKey key() throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        SecretKey existing = (SecretKey) store.getKey(ALIAS, null);
        if (existing != null) return existing;
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(
                ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256).build());
        return generator.generateKey();
    }

    public void save(String url, String token) throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("url", url);
        payload.put("token", token);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key());
        byte[] iv = cipher.getIV();
        byte[] encrypted = cipher.doFinal(payload.toString().getBytes(StandardCharsets.UTF_8));
        JSONObject envelope = new JSONObject();
        envelope.put("iv", Base64.encodeToString(iv, Base64.NO_WRAP));
        envelope.put("ciphertext", Base64.encodeToString(encrypted, Base64.NO_WRAP));
        if (!prefs().edit().putString(SECRET, envelope.toString()).commit()) {
            throw new IllegalStateException("Could not save settings on the TV");
        }
    }

    public Connection load() throws Exception {
        String stored = prefs().getString(SECRET, null);
        if (stored == null) return null;
        JSONObject envelope = new JSONObject(stored);
        byte[] iv = Base64.decode(envelope.getString("iv"), Base64.NO_WRAP);
        byte[] encrypted = Base64.decode(envelope.getString("ciphertext"), Base64.NO_WRAP);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, iv));
        JSONObject value = new JSONObject(new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8));
        return new Connection(value.getString("url"), value.getString("token"));
    }

    public void clear() {
        prefs().edit().remove(SECRET).apply();
        try {
            KeyStore store = KeyStore.getInstance("AndroidKeyStore");
            store.load(null);
            store.deleteEntry(ALIAS);
        } catch (Exception ignored) { }
    }
}
