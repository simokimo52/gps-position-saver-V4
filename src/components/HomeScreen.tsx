import React, { useState, useEffect } from 'react';
import { AppSettings, Group, Place } from '../types';
import { useGeolocation } from '../hooks/useGeolocation';
import { reverseGeocode } from '../utils/geocoder';
import { formatAccuracy } from '../utils/geoUtils';
import { OsmMapView } from './OsmMapView';
import {
  Bookmark,
  Radio,
  Crosshair,
  CheckCircle2,
  X,
  Compass,
  MapPin,
  Clock,
  Sparkles,
  CloudOff,
  PlusCircle,
  HelpCircle,
  ChevronDown,
  ChevronUp,
  Minimize2,
  Maximize2,
  Eye,
  AlertTriangle,
} from 'lucide-react';

interface HomeScreenProps {
  groups: Group[];
  settings: AppSettings;
  onSavePosition: (placeData: {
    name?: string;
    latitude: number;
    longitude: number;
    accuracy: number;
    altitude: number | null;
    address?: string | null;
    note?: string | null;
    groupId?: number | null;
  }) => Place;
  onNavigateToPlaceDetail: (place: Place) => void;
  onNavigateToLiveShare: () => void;
  savedPlaces: Place[];
}

export const HomeScreen: React.FC<HomeScreenProps> = ({
  groups,
  settings,
  onSavePosition,
  onNavigateToPlaceDetail,
  onNavigateToLiveShare,
  savedPlaces,
}) => {
  const {
    location,
    isLocating,
    elapsedSeconds,
    locatingStatus,
    permissionState,
    errorMessage,
    refreshLocation,
    acquireGpsLock,
  } = useGeolocation();

  const [resolvedAddress, setResolvedAddress] = useState<string | null>(null);
  const [selectedGroupId, setSelectedGroupId] = useState<number | null>(settings.defaultGroupId);
  const [quickNote, setQuickNote] = useState<string>('');
  const [savedSuccessPlace, setSavedSuccessPlace] = useState<Place | null>(null);
  const [showManualModal, setShowManualModal] = useState(false);
  const [showPermissionModal, setShowPermissionModal] = useState(false);
  const [manualLat, setManualLat] = useState('');
  const [manualLng, setManualLng] = useState('');
  const [manualName, setManualName] = useState('');
  const [isCardMinimized, setIsCardMinimized] = useState(false);

  // Reverse geocode when location coordinates change
  useEffect(() => {
    let isCancelled = false;
    reverseGeocode(location.latitude, location.longitude).then((addr) => {
      if (!isCancelled && addr) {
        setResolvedAddress(addr);
      }
    });
    return () => {
      isCancelled = true;
    };
  }, [location.latitude, location.longitude]);

  // Auto-dismiss success toast after 6 seconds
  useEffect(() => {
    if (savedSuccessPlace) {
      const timer = setTimeout(() => {
        setSavedSuccessPlace(null);
      }, 6000);
      return () => clearTimeout(timer);
    }
  }, [savedSuccessPlace]);

  const handleSaveCurrentPosition = async () => {
    try {
      // Acquire a fresh, real GPS fix. Never save fallback/demo coordinates as current location.
      const freshLoc = await acquireGpsLock();
      if (freshLoc.isSimulated) {
        window.alert(
          'GPS location is not available yet. Please allow Precise Location, turn on Location Services, and try again outdoors.'
        );
        return;
      }

      const timeStr = new Date().toLocaleTimeString('en-US', {
        hour12: false,
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
      });
      const defaultName = `Position (${timeStr})`;

      const saved = onSavePosition({
        name: defaultName,
        latitude: freshLoc.latitude,
        longitude: freshLoc.longitude,
        accuracy: freshLoc.accuracy,
        altitude: freshLoc.altitude,
        address: resolvedAddress,
        note: quickNote.trim() || null,
        groupId: selectedGroupId,
      });

      setSavedSuccessPlace(saved);
      setQuickNote('');
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Could not get your GPS location.';
      window.alert(message + '\n\nCheck that location permission is allowed and Location Services are enabled.');
    }
  };

  const handleSaveManualSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const lat = parseFloat(manualLat);
    const lng = parseFloat(manualLng);
    if (isNaN(lat) || isNaN(lng)) return;

    const saved = onSavePosition({
      name: manualName.trim() || `Manual Waypoint (${lat.toFixed(4)}, ${lng.toFixed(4)})`,
      latitude: lat,
      longitude: lng,
      accuracy: 1.0,
      altitude: null,
      address: null,
      note: null,
      groupId: selectedGroupId,
    });

    setShowManualModal(false);
    setManualLat('');
    setManualLng('');
    setManualName('');
    setSavedSuccessPlace(saved);
  };

  const isHighAccuracy = location.accuracy <= 15;

  return (
    <div className="relative w-full h-[calc(100vh-64px)] overflow-hidden bg-slate-900 select-none">
      {/* 1. FULLSCREEN INTERACTIVE OSM LEAFLET MAP */}
      <OsmMapView
        currentLat={location.latitude}
        currentLng={location.longitude}
        currentAccuracy={location.accuracy}
        places={savedPlaces}
        groups={groups}
        onSelectPlace={onNavigateToPlaceDetail}
        className="w-full h-full"
      />

      {/* 2. FLOATING TOP HEADER */}
      <div className="absolute top-4 left-4 right-4 z-[400] flex items-center justify-between pointer-events-none">
        {/* App Identity Badge */}
        <div className="pointer-events-auto bg-white/95 backdrop-blur-md rounded-2xl shadow-lg border border-slate-100/80 px-3.5 py-2 flex items-center gap-2.5">
          <img
            src="/app_logo_pin_1791573831039.jpg"
            alt="GPS Logo"
            className="w-8 h-8 rounded-lg object-cover shadow-xs"
            onError={(e) => {
              // fallback if not yet copied
              (e.target as HTMLElement).style.display = 'none';
            }}
          />
          <div>
            <h1 className="text-xs font-bold text-slate-800 leading-tight">GPS Position Saver</h1>
            <div className="flex items-center gap-1.5 text-[10px] text-slate-500 font-medium">
              <CloudOff className="w-3 h-3 text-[#0D47A1]" />
              <span>100% Offline GPS</span>
            </div>
          </div>
        </div>

        {/* Top Right Quick Actions */}
        <div className="pointer-events-auto flex items-center gap-2">
          <button
            onClick={() => setShowManualModal(true)}
            title="Add Manual Coordinates"
            className="w-10 h-10 bg-white/95 backdrop-blur-md text-slate-700 hover:text-[#0D47A1] rounded-full shadow-lg border border-slate-100 flex items-center justify-center active:scale-95 transition-all"
          >
            <PlusCircle className="w-5 h-5" />
          </button>
          <button
            onClick={() => refreshLocation()}
            title="Refresh GPS"
            className="w-10 h-10 bg-white/95 backdrop-blur-md text-[#0D47A1] rounded-full shadow-lg border border-slate-100 flex items-center justify-center active:scale-95 transition-all"
          >
            <Crosshair className="w-5 h-5" />
          </button>
        </div>
      </div>

      {/* 2.5 GPS PERMISSION / STATUS BANNER */}
      {(location.isSimulated || permissionState === 'denied') && (
        <div className="absolute top-18 left-4 right-4 z-[450] max-w-xl mx-auto animate-in slide-in-from-top-2 duration-200">
          <div className="bg-amber-500/95 backdrop-blur-md text-white px-3.5 py-2.5 rounded-2xl shadow-xl flex items-center justify-between gap-2.5 text-xs border border-amber-400">
            <div className="flex items-center gap-2 min-w-0">
              <AlertTriangle className="w-4 h-4 text-amber-100 shrink-0 animate-pulse" />
              <div className="truncate">
                <span className="font-bold">
                  {permissionState === 'denied' ? 'Permission Denied: ' : 'Live GPS Inactive: '}
                </span>
                <span className="text-amber-100 text-[11px]">
                  {permissionState === 'denied' ? 'Tap to authorize Android location' : 'Tap to acquire live satellites'}
                </span>
              </div>
            </div>
            <div className="flex items-center gap-1.5 shrink-0">
              <button
                onClick={() => refreshLocation()}
                className="px-2.5 py-1 bg-white text-amber-900 rounded-xl font-bold text-[11px] shadow-sm active:scale-95 transition-all cursor-pointer"
              >
                Enable GPS
              </button>
              <button
                onClick={() => setShowPermissionModal(true)}
                title="Help"
                className="p-1 bg-amber-600/60 hover:bg-amber-600 rounded-xl text-white text-[11px] cursor-pointer"
              >
                <HelpCircle className="w-4 h-4" />
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 3. FLOATING SUCCESS TOAST / HUD */}
      {savedSuccessPlace && (
        <div className="absolute top-20 left-4 right-4 z-[500] max-w-md mx-auto animate-in slide-in-from-top-4 duration-300">
          <div
            onClick={() => onNavigateToPlaceDetail(savedSuccessPlace)}
            className="bg-white rounded-2xl p-3.5 shadow-2xl border border-emerald-100 flex items-center justify-between gap-3 cursor-pointer hover:bg-slate-50 transition-colors"
          >
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-emerald-100 text-emerald-600 flex items-center justify-center shrink-0">
                <CheckCircle2 className="w-6 h-6" />
              </div>
              <div>
                <div className="text-xs font-bold text-slate-800">Location Saved!</div>
                <div className="text-xs font-mono font-semibold text-[#0D47A1]">
                  {savedSuccessPlace.latitude.toFixed(6)}, {savedSuccessPlace.longitude.toFixed(6)}
                </div>
                <div className="text-[10px] text-slate-400 mt-0.5">Tap to view & edit details</div>
              </div>
            </div>

            <button
              onClick={(e) => {
                e.stopPropagation();
                setSavedSuccessPlace(null);
              }}
              className="p-1.5 text-slate-400 hover:text-slate-600 rounded-lg hover:bg-slate-100"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        </div>
      )}

      {/* 4. FLOATING BOTTOM CARD OR MINIMIZED PILL */}
      <div className="absolute bottom-4 left-4 right-4 z-[400] max-w-xl mx-auto flex justify-center">
        {isCardMinimized ? (
          <button
            onClick={() => setIsCardMinimized(false)}
            className="bg-white/95 backdrop-blur-md text-[#0D47A1] px-5 py-3 rounded-full shadow-2xl border border-blue-100 flex items-center gap-2.5 font-bold text-xs sm:text-sm active:scale-95 transition-all cursor-pointer hover:bg-blue-50/80 animate-in fade-in slide-in-from-bottom-3 duration-200"
          >
            <MapPin className="w-4 h-4 text-[#0D47A1] animate-bounce" />
            <span>Show Save & Position Panel</span>
            <ChevronUp className="w-4 h-4 text-slate-400 ml-1" />
          </button>
        ) : (
          <div className="w-full bg-white/95 backdrop-blur-md rounded-3xl p-4 sm:p-5 shadow-2xl border border-slate-100/90 space-y-3.5 animate-in fade-in slide-in-from-bottom-3 duration-200">
            {/* Top row: Accuracy badge, Satellite Status & Minimize Button */}
            <div className="flex items-center justify-between">
              <div className="relative flex items-center gap-2 bg-blue-50/90 px-3 py-1.5 rounded-full border border-blue-200 shadow-sm animate-pulse">
                <span className="relative flex h-2.5 w-2.5">
                  <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
                  <span
                    className={`relative inline-flex rounded-full h-2.5 w-2.5 ${
                      isHighAccuracy ? 'bg-emerald-500' : 'bg-amber-500'
                    }`}
                  />
                </span>
                <span className="text-[11px] font-bold text-[#0D47A1]">
                  Radar • {formatAccuracy(location.accuracy, settings.unitSystem)}
                </span>
              </div>

              <div className="flex items-center gap-2">
                <div className="text-[11px] font-medium text-slate-500 hidden sm:flex items-center gap-1.5">
                  <Clock className="w-3.5 h-3.5 text-slate-400" />
                  <span>{isLocating ? `${elapsedSeconds}s • ${locatingStatus}` : location.statusText}</span>
                </div>

                <button
                  onClick={() => setIsCardMinimized(true)}
                  title="Minimize Panel to view full map"
                  className="p-1.5 text-slate-400 hover:text-slate-700 hover:bg-slate-100 rounded-xl transition-colors cursor-pointer"
                >
                  <Minimize2 className="w-4 h-4" />
                </button>
              </div>
            </div>

          {/* Coordinates Row */}
          <div className="flex items-center gap-2.5 text-slate-800">
            <MapPin className="w-4 h-4 text-[#0D47A1] shrink-0" />
            <div className="text-xs sm:text-sm font-semibold tracking-tight font-mono truncate">
              Latitude: {location.latitude.toFixed(6)} &nbsp; Longitude: {location.longitude.toFixed(6)}
            </div>
          </div>

          {/* Address Description */}
          <div className="flex items-start gap-2.5 text-slate-600 text-xs line-clamp-2">
            <Compass className="w-4 h-4 text-cyan-700 shrink-0 mt-0.5" />
            <span className="leading-tight text-slate-600">
              {resolvedAddress || (isLocating ? 'Acquiring GPS fix...' : 'Direct satellite coordinates')}
            </span>
          </div>

          {/* Category Chips Selector */}
          {groups.length > 0 && (
            <div className="flex items-center gap-1.5 overflow-x-auto pb-1 pt-0.5 no-scrollbar">
              <button
                type="button"
                onClick={() => setSelectedGroupId(null)}
                className={`px-3 py-1 rounded-full text-[11px] font-semibold whitespace-nowrap transition-all ${
                  selectedGroupId === null
                    ? 'bg-[#0D47A1] text-white shadow-xs'
                    : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                }`}
              >
                All
              </button>
              {groups.map((group) => {
                const isSelected = selectedGroupId === group.id;
                return (
                  <button
                    key={group.id}
                    type="button"
                    onClick={() => setSelectedGroupId(group.id)}
                    className={`px-3 py-1 rounded-full text-[11px] font-semibold whitespace-nowrap flex items-center gap-1.5 transition-all ${
                      isSelected
                        ? 'text-white shadow-xs'
                        : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
                    }`}
                    style={isSelected ? { backgroundColor: group.colorHex } : {}}
                  >
                    <span
                      className="w-1.5 h-1.5 rounded-full"
                      style={{ backgroundColor: isSelected ? 'white' : group.colorHex }}
                    />
                    {group.name}
                  </button>
                );
              })}
            </div>
          )}

          {/* Action Buttons Bar */}
          <div className="flex items-center gap-2.5 pt-1">
            {/* Primary Save Position Button */}
            <button
              onClick={handleSaveCurrentPosition}
              disabled={isLocating}
              className="flex-1 h-12 bg-[#0D47A1] hover:bg-[#002F6E] active:scale-[0.98] text-white rounded-2xl font-bold text-xs sm:text-sm flex items-center justify-center gap-2 shadow-lg shadow-blue-900/25 transition-all disabled:opacity-75 cursor-pointer"
            >
              {isLocating ? (
                <>
                  <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                  <span>Locking {elapsedSeconds}s...</span>
                </>
              ) : (
                <>
                  <Bookmark className="w-4 h-4 fill-current" />
                  <span>Save Position</span>
                </>
              )}
            </button>

            {/* Live Sharing Shortcut Button */}
            <button
              onClick={onNavigateToLiveShare}
              className="h-12 px-4 border border-blue-200 bg-blue-50/60 hover:bg-blue-100/60 text-[#0D47A1] active:scale-[0.98] rounded-2xl font-semibold text-xs sm:text-sm flex items-center justify-center gap-2 transition-all cursor-pointer whitespace-nowrap"
            >
              <Radio className="w-4 h-4 text-[#0D47A1]" />
              <span>Live Sharing</span>
            </button>
          </div>
        </div>
      )}
      </div>

      {/* Manual Coordinates Modal */}
      {showManualModal && (
        <div className="fixed inset-0 z-60 flex items-center justify-center p-4 bg-black/60 backdrop-blur-2xs">
          <form
            onSubmit={handleSaveManualSubmit}
            className="bg-white rounded-3xl p-5 max-w-sm w-full shadow-2xl space-y-4"
          >
            <div className="flex items-center justify-between border-b border-slate-100 pb-3">
              <h3 className="text-sm font-bold text-slate-800">Enter Coordinates Manually</h3>
              <button
                type="button"
                onClick={() => setShowManualModal(false)}
                className="text-slate-400 hover:text-slate-600"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="space-y-3">
              <div>
                <label className="text-[11px] font-semibold text-slate-500 uppercase">Place Name</label>
                <input
                  type="text"
                  value={manualName}
                  onChange={(e) => setManualName(e.target.value)}
                  placeholder="e.g. Secret Fishing Spot"
                  className="w-full px-3 py-2 rounded-xl border border-slate-200 text-xs font-medium focus:ring-2 focus:ring-[#0D47A1]"
                />
              </div>

              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label className="text-[11px] font-semibold text-slate-500 uppercase">Latitude</label>
                  <input
                    type="number"
                    step="any"
                    required
                    value={manualLat}
                    onChange={(e) => setManualLat(e.target.value)}
                    placeholder="48.8584"
                    className="w-full px-3 py-2 rounded-xl border border-slate-200 text-xs font-mono focus:ring-2 focus:ring-[#0D47A1]"
                  />
                </div>
                <div>
                  <label className="text-[11px] font-semibold text-slate-500 uppercase">Longitude</label>
                  <input
                    type="number"
                    step="any"
                    required
                    value={manualLng}
                    onChange={(e) => setManualLng(e.target.value)}
                    placeholder="2.2945"
                    className="w-full px-3 py-2 rounded-xl border border-slate-200 text-xs font-mono focus:ring-2 focus:ring-[#0D47A1]"
                  />
                </div>
              </div>
            </div>

            <div className="flex gap-2 pt-2">
              <button
                type="button"
                onClick={() => setShowManualModal(false)}
                className="flex-1 py-2.5 rounded-xl border border-slate-200 text-slate-600 text-xs font-semibold"
              >
                Cancel
              </button>
              <button
                type="submit"
                className="flex-1 py-2.5 rounded-xl bg-[#0D47A1] text-white text-xs font-bold shadow-md"
              >
                Save Waypoint
              </button>
            </div>
          </form>
        </div>
      )}

      {/* 6. GPS PERMISSION GUIDE MODAL */}
      {showPermissionModal && (
        <div className="fixed inset-0 z-[600] flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs">
          <div className="bg-white rounded-3xl p-5 max-w-sm w-full shadow-2xl space-y-4 animate-in fade-in zoom-in-95 duration-200">
            <div className="flex items-center justify-between pb-2 border-b border-slate-100">
              <div className="flex items-center gap-2">
                <div className="w-8 h-8 rounded-xl bg-amber-100 text-amber-700 flex items-center justify-center">
                  <MapPin className="w-4 h-4" />
                </div>
                <h3 className="font-bold text-slate-800 text-sm">Activer la Localisation GPS</h3>
              </div>
              <button
                onClick={() => setShowPermissionModal(false)}
                className="text-slate-400 hover:text-slate-600 p-1 cursor-pointer"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="space-y-2.5 text-xs text-slate-600 leading-relaxed">
              <div className="p-3 bg-blue-50/80 rounded-2xl border border-blue-100 space-y-1">
                <span className="font-bold text-[#0D47A1] block text-[11px]">
                  1. Autoriser dans Chrome / Navigateur:
                </span>
                <p className="text-slate-600 text-[11px]">
                  Cliqui 3la l-cadenas 🔒 (ou l'icône réglages 🎚️) f la barre d'adresse en haut, puis khtar <strong>Autoriser la position (Location: Allow)</strong>.
                </p>
              </div>

              <div className="p-3 bg-emerald-50/80 rounded-2xl border border-emerald-100 space-y-1">
                <span className="font-bold text-emerald-800 block text-[11px]">
                  2. Activer le GPS dans Android:
                </span>
                <p className="text-slate-600 text-[11px]">
                  Hebbet le menu rapide d'Android vers le bas et vérifie que l'icône <strong>Localisation (GPS)</strong> est bien allumée (Bleue).
                </p>
              </div>

              <div className="p-3 bg-indigo-50/80 rounded-2xl border border-indigo-100 space-y-1">
                <span className="font-bold text-indigo-900 block text-[11px]">
                  3. Précision Exacte (Precise Location):
                </span>
                <p className="text-slate-600 text-[11px]">
                  Dans les paramètres Android de Chrome, choisis <strong>&ldquo;Position exacte&rdquo;</strong> pour obtenir une précision de 3 à 5 mètres.
                </p>
              </div>
            </div>

            <div className="flex items-center gap-2 pt-1">
              <button
                onClick={async () => {
                  setShowPermissionModal(false);
                  await refreshLocation();
                }}
                className="w-full py-2.5 bg-[#0D47A1] hover:bg-blue-800 text-white rounded-xl text-xs font-bold shadow-md active:scale-95 transition-all cursor-pointer flex items-center justify-center gap-1.5"
              >
                <Crosshair className="w-4 h-4" />
                <span>Tester la Position GPS en Direct</span>
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
