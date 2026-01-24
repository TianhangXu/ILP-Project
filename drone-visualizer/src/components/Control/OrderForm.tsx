import { useState, useEffect } from 'react';
import { Form, InputNumber, Button, Checkbox, DatePicker, TimePicker, Space, Alert, Modal } from 'antd';
import dayjs from 'dayjs';
import type { MedDispatchRec, RestrictedArea } from '../../types';

interface OrderFormProps {
  onAddOrder: (order: MedDispatchRec) => void;
  existingOrders?: MedDispatchRec[];
  maxOrders?: number;
  restrictedAreas?: RestrictedArea[];
  clickedLocation?: { lat: number; lng: number } | null;
  onClearClickedLocation?: () => void;
}

const OrderForm: React.FC<OrderFormProps> = ({
  onAddOrder,
  existingOrders = [],
  maxOrders = 30,
  restrictedAreas = [],
  clickedLocation,
  onClearClickedLocation,
}) => {
  const [form] = Form.useForm();
  const [nextOrderId, setNextOrderId] = useState(1);
  const [validationError, setValidationError] = useState<string>('');
  const [isModalVisible, setIsModalVisible] = useState(false);
  const [cooling, setCooling] = useState(false);  // 新增：跟踪 cooling 状态
  const [heating, setHeating] = useState(false);  // 新增：跟踪 heating 状态

  useEffect(() => {
    if (existingOrders.length === 0) {
      setNextOrderId(1);
    } else {
      const maxId = Math.max(...existingOrders.map(o => o.id || 0));
      setNextOrderId(maxId + 1);
    }
  }, [existingOrders]);

  useEffect(() => {
    if (clickedLocation) {
      const restrictedAreaName = isInRestrictedArea(clickedLocation.lng, clickedLocation.lat);

      if (restrictedAreaName) {
        Modal.error({
          title: 'Location in Restricted Area',
          content: `The selected location (${clickedLocation.lng.toFixed(4)}, ${clickedLocation.lat.toFixed(4)}) is inside restricted area: ${restrictedAreaName}. Please choose a different location.`,
          onOk: () => {
            if (onClearClickedLocation) {
              onClearClickedLocation();
            }
          }
        });
      } else if (!isValidCoordinate(clickedLocation.lng, clickedLocation.lat)) {
        Modal.error({
          title: 'Invalid Coordinates',
          content: `The selected location (${clickedLocation.lng.toFixed(4)}, ${clickedLocation.lat.toFixed(4)}) is outside the valid geographic range.`,
          onOk: () => {
            if (onClearClickedLocation) {
              onClearClickedLocation();
            }
          }
        });
      } else {
        form.setFieldsValue({
          lng: clickedLocation.lng,
          lat: clickedLocation.lat,
        });
        setIsModalVisible(true);
      }
    }
  }, [clickedLocation]);

  const isValidCoordinate = (lng: number, lat: number): boolean => {
    if (lat < -90 || lat > 90) {
      return false;
    }
    if (lng < -180 || lng > 180) {
      return false;
    }
    return true;
  };

  const isPointInPolygon = (point: { lng: number; lat: number }, vertices: Array<{ lng: number; lat: number }>): boolean => {
    let inside = false;
    for (let i = 0, j = vertices.length - 1; i < vertices.length; j = i++) {
      const xi = vertices[i].lng;
      const yi = vertices[i].lat;
      const xj = vertices[j].lng;
      const yj = vertices[j].lat;

      const intersect = ((yi > point.lat) !== (yj > point.lat)) &&
        (point.lng < (xj - xi) * (point.lat - yi) / (yj - yi) + xi);
      if (intersect) inside = !inside;
    }
    return inside;
  };

  const isInRestrictedArea = (lng: number, lat: number): string | null => {
    for (const area of restrictedAreas) {
      if (isPointInPolygon({ lng, lat }, area.vertices)) {
        return area.name || area.id;
      }
    }
    return null;
  };

  // 新增：处理 cooling 复选框变化
  const handleCoolingChange = (e: any) => {
    const checked = e.target.checked;
    setCooling(checked);

    // 如果选中 cooling，自动取消 heating
    if (checked && heating) {
      form.setFieldsValue({ heating: false });
      setHeating(false);
    }
  };

  // 新增：处理 heating 复选框变化
  const handleHeatingChange = (e: any) => {
    const checked = e.target.checked;
    setHeating(checked);

    // 如果选中 heating，自动取消 cooling
    if (checked && cooling) {
      form.setFieldsValue({ cooling: false });
      setCooling(false);
    }
  };

  const handleSubmit = (values: any) => {
    setValidationError('');

    if (existingOrders.length >= maxOrders) {
      return;
    }

    const lng = values.lng;
    const lat = values.lat;

    if (!isValidCoordinate(lng, lat)) {
      setValidationError(
        `Invalid coordinates! The location (${lng.toFixed(4)}, ${lat.toFixed(4)}) is outside the valid geographic range. ` +
        `Valid range: Latitude -90 to 90, Longitude -180 to 180.`
      );
      return;
    }

    const restrictedAreaName = isInRestrictedArea(lng, lat);
    if (restrictedAreaName) {
      setValidationError(
        `Cannot create order! The delivery location (${lng.toFixed(4)}, ${lat.toFixed(4)}) is inside restricted area: ${restrictedAreaName}. ` +
        `Please choose a different location.`
      );
      return;
    }

    // 新增：验证 cooling 和 heating 不能同时为 true
    if (values.cooling && values.heating) {
      setValidationError(
        'Invalid requirements! Cooling and Heating cannot be enabled at the same time. Please choose only one.'
      );
      return;
    }

    const order: MedDispatchRec = {
      id: nextOrderId,
      date: values.date ? dayjs(values.date).format('YYYY-MM-DD') : dayjs().format('YYYY-MM-DD'),
      time: values.time ? dayjs(values.time).format('HH:mm:ss') : dayjs().format('HH:mm:ss'),
      requirements: {
        capacity: values.capacity || 0,
        cooling: values.cooling || false,
        heating: values.heating || false,
        maxCost: values.maxCost || undefined,
      },
      delivery: {
        lng: lng,
        lat: lat,
      },
    };

    onAddOrder(order);
    form.resetFields();
    setNextOrderId(nextOrderId + 1);
    setValidationError('');
    setCooling(false);  // 新增：重置状态
    setHeating(false);  // 新增：重置状态

    setIsModalVisible(false);
    if (onClearClickedLocation) {
      onClearClickedLocation();
    }
  };

  const handleModalCancel = () => {
    setIsModalVisible(false);
    if (onClearClickedLocation) {
      onClearClickedLocation();
    }
  };

  const remainingOrders = maxOrders - existingOrders.length;
  const isLimitReached = existingOrders.length >= maxOrders;

  const FormContent = () => (
    <>
      {validationError && (
        <Alert
          message="Validation Error"
          description={validationError}
          type="error"
          showIcon
          closable
          onClose={() => setValidationError('')}
          style={{ marginBottom: 12 }}
        />
      )}

      {isLimitReached && (
        <Alert
          message="Maximum Order Limit Reached"
          description={`You can only add up to ${maxOrders} orders. Please remove some orders to add new ones.`}
          type="warning"
          showIcon
          style={{ marginBottom: 12 }}
        />
      )}

      {!isLimitReached && remainingOrders <= 10 && (
        <Alert
          message={`${remainingOrders} order${remainingOrders === 1 ? '' : 's'} remaining`}
          type="info"
          showIcon
          style={{ marginBottom: 12 }}
        />
      )}

      <div style={{
        marginBottom: 12,
        padding: '8px 12px',
        background: isLimitReached ? '#fff1f0' : '#f0f0f0',
        borderRadius: 4,
        fontSize: 13,
        color: '#666',
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center'
      }}>
        <div>
          Next Order ID: <strong style={{ color: '#1890ff' }}>#{nextOrderId}</strong>
        </div>
        <div style={{ color: isLimitReached ? '#ff4d4f' : '#666' }}>
          {existingOrders.length}/{maxOrders} orders
        </div>
      </div>

      <Space direction="horizontal" style={{ width: '100%' }} size="small">
        <Form.Item
          label="Date"
          name="date"
          style={{ flex: 1, marginBottom: 12 }}
        >
          <DatePicker
            style={{ width: '100%' }}
            disabled={isLimitReached}
          />
        </Form.Item>
        <Form.Item
          label="Time"
          name="time"
          style={{ flex: 1, marginBottom: 12 }}
        >
          <TimePicker
            style={{ width: '100%' }}
            disabled={isLimitReached}
          />
        </Form.Item>
      </Space>

      <Form.Item
        label="Capacity (kg)"
        name="capacity"
        rules={[{ required: true, message: 'Please enter capacity' }]}
      >
        <InputNumber
          min={0.1}
          max={20}
          step={0.1}
          style={{ width: '100%' }}
          disabled={isLimitReached}
        />
      </Form.Item>

      {/* 修改：添加互斥提示和事件处理 */}
      <div style={{ marginBottom: 12 }}>
        <div style={{
          fontSize: 12,
          color: '#666',
          marginBottom: 8
        }}>
          Special Requirements (Choose only one):
        </div>
        <Space>
          <Form.Item
            name="cooling"
            valuePropName="checked"
            style={{ marginBottom: 0 }}
          >
            <Checkbox
              disabled={isLimitReached}
              onChange={handleCoolingChange}
            >
              🧊 Cooling
            </Checkbox>
          </Form.Item>
          <Form.Item
            name="heating"
            valuePropName="checked"
            style={{ marginBottom: 0 }}
          >
            <Checkbox
              disabled={isLimitReached}
              onChange={handleHeatingChange}
            >
              🔥 Heating
            </Checkbox>
          </Form.Item>
        </Space>
        {(cooling || heating) && (
          <div style={{
            fontSize: 11,
            color: '#faad14',
            marginTop: 4,
            fontStyle: 'italic'
          }}>
            ⚠️ {cooling ? 'Cooling' : 'Heating'} selected. The other option is disabled.
          </div>
        )}
      </div>

      <Form.Item
        label="Max Cost (£)"
        name="maxCost"
      >
        <InputNumber
          min={0}
          step={1}
          style={{ width: '100%' }}
          placeholder="Optional"
          disabled={isLimitReached}
        />
      </Form.Item>

      <Space direction="horizontal" style={{ width: '100%' }} size="small">
        <Form.Item
          label="Longitude"
          name="lng"
          rules={[
            { required: true, message: 'Required' },
            { type: 'number', min: -180, max: 180, message: 'Invalid longitude' }
          ]}
          style={{ flex: 1, marginBottom: 12 }}
        >
          <InputNumber
            step={0.0001}
            style={{ width: '100%' }}
            disabled={isLimitReached}
            placeholder="-3.1883"
          />
        </Form.Item>
        <Form.Item
          label="Latitude"
          name="lat"
          rules={[
            { required: true, message: 'Required' },
            { type: 'number', min: -90, max: 90, message: 'Invalid latitude' }
          ]}
          style={{ flex: 1, marginBottom: 12 }}
        >
          <InputNumber
            step={0.0001}
            style={{ width: '100%' }}
            disabled={isLimitReached}
            placeholder="55.9445"
          />
        </Form.Item>
      </Space>

      <Form.Item>
        <Button
          type="primary"
          htmlType="submit"
          block
          disabled={isLimitReached}
        >
          {isLimitReached ? 'Maximum Orders Reached' : 'Add Order'}
        </Button>
      </Form.Item>
    </>
  );

  return (
    <>
      <Form
        form={form}
        layout="vertical"
        onFinish={handleSubmit}
        initialValues={{
          date: dayjs(),
          time: dayjs(),
          capacity: 1.0,
          cooling: false,
          heating: false,
          lng: -3.1883,
          lat: 55.9445,
        }}
      >
        <FormContent />
      </Form>

      <Modal
        title="📍 Add Order at Selected Location"
        open={isModalVisible}
        onCancel={handleModalCancel}
        footer={null}
        width={500}
      >
        <Alert
          message="Location Selected"
          description={clickedLocation ?
            `Longitude: ${clickedLocation.lng.toFixed(4)}, Latitude: ${clickedLocation.lat.toFixed(4)}` :
            ''
          }
          type="success"
          showIcon
          style={{ marginBottom: 16 }}
        />
        <Form
          form={form}
          layout="vertical"
          onFinish={handleSubmit}
        >
          <FormContent />
        </Form>
      </Modal>
    </>
  );
};

export default OrderForm;