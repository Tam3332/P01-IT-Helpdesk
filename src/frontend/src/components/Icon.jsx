// Bộ icon SVG lấy nguyên từ prototype (viewBox 24x24, nét 1.8)
const PATHS = {
  home: "M3 11l9-8 9 8v10a1 1 0 0 1-1 1h-5v-7H9v7H4a1 1 0 0 1-1-1z",
  ticket: "M3 9a2 2 0 0 0 0 6v3a1 1 0 0 0 1 1h16a1 1 0 0 0 1-1v-3a2 2 0 0 1 0-6V6a1 1 0 0 0-1-1H4a1 1 0 0 0-1 1zM14 5v14",
  plus: "M12 5v14M5 12h14",
  clock: "M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18zM12 7v5l3 2",
  refresh: "M21 12a9 9 0 1 1-3-6.7M21 4v5h-5",
  list: "M8 6h13M8 12h13M8 18h13M3 6h.01M3 12h.01M3 18h.01",
  tag: "M20 12l-8 8-9-9V3h8zM7.5 7.5h.01",
  flag: "M5 21V4h12l-2 4 2 4H5",
  layers: "M12 3l9 5-9 5-9-5zM3 13l9 5 9-5",
  sliders: "M4 6h10M18 6h2M4 12h4M12 12h8M4 18h12M20 18h0M16 4v4M10 10v4M18 16v4",
  shield: "M12 3l8 3v6c0 5-3.5 8-8 9-4.5-1-8-4-8-9V6z",
  bell: "M6 9a6 6 0 0 1 12 0c0 6 2 7 2 7H4s2-1 2-7M10 20a2 2 0 0 0 4 0",
  search: "M11 4a7 7 0 1 0 0 14 7 7 0 0 0 0-14zM20 20l-4-4",
  menu: "M4 6h16M4 12h16M4 18h16",
  x: "M6 6l12 12M18 6L6 18",
  check: "M5 12l5 5 9-10",
  eye: "M2 12s4-7 10-7 10 7 10 7-4 7-10 7S2 12 2 12zM12 9a3 3 0 1 0 0 6 3 3 0 0 0 0-6z",
  eyeoff: "M2 12s4-7 10-7 10 7 10 7-4 7-10 7S2 12 2 12zM3 3l18 18",
  upload: "M12 16V4M7 9l5-5 5 5M4 20h16",
  clip: "M21 11l-9 9a5 5 0 0 1-7-7l9-9a3 3 0 0 1 4 4l-9 9a1 1 0 0 1-2-2l8-8",
  logout: "M9 4H5a1 1 0 0 0-1 1v14a1 1 0 0 0 1 1h4M16 8l4 4-4 4M20 12H9",
  alert: "M12 3l10 18H2zM12 10v5M12 18h.01",
  chart: "M4 20V4M4 20h16M8 16v-5M13 16V8M18 16v-7",
  file: "M6 3h9l4 4v14H6zM14 3v5h5M9 13h7M9 17h7",
  user: "M12 4a4 4 0 1 0 0 8 4 4 0 0 0 0-8zM4 21a8 8 0 0 1 16 0",
  send: "M22 2L11 13M22 2l-7 20-4-9-9-4z",
  back: "M19 12H5M11 6l-6 6 6 6",
  edit: "M4 20h4L19 9l-4-4L4 16zM13 7l4 4",
  info: "M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18zM12 11v5M12 8h.01",
  inbox: "M3 13l3-8h12l3 8v6H3zM3 13h5l1 3h6l1-3h5",
  lock: "M6 11h12v10H6zM8 11V7a4 4 0 0 1 8 0v4",
  users: "M9 11a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7zM2 20a7 7 0 0 1 14 0M17 4.5a3.5 3.5 0 0 1 0 6.5M18 14a6 6 0 0 1 4 6",
}

export default function Icon({ name, className = '' }) {
  return (
    <svg className={`ic ${className}`.trim()} viewBox="0 0 24 24" aria-hidden="true">
      <path d={PATHS[name] || ''} />
    </svg>
  )
}
