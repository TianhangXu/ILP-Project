import { Polygon, Popup } from 'react-leaflet';
import type { RestrictedArea } from '../../types';

interface RestrictedAreasProps {
  areas: RestrictedArea[];
  showLabels?: boolean;
}

const RestrictedAreas: React.FC<RestrictedAreasProps> = ({
  areas,
  showLabels = true
}) => {
  if (!areas || areas.length === 0) {
    return null;
  }

  return (
    <>
      {areas.map((area) => {
        const positions = area.vertices.map(v => [v.lat, v.lng] as [number, number]);

        const getOpacity = () => {
          if (!area.limits) return 0.2;

          const range = area.limits.upper - area.limits.lower;
          if (range > 100) return 0.3;
          if (range > 50) return 0.25;
          return 0.2;
        };

        return (
          <Polygon
            key={area.id}
            positions={positions}
            pathOptions={{
              color: '#FF0000',
              fillColor: '#FF0000',
              fillOpacity: getOpacity(),
              weight: 2,
              opacity: 0.6,
            }}
            eventHandlers={{
              mouseover: (e) => {
                e.target.setStyle({
                  fillOpacity: getOpacity() + 0.2,
                  weight: 3,
                });
              },
              mouseout: (e) => {
                e.target.setStyle({
                  fillOpacity: getOpacity(),
                  weight: 2,
                });
              },
            }}
          >
            {showLabels && (
              <Popup>
                <div style={{ minWidth: 150 }}>
                  <strong style={{ color: '#FF0000' }}>⚠️ Restricted Area</strong>
                  <br />
                  <strong>Name:</strong> {area.name}
                  <br />
                  <strong>ID:</strong> {area.id}
                  <br />
                  {area.limits && (
                    <>
                      <strong>Altitude Limits:</strong>
                      <br />
                      Lower: {area.limits.lower}m
                      <br />
                      Upper: {area.limits.upper}m
                    </>
                  )}
                </div>
              </Popup>
            )}
          </Polygon>
        );
      })}
    </>
  );
};

export default RestrictedAreas;