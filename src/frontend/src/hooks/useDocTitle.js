import { useEffect } from 'react'

// Tiêu đề tab theo trang hiện tại: "<trang> · Helpdesk".
export default function useDocTitle(page) {
  useEffect(() => {
    if (page) document.title = `${page} · Helpdesk`
  }, [page])
}
