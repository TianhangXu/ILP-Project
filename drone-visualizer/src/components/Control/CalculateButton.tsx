import { Button, Dropdown, Space, message } from 'antd';
import { CalculatorOutlined, DownOutlined, ThunderboltOutlined, StopOutlined } from '@ant-design/icons';
import type { MenuProps } from 'antd';
import type { MedDispatchRec } from '../../types';

interface CalculateButtonProps {
  orders: MedDispatchRec[];
  isCalculating: boolean;
  onCalculate: () => void;
  onCancelCalculation: () => void;
  onLoadScenario: (orders: MedDispatchRec[]) => void;
}

const CalculateButton: React.FC<CalculateButtonProps> = ({
  orders,
  isCalculating,
  onCalculate,
  onCancelCalculation,
  onLoadScenario,
}) => {
  const demoScenarios: Record<string, MedDispatchRec[]> = {
    simple: [
      {
        id: 101,
        date: '2025-11-12',
        time: '09:00',
        requirements: {
          capacity: 1.0,
          cooling: true,
          heating: false,
          maxCost: 35.0
        },
        delivery: {
          lng: -3.1900,
          lat: 55.9450
        }
      },
      {
        id: 102,
        date: '2025-11-12',
        time: '09:30',
        requirements: {
          capacity: 2.0,
          cooling: true,
          heating: false,
          maxCost: 40.0
        },
        delivery: {
          lng: -3.1850,
          lat: 55.9430
        }
      }
    ],
    complex: [
      {
        id: 1001,
        date: '2025-11-13',
        time: '09:30',
        delivery: {
          lng: -3.188,
          lat: 55.946
        },
        requirements: {
          capacity: 3.5,
          cooling: true,
          heating: false,
          maxCost: 25.0
        }
      },
      {
        id: 1002,
        date: '2025-11-13',
        time: '10:15',
        delivery: {
          lng: -3.192,
          lat: 55.943
        },
        requirements: {
          capacity: 7.0,
          cooling: false,
          heating: true,
          maxCost: 30.0
        }
      },
      {
        id: 1003,
        date: '2025-11-13',
        time: '11:00',
        delivery: {
          lng: -3.175,
          lat: 55.982
        },
        requirements: {
          capacity: 6.5,
          cooling: false,
          heating: true,
          maxCost: 40.0
        }
      },
      {
        id: 1004,
        date: '2025-11-13',
        time: '13:45',
        delivery: {
          lng: -3.189,
          lat: 55.945
        },
        requirements: {
          capacity: 4.2,
          cooling: true,
          heating: false,
          maxCost: 40.0
        }
      },
      {
        id: 1005,
        date: '2025-11-13',
        time: '14:20',
        delivery: {
          lng: -3.178,
          lat: 55.980
        },
        requirements: {
          capacity: 11.0,
          cooling: false,
          heating: false,
          maxCost: 50.0
        }
      },
      {
        id: 1006,
        date: '2025-11-13',
        time: '15:30',
        delivery: {
          lng: -3.187,
          lat: 55.944
        },
        requirements: {
          capacity: 2.8,
          cooling: false,
          heating: true,
          maxCost: 20.0
        }
      }
    ],
    multiDay: [
      {
        id: 501,
        date: '2025-11-17',
        time: '09:30',
        requirements: {
          capacity: 3.5,
          cooling: true,
          heating: false,
          maxCost: 28.0
        },
        delivery: {
          lng: -3.1860,
          lat: 55.9465
        }
      },
      {
        id: 502,
        date: '2025-11-17',
        time: '14:00',
        requirements: {
          capacity: 7.5,
          cooling: false,
          heating: true,
          maxCost: 32.0
        },
        delivery: {
          lng: -3.1920,
          lat: 55.9425
        }
      },
      {
        id: 503,
        date: '2025-11-18',
        time: '13:00',
        requirements: {
          capacity: 18.0,
          cooling: false,
          heating: false,
          maxCost: 55.0
        },
        delivery: {
          lng: -3.1845,
          lat: 55.9420
        }
      },
      {
        id: 504,
        date: '2025-11-18',
        time: '08:30',
        requirements: {
          capacity: 2.5,
          cooling: true,
          heating: false,
          maxCost: 22.0
        },
        delivery: {
          lng: -3.1910,
          lat: 55.9470
        }
      },
      {
        id: 505,
        date: '2025-11-19',
        time: '08:00',
        requirements: {
          capacity: 6.0,
          cooling: false,
          heating: true,
          maxCost: 26.0
        },
        delivery: {
          lng: -3.1865,
          lat: 55.9415
        }
      },
      {
        id: 506,
        date: '2025-11-19',
        time: '09:00',
        requirements: {
          capacity: 7.0,
          cooling: true,
          heating: false,
          maxCost: 38.0
        },
        delivery: {
          lng: -3.1925,
          lat: 55.9480
        }
      },
      {
        id: 507,
        date: '2025-11-20',
        time: '13:00',
        requirements: {
          capacity: 1.8,
          cooling: false,
          heating: true,
          maxCost: 19.0
        },
        delivery: {
          lng: -3.1855,
          lat: 55.9472
        }
      },
      {
        id: 508,
        date: '2025-11-20',
        time: '15:00',
        requirements: {
          capacity: 4.2,
          cooling: false,
          heating: false,
          maxCost: 24.0
        },
        delivery: {
          lng: -3.1840,
          lat: 55.9455
        }
      },
      {
        id: 509,
        date: '2025-11-21',
        time: '10:30',
        requirements: {
          capacity: 7.5,
          cooling: false,
          heating: true,
          maxCost: 35.0
        },
        delivery: {
          lng: -3.1915,
          lat: 55.9462
        }
      },
      {
        id: 510,
        date: '2025-11-21',
        time: '14:00',
        requirements: {
          capacity: 5.5,
          cooling: true,
          heating: false,
          maxCost: 29.0
        },
        delivery: {
          lng: -3.1870,
          lat: 55.9425
        }
      },
      {
        id: 511,
        date: '2025-11-22',
        time: '08:45',
        requirements: {
          capacity: 3.0,
          cooling: false,
          heating: true,
          maxCost: 21.0
        },
        delivery: {
          lng: -3.1850,
          lat: 55.9468
        }
      },
      {
        id: 512,
        date: '2025-11-22',
        time: '14:30',
        requirements: {
          capacity: 7.0,
          cooling: true,
          heating: false,
          maxCost: 30.0
        },
        delivery: {
          lng: -3.1895,
          lat: 55.9418
        }
      },
      {
        id: 513,
        date: '2025-11-23',
        time: '13:00',
        requirements: {
          capacity: 10.0,
          cooling: false,
          heating: false,
          maxCost: 40.0
        },
        delivery: {
          lng: -3.1835,
          lat: 55.9440
        }
      },
      {
        id: 514,
        date: '2025-11-23',
        time: '16:00',
        requirements: {
          capacity: 2.0,
          cooling: true,
          heating: false,
          maxCost: 20.0
        },
        delivery: {
          lng: -3.1905,
          lat: 55.9475
        }
      },
      {
        id: 515,
        date: '2025-11-24',
        time: '14:30',
        requirements: {
          capacity: 7.0,
          cooling: false,
          heating: true,
          maxCost: 33.0
        },
        delivery: {
          lng: -3.1880,
          lat: 55.9410
        }
      }
    ],
  };

  const menuItems: MenuProps['items'] = [
    {
      key: 'simple',
      label: 'Simple Scenario (2 orders)',
      icon: <ThunderboltOutlined />,
      onClick: () => {
        onLoadScenario(demoScenarios.simple);
        message.success('Loaded simple scenario with 2 orders');
      },
    },
    {
      key: 'complex',
      label: 'Complex Scenario (6 orders)',
      icon: <ThunderboltOutlined />,
      onClick: () => {
        onLoadScenario(demoScenarios.complex);
        message.success('Loaded complex scenario with 6 orders');
      },
    },
    {
      key: 'multiDay',
      label: 'Multi-Day Scenario (15 orders)',
      icon: <ThunderboltOutlined />,
      onClick: () => {
        onLoadScenario(demoScenarios.multiDay);
        message.success('Loaded multi-day scenario with 15 orders');
      },
    },
  ];

  return (
    <div style={{ marginTop: 20 }}>
      {!isCalculating ? (
        <Space.Compact block>
          <Button
            type="primary"
            size="large"
            icon={<CalculatorOutlined />}
            onClick={onCalculate}
            disabled={orders.length === 0}
            style={{ flex: 1 }}
          >
            Calculate Path
          </Button>
          <Dropdown menu={{ items: menuItems }} placement="bottomRight">
            <Button
              type="primary"
              size="large"
              icon={<DownOutlined />}
            />
          </Dropdown>
        </Space.Compact>
      ) : (
        <Button
          danger
          size="large"
          icon={<StopOutlined />}
          onClick={onCancelCalculation}
          block
          style={{
            animation: 'pulse 1.5s ease-in-out infinite',
          }}
        >
          Cancel Calculation
        </Button>
      )}

      <style>{`
        @keyframes pulse {
          0%, 100% {
            opacity: 1;
          }
          50% {
            opacity: 0.7;
          }
        }
      `}</style>
    </div>
  );
};

export default CalculateButton;