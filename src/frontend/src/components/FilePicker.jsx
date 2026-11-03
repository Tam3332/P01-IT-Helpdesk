import { useRef } from 'react'
import Icon from './Icon'
import { ALLOWED_EXT, LIMITS } from '@/config/domain'
import { humanSize } from '@/utils/sla'

// Kiểm tra tệp phía giao diện theo SRS HD04: ≤ 5 tệp, ≤ 10 MB, đuôi trong danh sách cho phép.
// Backend vẫn kiểm tra lại (MIME, kích thước, SHA-256).
export function validateFiles(incoming, existingCount = 0) {
  const ok = []
  const errs = []
  for (const f of incoming) {
    const ext = f.name.split('.').pop().toLowerCase()
    if (existingCount + ok.length >= LIMITS.maxFiles) errs.push(`Tối đa ${LIMITS.maxFiles} tệp mỗi ticket.`)
    else if (!ALLOWED_EXT.includes(ext)) errs.push(`"${f.name}": định dạng không được hỗ trợ.`)
    else if (f.size > LIMITS.maxFileBytes) errs.push(`"${f.name}": vượt quá 10 MB.`)
    else ok.push(f)
  }
  return { ok, errors: [...new Set(errs)] }
}

export default function FilePicker({ files, onChange, error }) {
  const input = useRef(null)
  const pick = (e) => {
    const { ok, errors } = validateFiles([...e.target.files], files.length)
    e.target.value = ''
    onChange([...files, ...ok], errors.join(' '))
  }
  return (
    <div className={`field ${error ? 'has-err' : ''}`}>
      <span className="lbl">Tệp đính kèm</span>
      <label className="drop" htmlFor="f-files">
        <Icon name="upload" />
        <b style={{ color: 'var(--text)' }}>Bấm để chọn tệp</b>
        <span className="sm">Tối đa {LIMITS.maxFiles} tệp, mỗi tệp ≤ 10 MB ({ALLOWED_EXT.join(', ')})</span>
      </label>
      <input type="file" id="f-files" multiple hidden ref={input} onChange={pick} />
      {files.length > 0 && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
          {files.map((f, i) => (
            <div className="att" key={`${f.name}-${i}`}>
              <Icon name="clip" />
              <span>{f.name}</span>
              <span className="sm muted" style={{ flex: 'none' }}>{humanSize(f.size)}</span>
              <button type="button" className="btn ghost sm" aria-label={`Bỏ tệp ${f.name}`} onClick={() => onChange(files.filter((_, j) => j !== i), '')}>
                <Icon name="x" />
              </button>
            </div>
          ))}
        </div>
      )}
      {error && <span className="err">{error}</span>}
    </div>
  )
}
