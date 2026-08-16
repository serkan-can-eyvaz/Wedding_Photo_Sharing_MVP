# Wedding Photo Sharing MVP

QR kod aracılığıyla etkinlik misafirlerinden fotoğraf ve video toplayan, mobil öncelikli web MVP'si.

## Durum

Tamamlanan kilometre taşları: **M0-M16**. Yerel Docker Compose stack'i PostgreSQL, Spring Boot backend ve React frontend içerir. Cloudflare R2 dış servistir. M16, tek VPS için HTTPS-ready production Compose topolojisini ekler; gerçek domain/TLS launch M19 kapsamındadır.

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

1. Repository kökünde untracked `.env` dosyasını oluşturun ve gerekli local değerleri girin.
2. Local admin şifresini, JWT secret'ını ve R2 ayarlarını güvenli tutun; gerçek credential'ları commit etmeyin.
3. Stack'i repository kökünden başlatın: `docker compose --env-file .env -f infrastructure/docker-compose.yml up --build`

Varsayılan adresler frontend için `http://localhost:5173`, backend API için `http://localhost:8080` ve PostgreSQL debug bağlantısı için `localhost:5432`'dir. Frontend portu değişirse `CORS_ALLOWED_ORIGIN` ve `APP_PUBLIC_BASE_URL`; backend portu değişirse `VITE_API_BASE_URL` birlikte güncellenmelidir.

PostgreSQL şeması `infrastructure/postgres/init.sql` ile yalnızca `postgres_data` volume'u ilk kez boş oluşturulduğunda çalışır. Mevcut volume için otomatik migration yapılmaz; ileride schema değişirse migration gerekir veya local geliştirme verisi açıkça sıfırlanmalıdır.

Backend, başlarken R2 endpoint, access key, secret ve bucket değerlerini doğrular. Local `.env` içindeki dummy R2 değerleri yalnızca backend startup/configuration denemesi için kullanılabilir; upload, preview ve download çalıştırmaz. Bu akışları test etmek için untracked `.env` içinde gerçek R2 bilgileri sağlayın.

## Production Docker Compose (M16)

Production VPS'te `infrastructure/production.env.example` dosyasını untracked `infrastructure/.env.production` olarak kopyalayın ve tüm placeholder değerleri gerçek production değerleriyle değiştirin. Bu dosya, TLS certificate/private key dosyaları ve R2 credential'ları tracked değildir.

Production stack'i VPS'te repository kökünden çalıştırın:

```text
docker compose --env-file infrastructure/.env.production -f infrastructure/docker-compose.production.yml up --build -d
```

Yalnız Nginx `80` ve `443` portlarını yayınlar. Frontend, backend ve PostgreSQL yalnız Docker ağında erişilir; PostgreSQL public internete açılmaz. Nginx `/` isteklerini frontend'e, `/api/` isteklerini backend'e yönlendirir. Frontend container mevcut SPA fallback'i ile `/admin`, `/admin/events/{id}` ve `/e/{token}` deep-link isteklerini destekler.

`TLS_CERT_DIR`, VPS'te bulunan ve repository dışındaki `fullchain.pem` ile `privkey.pem` dosyalarını içeren mutlak dizini göstermelidir. HTTP'den HTTPS'e redirect hazırdır. Gerçek DNS, sertifika provision ve HTTPS smoke test M19'da yapılacaktır.

Production PostgreSQL verisi `postgres_data_production` named volume'unda kalır. `infrastructure/postgres/init.sql` yalnız volume ilk kez boş oluşturulduğunda çalışır; mevcut volume otomatik migrate edilmez. Şema değişiklikleri için sonradan açık bir migration planı gerekir. Minimum manuel backup örneği: `docker compose --env-file infrastructure/.env.production -f infrastructure/docker-compose.production.yml exec -T db sh -c 'pg_dump -U "$POSTGRES_USER" "$POSTGRES_DB"' > backup.sql`; restore işlemi boş/uygun bir veritabanına `psql` ile yapılmalıdır.

R2 bucket private kalır. R2 Dashboard'da production frontend origin'i için CORS elle eklenmelidir; presigned upload browser'dan doğrudan R2'ye gider, medya body Nginx veya backend üzerinden geçmez.

## Repository Yapısı

```text
backend/              # Spring Boot uygulaması ve Docker image
frontend/             # React + Vite uygulaması ve static Docker image
infrastructure/
  docker-compose.yml  # M14 local stack
  docker-compose.production.yml # M16 production stack
  postgres/           # PostgreSQL init SQL
  nginx/              # M16 Nginx production proxy yapılandırması
```

Local ve production environment dosyaları untracked tutulur; gerçek secret değerlerini repository'ye eklemeyin.
