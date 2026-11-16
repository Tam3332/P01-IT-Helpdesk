// Hình minh họa vẽ tay cho trang 403/404 (SVG thuần, đổi màu theo giao diện sáng/tối).
export default function ErrorArt({ kind }) {
  return (
    <svg className="errart" viewBox="0 0 200 140" width="200" height="140" role="img" aria-hidden="true" fill="none" strokeLinecap="round" strokeLinejoin="round">
      <ellipse cx="100" cy="124" rx="62" ry="7" fill="var(--surface2)" />
      {kind === '403' ? (
        <>
          <rect x="52" y="58" width="96" height="62" rx="12" fill="var(--primary-soft)" stroke="var(--primary-t)" strokeWidth="4" />
          <path d="M72 58V44a28 28 0 0 1 56 0v14" stroke="var(--primary-t)" strokeWidth="4" />
          <circle cx="100" cy="86" r="8" fill="var(--primary-t)" />
          <path d="M100 92v12" stroke="var(--primary-t)" strokeWidth="5" />
        </>
      ) : (
        <>
          <circle cx="92" cy="66" r="38" fill="var(--primary-soft)" stroke="var(--primary-t)" strokeWidth="4" />
          <path d="M120 94l34 28" stroke="var(--primary-t)" strokeWidth="9" />
          <path d="M78 54l28 28M106 54L78 82" stroke="var(--primary-t)" strokeWidth="5" />
        </>
      )}
      <path d="M24 30l6 6M30 30l-6 6M172 40a4 4 0 1 0 0 .1M160 16l4 4M164 16l-4 4" stroke="var(--faint)" strokeWidth="3" />
    </svg>
  )
}
