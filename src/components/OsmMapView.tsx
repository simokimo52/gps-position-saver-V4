import React, { useEffect, useRef, useState } from 'react';
import L from 'leaflet';
import { Group, Place } from '../types';
import { Compass, Crosshair, MapPin, Layers, Globe, Check } from 'lucide-react';

export type MapLayerType = 'standard' | 'satellite';

interface OsmMapViewProps {
  currentLat: number;
  currentLng: number;
  currentAccuracy?: number;
  places: Place[];
  groups: Group[];
  selectedPlaceId?: number | null;
  onSelectPlace?: (place: Place) => void;
  className?: string;
  zoom?: number;
  defaultLayer?: MapLayerType;
}

export const OsmMapView: React.FC<OsmMapViewProps> = ({
  currentLat,
  currentLng,
  currentAccuracy,
  places,
  groups,
  selectedPlaceId,
  onSelectPlace,
  className = 'w-full h-full',
  zoom = 15,
  defaultLayer = 'standard',
}) => {
  const mapContainerRef = useRef<HTMLDivElement | null>(null);
  const mapInstanceRef = useRef<L.Map | null>(null);
  const tileLayerRef = useRef<L.TileLayer | null>(null);
  const markersLayerRef = useRef<L.LayerGroup | null>(null);
  const userMarkerRef = useRef<L.Marker | null>(null);
  const userCircleRef = useRef<L.Circle | null>(null);

  const [currentMapLayer, setCurrentMapLayer] = useState<MapLayerType>(() => {
    try {
      const saved = localStorage.getItem('gps_map_layer_preference');
      return saved === 'satellite' ? 'satellite' : 'standard';
    } catch {
      return 'standard';
    }
  });
  const [showLayerMenu, setShowLayerMenu] = useState(false);

  const groupColorMap = new Map(groups.map((g) => [g.id, g.colorHex]));

  // Initialize Map
  useEffect(() => {
    if (!mapContainerRef.current) return;

    if (!mapInstanceRef.current) {
      const map = L.map(mapContainerRef.current, {
        center: [currentLat, currentLng],
        zoom,
        zoomControl: false,
        attributionControl: false,
      });

      // Attribution small at bottom right
      L.control.attribution({ position: 'bottomright' }).addTo(map);

      markersLayerRef.current = L.layerGroup().addTo(map);
      mapInstanceRef.current = map;
    }

    return () => {
      if (mapInstanceRef.current) {
        mapInstanceRef.current.remove();
        mapInstanceRef.current = null;
      }
    };
  }, []);

  // Update base tile layer on layer switch
  useEffect(() => {
    const map = mapInstanceRef.current;
    if (!map) return;

    if (tileLayerRef.current) {
      map.removeLayer(tileLayerRef.current);
    }

    if (currentMapLayer === 'satellite') {
      tileLayerRef.current = L.tileLayer(
        'https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}',
        {
          maxZoom: 19,
          attribution: '&copy; Esri World Imagery',
        }
      ).addTo(map);
    } else {
      tileLayerRef.current = L.tileLayer(
        'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',
        {
          maxZoom: 19,
          attribution: '&copy; OpenStreetMap contributors',
        }
      ).addTo(map);
    }

    try {
      localStorage.setItem('gps_map_layer_preference', currentMapLayer);
    } catch {
      // ignore
    }
  }, [currentMapLayer]);

  // Update user location pin
  useEffect(() => {
    const map = mapInstanceRef.current;
    if (!map) return;

    const userLatLng = L.latLng(currentLat, currentLng);

    // Create user position marker if not exists
    if (!userMarkerRef.current) {
      const userIcon = L.divIcon({
        className: 'user-gps-marker',
        html: `
          <div style="position: relative; width: 24px; height: 24px; display: flex; align-items: center; justify-content: center;">
            <div style="position: absolute; width: 24px; height: 24px; border-radius: 50%; background: rgba(13, 71, 161, 0.25); animation: ping 2s cubic-bezier(0, 0, 0.2, 1) infinite;"></div>
            <div style="width: 14px; height: 14px; border-radius: 50%; background: #0D47A1; border: 2.5px solid #FFFFFF; box-shadow: 0 2px 4px rgba(0,0,0,0.3);"></div>
          </div>
        `,
        iconSize: [24, 24],
        iconAnchor: [12, 12],
      });

      userMarkerRef.current = L.marker(userLatLng, {
        icon: userIcon,
        zIndexOffset: 1000,
      }).addTo(map);

      if (currentAccuracy) {
        userCircleRef.current = L.circle(userLatLng, {
          radius: Math.min(100, Math.max(10, currentAccuracy)),
          color: '#0D47A1',
          weight: 1,
          fillColor: '#0D47A1',
          fillOpacity: 0.08,
        }).addTo(map);
      }
    } else {
      userMarkerRef.current.setLatLng(userLatLng);
      if (userCircleRef.current) {
        userCircleRef.current.setLatLng(userLatLng);
        if (currentAccuracy) {
          userCircleRef.current.setRadius(Math.min(100, Math.max(10, currentAccuracy)));
        }
      }
    }
  }, [currentLat, currentLng, currentAccuracy]);

  // Update places markers
  useEffect(() => {
    const map = mapInstanceRef.current;
    const layer = markersLayerRef.current;
    if (!map || !layer) return;

    layer.clearLayers();

    places.forEach((place) => {
      const color = (place.groupId && groupColorMap.get(place.groupId)) || '#1E88E5';
      const isSelected = selectedPlaceId === place.id;

      const pinIcon = L.divIcon({
        className: 'custom-pin-marker',
        html: `
          <div style="
            background-color: ${color};
            width: ${isSelected ? '36px' : '30px'};
            height: ${isSelected ? '36px' : '30px'};
            border-radius: 50% 50% 50% 0;
            transform: rotate(-45deg);
            display: flex;
            align-items: center;
            justify-content: center;
            border: 2px solid white;
            box-shadow: 0 3px 6px rgba(0,0,0,0.35);
            cursor: pointer;
            transition: all 0.2s ease;
          ">
            <div style="
              transform: rotate(45deg);
              color: white;
              font-size: 14px;
              font-weight: bold;
              display: flex;
              align-items: center;
              justify-content: center;
            ">
              ${place.isFavorite ? '★' : '•'}
            </div>
          </div>
        `,
        iconSize: isSelected ? [36, 36] : [30, 30],
        iconAnchor: isSelected ? [18, 36] : [15, 30],
        popupAnchor: [0, -32],
      });

      const marker = L.marker([place.latitude, place.longitude], { icon: pinIcon }).addTo(layer);

      // Popup Content
      const popupDiv = document.createElement('div');
      popupDiv.className = 'p-1 text-slate-800';
      popupDiv.innerHTML = `
        <div style="min-width: 170px;">
          <div style="font-weight: 700; font-size: 14px; margin-bottom: 2px; color: #0f172a;">${place.name}</div>
          <div style="font-size: 11px; color: #64748b; margin-bottom: 4px;">${place.latitude.toFixed(6)}, ${place.longitude.toFixed(6)}</div>
          ${place.address ? `<div style="font-size: 11px; color: #475569; margin-bottom: 6px; line-height: 1.2;">${place.address}</div>` : ''}
          <button id="btn-view-${place.id}" style="
            width: 100%;
            background: #0D47A1;
            color: white;
            border: none;
            padding: 5px 8px;
            border-radius: 6px;
            font-size: 11px;
            font-weight: 600;
            cursor: pointer;
            margin-top: 4px;
          ">
            View Details
          </button>
        </div>
      `;

      marker.bindPopup(popupDiv);

      popupDiv.querySelector(`#btn-view-${place.id}`)?.addEventListener('click', (e) => {
        e.stopPropagation();
        if (onSelectPlace) {
          onSelectPlace(place);
        }
      });

      marker.on('click', () => {
        if (onSelectPlace) {
          onSelectPlace(place);
        }
      });
    });
  }, [places, groups, selectedPlaceId, onSelectPlace]);

  const recenterOnGps = () => {
    if (mapInstanceRef.current) {
      mapInstanceRef.current.flyTo([currentLat, currentLng], 16.5, {
        animate: true,
        duration: 0.8,
      });
    }
  };

  const fitAllMarkers = () => {
    if (!mapInstanceRef.current) return;
    const points: [number, number][] = [[currentLat, currentLng]];
    places.forEach((p) => points.push([p.latitude, p.longitude]));

    if (points.length > 0) {
      const bounds = L.latLngBounds(points);
      mapInstanceRef.current.fitBounds(bounds, { padding: [50, 50], maxZoom: 16 });
    }
  };

  return (
    <div className={`relative ${className}`}>
      <div ref={mapContainerRef} className="w-full h-full z-0" />

      {/* Map Control Buttons floating on side */}
      <div className="absolute right-4 top-20 z-[400] flex flex-col gap-2 items-end">
        {/* Layer Switcher Toggle */}
        <div className="relative">
          <button
            onClick={() => setShowLayerMenu(!showLayerMenu)}
            title={`Current: ${currentMapLayer === 'satellite' ? 'Satellite View' : 'Standard OSM'}. Click to switch.`}
            className={`w-11 h-11 rounded-full shadow-lg flex items-center justify-center transition-all border ${
              currentMapLayer === 'satellite'
                ? 'bg-[#0D47A1] text-white border-blue-900 shadow-blue-900/30'
                : 'bg-white text-slate-700 hover:bg-slate-50 border-slate-100'
            }`}
          >
            <Layers className="w-5 h-5" />
          </button>

          {/* Layer Selector Popover Menu */}
          {showLayerMenu && (
            <div className="absolute right-14 top-0 bg-white rounded-2xl shadow-xl border border-slate-200/90 p-2 w-48 space-y-1 animate-in fade-in zoom-in-95 duration-150 z-50">
              <div className="text-[10px] font-bold text-slate-400 uppercase tracking-wider px-2 py-1">
                Map View Layer
              </div>

              {/* Standard OSM Option */}
              <button
                onClick={() => {
                  setCurrentMapLayer('standard');
                  setShowLayerMenu(false);
                }}
                className={`w-full flex items-center justify-between p-2 rounded-xl text-xs font-semibold transition-all ${
                  currentMapLayer === 'standard'
                    ? 'bg-blue-50 text-[#0D47A1] border border-blue-200/70'
                    : 'text-slate-700 hover:bg-slate-50'
                }`}
              >
                <div className="flex items-center gap-2">
                  <div className="w-6 h-6 rounded-lg bg-blue-100 text-[#0D47A1] flex items-center justify-center">
                    <MapPin className="w-3.5 h-3.5" />
                  </div>
                  <span>Standard Map</span>
                </div>
                {currentMapLayer === 'standard' && <Check className="w-4 h-4 text-[#0D47A1]" />}
              </button>

              {/* Satellite Option */}
              <button
                onClick={() => {
                  setCurrentMapLayer('satellite');
                  setShowLayerMenu(false);
                }}
                className={`w-full flex items-center justify-between p-2 rounded-xl text-xs font-semibold transition-all ${
                  currentMapLayer === 'satellite'
                    ? 'bg-blue-50 text-[#0D47A1] border border-blue-200/70'
                    : 'text-slate-700 hover:bg-slate-50'
                }`}
              >
                <div className="flex items-center gap-2">
                  <div className="w-6 h-6 rounded-lg bg-slate-800 text-white flex items-center justify-center">
                    <Globe className="w-3.5 h-3.5" />
                  </div>
                  <span>Satellite View</span>
                </div>
                {currentMapLayer === 'satellite' && <Check className="w-4 h-4 text-[#0D47A1]" />}
              </button>
            </div>
          )}
        </div>

        <button
          onClick={recenterOnGps}
          title="Center on GPS"
          className="w-11 h-11 bg-white text-[#0D47A1] rounded-full shadow-lg flex items-center justify-center hover:bg-slate-50 active:scale-95 transition-all border border-slate-100"
        >
          <Crosshair className="w-5 h-5" />
        </button>

        {places.length > 0 && (
          <button
            onClick={fitAllMarkers}
            title="Fit all saved places"
            className="w-11 h-11 bg-white text-slate-700 rounded-full shadow-lg flex items-center justify-center hover:bg-slate-50 active:scale-95 transition-all border border-slate-100"
          >
            <Compass className="w-5 h-5" />
          </button>
        )}
      </div>
    </div>
  );
};
