package com.veyra.core.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * Object storage için soyut kontrat.
 *
 * Implementation'lar:
 *   - {@code S3StorageService} → MinIO (dev) + Cloudflare R2 (prod) + AWS S3
 *
 * Bu interface domain-agnostic'tir — sadece dosya yükler/siler,
 * CarImage / UserAvatar gibi domain kavramlarını bilmez (DIP).
 */
public interface StorageService {

    /**
     * Dosyayı storage'a yükler.
     *
     * @param file   Yüklenecek dosya (MultipartFile)
     * @param folder Bucket içindeki hedef klasör (örn. "cars/42")
     * @return       Yüklenen dosyanın metadata'sı — sadece storageKey + content info
     * @throws com.veyra.core.exception.StorageException upload başarısız olursa
     */
    StoredFile upload(MultipartFile file, String folder);

    /**
     * Dosyayı storage'dan siler. Key yoksa sessizce geçer (idempotent).
     *
     * @param storageKey Silinecek dosyanın kalıcı kimliği
     * @throws com.veyra.core.exception.StorageException delete çağrısı başarısız olursa
     */
    void delete(String storageKey);

    /**
     * storageKey'den public URL üretir.
     *
     * Bu metot kasıtlı olarak service katmanındadır (DB'de URL tutulmaz).
     * URL format'ı değişirse — CDN domain, vendor geçişi, signed URL'e geçiş —
     * yalnızca bu implementation değişir; hiçbir DB migration gerekmez.
     *
     * @param storageKey Dosyanın kalıcı kimliği
     * @return           Tarayıcının erişebileceği tam URL
     */
    String getPublicUrl(String storageKey);
}
