import { Link } from "react-router-dom";
import { useState } from "react";

export default function HomePage() {
  const [hoveredCard, setHoveredCard] = useState(null);

  const features = [
    {
      id: 1,
      icon: "🔒",
      title: "Private by default",
      description: "JWT authentication protects uploads, document lists, and Q&A requests."
    },
    {
      id: 2,
      icon: "🤖",
      title: "RAG-based answers",
      description: "Uploaded PDFs are chunked, embedded, and searched before the model responds."
    },
    {
      id: 3,
      icon: "👥",
      title: "Built for teams",
        description: "A clean dashboard keeps document management and question workflows in one place. Collaborate securely."
    }
  ];

  return (
    <main className="page shell">
      <section className="hero-card">
        <p className="eyebrow">SmartDocs AI</p>
        <h1>Ask your PDFs questions through a secure full-stack workspace.</h1>
        <p className="hero-copy">
          Upload documents, keep access private to signed-in users,
          and receive concise, grounded answers from your content — powered by Spring Boot + React.
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
          {features.map((feature) => (
            <article
              key={feature.id}
              className="feature-card"
              onMouseEnter={() => setHoveredCard(feature.id)}
              onMouseLeave={() => setHoveredCard(null)}
            >
              <div className="feature-icon">{feature.icon}</div>
              <h2>{feature.title}</h2>
              <p>{feature.description}</p>
            </article>
          ))}
        </div>
      </section>
    </main>
  );
}
