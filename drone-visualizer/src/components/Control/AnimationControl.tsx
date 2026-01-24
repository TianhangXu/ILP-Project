import { Button, Slider, Space, Card } from 'antd';
import { PlayCircleOutlined, PauseCircleOutlined, ReloadOutlined } from '@ant-design/icons';

interface AnimationControlProps {
  isAnimating: boolean;
  speed: number;
  hasPath: boolean;
  onStart: () => void;
  onPause: () => void;
  onReset: () => void;
  onSpeedChange: (speed: number) => void;
}

const AnimationControl: React.FC<AnimationControlProps> = ({
  isAnimating,
  speed,
  hasPath,
  onStart,
  onPause,
  onReset,
  onSpeedChange,
}) => {
  return (
    <Card
      title="🎬 Animation Control"
      size="small"
      style={{ marginTop: 16 }}
    >
      <Space direction="vertical" style={{ width: '100%' }} size="middle">
        <Space wrap>
          <Button
            type="primary"
            icon={<PlayCircleOutlined />}
            onClick={onStart}
            disabled={isAnimating || !hasPath}
            size="small"
          >
            Start
          </Button>
          <Button
            icon={<PauseCircleOutlined />}
            onClick={onPause}
            disabled={!isAnimating}
            size="small"
          >
            Pause
          </Button>
          <Button
            icon={<ReloadOutlined />}
            onClick={onReset}
            disabled={!hasPath}
            size="small"
          >
            Reset
          </Button>
        </Space>

        <div>
          <div style={{
            display: 'flex',
            justifyContent: 'space-between',
            marginBottom: 8,
            fontSize: 12,
            color: '#666'
          }}>
            <span>Speed:</span>
            <span style={{ fontWeight: 'bold', color: '#1890ff' }}>{speed}x</span>
          </div>
          <Slider
            min={0.5}
            max={5}
            step={0.5}
            value={speed}
            onChange={onSpeedChange}
            marks={{
              0.5: '0.5x',
              1: '1x',
              2: '2x',
              5: '5x'
            }}
            disabled={!hasPath}
          />
        </div>

        {!hasPath && (
          <div style={{
            padding: '8px 12px',
            background: '#f0f0f0',
            borderRadius: 4,
            fontSize: 12,
            color: '#666',
            textAlign: 'center'
          }}>
            Calculate a path first to enable animation
          </div>
        )}
      </Space>
    </Card>
  );
};

export default AnimationControl;