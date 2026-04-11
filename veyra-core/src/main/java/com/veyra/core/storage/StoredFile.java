package com.veyra.core.storage;

/**
 * Storage'a yüklenen dosyanın kalıcı metadata'sı.
 *
 * Önemli: URL burada TUTULMAZ.
 * Public URL format'ı (CDN domain, signed URL, vendor değişimi) değişebilir,
 * bu yüzden DB'ye kaydedilecek olan sadece {@code storageKey}'dir.
 * URL gerektiğinde {@link StorageService#getPublicUrl(String)} ile türetilir.
 *
 * @param storageKey  S3 object key (bucket içindeki yol) — kalıcı kimlik
 * @param contentType MIME tipi (image/jpeg, image/png, vb.)
 * @param sizeBytes   Dosya boyutu — byte
 */
public record StoredFile(
        String storageKey,
        String contentType,
        long sizeBytes
) {
}
