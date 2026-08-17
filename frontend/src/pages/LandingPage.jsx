import { Link } from 'react-router-dom';

const BRAND_NAME = 'Marka Adı';

const steps = [
  {
    number: '01',
    title: 'Etkinliğini oluştur',
    description: 'Etkinliğiniz için özel galeriyi birkaç adımda hazırlayın.',
    icon: 'spark',
  },
  {
    number: '02',
    title: 'QR kodunu paylaş',
    description: 'Misafirlerinize tek bir bağlantı ve QR kod sunun.',
    icon: 'qr',
  },
  {
    number: '03',
    title: 'Anılar yüklensin',
    description: 'Fotoğraf ve videolar hesap gerektirmeden doğrudan yüklensin.',
    icon: 'upload',
  },
  {
    number: '04',
    title: 'Galeriden indir',
    description: 'Tüm anıları seçin, görüntüleyin ve istediğiniz gibi indirin.',
    icon: 'gallery',
  },
];

const featureStories = [
  {
    number: '01',
    label: 'Paylaş',
    title: 'Misafir için sıfır sürtünme.',
    capabilities: ['QR ile hızlı erişim', 'Üyeliksiz misafir yükleme', 'Mobil uyumlu deneyim'],
  },
  {
    number: '02',
    label: 'Yükle',
    title: 'Anlar kesintisiz toplansın.',
    capabilities: ['Fotoğraf ve video desteği', 'Çoklu dosya yükleme', 'Gerçek zamanlı yükleme ilerlemesi', 'Başarısız yüklemelerde tekrar deneme'],
  },
  {
    number: '03',
    label: 'Sakla',
    title: 'Her şey tek galeride, sizin kontrolünüzde.',
    capabilities: ['Özel etkinlik galerisi', 'Tekli dosya indirme', 'Seçilenleri ZIP olarak indirme', 'Tüm galeriyi ZIP olarak indirme', 'Güvenli bulut depolama'],
  },
];

const pricingPackages = [
  { number: '01', duration: '30 GÜN', price: '₺499,99', retention: '30 gün saklama', detail: 'Tek etkinlik için.' },
  { number: '02', duration: '60 GÜN', price: '₺599,99', retention: '60 gün saklama', detail: 'Anılarınız için daha fazla zaman.', recommended: true },
  { number: '03', duration: '90 GÜN', price: '₺699,99', retention: '90 gün saklama', detail: 'Galerinizi daha uzun süre saklayın.' },
];

function BrandMark() {
  return (
    <span className="landing-brand-mark" aria-hidden="true">
      <svg viewBox="0 0 24 24" focusable="false">
        <path d="M12 4.5c3.7 0 6.7 2.8 6.7 6.3 0 4.8-6.7 8.7-6.7 8.7s-6.7-3.9-6.7-8.7C5.3 7.3 8.3 4.5 12 4.5Z" />
        <path d="M9.3 10.9 11.1 13l3.7-4.1" />
      </svg>
    </span>
  );
}

function StepIcon({ name }) {
  const paths = {
    spark: <><path d="m12 3 .8 4.2L17 8l-4.2.8L12 13l-.8-4.2L7 8l4.2-.8L12 3Z" /><path d="m18.5 14 .4 2.1L21 16.5l-2.1.4-.4 2.1-.4-2.1-2.1-.4 2.1-.4.4-2.1Z" /></>,
    qr: <><path d="M4 4h6v6H4zM14 4h6v6h-6zM4 14h6v6H4z" /><path d="M16 14h2v2h2v4h-4v-2h-2v-2h2z" /></>,
    upload: <><path d="M12 15V4m0 0L8.5 7.5M12 4l3.5 3.5" /><path d="M5 13v6h14v-6" /></>,
    gallery: <><rect x="4" y="5" width="16" height="14" rx="2" /><path d="m6.5 16 3.7-3.8 2.5 2.4 2.1-2.1 2.8 3.5M8.5 9.3h.1" /></>,
  };

  return <svg className="landing-step-icon" viewBox="0 0 24 24" aria-hidden="true" focusable="false">{paths[name]}</svg>;
}

function ArrowIcon() {
  return <svg viewBox="0 0 20 20" aria-hidden="true" focusable="false"><path d="M3.5 10h12m-4.2-4.2L15.5 10l-4.2 4.2" /></svg>;
}

function QrPattern() {
  return (
    <div className="landing-qr-pattern" aria-hidden="true">
      <span className="landing-qr-corner landing-qr-corner-one" />
      <span className="landing-qr-corner landing-qr-corner-two" />
      <span className="landing-qr-corner landing-qr-corner-three" />
      <i /><i /><i /><i /><i /><i /><i /><i /><i />
    </div>
  );
}

export default function LandingPage() {
  const currentYear = new Date().getFullYear();

  return (
    <div className="landing-page">
      <header className="landing-header">
        <nav className="landing-nav" aria-label="Ana navigasyon">
          <Link to="/" className="landing-brand" aria-label={`${BRAND_NAME} ana sayfa`}>
            <BrandMark />
            <span>{BRAND_NAME}</span>
          </Link>
          <div className="landing-nav-links">
            <a href="#nasil-calisir">Nasıl Çalışır?</a>
            <a href="#ozellikler">Özellikler</a>
            <a href="#paketler">Paketler</a>
            <Link className="landing-login-link" to="/admin/login">Giriş Yap</Link>
          </div>
        </nav>
      </header>

      <main>
        <section className="landing-hero" aria-labelledby="landing-hero-title">
          <div className="landing-hero-copy">
            <p className="landing-eyebrow">Düğün anıları için sade paylaşım</p>
            <h1 id="landing-hero-title"><span>Düğününüzde</span><span>çekilen tüm</span><span>anılar tek yerde.</span></h1>
            <p className="landing-lead">Misafirleriniz QR kodu okutsun, fotoğraf ve videolarını saniyeler içinde yüklesin. Siz de tüm anılara tek galeriden ulaşın.</p>
            <div className="landing-actions">
              <a className="landing-primary-action" href="#nasil-calisir">Nasıl Çalışır? <ArrowIcon /></a>
              <Link className="landing-secondary-action" to="/admin/login">Giriş Yap <ArrowIcon /></Link>
            </div>
            <p className="landing-hero-note">Misafirler için hesap gerekmez.</p>
          </div>

          <div className="landing-hero-visual" aria-label="Ürün kullanım önizlemesi">
            <div className="landing-visual-glow" aria-hidden="true" />
            <span className="landing-visual-annotation landing-annotation-scan" aria-hidden="true">01 — TARA</span>
            <div className="landing-qr-card">
              <span className="landing-card-label">TARA &amp; PAYLAŞ</span>
              <QrPattern />
              <span className="landing-qr-caption">Anılarınızı buraya ekleyin</span>
            </div>
            <span className="landing-visual-annotation landing-annotation-upload" aria-hidden="true">02 — YÜKLE</span>
            <div className="landing-upload-card">
              <div className="landing-upload-card-head"><span className="landing-mini-avatar" aria-hidden="true" /><span>Misafir yüklemesi</span><span className="landing-upload-check">✓</span></div>
              <div className="landing-upload-file"><span className="landing-file-thumbnail landing-file-thumbnail-one" aria-hidden="true" /><div><strong>anılarımız.jpg</strong><span className="landing-upload-file-meta">Fotoğraf · 4.2 MB</span></div></div>
              <div className="landing-progress-track"><span /></div>
              <p><span>Yükleniyor</span><strong>%72 tamamlandı</strong></p>
            </div>
            <span className="landing-visual-annotation landing-annotation-gallery" aria-hidden="true">03 — GALERİ</span>
            <div className="landing-gallery-mini-card">
              <div><span>Etkinlik galerisi</span><strong>128 anı</strong></div>
              <div className="landing-mini-gallery" aria-hidden="true"><i /><i /><i /><i /></div>
            </div>
          </div>
        </section>

        <section className="landing-problem" aria-labelledby="problem-title">
          <div className="landing-problem-inner">
            <div className="landing-problem-copy">
              <p className="landing-eyebrow">Anıları koruyun</p>
              <h2 id="problem-title">Anılar WhatsApp gruplarında kaybolmasın.</h2>
              <p>Tek QR, tek galeri, tüm anılar.</p>
            </div>
            <div className="landing-problem-visual" aria-hidden="true">
              <div className="landing-problem-fragments">
                <span>DAĞINIK</span>
                <div><i /><i /><i /><i /></div>
              </div>
              <div className="landing-problem-rule" />
              <div className="landing-problem-gallery">
                <span>TEK GALERİ</span>
                <div><i /><i /><i /><i /><i /><i /></div>
              </div>
            </div>
          </div>
        </section>

        <section className="landing-section landing-how" id="nasil-calisir" aria-labelledby="how-title">
          <div className="landing-section-intro">
            <p className="landing-eyebrow">Basit akış</p>
            <h2 id="how-title">Anıları toplamak dört küçük adım.</h2>
            <p>Teknik kurulumla uğraşmadan, davetlilerinizin zaten bildiği kadar kolay bir deneyim.</p>
          </div>
          <ol className="landing-steps">
            {steps.map((step) => (
              <li key={step.number}>
                <span className="landing-step-number">{step.number}</span>
                <StepIcon name={step.icon} />
                <h3>{step.title}</h3>
                <p>{step.description}</p>
              </li>
            ))}
          </ol>
        </section>

        <section className="landing-section landing-features" id="ozellikler" aria-labelledby="features-title">
          <div className="landing-feature-layout">
            <div className="landing-section-intro landing-section-intro-wide">
              <p className="landing-eyebrow">İhtiyacınız olan her şey</p>
              <h2 id="features-title">Kutlamaya odaklanın, anılar düzenli kalsın.</h2>
              <p>Misafir deneyiminden güvenli arşivlemeye kadar, mevcut ürün akışının sunduğu özellikler.</p>
              <div className="landing-feature-summary" aria-label="Ürün özeti">
                <div className="landing-feature-summary-meta">
                  <div><span>03 ADIM</span><strong>Paylaş · Yükle · Sakla</strong></div>
                  <div><span>12 ÖZELLİK</span><strong>Tek akışta</strong></div>
                  <div><span>MOBİL ÖNCELİKLİ</span><strong>Misafir için sürtünmesiz</strong></div>
                </div>
                <div className="landing-feature-summary-motif" aria-hidden="true">
                  <span>GALERİ ARŞİVİ</span>
                  <div><i /><i /><i /></div>
                </div>
              </div>
            </div>
            <div className="landing-feature-stories">
              {featureStories.map((story) => (
                <article className={`landing-feature-story landing-feature-story-${story.label.toLowerCase()}`} key={story.number}>
                  <div className="landing-feature-story-copy">
                    <p className="landing-feature-story-label">{story.number} — {story.label}</p>
                    <h3>{story.title}</h3>
                    <ul className="landing-story-capabilities">
                      {story.capabilities.map((capability) => <li key={capability}>{capability}</li>)}
                    </ul>
                  </div>
                  <div className={`landing-feature-visual landing-feature-visual-${story.label.toLowerCase()}`} aria-hidden="true">
                    {story.label === 'Paylaş' && <><div className="landing-feature-qr"><QrPattern /></div><div className="landing-feature-share-surface"><span>ETKİNLİK BAĞLANTISI</span><b>Misafirler için hazır</b><i /><i /></div></>}
                    {story.label === 'Yükle' && <><div className="landing-feature-upload-head"><span>Misafir yüklemesi</span><b>3 dosya</b></div><div className="landing-feature-upload-row"><i /><div><strong>IMG_2481.jpg</strong><span>Fotoğraf · 4.2 MB</span><b /></div><em>%72</em></div><div className="landing-feature-upload-row is-completed"><i /><div><strong>video_07.mp4</strong><span>Video · 18.4 MB</span><b /></div><em>Tamamlandı</em></div><div className="landing-feature-upload-row is-retrying"><i /><div><strong>IMG_2490.jpg</strong><span>Tekrar deneniyor</span><b /></div><em>↻</em></div></>}
                    {story.label === 'Sakla' && <><div className="landing-feature-gallery-head"><span>Etkinlik galerisi</span><b>128 anı</b></div><div className="landing-feature-gallery-stack"><i /><i /><i /><i /><i /><i /><i /></div><span className="landing-feature-archive-label">12 seçili · ZIP olarak indir →</span></>}
                  </div>
                </article>
              ))}
            </div>
          </div>
        </section>

        <section className="landing-section landing-preview" aria-labelledby="preview-title">
          <div className="landing-section-intro">
            <p className="landing-eyebrow">Her ekranda anlaşılır</p>
            <h2 id="preview-title">Misafir için hızlı, sizin için düzenli.</h2>
            <p>Ürünün üç temel anı, sade ve odaklı ekranlar olarak bir araya gelir.</p>
          </div>
          <div className="landing-preview-grid">
            <article className="landing-preview-card landing-phone-preview">
              <span className="landing-preview-kicker">MİSAFİR EKRANI</span>
              <div className="landing-phone-frame" aria-hidden="true">
                <div className="landing-phone-top" />
                <strong>Deniz &amp; Ece</strong><small>14 Eylül 2026</small>
                <div className="landing-phone-cover" />
                <div className="landing-phone-upload">Fotoğraf veya video seçin <span>+</span></div>
                <div className="landing-phone-status"><i /> 2 dosya hazır</div>
              </div>
              <h3>Telefonda rahat yükleme</h3>
              <p>Misafirler doğrudan etkinlik sayfasından anılarını paylaşır.</p>
            </article>
            <article className="landing-preview-card landing-gallery-preview">
              <span className="landing-preview-kicker">YÖNETİM GALERİSİ</span>
              <div className="landing-gallery-frame" aria-hidden="true">
                <div className="landing-gallery-frame-header"><span /><i /><i /></div>
                <div className="landing-gallery-tiles"><i /><i /><i /><i /><i /><i /></div>
                <div className="landing-gallery-controls"><span>12 seçili</span><b>ZIP indir</b></div>
              </div>
              <h3>Tek galeride tüm içerik</h3>
              <p>Fotoğraf ve videoları düzenli biçimde görüntüleyin, seçerek indirin.</p>
            </article>
            <article className="landing-preview-card landing-qr-preview">
              <span className="landing-preview-kicker">PAYLAŞMAYA HAZIR</span>
              <div className="landing-qr-preview-inner" aria-hidden="true"><QrPattern /><span>Etkinlik QR kodu</span></div>
              <h3>Tek QR, kolay katılım</h3>
              <p>Davetiye, masa kartı veya ekranda paylaşmaya uygun sade erişim.</p>
            </article>
          </div>
        </section>

        <section className="landing-section landing-pricing" id="paketler" aria-labelledby="pricing-title">
          <div className="landing-pricing-layout">
            <div className="landing-section-intro landing-pricing-intro">
              <p className="landing-eyebrow">Paketler</p>
              <h2 id="pricing-title">Anılarınız ne kadar sizinle kalsın?</h2>
              <p>Tüm paketlerde aynı paylaşım deneyimi. Yalnızca anılarınızın saklanacağı süreyi seçin.</p>
              <span className="landing-pricing-note">Tek etkinlik · Tek ödeme</span>
            </div>
            <div className="landing-pricing-options" aria-label="Etkinlik paketleri">
              {pricingPackages.map((item) => (
                <article className={`landing-pricing-option${item.recommended ? ' is-recommended' : ''}`} key={item.number}>
                  <div className="landing-pricing-option-topline"><span>{item.number}</span>{item.recommended && <b>Önerilen</b>}</div>
                  <h3>{item.duration}</h3>
                  <p className="landing-pricing-price">{item.price}</p>
                  <p className="landing-pricing-retention">{item.retention}</p>
                  <p className="landing-pricing-detail">{item.detail}</p>
                </article>
              ))}
            </div>
          </div>
          <div className="landing-pricing-shared">
            <span>Tüm paketlerde</span>
            <p>QR ile misafir erişimi · Fotoğraf/video ve çoklu dosya yükleme · Etkinlik galerisi · Tekli ve toplu indirme · Mobil uyumlu deneyim · Güvenli bulut depolama</p>
          </div>
          <div className="landing-pricing-clarification">
            <p>Tüm paketler tek etkinlik içindir.</p>
            <p>Saklama süresi, seçtiğiniz paketin ticari kapsamını ifade eder.</p>
          </div>
        </section>

        <section className="landing-final-cta" aria-labelledby="final-cta-title">
          <div>
            <p className="landing-eyebrow">Etkinliğinizi oluşturun</p>
            <h2 id="final-cta-title">Anılarınızı tek yerde toplamaya başlayın.</h2>
            <p>QR kodunuzu oluşturun, misafirleriniz paylaşmaya başlasın.</p>
          </div>
          <Link className="landing-cta-light" to="/admin/login">Etkinlik oluşturmaya başla <ArrowIcon /></Link>
        </section>
      </main>

      <footer className="landing-footer">
        <div><BrandMark /><span>{BRAND_NAME}</span></div>
        <p>© {currentYear} {BRAND_NAME}</p>
        <Link to="/admin/login">Giriş Yap</Link>
      </footer>
    </div>
  );
}
