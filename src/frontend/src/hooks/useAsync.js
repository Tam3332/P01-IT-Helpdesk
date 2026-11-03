import { useCallback, useEffect, useRef, useState } from 'react'
import { apiError } from '@/services/apiClient'

// Gọi API khi mount / khi deps đổi. Trả { data, loading, error, reload, setData }.
export default function useAsync(fn, deps = []) {
  const [state, setState] = useState({ data: null, loading: true, error: '', status: 0 })
  const seq = useRef(0)
  const fnRef = useRef(fn)
  fnRef.current = fn

  const run = useCallback(async () => {
    const id = ++seq.current
    setState((s) => ({ ...s, loading: true, error: '' }))
    try {
      const data = await fnRef.current()
      if (id === seq.current) setState({ data, loading: false, error: '', status: 200 })
    } catch (e) {
      if (id === seq.current) setState((s) => ({ ...s, loading: false, error: apiError(e), status: e?.response?.status || 0 }))
    }
  }, [])

  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(() => { run() }, deps)

  const setData = useCallback((data) => setState((s) => ({ ...s, data })), [])
  return { ...state, reload: run, setData }
}
