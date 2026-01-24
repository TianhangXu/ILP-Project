import { useEffect, useRef } from 'react';
import L from 'leaflet';
import { useMap } from 'react-leaflet';
import type { PathfindingProgress } from '../../types';

interface PathAnimationProps {
  progress: PathfindingProgress[];
  isCalculating: boolean;
}

const PathAnimation: React.FC<PathAnimationProps> = ({ progress, isCalculating }) => {
  const map = useMap();
  const layerRef = useRef<L.LayerGroup | null>(null);
  const lastProgressLength = useRef(0);
  const updateThrottleRef = useRef<NodeJS.Timeout | null>(null);

  useEffect(() => {
    if (!isCalculating) {
      if (layerRef.current) {
        layerRef.current.remove();
        layerRef.current = null;
      }
      lastProgressLength.current = 0;
      return;
    }

    if (updateThrottleRef.current) {
      clearTimeout(updateThrottleRef.current);
    }

    updateThrottleRef.current = setTimeout(() => {
      if (Math.abs(progress.length - lastProgressLength.current) < 20) {
        return;
      }

      const sampled = progress
        .filter((n) => n.type === "node_explored" && n.position)
        .slice(-600)
        .filter((_, i) => i % 15 === 0);

      // 移除旧图层
      if (layerRef.current) {
        layerRef.current.remove();
      }

      const layer = L.layerGroup([], { pane: 'overlayPane' });

      sampled.forEach((node) => {
        const c = L.circleMarker([node.position!.lat, node.position!.lng], {
          radius: 1,
          color: '#888',
          fillColor: '#888',
          fillOpacity: 0.08,
          weight: 0.5,
          opacity: 0.15,
          interactive: false,
          renderer: L.canvas()
        });
        layer.addLayer(c);
      });

      layer.addTo(map);
      layerRef.current = layer;
      lastProgressLength.current = progress.length;
    }, 200);

  }, [progress, isCalculating, map]);

  useEffect(() => {
    return () => {
      if (layerRef.current) {
        layerRef.current.remove();
      }
      if (updateThrottleRef.current) {
        clearTimeout(updateThrottleRef.current);
      }
    };
  }, []);

  return null;
};

export default PathAnimation;