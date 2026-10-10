import React, { useState, useMemo } from 'react';
import { AppSettings, Group, Place } from '../types';
import {
  calculateDistance,
  calculateBearing,
  bearingToCardinal,
  getProximityTier,
  formatAccuracy,
  formatCoordinates,
  formatDistance,
  getGoogleMapsUrl,
  createShareText,
} from '../utils/geoUtils';
import {
  downloadFile,
  generateCsv,
  generateGpx,
  generateJson,
  generateKml,
  parseImportContent,
} from '../utils/exportImport';
import { OsmMapView } from './OsmMapView';
import {
  Search,
  List,
  Map as MapIcon,
  Heart,
  Navigation,
  Navigation2,
  Share2,
  Trash2,
  Download,
  Upload,
  ArrowUpDown,
  Compass,
  FileCode,
  FileSpreadsheet,
  X,
  Plus,
  Check,
  LocateFixed,
  Clock,
  Calendar,
  ArrowDownAZ,
  ArrowUpAZ,
} from 'lucide-react';

export type SortOption =
  | 'distance-asc'
  | 'distance-desc'
  | 'date-desc'
  | 'date-asc'
  | 'name-asc'
  | 'name-desc';

function formatRelativeTime(timestamp: number): string {
  const diffMs = Date.now() - timestamp;
  const diffMins = Math.floor(diffMs / 60000);
  const diffHours = Math.floor(diffMins / 60);
  const diffDays = Math.floor(diffHours / 24);

  if (diffDays > 0) return `${diffDays}d ago`;
  if (diffHours > 0) return `${diffHours}h ago`;
  if (diffMins > 0) return `${diffMins}m ago`;
  return 'Just now';
}

interface PlacesScreenProps {
  places: Place[];
  groups: Group[];
  settings: AppSettings;
  currentLat: number;
  currentLng: number;
  onSelectPlace: (place: Place) => void;
  onToggleFavorite: (place: Place) => void;
  onDeletePlace: (id: number) => void;
  onImportPlaces: (imported: Partial<Place>[]) => void;
  onNavigateToSave: () => void;
}

export const PlacesScreen: React.FC<PlacesScreenProps> = ({
  places,
  groups,
  settings,
  currentLat,
  currentLng,
  onSelectPlace,
  onToggleFavorite,
  onDeletePlace,
  onImportPlaces,
  onNavigateToSave,
}) => {
  const [tab, setTab] = useState<'list' | 'map'>('list');
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedGroupId, setSelectedGroupId] = useState<number | null>(null);
  const [onlyFavorites, setOnlyFavorites] = useState(false);
  const [sortBy, setSortBy] = useState<SortOption>('distance-asc');

  const [showBackupModal, setShowBackupModal] = useState(false);
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  const showToast = (msg: string) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(null), 3000);
  };

  const groupMap = useMemo(() => new Map(groups.map((g) => [g.id, g])), [groups]);

  // Filtered & sorted places
  const filteredPlaces = useMemo(() => {
    return places
      .filter((place) => {
        // Group filter
        if (selectedGroupId !== null && place.groupId !== selectedGroupId) {
          return false;
        }
        // Favorite filter
        if (onlyFavorites && !place.isFavorite) {
          return false;
        }
        // Search query
        if (searchQuery.trim()) {
          const q = searchQuery.toLowerCase();
          const matchName = place.name.toLowerCase().includes(q);
          const matchAddr = place.address?.toLowerCase().includes(q) || false;
          const matchNote = place.note?.toLowerCase().includes(q) || false;
          if (!matchName && !matchAddr && !matchNote) return false;
        }
        return true;
      })
      .sort((a, b) => {
        if (sortBy === 'distance-asc') {
          const distA = calculateDistance(currentLat, currentLng, a.latitude, a.longitude);
          const distB = calculateDistance(currentLat, currentLng, b.latitude, b.longitude);
          return distA - distB;
        }
        if (sortBy === 'distance-desc') {
          const distA = calculateDistance(currentLat, currentLng, a.latitude, a.longitude);
          const distB = calculateDistance(currentLat, currentLng, b.latitude, b.longitude);
          return distB - distA;
        }
        if (sortBy === 'date-desc') {
          return b.createdAt - a.createdAt;
        }
        if (sortBy === 'date-asc') {
          return a.createdAt - b.createdAt;
        }
        if (sortBy === 'name-asc') {
          return a.name.localeCompare(b.name, undefined, { numeric: true, sensitivity: 'base' });
        }
        if (sortBy === 'name-desc') {
          return b.name.localeCompare(a.name, undefined, { numeric: true, sensitivity: 'base' });
        }
        return 0;
      });
  }, [places, selectedGroupId, onlyFavorites, searchQuery, sortBy, currentLat, currentLng]);

  const handleExport = (format: 'gpx' | 'kml' | 'csv' | 'json') => {
    const timeStamp = new Date().toISOString().replace(/[:.]/g, '-').slice(0, 19);
    if (format === 'gpx') {
      const gpx = generateGpx(places);
      downloadFile(gpx, `gps_places_${timeStamp}.gpx`, 'application/gpx+xml');
    } else if (format === 'kml') {
      const kml = generateKml(places);
      downloadFile(kml, `gps_places_${timeStamp}.kml`, 'application/vnd.google-earth.kml+xml');
    } else if (format === 'csv') {
      const csv = generateCsv(places, groups);
      downloadFile(csv, `gps_places_${timeStamp}.csv`, 'text/csv');
    } else if (format === 'json') {
      const json = generateJson(places, groups);
      downloadFile(json, `gps_places_${timeStamp}.json`, 'application/json');
    }
    showToast(`Exported ${places.length} places as ${format.toUpperCase()}`);
    setShowBackupModal(false);
  };

  const handleFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = (evt) => {
      const content = evt.target?.result as string;
      if (!content) return;
      const parsed = parseImportContent(content);
      if (parsed.places.length > 0) {
        onImportPlaces(parsed.places);
        showToast(`Successfully imported ${parsed.places.length} places!`);
        setShowBackupModal(false);
      } else {
        showToast('Could not parse any valid waypoints from file.');
      }
    };
    reader.readAsText(file);
  };

  return (
    <div className="flex flex-col h-[calc(100vh-64px)] bg-[#F8F9FF] select-none">
      {/* Toast */}
      {toastMessage && (
        <div className="fixed top-4 right-4 z-50 bg-slate-800 text-white text-xs font-semibold px-4 py-2.5 rounded-xl shadow-xl flex items-center gap-2 animate-in fade-in">
          <Check className="w-4 h-4 text-emerald-400" />
          <span>{toastMessage}</span>
        </div>
      )}

      {/* Top Header & Search Bar */}
      <div className="bg-white border-b border-slate-200/80 px-4 py-3 space-y-2.5 shrink-0">
        <div className="flex items-center justify-between gap-3">
          <div className="relative flex-1">
            <Search className="w-4 h-4 text-slate-400 absolute left-3 top-2.5" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Search places by name, address, notes..."
              className="w-full pl-9 pr-8 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs sm:text-sm text-slate-800 focus:outline-hidden focus:ring-2 focus:ring-[#0D47A1]"
            />
            {searchQuery && (
              <button
                onClick={() => setSearchQuery('')}
                className="absolute right-2.5 top-2.5 text-slate-400 hover:text-slate-600"
              >
                <X className="w-4 h-4" />
              </button>
            )}
          </div>

          {/* Backup / Export Button */}
          <button
            onClick={() => setShowBackupModal(true)}
            title="Backup & Export"
            className="p-2 border border-slate-200 text-slate-600 hover:text-[#0D47A1] hover:bg-blue-50/50 rounded-xl transition-colors cursor-pointer"
          >
            <Download className="w-4 h-4" />
          </button>

          {/* Tab Switcher: List vs Map */}
          <div className="flex bg-slate-100 p-0.5 rounded-xl border border-slate-200/60">
            <button
              onClick={() => setTab('list')}
              className={`p-1.5 rounded-lg text-xs font-semibold flex items-center gap-1 transition-all ${
                tab === 'list' ? 'bg-white text-[#0D47A1] shadow-xs' : 'text-slate-500 hover:text-slate-700'
              }`}
            >
              <List className="w-4 h-4" />
            </button>
            <button
              onClick={() => setTab('map')}
              className={`p-1.5 rounded-lg text-xs font-semibold flex items-center gap-1 transition-all ${
                tab === 'map' ? 'bg-white text-[#0D47A1] shadow-xs' : 'text-slate-500 hover:text-slate-700'
              }`}
            >
              <MapIcon className="w-4 h-4" />
            </button>
          </div>
        </div>

        {/* Filter Chips Bar */}
        <div className="flex items-center gap-1.5 overflow-x-auto pb-1 no-scrollbar text-xs">
          {/* All */}
          <button
            onClick={() => {
              setSelectedGroupId(null);
              setOnlyFavorites(false);
            }}
            className={`px-3 py-1 rounded-full font-semibold transition-all whitespace-nowrap ${
              selectedGroupId === null && !onlyFavorites
                ? 'bg-[#0D47A1] text-white shadow-xs'
                : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
            }`}
          >
            All ({places.length})
          </button>

          {/* Favorites */}
          <button
            onClick={() => setOnlyFavorites(!onlyFavorites)}
            className={`px-3 py-1 rounded-full font-semibold flex items-center gap-1.5 transition-all whitespace-nowrap ${
              onlyFavorites
                ? 'bg-rose-500 text-white shadow-xs'
                : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
            }`}
          >
            <Heart className={`w-3.5 h-3.5 ${onlyFavorites ? 'fill-current' : ''}`} />
            Favorites
          </button>

          {/* Category Chips */}
          {groups.map((group) => {
            const isSelected = selectedGroupId === group.id;
            return (
              <button
                key={group.id}
                onClick={() => setSelectedGroupId(isSelected ? null : group.id)}
                className={`px-3 py-1 rounded-full font-semibold flex items-center gap-1.5 transition-all whitespace-nowrap ${
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

        {/* Dedicated Sorting Control Bar */}
        <div className="flex items-center justify-between gap-2 pt-2 border-t border-slate-100 flex-wrap">
          <div className="flex items-center gap-1.5 overflow-x-auto no-scrollbar">
            <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider shrink-0 mr-0.5 flex items-center gap-1">
              <ArrowUpDown className="w-3 h-3 text-slate-400" />
              <span>Sort:</span>
            </span>

            {/* Proximity / Distance Sort Button */}
            <button
              onClick={() => {
                setSortBy((prev) => (prev === 'distance-asc' ? 'distance-desc' : 'distance-asc'));
              }}
              title="Click to toggle Nearest / Farthest"
              className={`px-2.5 py-1 rounded-xl text-xs font-semibold flex items-center gap-1.5 transition-all cursor-pointer whitespace-nowrap ${
                sortBy.startsWith('distance')
                  ? 'bg-[#0D47A1] text-white shadow-xs'
                  : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
              }`}
            >
              <Navigation2
                className={`w-3 h-3 transition-transform ${
                  sortBy === 'distance-desc' ? 'rotate-180' : ''
                }`}
              />
              <span>Proximity</span>
              <span className="text-[10px] opacity-80 font-mono">
                {sortBy === 'distance-desc' ? '↑ Far' : '↓ Near'}
              </span>
            </button>

            {/* Date Sort Button */}
            <button
              onClick={() => {
                setSortBy((prev) => (prev === 'date-desc' ? 'date-asc' : 'date-desc'));
              }}
              title="Click to toggle Newest / Oldest"
              className={`px-2.5 py-1 rounded-xl text-xs font-semibold flex items-center gap-1.5 transition-all cursor-pointer whitespace-nowrap ${
                sortBy.startsWith('date')
                  ? 'bg-[#0D47A1] text-white shadow-xs'
                  : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
              }`}
            >
              <Clock className="w-3 h-3" />
              <span>Date</span>
              <span className="text-[10px] opacity-80 font-mono">
                {sortBy === 'date-asc' ? '↑ Old' : '↓ New'}
              </span>
            </button>

            {/* Name Sort Button */}
            <button
              onClick={() => {
                setSortBy((prev) => (prev === 'name-asc' ? 'name-desc' : 'name-asc'));
              }}
              title="Click to toggle A-Z / Z-A"
              className={`px-2.5 py-1 rounded-xl text-xs font-semibold flex items-center gap-1.5 transition-all cursor-pointer whitespace-nowrap ${
                sortBy.startsWith('name')
                  ? 'bg-[#0D47A1] text-white shadow-xs'
                  : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
              }`}
            >
              <ArrowDownAZ className="w-3 h-3" />
              <span>Name</span>
              <span className="text-[10px] opacity-80 font-mono">
                {sortBy === 'name-desc' ? 'Z→A' : 'A→Z'}
              </span>
            </button>
          </div>

          {/* Full select dropdown */}
          <div className="ml-auto flex items-center gap-1 shrink-0 bg-slate-50 px-2 py-1 rounded-lg border border-slate-200/60">
            <select
              value={sortBy}
              onChange={(e) => setSortBy(e.target.value as SortOption)}
              className="bg-transparent text-slate-700 text-[11px] font-semibold focus:outline-hidden cursor-pointer"
            >
              <option value="distance-asc">Proximity: Nearest First</option>
              <option value="distance-desc">Proximity: Farthest First</option>
              <option value="date-desc">Date: Newest First</option>
              <option value="date-asc">Date: Oldest First</option>
              <option value="name-asc">Name: A → Z</option>
              <option value="name-desc">Name: Z → A</option>
            </select>
          </div>
        </div>
      </div>

      {/* Real-time GPS Location & Haversine Distance Banner */}
      <div className="flex items-center justify-between px-4 py-2 bg-gradient-to-r from-blue-50/90 to-indigo-50/90 border-b border-blue-100/70 text-xs text-[#0D47A1] shrink-0">
        <div className="flex items-center gap-2">
          <span className="relative flex h-2.5 w-2.5">
            <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
            <span className="relative inline-flex rounded-full h-2.5 w-2.5 bg-emerald-500"></span>
          </span>
          <span className="font-bold text-slate-800">Live GPS:</span>
          <span className="font-mono text-slate-600 font-semibold">
            {currentLat.toFixed(5)}°, {currentLng.toFixed(5)}°
          </span>
        </div>
        <div className="flex items-center gap-1.5 text-[11px] font-medium text-slate-500">
          <Compass className="w-3.5 h-3.5 text-[#0D47A1]" />
          <span>Real-time Haversine calculation</span>
        </div>
      </div>

      {/* Main Content: List Tab OR Map Tab */}
      <div className="flex-1 overflow-hidden relative">
        {tab === 'list' ? (
          <div className="h-full overflow-y-auto p-4 space-y-3 max-w-3xl mx-auto">
            {filteredPlaces.length === 0 ? (
              <div className="flex flex-col items-center justify-center py-16 px-4 text-center">
                <div className="w-16 h-16 rounded-3xl bg-blue-50 text-[#0D47A1] flex items-center justify-center mb-3">
                  <Compass className="w-8 h-8" />
                </div>
                <h3 className="text-base font-bold text-slate-800">No saved places yet</h3>
                <p className="text-xs text-slate-500 max-w-xs mt-1 mb-5">
                  Tap &lsquo;Save&rsquo; in the bottom bar to record your first GPS position offline.
                </p>
                <button
                  onClick={onNavigateToSave}
                  className="px-5 py-2.5 bg-[#0D47A1] text-white text-xs font-bold rounded-2xl shadow-lg shadow-blue-900/20 active:scale-95 transition-all"
                >
                  Go to One-Tap Save
                </button>
              </div>
            ) : (
              filteredPlaces.map((place) => {
                const group = place.groupId ? groupMap.get(place.groupId) : null;
                const distanceMeters = calculateDistance(
                  currentLat,
                  currentLng,
                  place.latitude,
                  place.longitude
                );
                const bearing = calculateBearing(
                  currentLat,
                  currentLng,
                  place.latitude,
                  place.longitude
                );
                const cardinal = bearingToCardinal(bearing);
                const proximity = getProximityTier(distanceMeters);

                return (
                  <div
                    key={place.id}
                    onClick={() => onSelectPlace(place)}
                    className="bg-white rounded-2xl p-4 shadow-xs border border-slate-200/80 hover:shadow-md hover:border-blue-200 transition-all cursor-pointer flex flex-col gap-2.5 group"
                  >
                    <div className="flex items-start justify-between gap-3">
                      <div className="flex items-start gap-3 min-w-0">
                        {/* Thumbnail or Category Dot */}
                        {place.photoUri ? (
                          <img
                            src={place.photoUri}
                            alt=""
                            className="w-12 h-12 rounded-xl object-cover shrink-0 border border-slate-200"
                          />
                        ) : (
                          <div
                            className="w-10 h-10 rounded-xl flex items-center justify-center shrink-0 text-white font-bold text-sm shadow-xs"
                            style={{ backgroundColor: group?.colorHex || '#1E88E5' }}
                          >
                            {place.name.charAt(0).toUpperCase()}
                          </div>
                        )}

                        <div className="min-w-0 flex-1">
                          <div className="flex items-center gap-2 flex-wrap">
                            <h3 className="text-sm font-bold text-slate-800 truncate">
                              {place.name}
                            </h3>
                            {group && (
                              <span
                                className="px-2 py-0.5 rounded-md text-[10px] font-semibold text-white shrink-0"
                                style={{ backgroundColor: group.colorHex }}
                              >
                                {group.name}
                              </span>
                            )}
                          </div>

                          {/* Coordinates */}
                          <div className="text-[11px] font-mono text-slate-500 mt-0.5">
                            {formatCoordinates(place.latitude, place.longitude, settings.coordinateFormat)}
                          </div>

                          {/* Live Distance & Bearing Badge */}
                          <div className="flex items-center gap-2 mt-1.5 flex-wrap">
                            <div className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-blue-50/90 border border-blue-200/80 text-[#0D47A1] font-bold text-xs shadow-2xs">
                              <Navigation2
                                className="w-3.5 h-3.5 text-[#0D47A1] shrink-0 transition-transform duration-300"
                                style={{ transform: `rotate(${bearing}deg)` }}
                              />
                              <span className="font-mono font-bold tracking-tight">
                                {formatDistance(distanceMeters, settings.unitSystem)}
                              </span>
                              <span className="text-[10px] text-blue-800/70 font-semibold">• {cardinal}</span>
                            </div>

                            <span
                              className={`text-[10px] font-semibold px-2 py-0.5 rounded-md border ${proximity.colorClass}`}
                            >
                              {proximity.label}
                            </span>
                          </div>
                        </div>
                      </div>

                      {/* Favorite Button */}
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          onToggleFavorite(place);
                        }}
                        className={`p-1.5 rounded-full transition-colors shrink-0 ${
                          place.isFavorite
                            ? 'text-rose-500 bg-rose-50'
                            : 'text-slate-300 hover:text-slate-500'
                        }`}
                      >
                        <Heart className={`w-4 h-4 ${place.isFavorite ? 'fill-current' : ''}`} />
                      </button>
                    </div>

                    {/* Address or Note Preview */}
                    {(place.address || place.note) && (
                      <p className="text-xs text-slate-600 line-clamp-1">
                        {place.address || place.note}
                      </p>
                    )}

                    {/* Meta info & Quick actions */}
                    <div className="flex items-center justify-between pt-2 border-t border-slate-100 text-[11px] text-slate-500 flex-wrap gap-2">
                      <div className="flex items-center gap-2.5 flex-wrap">
                        <span className="font-bold text-[#0D47A1] flex items-center gap-1 bg-blue-50/50 px-2 py-0.5 rounded-md border border-blue-100/60">
                          <Navigation2
                            className="w-3 h-3 text-[#0D47A1]"
                            style={{ transform: `rotate(${bearing}deg)` }}
                          />
                          {formatDistance(distanceMeters, settings.unitSystem)} ({Math.round(bearing)}° {cardinal})
                        </span>
                        <span className="text-slate-500 font-medium flex items-center gap-1">
                          <Clock className="w-3 h-3 text-slate-400" />
                          <span>{formatRelativeTime(place.createdAt)}</span>
                        </span>
                        <span>{formatAccuracy(place.accuracy, settings.unitSystem)}</span>
                      </div>

                      <div className="flex items-center gap-1 ml-auto">
                        <button
                          onClick={(e) => {
                            e.stopPropagation();
                            window.open(getGoogleMapsUrl(place.latitude, place.longitude), '_blank');
                          }}
                          title="Navigate"
                          className="p-1.5 text-slate-500 hover:text-[#0D47A1] hover:bg-blue-50 rounded-lg transition-colors"
                        >
                          <Navigation className="w-3.5 h-3.5" />
                        </button>
                        <button
                          onClick={(e) => {
                            e.stopPropagation();
                            const shareText = createShareText(place, settings.coordinateFormat);
                            navigator.clipboard.writeText(shareText);
                            showToast('Copied share text');
                          }}
                          title="Share"
                          className="p-1.5 text-slate-500 hover:text-slate-700 hover:bg-slate-100 rounded-lg transition-colors"
                        >
                          <Share2 className="w-3.5 h-3.5" />
                        </button>
                        <button
                          onClick={(e) => {
                            e.stopPropagation();
                            onDeletePlace(place.id);
                            showToast('Place deleted');
                          }}
                          title="Delete"
                          className="p-1.5 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded-lg transition-colors"
                        >
                          <Trash2 className="w-3.5 h-3.5" />
                        </button>
                      </div>
                    </div>
                  </div>
                );
              })
            )}
          </div>
        ) : (
          /* Map View Tab */
          <OsmMapView
            currentLat={currentLat}
            currentLng={currentLng}
            places={filteredPlaces}
            groups={groups}
            onSelectPlace={onSelectPlace}
            className="w-full h-full"
          />
        )}
      </div>

      {/* Backup & Export Modal */}
      {showBackupModal && (
        <div className="fixed inset-0 z-60 flex items-center justify-center p-4 bg-black/60 backdrop-blur-2xs">
          <div className="bg-white rounded-3xl p-6 max-w-sm w-full shadow-2xl space-y-4">
            <div className="flex items-center justify-between border-b border-slate-100 pb-3">
              <h3 className="text-base font-bold text-slate-800">Backup & Export Places</h3>
              <button
                onClick={() => setShowBackupModal(false)}
                className="text-slate-400 hover:text-slate-600"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="space-y-2">
              <button
                onClick={() => handleExport('gpx')}
                className="w-full p-3 rounded-2xl border border-slate-200 hover:border-[#0D47A1] hover:bg-blue-50/50 flex items-center justify-between text-left transition-all"
              >
                <div>
                  <div className="text-xs font-bold text-slate-800">Export as GPX</div>
                  <div className="text-[10px] text-slate-500">Universal GPS Exchange Format</div>
                </div>
                <FileCode className="w-5 h-5 text-[#0D47A1]" />
              </button>

              <button
                onClick={() => handleExport('kml')}
                className="w-full p-3 rounded-2xl border border-slate-200 hover:border-[#0D47A1] hover:bg-blue-50/50 flex items-center justify-between text-left transition-all"
              >
                <div>
                  <div className="text-xs font-bold text-slate-800">Export as KML</div>
                  <div className="text-[10px] text-slate-500">Google Earth / Maps placemarks</div>
                </div>
                <Compass className="w-5 h-5 text-emerald-600" />
              </button>

              <button
                onClick={() => handleExport('csv')}
                className="w-full p-3 rounded-2xl border border-slate-200 hover:border-[#0D47A1] hover:bg-blue-50/50 flex items-center justify-between text-left transition-all"
              >
                <div>
                  <div className="text-xs font-bold text-slate-800">Export as CSV</div>
                  <div className="text-[10px] text-slate-500">Excel / Spreadsheet tabular data</div>
                </div>
                <FileSpreadsheet className="w-5 h-5 text-amber-600" />
              </button>

              <button
                onClick={() => handleExport('json')}
                className="w-full p-3 rounded-2xl border border-slate-200 hover:border-[#0D47A1] hover:bg-blue-50/50 flex items-center justify-between text-left transition-all"
              >
                <div>
                  <div className="text-xs font-bold text-slate-800">Full JSON Backup</div>
                  <div className="text-[10px] text-slate-500">Includes categories, photos & tags</div>
                </div>
                <Download className="w-5 h-5 text-indigo-600" />
              </button>
            </div>

            <div className="pt-2 border-t border-slate-100">
              <label className="w-full py-3 px-4 rounded-2xl bg-slate-100 hover:bg-slate-200 text-slate-800 text-xs font-bold flex items-center justify-center gap-2 cursor-pointer transition-colors">
                <Upload className="w-4 h-4 text-[#0D47A1]" />
                <span>Import / Restore from File</span>
                <input
                  type="file"
                  accept=".gpx,.kml,.csv,.json,application/json,text/csv,text/xml"
                  onChange={handleFileUpload}
                  className="hidden"
                />
              </label>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
