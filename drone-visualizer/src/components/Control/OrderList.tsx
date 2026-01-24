import { Table, Button, Space, Tag, Popconfirm } from 'antd';
import { DeleteOutlined, ClearOutlined } from '@ant-design/icons';
import type { MedDispatchRec } from '../../types';

interface OrderListProps {
  orders: MedDispatchRec[];
  onRemoveOrder: (id: number) => void;
  onClearAll: () => void;
}

const OrderList: React.FC<OrderListProps> = ({ orders, onRemoveOrder, onClearAll }) => {
  const columns = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      width: 45,
      align: 'center' as const,
    },
    {
      title: 'Date',
      dataIndex: 'date',
      key: 'date',
      width: 110,
    },
    {
      title: 'Time',
      dataIndex: 'time',
      key: 'time',
      width: 90,
    },
    {
      title: 'Capacity',
      key: 'capacity',
      width: 80,
      render: (_: any, record: MedDispatchRec) => (
        <span>{record.requirements?.capacity?.toFixed(2) ?? 'N/A'} kg</span>
      ),
    },
    {
      title: 'Special',
      key: 'special',
      width: 120,
      render: (_: any, record: MedDispatchRec) => (
        <Space size={4}>
          {record.requirements?.cooling && <Tag color="blue">Cooling</Tag>}
          {record.requirements?.heating && <Tag color="red">Heating</Tag>}
          {record.requirements?.maxCost && (
            <Tag color="orange">£{record.requirements.maxCost}</Tag>
          )}
        </Space>
      ),
    },
    {
      title: 'Location',
      key: 'location',
      width: 140,
      render: (_: any, record: MedDispatchRec) => (
        <div style={{
          fontSize: 11,
          fontFamily: 'monospace',
          lineHeight: '1.4'
        }}>
          <div>Lng: {record.delivery?.lng?.toFixed(4) ?? 'N/A'}</div>
          <div>Lat: {record.delivery?.lat?.toFixed(4) ?? 'N/A'}</div>
        </div>
      ),
    },
    {
      title: 'Action',
      key: 'action',
      width: 70,
      align: 'center' as const,
      render: (_: any, record: MedDispatchRec) => (
        <Popconfirm
          title="Remove this order?"
          onConfirm={() => record.id !== undefined && onRemoveOrder(record.id)}
          okText="Yes"
          cancelText="No"
        >
          <Button
            type="text"
            danger
            icon={<DeleteOutlined />}
            size="small"
          />
        </Popconfirm>
      ),
    },
  ];

  return (
    <div style={{ marginTop: 20 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 12 }}>
        <h3 style={{ margin: 0 }}>Orders ({orders.length})</h3>
        {orders.length > 0 && (
          <Popconfirm
            title="Clear all orders?"
            onConfirm={onClearAll}
            okText="Yes"
            cancelText="No"
          >
            <Button
              type="text"
              danger
              icon={<ClearOutlined />}
              size="small"
            >
              Clear All
            </Button>
          </Popconfirm>
        )}
      </div>
      <Table
        dataSource={orders}
        columns={columns}
        rowKey="id"
        pagination={false}
        size="small"
        scroll={{ y: 300 }}
        locale={{ emptyText: 'No orders yet. Add your first order above.' }}
      />
    </div>
  );
};

export default OrderList;