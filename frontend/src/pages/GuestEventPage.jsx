import { useParams } from 'react-router-dom';

export default function GuestEventPage() {
  const { token } = useParams();

  return (
    <section className="placeholder-page">
      <h1>Etkinlik sayfası</h1>
      <p>Etkinlik kodu: {token}</p>
      <p>Misafir yükleme deneyimi M10 kapsamında eklenecek.</p>
    </section>
  );
}
