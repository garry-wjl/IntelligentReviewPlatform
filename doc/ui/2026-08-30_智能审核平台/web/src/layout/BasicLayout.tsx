import { Link, Outlet, useLocation } from 'react-router-dom';
import { ProLayout } from '@ant-design/pro-components';
import {
  AuditOutlined,
  ClusterOutlined,
  DashboardOutlined,
  FileProtectOutlined,
  SettingOutlined,
} from '@ant-design/icons';
import { Dropdown } from 'antd';

const MENU = [
  { path: '/', name: '工作台', icon: <DashboardOutlined /> },
  { path: '/rule-sets', name: '审核规则', icon: <FileProtectOutlined /> },
  { path: '/auditors', name: '审核器管理', icon: <ClusterOutlined /> },
  { path: '/tasks', name: '审核任务', icon: <AuditOutlined /> },
];

function menuPathname(pathname: string) {
  if (pathname.startsWith('/rule-sets') || pathname.startsWith('/types')) return '/rule-sets';
  if (pathname.startsWith('/auditors')) return '/auditors';
  if (pathname.startsWith('/tasks') || pathname.startsWith('/playground')) return '/tasks';
  if (pathname.startsWith('/settings')) return '/';
  return '/';
}

export default function BasicLayout() {
  const location = useLocation();

  return (
    <div style={{ height: '100vh' }}>
      <ProLayout
        title="智能审核平台"
        layout="mix"
        fixSiderbar
        token={{
          header: { colorBgHeader: '#001529', colorHeaderTitle: '#fff', colorTextMenu: 'rgba(255,255,255,0.85)' },
        }}
        location={{ pathname: menuPathname(location.pathname) }}
        route={{ path: '/', routes: MENU }}
        menuItemRender={(item, dom) => <Link to={item.path || '/'}>{dom}</Link>}
        avatarProps={{
          src: 'https://gw.alipayobjects.com/zos/antfincdn/efFD%24IOql2/weixintupian_20170331104822.jpg',
          title: '张敏 · 规则专家',
          render: (_, defaultDom) => (
            <Dropdown
              menu={{
                items: [
                  { key: 'role', label: '当前身份：规则专家 / 管理员（静态演示）', disabled: true },
                  { key: 'settings', label: <Link to="/settings">接入设置</Link> },
                ],
              }}
            >
              {defaultDom}
            </Dropdown>
          ),
        }}
        actionsRender={() => [
          <Link key="set" to="/settings" style={{ color: 'inherit' }}>
            <SettingOutlined />
          </Link>,
        ]}
      >
        <Outlet />
      </ProLayout>
    </div>
  );
}
