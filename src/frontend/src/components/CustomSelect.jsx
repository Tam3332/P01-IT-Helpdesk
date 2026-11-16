import { useEffect, useRef, useState } from 'react'
import Icon from './Icon'

export default function CustomSelect({
  value,
  onChange,
  options = [],
  placeholder = '',
  ariaLabel,
  className = '',
  disabled = false,
}) {
  const [open, setOpen] = useState(false)
  const [dropUp, setDropUp] = useState(false)
  const ref = useRef(null)

  useEffect(() => {
    if (!open) return undefined
    if (ref.current) {
      const rect = ref.current.getBoundingClientRect()
      const spaceBelow = window.innerHeight - rect.bottom
      setDropUp(spaceBelow < 220 && rect.top > 220)
    }
    const onDocClick = (e) => {
      if (ref.current && !ref.current.contains(e.target)) setOpen(false)
    }
    const onKey = (e) => {
      if (e.key === 'Escape') setOpen(false)
    }
    document.addEventListener('mousedown', onDocClick)
    window.addEventListener('keydown', onKey)
    return () => {
      document.removeEventListener('mousedown', onDocClick)
      window.removeEventListener('keydown', onKey)
    }
  }, [open])

  const selected = options.find((o) => String(o.value) === String(value ?? ''))
  const displayLabel = selected ? selected.label : (placeholder || (options[0]?.label ?? ''))

  return (
    <div className={`c-select ${className} ${open ? 'open' : ''} ${disabled ? 'disabled' : ''}`.trim()} ref={ref}>
      <button
        type="button"
        className="c-select-btn"
        onClick={() => !disabled && setOpen((prev) => !prev)}
        aria-label={ariaLabel}
        aria-expanded={open}
        aria-haspopup="listbox"
        disabled={disabled}
      >
        <span className="c-select-label">{displayLabel}</span>
        <span className={`c-select-arrow ${open ? 'rotated' : ''}`}>
          <Icon name="chevDown" />
        </span>
      </button>

      {open && (
        <div className={`c-select-dropdown ${dropUp ? 'drop-up' : ''}`} role="listbox">
          {options.map((opt) => {
            const isSel = String(opt.value) === String(value ?? '')
            return (
              <div
                key={String(opt.value)}
                className={`c-select-option ${isSel ? 'selected' : ''}`}
                role="option"
                aria-selected={isSel}
                tabIndex={0}
                onClick={() => {
                  onChange(opt.value)
                  setOpen(false)
                }}
                onKeyDown={(e) => {
                  if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault()
                    onChange(opt.value)
                    setOpen(false)
                  }
                }}
              >
                <span>{opt.label}</span>
                {isSel && <Icon name="check" className="c-select-check" />}
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}
