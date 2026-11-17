type PlaceholderRoutePageProps = {
  eyebrow: string;
  title: string;
  description: string;
};

export function PlaceholderRoutePage({ eyebrow, title, description }: PlaceholderRoutePageProps) {
  return (
    <>
      <header className="shell__hero">
        <span className="eyebrow" style={{ background: "rgba(24, 50, 37, 0.08)", color: "#254a34" }}>
          {eyebrow}
        </span>
        <h1>{title}</h1>
        <p>{description}</p>
      </header>

      <article className="shell__panel shell__placeholder">
        <h2>Coming soon</h2>
        <p>This route already participates in permission-aware navigation and safe redirects, so feature work can land here without reshaping the shell again.</p>
      </article>
    </>
  );
}
