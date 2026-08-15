# Wedding Photo Sharing MVP

QR kod aracılığıyla etkinlik misafirlerinden fotoğraf ve video toplayan, mobil öncelikli web MVP'si.

## Durum

Tamamlanan kilometre taşları: **M0-M14**. Yerel Docker Compose stack'i PostgreSQL, Spring Boot backend ve React frontend içerir. Cloudflare R2 dış servistir.

## Hedef Mimari

- Backend: Java 17 + Spring Boot modular monolith
- Frontend: React + Vite, mobile-first
- Veritabanı: PostgreSQL
- Medya depolama: Cloudflare R2 (S3-compatible)
- Dağıtım: Tek VPS üzerinde Docker Compose + Nginx
- CI/CD: GitHub -> Jenkins -> Container Registry -> VPS

Mimari kararlar ve M0-M19 yol haritası için [architecture document](Wedding_Photo_Sharing_MVP_Architecture_Roadmap_v2.pdf) bağlayıcı kaynaktır.

## Yerel Docker Compose (M14)

Docker Desktop ve Docker Compose plugin'i gerekir.

1. Örnek environment dosyasını kopyalayın: `Copy-Item .env.example .env`
2. `.env` içindeki local admin şifresini, JWT secret'ını ve R2 ayarlarını değiştirin. Gerçek credential'ları commit etmeyin.
3. Stack'i repository kökünden başlatın: `docker compose --env-file .env -f infrastructure/docker-compose.yml up --build`

Varsayılan adresler frontend için `http://localhost:5173`, backend API için `http://localhost:8080` ve PostgreSQL debug bağlantısı için `localhost:5432`'dir. Frontend portu değişirse `CORS_ALLOWED_ORIGIN` ve `APP_PUBLIC_BASE_URL`; backend portu değişirse `VITE_API_BASE_URL` birlikte güncellenmelidir.

PostgreSQL şeması `infrastructure/postgres/init.sql` ile yalnızca `postgres_data` volume'u ilk kez boş oluşturulduğunda çalışır. Mevcut volume için otomatik migration yapılmaz; ileride schema değişirse migration gerekir veya local geliştirme verisi açıkça sıfırlanmalıdır.

Backend, başlarken R2 endpoint, access key, secret ve bucket değerlerini doğrular. `.env.example` içindeki R2 değerleri yalnızca backend startup/configuration denemesi için dummy değerlerdir; upload, preview ve download çalıştırmaz. Bu akışları test etmek için untracked `.env` içinde gerçek R2 bilgileri sağlayın.

## Repository Yapısı

```text
backend/              # Spring Boot uygulaması ve Docker image
frontend/             # React + Vite uygulaması ve static Docker image
infrastructure/
  docker-compose.yml  # M14 local stack
  postgres/           # M14 PostgreSQL init SQL
  nginx/              # M16 production proxy yapılandırması
```

Yerel değerler için `.env.example` dosyasını `.env` olarak kopyalayın; gerçek secret değerlerini repository'ye eklemeyin.
