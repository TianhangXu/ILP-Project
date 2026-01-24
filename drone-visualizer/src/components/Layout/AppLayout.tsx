import { Layout } from 'antd';
import { ReactNode } from 'react';

const { Header, Content, Sider } = Layout;

interface AppLayoutProps {
  leftPanel: ReactNode;
  map: ReactNode;
  rightPanel: ReactNode;
}

const AppLayout: React.FC<AppLayoutProps> = ({ leftPanel, map, rightPanel }) => {
  return (
    <Layout
      style={{
        height: '100vh',
        width: '100vw',
        position: 'relative',
        overflow: 'hidden',
        transform: 'none'
      }}
    >
      <Header
        style={{
          background: '#001529',
          color: 'white',
          display: 'flex',
          alignItems: 'center',
          padding: '0 24px',
          height: '64px',
          flexShrink: 0,
          transform: 'none'
        }}
      >
        <h2 style={{ margin: 0, color: 'white' }}>
          🚁 Drone Delivery Visualization System
        </h2>
      </Header>

      <Layout
        style={{
          height: 'calc(100vh - 64px)',
          overflow: 'hidden',
          transform: 'none'
        }}
      >
        <Sider
          width={400}
          style={{
            background: '#fff',
            borderRight: '1px solid #f0f0f0',
            overflow: 'auto',
            flexShrink: 0,
            transform: 'none'
          }}
        >
          {leftPanel}
        </Sider>

        <Content
          style={{
            position: 'relative',
            minWidth: 0,
            flex: 1,
            height: '100%',
            overflow: 'hidden',
            transform: 'none',
            WebkitTransform: 'none',
            MozTransform: 'none',
            msTransform: 'none'
          }}
        >
          {map}
        </Content>

        <Sider
          width={400}
          style={{
            background: '#fff',
            borderLeft: '1px solid #f0f0f0',
            overflow: 'auto',
            flexShrink: 0,
            transform: 'none'
          }}
        >
          {rightPanel}
        </Sider>
      </Layout>
    </Layout>
  );
};

export default AppLayout;