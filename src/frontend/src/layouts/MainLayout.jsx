import { useState } from 'react'
import { Outlet, useLocation, useNavigate } from 'react-router-dom'
import { Layout, Menu, Dropdown, Avatar, Button, Drawer, Grid, Tag } from 'antd'
import { MenuOutlined, LogoutOutlined } from '@ant-design/icons'
import { useAuth } from '@/context/AuthContext'
import { MENU_BY_ROLE } from '@/config/menu'
import { ROLE_LABEL } from '@/config/constants'

const { Sider, Header, Content } = Layout

export default function MainLayout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const { pathname } = useLocation()
  const screens = Grid.useBreakpoint()
  const [open, setOpen] = useState(false)
  const isMobile = !screens.md
  const items = MENU_BY_ROLE[user.role] ?? []
  // chọn mục menu dài nhất khớp với đường dẫn hiện tại
  const selected = items.map((i) => i.key).filter((k) => pathname === k || pathname.startsWith(k + '/')).sort((a, b) => b.length - a.length)[0]

  const menu = (
    <Menu mode="inline" selectedKeys={selected ? [selected] : []} items={items}
      onClick={({ key }) => { navigate(key); setOpen(false) }} />
  )
  const brand = <div style={{ padding: 16, fontWeight: 700, fontSize: 18, color: '#3157D5' }}>IT Helpdesk</div>

  return (
    <Layout style={{ minHeight: '100vh' }}>
      {!isMobile && <Sider width={240} theme="light">{brand}{menu}</Sider>}
      {isMobile && <Drawer placement="left" open={open} onClose={() => setOpen(false)} size={260} styles={{ body: { padding: 0 } }} title="IT Helpdesk">{menu}</Drawer>}
      <Layout>
        <Header style={{ background: '#fff', padding: '0 16px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          {isMobile ? <Button type="text" icon={<MenuOutlined />} onClick={() => setOpen(true)} /> : <span />}
          <Dropdown menu={{ items: [{ key: 'out', icon: <LogoutOutlined />, label: 'Đăng xuất', onClick: () => { logout(); navigate('/login') } }] }}>
            <span style={{ cursor: 'pointer' }}>
              <Avatar style={{ background: '#3157D5', marginRight: 8 }}>{user.fullName[0]}</Avatar>
              {!isMobile && user.fullName} <Tag style={{ marginLeft: 6 }}>{ROLE_LABEL[user.role]}</Tag>
            </span>
          </Dropdown>
        </Header>
        <Content className="page-pad"><Outlet /></Content>
      </Layout>
    </Layout>
  )
}
