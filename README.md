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

PostgreSQL şeması `infrastructure/postgres/init.sql` ile yalnızca `postgres_data` volume'u ilk kez boş oluşturulduğunda çalışır. Mevcut volume için otomatik migration yapılmaz. Customer Gallery Viewer gibi schema değişikliklerinde, backup sonrası `infrastructure/postgres/migrations/` altındaki staged upgrade SQL dosyaları operatör tarafından sırasıyla uygulanmalıdır; local geliştirme verisi açıkça sıfırlanmamalıdır.

Backend, başlarken R2 endpoint, access key, secret ve bucket değerlerini doğrular. Local `.env` içindeki dummy R2 değerleri yalnızca backend startup/configuration denemesi için kullanılabilir; upload, preview ve download çalıştırmaz. Bu akışları test etmek için untracked `.env` içinde gerçek R2 bilgileri sağlayın.

`ADMIN_EMAIL` ve `ADMIN_PASSWORD` yalnız `users` tablosu boşken ilk admin hesabını oluşturmak için kullanılır. Mevcut herhangi bir kullanıcı varken backend bu değerlerle hesap oluşturmaz veya mevcut email/password hash'ini değiştirmez.

## Production Docker Compose (M16)

Production VPS'te `infrastructure/production.env.example` dosyasını untracked `infrastructure/.env.production` olarak kopyalayın ve tüm placeholder değerleri gerçek production değerleriyle değiştirin. Bu dosya, TLS certificate/private key dosyaları ve R2 credential'ları tracked değildir.

Production stack'i VPS'te repository kökünden çalıştırın:

```text
docker compose --env-file infrastructure/.env.production -f infrastructure/docker-compose.production.yml up --build -d
```

Yalnız Nginx `80` ve `443` portlarını yayınlar. Frontend, backend ve PostgreSQL yalnız Docker ağında erişilir; PostgreSQL public internete açılmaz. Nginx `/` isteklerini frontend'e, `/api/` isteklerini backend'e yönlendirir. Frontend container mevcut SPA fallback'i ile `/admin`, `/admin/events/{id}`, `/e/{token}` ve `/gallery/{viewerToken}` deep-link isteklerini destekler.

`TLS_CERT_DIR`, VPS'te bulunan ve repository dışındaki `fullchain.pem` ile `privkey.pem` dosyalarını içeren mutlak dizini göstermelidir. HTTP'den HTTPS'e redirect hazırdır. Gerçek DNS, sertifika provision ve HTTPS smoke test M19'da yapılacaktır.

Production PostgreSQL verisi `postgres_data_production` named volume'unda kalır. `infrastructure/postgres/init.sql` yalnız volume ilk kez boş oluşturulduğunda çalışır; mevcut volume otomatik migrate edilmez. Şema değişiklikleri için sonradan açık bir migration planı gerekir. Minimum manuel backup örneği: `docker compose --env-file infrastructure/.env.production -f infrastructure/docker-compose.production.yml exec -T db sh -c 'pg_dump -U "$POSTGRES_USER" "$POSTGRES_DB"' > backup.sql`; restore işlemi boş/uygun bir veritabanına `psql` ile yapılmalıdır.

R2 bucket private kalır. R2 Dashboard'da production frontend origin'i için CORS elle eklenmelidir; presigned upload browser'dan doğrudan R2'ye gider, medya body Nginx veya backend üzerinden geçmez.

Fresh production DB/volume için ilk backend başlangıcından **önce** final `ADMIN_EMAIL` ve güçlü `ADMIN_PASSWORD` değerlerini `infrastructure/.env.production` dosyasına yazın. Bootstrap sonrası bu environment değerlerini değiştirmek mevcut admin credential'ını değiştirmez ve ikinci admin oluşturmaz. Parola değişikliği bu MVP'de ayrı, kontrollü bir operasyon gerektirir; otomatik reseed veya DB reset kullanılmaz.

## Jenkins CI (M17)

GitHub `main` push webhook'u Jenkins'teki tek Pipeline job'ını tetikler. Jenkins agent'ında Java 17, Node 22, Docker CLI, Docker Compose plugin'i ve `openssl` bulunmalıdır. Job'da non-secret `REGISTRY_IMAGE_PREFIX` (örneğin `ghcr.io/<owner>/wedding-photo-sharing`) tanımlanmalı; `ghcr-registry` Credentials Binding kaydı GHCR kullanıcı adı ve package write token'ını içermelidir.

Pipeline backend/frontend testlerini çalıştırır, Docker image'larını üretir, production Compose ile Nginx config'ini dummy değerlerle doğrular ve yalnız başarılıysa immutable full Git SHA tag'leriyle GHCR'a push eder. `latest` tag kullanılmaz. Docker socket erişimi hostta root-equivalent yetkidir; bu nedenle yalnız güvenilir `main` branch webhook build'leri çalıştırılmalı, PR/fork pipeline'ları açılmamalıdır.

M17 yalnız CI'dır: VPS SSH, image pull, `docker compose up`, restart, rollback ve deploy M18 kapsamındadır.

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
