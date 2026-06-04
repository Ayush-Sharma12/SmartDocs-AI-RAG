import { Link } from "react-router-dom";

const features = [
  {
    id: 1,
    title: "Private by default",
    description:
      "JWT authentication protects uploads, document lists, and Q&A requests.",
  },
  {
    id: 2,
    title: "RAG-based answers",
    description:
      "Uploaded PDFs are chunked, embedded, and searched before the model responds.",
  },
  {
    id: 3,
    title: "Built for teams",
    description:
      "A clean dashboard keeps document management and question workflows in one place. Collaborate securely.",
  },
];

export default function HomePage() {
  return (
    <main className="page shell">
      <section className="hero-card">
        <p className="eyebrow">SmartDocs AI</p>

        <h1>
          Ask your PDFs questions through a secure full-stack workspace.
        </h1>

        <p className="hero-copy">
          Upload documents, keep access private to signed-in users, and receive
          concise, grounded answers from your content — powered by Spring Boot +
          React.
        </p>

        <div className="hero-actions">
          <Link className="button primary" to="/register">
            Get started
          </Link>

          <Link className="button secondary" to="/login">
            Log in
          </Link>
        </div>

        <div className="hero-grid">
          {features.map(({ id, title, description }) => (
            <article key={id} className="feature-card">
              <h2>{title}</h2>
              <p>{description}</p>
            </article>
          ))}
        </div>
      </section>
    </main>
  );
}
