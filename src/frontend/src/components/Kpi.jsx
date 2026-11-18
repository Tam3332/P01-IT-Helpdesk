import { useNavigate } from 'react-router-dom'
import Icon from './Icon'
import CountUp from './CountUp'

// to: nếu có thì thẻ bấm được (chuyển tới danh sách đã lọc); logic lọc do trang gọi quyết định.
export const Kpi = ({ label, value, tone = 'primary', icon, to }) => {
  const navigate = useNavigate()
  const link = to ? { role: 'link', tabIndex: 0, onClick: () => navigate(to), onKeyDown: (e) => e.key === 'Enter' && navigate(to) } : {}
  return (
    <div className={`card kpi${to ? ' clk' : ''}`} {...link}>
      <div className={`ico t-${tone}`}>
        <Icon name={icon} />
      </div>
      <div>
        <span>{label}</span>
        <b><CountUp value={value} /></b>
      </div>
    </div>
  )
}
