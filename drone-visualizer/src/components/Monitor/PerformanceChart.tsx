import { Card, Statistic, Row, Col, Empty } from 'antd';
import {
  ClockCircleOutlined,
  DollarOutlined,
  RocketOutlined,
  NodeIndexOutlined,
} from '@ant-design/icons';
import type { DeliveryPathResponse, PathfindingProgress } from '../../types';

interface PerformanceChartProps {
  result: DeliveryPathResponse | null;
  progress: PathfindingProgress[];
  calculationTime: number;
}

const PerformanceChart: React.FC<PerformanceChartProps> = ({
  result,
  progress,
  calculationTime,
}) => {
  if (!result) {
    return (
      <Card title="📊 Performance Metrics" style={{ height: '100%' }}>
        <Empty
          description="No calculation results yet"
          style={{ marginTop: 60 }}
        />
      </Card>
    );
  }

  const totalNodesExplored = progress
    .filter(p => p.type === 'node_explored')
    .length;

  const pathsFound = progress
    .filter(p => p.type === 'path_found')
    .length;

  const calculationsCompleted = progress
    .filter(p => p.type === 'calculation_complete')
    .length;

  const totalDeliveries = result.dronePaths.reduce(
    (sum: number, path: any) => sum + path.deliveries.filter((d: any) => d.deliveryId !== null).length,
    0
  );
  const avgCostPerDelivery = totalDeliveries > 0
    ? result.totalCost / totalDeliveries
    : 0;

  return (
    <Card title="📊 Performance Metrics" style={{ height: '100%' }}>
      <Row gutter={[16, 16]}>
        <Col span={12}>
          <Statistic
            title="Total Cost"
            value={result.totalCost}
            precision={2}
            prefix="£"
            valueStyle={{ color: '#3f8600' }}
          />
        </Col>
        <Col span={12}>
          <Statistic
            title="Total Moves"
            value={result.totalMoves}
            valueStyle={{ color: '#1890ff' }}
            prefix={<RocketOutlined />}
          />
        </Col>
        <Col span={12}>
          <Statistic
            title="Calculation Time"
            value={calculationTime}
            suffix="ms"
            valueStyle={{ color: '#722ed1' }}
            prefix={<ClockCircleOutlined />}
          />
        </Col>
        <Col span={12}>
          <Statistic
            title="Nodes Explored"
            value={totalNodesExplored}
            valueStyle={{ color: '#faad14' }}
            prefix={<NodeIndexOutlined />}
          />
        </Col>
        <Col span={12}>
          <Statistic
            title="Drones Used"
            value={result.dronePaths.length}
            valueStyle={{ color: '#13c2c2' }}
          />
        </Col>
        <Col span={12}>
          <Statistic
            title="Avg Cost/Delivery"
            value={avgCostPerDelivery}
            precision={2}
            prefix="£"
            valueStyle={{ color: '#52c41a' }}
          />
        </Col>
        <Col span={12}>
          <Statistic
            title="Paths Found"
            value={pathsFound}
            valueStyle={{ color: '#eb2f96' }}
          />
        </Col>
        <Col span={12}>
          <Statistic
            title="Total Deliveries"
            value={totalDeliveries}
            valueStyle={{ color: '#fa8c16' }}
          />
        </Col>
      </Row>

      <div style={{ marginTop: 20 }}>
        <h4 style={{ marginBottom: 12 }}>🚁 Drone Details</h4>
        {result.dronePaths.map((path: any, index: number) => {
          const deliveries = path.deliveries.filter((d: any) => d.deliveryId !== null);
          const totalPathMoves = path.deliveries.reduce(
            (sum: number, d: any) => sum + d.flightPath.length,
            0
          );

          const orderIds = deliveries
            .map((d: any) => d.deliveryId)
            .filter((id: any) => id !== null)
            .join(', ');

          return (
            <div
              key={index}
              style={{
                padding: 10,
                marginBottom: 10,
                background: '#f5f5f5',
                borderRadius: 4,
                borderLeft: '3px solid #1890ff',
              }}
            >
              <div style={{ fontWeight: 'bold', marginBottom: 5 }}>
                Drone {path.droneId}
              </div>
              <div style={{ fontSize: 12, color: '#666' }}>
                Deliveries: {deliveries.length} | Moves: {totalPathMoves}
              </div>
              {orderIds && (
                <div style={{ fontSize: 11, color: '#1890ff', marginTop: 4 }}>
                  📦 Orders: {orderIds}
                </div>
              )}
            </div>
          );
        })}
      </div>
    </Card>
  );
};

export default PerformanceChart;