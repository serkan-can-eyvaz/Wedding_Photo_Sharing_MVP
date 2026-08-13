# Wedding Photo Sharing MVP - Çalışma Kuralları

`Wedding_Photo_Sharing_MVP_Architecture_Roadmap_v2.pdf` bu repository için ana mimari ve yol haritası kaynağıdır.

- M0-M19 kilometre taşlarını sırayla uygula; aktif kilometre taşının dışındaki özellikleri ekleme.
- Bu ticari MVP için basit, sürdürülebilir bir modular monolith yaklaşımını koru.
- Backend Java 17 + Spring Boot, frontend React + Vite, veritabanı PostgreSQL ve medya depolama Cloudflare R2/S3-compatible olacaktır.
- Misafir deneyimini mobile-first tasarla; öncelikli tarayıcılar iPhone Safari ve Android Chrome'dur.
- Misafirler hesap oluşturmadan, presigned URL ile medyayı doğrudan object storage'a yüklemelidir.
- Production dağıtımı tek VPS üzerinde Docker Compose ve Nginx ile; CI/CD GitHub -> Jenkins -> registry -> VPS akışıyla yapılacaktır.
- Microservice, Kubernetes, Redis, Kafka, RabbitMQ, Terraform, ArgoCD, blue/green veya canary deployment ekleme.
- Secret değerleri repository'ye yazma; environment variable ve güvenli credential store kullan.

Her implementasyondan önce repository durumunu ve aktif kilometre taşını kontrol et. Ardından yalnızca ilgili kapsamı uygula, uygun doğrulamaları çalıştır ve sonucu açıkça raporla.
