# Wedding Photo Sharing MVP

QR kod aracılığıyla etkinlik misafirlerinden fotoğraf ve video toplayan, mobil öncelikli web MVP'si.

## Durum

Aktif tamamlanan kilometre taşı: **M0 - Repository Bootstrap**.

Bu aşamada yalnızca repository iskeleti, çalışma kuralları, örnek environment değişkenleri ve Git dışlama kuralları oluşturulmuştur. Uygulama kodu, Docker Compose, Jenkins pipeline'ı ve framework bootstrap'ı henüz eklenmemiştir.

## Hedef Mimari

- Backend: Java 21 + Spring Boot modular monolith
- Frontend: React + Vite, mobile-first
- Veritabanı: PostgreSQL
- Medya depolama: Cloudflare R2 (S3-compatible)
- Dağıtım: Tek VPS üzerinde Docker Compose + Nginx
- CI/CD: GitHub -> Jenkins -> Container Registry -> VPS

Mimari kararlar ve M0-M19 yol haritası için [architecture document](Wedding_Photo_Sharing_MVP_Architecture_Roadmap_v2.pdf) bağlayıcı kaynaktır.

## Repository Yapısı

```text
backend/              # M1'de Spring Boot uygulaması eklenecek
frontend/             # M9'da React + Vite uygulaması eklenecek
infrastructure/
  nginx/              # M16'da Nginx yapılandırması eklenecek
```

Yerel değerler için `.env.example` dosyasını `.env` olarak kopyalayın; gerçek secret değerlerini repository'ye eklemeyin.
