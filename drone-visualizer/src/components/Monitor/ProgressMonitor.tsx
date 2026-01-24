import { useEffect, useRef } from 'react';
import { Badge, Card, Timeline } from 'antd';
import {
  ClockCircleOutlined,
  CheckCircleOutlined,
  LoadingOutlined,
  CloseCircleOutlined,
  WarningOutlined
} from '@ant-design/icons';
import type { PathfindingProgress } from '../../types';

interface ProgressMonitorProps {
  progress: PathfindingProgress[];
  isConnected: boolean;
}

const ProgressMonitor: React.FC<ProgressMonitorProps> = ({ progress, isConnected }) => {
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (containerRef.current) {
      containerRef.current.scrollTop = containerRef.current.scrollHeight;
    }
  }, [progress]);

  const getTimelineItems = () => {
    return progress.slice(-20).map((item, index) => {
      let color = 'blue';
      let icon = <ClockCircleOutlined />;
      let message = '';

      switch (item.type) {
        case 'calculation_start':
          color = 'blue';
          icon = <LoadingOutlined />;
          message = 'Calculation started';
          break;

        case 'node_explored':
          if (item.message?.toLowerCase().includes('restricted') ||
              item.message?.toLowerCase().includes('forbidden') ||
              item.message?.toLowerCase().includes('blocked')) {
            color = 'red';
            icon = <CloseCircleOutlined />;
            message = `❌ Blocked: ${item.message}`;
          } else if (item.message?.toLowerCase().includes('cost') &&
                     item.message?.toLowerCase().includes('exceed')) {
            color = 'red';
            icon = <WarningOutlined />;
            message = `⚠️ ${item.message}`;
          } else {
            color = 'gray';
            message = item.position
              ? `Explored node at (${item.position.lng.toFixed(4)}, ${item.position.lat.toFixed(4)})`
              : 'Exploring node...';
          }
          break;

        case 'path_found':
          color = 'green';
          icon = <CheckCircleOutlined />;
          message = item.cost
            ? `✅ Path found with cost: ${item.cost.toFixed(2)}`
            : '✅ Path found';
          break;

        case 'calculation_complete':
          color = 'green';
          icon = <CheckCircleOutlined />;
          message = item.totalCost
            ? `🎉 Calculation complete! Total cost: ${item.totalCost.toFixed(2)}`
            : '🎉 Calculation complete!';
          break;

        case 'error':
          color = 'red';
          icon = <CloseCircleOutlined />;
          // ✅ 检测特定错误类型
          const errorMsg = item.message || 'Unknown error';
          if (errorMsg.toLowerCase().includes('restricted') ||
              errorMsg.toLowerCase().includes('no-fly')) {
            message = `🚫 Error: Delivery point in restricted area`;
          } else if (errorMsg.toLowerCase().includes('cost') ||
                     errorMsg.toLowerCase().includes('budget')) {
            message = `💰 Error: Insufficient budget (max cost exceeded)`;
          } else if (errorMsg.toLowerCase().includes('no path') ||
                     errorMsg.toLowerCase().includes('unreachable')) {
            message = `🚫 Error: No valid path found`;
          } else {
            message = `❌ Error: ${errorMsg}`;
          }
          break;

        case 'warning':
          color = 'orange';
          icon = <WarningOutlined />;
          message = `⚠️ Warning: ${item.message || 'Unknown warning'}`;
          break;

        case 'no_solution':
          color = 'red';
          icon = <CloseCircleOutlined />;
          message = `🚫 No solution found: ${item.message || 'Unable to deliver all orders'}`;
          break;

        default:
          message = item.message || 'Unknown event';
      }

      return {
        key: `${item.timestamp}-${index}`,
        color,
        dot: icon,
        children: (
          <div style={{ fontSize: '12px' }}>
            <div style={{ color: '#666', fontSize: '11px' }}>
              {item.timestamp ? new Date(item.timestamp).toLocaleTimeString() : 'N/A'}
            </div>
            <div style={{
              marginTop: 2,
              color: color === 'red' ? '#ff4d4f' :
                     color === 'green' ? '#52c41a' :
                     color === 'orange' ? '#faad14' :
                     '#000'
            }}>
              {message}
            </div>
          </div>
        ),
      };
    });
  };

  return (
    <Card
      title={
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <span>📊 Progress Monitor</span>
          <Badge
            status={isConnected ? 'success' : 'error'}
            text={isConnected ? 'Connected' : 'Disconnected'}
          />
        </div>
      }
      size="small"
      style={{ height: '100%', display: 'flex', flexDirection: 'column' }}
      bodyStyle={{ flex: 1, overflow: 'hidden', padding: '12px' }}
    >
      <div
        ref={containerRef}
        style={{
          height: '100%',
          overflowY: 'auto',
          overflowX: 'hidden',
        }}
      >
        {progress.length === 0 ? (
          <div style={{
            textAlign: 'center',
            color: '#999',
            padding: '40px 20px',
            fontSize: '13px'
          }}>
            <LoadingOutlined style={{ fontSize: 24, marginBottom: 12, display: 'block' }} />
            No activity yet. Start by adding orders and calculating paths.
          </div>
        ) : (
          <Timeline items={getTimelineItems()} />
        )}
      </div>
    </Card>
  );
};

export default ProgressMonitor;