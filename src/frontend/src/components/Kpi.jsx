import Icon from './Icon'

export const Kpi = ({ label, value, tone = 'primary', icon }) => (
  <div className="card kpi">
    <div className={`ico t-${tone}`}>
      <Icon name={icon} />
    </div>
    <div>
      <span>{label}</span>
      <b>{value ?? '—'}</b>
    </div>
  </div>
)
