import React, { useState, useEffect } from 'react';
import { AppSettings, Group, Place } from '../types';
import {
  formatCoordinates,
  formatAccuracy,
  formatDistance,
  calculateDistance,
  calculateBearing,
  bearingToCardinal,
  getProximityTier,
  getGoogleMapsUrl,
  createShareText,
} from '../utils/geoUtils';
import { reverseGeocode } from '../utils/geocoder';
import {
  X,
  MapPin,
  Compass,
  Phone,
  FileText,
  Camera,
  Share2,
  Navigation,
  Navigation2,
  Trash2,
  Check,
  Copy,
  RefreshCw,
  Plus,
  Heart,
  ExternalLink,
} from 'lucide-react';

interface PlaceDetailModalProps {
  place: Place;
  groups: Group[];
  settings: AppSettings;
  currentLat?: number;
  currentLng?: number;
  onClose: () => void;
  onUpdate: (updated: Place) => void;
  onDelete: (id: number) => void;
  onCreateGroup: (name: string, colorHex: string) => Group;
}

export const PlaceDetailModal: React.FC<PlaceDetailModalProps> = ({
  place,
  groups,
  settings,
  currentLat,
  currentLng,
  onClose,
  onUpdate,
  onDelete,
  onCreateGroup,
}) => {
  const [name, setName] = useState(place.name);
  const [address, setAddress] = useState(place.address || '');
  const [note, setNote] = useState(place.note || '');
  const [phone, setPhone] = useState(place.phone || '');
  const [groupId, setGroupId] = useState<number | null>(place.groupId ?? null);
  const [isFavorite, setIsFavorite] = useState(place.isFavorite);
  const [photoUri, setPhotoUri] = useState<string | null>(place.photoUri || null);

  const [isGeocoding, setIsGeocoding] = useState(false);
  const [copiedCoords, setCopiedCoords] = useState(false);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [showNewGroupModal, setShowNewGroupModal] = useState(false);
  const [newGroupName, setNewGroupName] = useState('');
  const [newGroupColor, setNewGroupColor] = useState('#1E88E5');
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  const showToast = (msg: string) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(null), 3000);
  };

  const handleCopyCoordinates = () => {
    const text = formatCoordinates(place.latitude, place.longitude, settings.coordinateFormat);
    navigator.clipboard.writeText(text);
    setCopiedCoords(true);
    showToast('Coordinates copied to clipboard');
    setTimeout(() => setCopiedCoords(false), 2000);
  };

  const handleRefreshAddress = async () => {
    setIsGeocoding(true);
    const resolved = await reverseGeocode(place.latitude, place.longitude);
    setIsGeocoding(false);
    if (resolved) {
      setAddress(resolved);
      showToast('Address updated via OpenStreetMap');
    } else {
      showToast('Could not resolve address (offline mode)');
    }
  };

  const handlePhotoUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      const reader = new FileReader();
      reader.onload = () => {
        setPhotoUri(reader.result as string);
        showToast('Photo attached');
      };
      reader.readAsDataURL(file);
    }
  };

  const handleSave = () => {
    const updated: Place = {
      ...place,
      name: name.trim() || place.name,
      address: address.trim() || null,
      note: note.trim() || null,
      phone: phone.trim() || null,
      groupId,
      isFavorite,
      photoUri,
      updatedAt: Date.now(),
    };
    onUpdate(updated);
    showToast('Changes saved');
    onClose();
  };

  const handleShare = async () => {
    const shareText = createShareText(
      { ...place, name, address, note, phone },
      settings.coordinateFormat
    );

    if (navigator.share) {
      try {
        await navigator.share({
          title: name,
          text: shareText,
          url: getGoogleMapsUrl(place.latitude, place.longitude),
        });
        return;
      } catch {
        // Fallback to clipboard
      }
    }

    navigator.clipboard.writeText(shareText);
    showToast('Share text copied to clipboard!');
  };

  const handleNavigate = () => {
    window.open(getGoogleMapsUrl(place.latitude, place.longitude), '_blank');
  };

  const handleCreateGroupSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newGroupName.trim()) return;
    const newG = onCreateGroup(newGroupName.trim(), newGroupColor);
    setGroupId(newG.id);
    setShowNewGroupModal(false);
    setNewGroupName('');
  };

  const currentGroup = groups.find((g) => g.id === groupId);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 bg-black/60 backdrop-blur-xs overflow-y-auto">
      <div className="relative w-full max-w-xl bg-white rounded-2xl shadow-2xl flex flex-col max-h-[92vh] overflow-hidden animate-in fade-in zoom-in-95 duration-200">
        {/* Top Header Bar */}
        <div className="flex items-center justify-between px-5 py-4 border-b border-slate-100 bg-white sticky top-0 z-10">
          <div className="flex items-center gap-2.5">
            <div
              className="w-4 h-4 rounded-full border border-white shadow-xs"
              style={{ backgroundColor: currentGroup?.colorHex || '#1E88E5' }}
            />
            <h2 className="text-lg font-bold text-slate-800">Place Details</h2>
          </div>
          <div className="flex items-center gap-1">
            <button
              onClick={() => setIsFavorite(!isFavorite)}
              className={`p-2 rounded-full transition-colors ${
                isFavorite ? 'text-rose-500 bg-rose-50' : 'text-slate-400 hover:bg-slate-100'
              }`}
              title={isFavorite ? 'Remove from favorites' : 'Add to favorites'}
            >
              <Heart className={`w-5 h-5 ${isFavorite ? 'fill-current' : ''}`} />
            </button>
            <button
              onClick={onClose}
              className="p-2 rounded-full text-slate-500 hover:bg-slate-100 transition-colors"
            >
              <X className="w-5 h-5" />
            </button>
          </div>
        </div>

        {/* Modal Body */}
        <div className="p-5 overflow-y-auto space-y-5">
          {/* Toast inside modal */}
          {toastMessage && (
            <div className="p-3 bg-emerald-50 text-emerald-800 text-xs font-medium rounded-xl border border-emerald-200 flex items-center gap-2 animate-in fade-in">
              <Check className="w-4 h-4 text-emerald-600 shrink-0" />
              <span>{toastMessage}</span>
            </div>
          )}

          {/* Photo Section */}
          <div className="space-y-2">
            <label className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
              Photo Attachment
            </label>
            {photoUri ? (
              <div className="relative rounded-xl overflow-hidden border border-slate-200 max-h-56 bg-slate-100 group">
                <img src={photoUri} alt={name} className="w-full h-48 object-cover" />
                <div className="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center gap-3">
                  <label className="px-3 py-1.5 bg-white text-slate-800 rounded-lg text-xs font-semibold cursor-pointer hover:bg-slate-100 shadow-md">
                    Change Photo
                    <input
                      type="file"
                      accept="image/*"
                      onChange={handlePhotoUpload}
                      className="hidden"
                    />
                  </label>
                  <button
                    onClick={() => setPhotoUri(null)}
                    className="px-3 py-1.5 bg-rose-600 text-white rounded-lg text-xs font-semibold hover:bg-rose-700 shadow-md"
                  >
                    Remove
                  </button>
                </div>
              </div>
            ) : (
              <label className="flex flex-col items-center justify-center p-6 border-2 border-dashed border-slate-200 rounded-xl hover:border-[#0D47A1] hover:bg-blue-50/30 transition-all cursor-pointer">
                <Camera className="w-7 h-7 text-slate-400 mb-2" />
                <span className="text-xs font-semibold text-[#0D47A1]">Add Photo</span>
                <span className="text-[11px] text-slate-400">Take photo or upload image</span>
                <input
                  type="file"
                  accept="image/*"
                  onChange={handlePhotoUpload}
                  className="hidden"
                />
              </label>
            )}
          </div>

          {/* Place Name */}
          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
              Place Name
            </label>
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 focus:outline-hidden focus:ring-2 focus:ring-[#0D47A1] focus:border-transparent text-slate-800 font-semibold"
              placeholder="e.g. Campsite Alpha"
            />
          </div>

          {/* Category Chip Selector */}
          <div className="space-y-1.5">
            <div className="flex items-center justify-between">
              <label className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
                Category
              </label>
              <button
                type="button"
                onClick={() => setShowNewGroupModal(true)}
                className="text-xs text-[#0D47A1] font-semibold flex items-center gap-1 hover:underline"
              >
                <Plus className="w-3.5 h-3.5" /> New Category
              </button>
            </div>
            <div className="flex flex-wrap gap-2 pt-1">
              <button
                type="button"
                onClick={() => setGroupId(null)}
                className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-all ${
                  groupId === null
                    ? 'bg-slate-800 text-white shadow-xs'
                    : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                }`}
              >
                Uncategorized
              </button>
              {groups.map((group) => {
                const isSelected = groupId === group.id;
                return (
                  <button
                    key={group.id}
                    type="button"
                    onClick={() => setGroupId(group.id)}
                    className={`px-3 py-1.5 rounded-lg text-xs font-medium flex items-center gap-1.5 transition-all ${
                      isSelected
                        ? 'text-white shadow-xs'
                        : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
                    }`}
                    style={isSelected ? { backgroundColor: group.colorHex } : {}}
                  >
                    <span
                      className="w-2 h-2 rounded-full"
                      style={{ backgroundColor: isSelected ? 'white' : group.colorHex }}
                    />
                    {group.name}
                  </button>
                );
              })}
            </div>
          </div>

          {/* GPS Coordinates Box */}
          <div className="p-4 bg-slate-50 rounded-xl border border-slate-200/80 space-y-3">
            <div className="flex items-center justify-between">
              <span className="text-xs font-bold text-slate-500 uppercase tracking-wider flex items-center gap-1.5">
                <Compass className="w-4 h-4 text-[#0D47A1]" /> GPS Coordinates
              </span>
              <button
                onClick={handleCopyCoordinates}
                className="text-xs text-[#0D47A1] font-semibold flex items-center gap-1 hover:underline active:scale-95 transition-transform"
              >
                {copiedCoords ? <Check className="w-3.5 h-3.5 text-emerald-600" /> : <Copy className="w-3.5 h-3.5" />}
                {copiedCoords ? 'Copied!' : 'Copy'}
              </button>
            </div>

            <div className="font-mono text-sm text-slate-800 font-semibold bg-white p-2.5 rounded-lg border border-slate-200">
              {formatCoordinates(place.latitude, place.longitude, settings.coordinateFormat)}
            </div>

            <div className="grid grid-cols-2 gap-2 text-xs text-slate-600">
              <div className="bg-white p-2 rounded-lg border border-slate-100">
                <span className="text-slate-400 block text-[10px]">ACCURACY</span>
                <span className="font-semibold">{formatAccuracy(place.accuracy, settings.unitSystem)}</span>
              </div>
              <div className="bg-white p-2 rounded-lg border border-slate-100">
                <span className="text-slate-400 block text-[10px]">ALTITUDE</span>
                <span className="font-semibold">
                  {place.altitude != null
                    ? settings.unitSystem === 'METRIC'
                      ? `${place.altitude.toFixed(1)} m`
                      : `${(place.altitude * 3.28084).toFixed(1)} ft`
                    : 'Unknown'}
                </span>
              </div>
            </div>

            {/* Real-time Haversine Distance & Bearing */}
            {currentLat != null && currentLng != null && (() => {
              const distanceMeters = calculateDistance(currentLat, currentLng, place.latitude, place.longitude);
              const bearing = calculateBearing(currentLat, currentLng, place.latitude, place.longitude);
              const cardinal = bearingToCardinal(bearing);
              const proximity = getProximityTier(distanceMeters);

              return (
                <div className="p-2.5 bg-blue-50/80 rounded-lg border border-blue-200/80 flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <div className="w-7 h-7 rounded-lg bg-[#0D47A1] text-white flex items-center justify-center shrink-0 shadow-2xs">
                      <Navigation2
                        className="w-3.5 h-3.5"
                        style={{ transform: `rotate(${bearing}deg)` }}
                      />
                    </div>
                    <div>
                      <div className="text-[10px] font-bold text-slate-500 uppercase leading-none">
                        Live Distance
                      </div>
                      <div className="text-xs font-bold text-[#0D47A1] mt-0.5">
                        {formatDistance(distanceMeters, settings.unitSystem)} • {cardinal} ({Math.round(bearing)}°)
                      </div>
                    </div>
                  </div>
                  <span className={`text-[10px] font-semibold px-2 py-0.5 rounded-md border ${proximity.colorClass}`}>
                    {proximity.label}
                  </span>
                </div>
              );
            })()}
          </div>

          {/* Address / Location Description */}
          <div className="space-y-1.5">
            <div className="flex items-center justify-between">
              <label className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
                Address / Location
              </label>
              <button
                type="button"
                onClick={handleRefreshAddress}
                disabled={isGeocoding}
                className="text-xs text-[#0D47A1] font-semibold flex items-center gap-1 hover:underline disabled:opacity-50"
              >
                <RefreshCw className={`w-3.5 h-3.5 ${isGeocoding ? 'animate-spin' : ''}`} />
                {isGeocoding ? 'Locating...' : 'Re-geocode'}
              </button>
            </div>
            <textarea
              rows={2}
              value={address}
              onChange={(e) => setAddress(e.target.value)}
              className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 focus:outline-hidden focus:ring-2 focus:ring-[#0D47A1] focus:border-transparent text-sm text-slate-700"
              placeholder="e.g. Street name, city, landmark..."
            />
          </div>

          {/* Phone Number */}
          <div className="space-y-1.5">
            <div className="flex items-center justify-between">
              <label className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
                Phone Number (Optional)
              </label>
              {phone && (
                <a
                  href={`tel:${phone}`}
                  className="text-xs text-emerald-600 font-semibold flex items-center gap-1 hover:underline"
                >
                  <Phone className="w-3 h-3" /> Call
                </a>
              )}
            </div>
            <div className="relative">
              <Phone className="w-4 h-4 text-slate-400 absolute left-3.5 top-3" />
              <input
                type="tel"
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                className="w-full pl-10 pr-3.5 py-2.5 rounded-xl border border-slate-200 focus:outline-hidden focus:ring-2 focus:ring-[#0D47A1] text-sm text-slate-800"
                placeholder="+1 234 567 8900"
              />
            </div>
          </div>

          {/* Notes & Details */}
          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
              Notes & Details
            </label>
            <textarea
              rows={3}
              value={note}
              onChange={(e) => setNote(e.target.value)}
              className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 focus:outline-hidden focus:ring-2 focus:ring-[#0D47A1] text-sm text-slate-700"
              placeholder="Add key notes, access gate codes, parking spot info, or reminders..."
            />
          </div>

          {/* Timestamps */}
          <div className="text-[11px] text-slate-400 pt-2 flex justify-between">
            <span>Saved: {new Date(place.createdAt).toLocaleString()}</span>
            <span>ID: #{place.id}</span>
          </div>
        </div>

        {/* Modal Footer Bar */}
        <div className="p-4 border-t border-slate-100 bg-slate-50 flex flex-wrap items-center justify-between gap-2">
          <div className="flex items-center gap-2">
            <button
              onClick={handleNavigate}
              className="px-3.5 py-2 rounded-xl bg-blue-100 text-[#0D47A1] hover:bg-blue-200 text-xs font-bold flex items-center gap-1.5 transition-colors"
            >
              <Navigation className="w-3.5 h-3.5" /> Navigate
            </button>
            <button
              onClick={handleShare}
              className="px-3.5 py-2 rounded-xl bg-slate-200 text-slate-700 hover:bg-slate-300 text-xs font-bold flex items-center gap-1.5 transition-colors"
            >
              <Share2 className="w-3.5 h-3.5" /> Share
            </button>
            <button
              onClick={() => setShowDeleteConfirm(true)}
              className="p-2 rounded-xl text-rose-600 hover:bg-rose-100 text-xs font-bold transition-colors"
              title="Delete Place"
            >
              <Trash2 className="w-4 h-4" />
            </button>
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={onClose}
              className="px-4 py-2 rounded-xl border border-slate-200 text-slate-600 hover:bg-white text-xs font-semibold transition-colors"
            >
              Cancel
            </button>
            <button
              onClick={handleSave}
              className="px-5 py-2 rounded-xl bg-[#0D47A1] text-white hover:bg-[#002F6E] text-xs font-bold shadow-md shadow-blue-900/20 active:scale-95 transition-all"
            >
              Save Changes
            </button>
          </div>
        </div>
      </div>

      {/* Delete Confirmation Dialog */}
      {showDeleteConfirm && (
        <div className="fixed inset-0 z-60 flex items-center justify-center p-4 bg-black/50 backdrop-blur-2xs">
          <div className="bg-white rounded-2xl p-6 max-w-sm w-full shadow-2xl space-y-4">
            <div className="w-12 h-12 rounded-full bg-rose-100 text-rose-600 flex items-center justify-center mx-auto">
              <Trash2 className="w-6 h-6" />
            </div>
            <div className="text-center">
              <h3 className="text-base font-bold text-slate-800">Delete this Place?</h3>
              <p className="text-xs text-slate-500 mt-1">
                Are you sure you want to delete &ldquo;{place.name}&rdquo;? This will remove its
                coordinates and photo from local storage.
              </p>
            </div>
            <div className="flex gap-2 pt-2">
              <button
                onClick={() => setShowDeleteConfirm(false)}
                className="flex-1 py-2.5 rounded-xl border border-slate-200 text-slate-700 text-xs font-bold hover:bg-slate-50"
              >
                Cancel
              </button>
              <button
                onClick={() => {
                  onDelete(place.id);
                  setShowDeleteConfirm(false);
                  onClose();
                }}
                className="flex-1 py-2.5 rounded-xl bg-rose-600 text-white text-xs font-bold hover:bg-rose-700 shadow-md"
              >
                Delete
              </button>
            </div>
          </div>
        </div>
      )}

      {/* New Category Modal */}
      {showNewGroupModal && (
        <div className="fixed inset-0 z-60 flex items-center justify-center p-4 bg-black/50 backdrop-blur-2xs">
          <form
            onSubmit={handleCreateGroupSubmit}
            className="bg-white rounded-2xl p-5 max-w-xs w-full shadow-2xl space-y-4"
          >
            <h3 className="text-sm font-bold text-slate-800">New Category</h3>
            <div>
              <label className="text-[11px] font-semibold text-slate-500 uppercase">Name</label>
              <input
                type="text"
                autoFocus
                value={newGroupName}
                onChange={(e) => setNewGroupName(e.target.value)}
                placeholder="e.g. Fishing Spots"
                className="w-full px-3 py-2 rounded-lg border border-slate-200 text-xs font-medium focus:ring-2 focus:ring-[#0D47A1] focus:outline-hidden"
              />
            </div>
            <div>
              <label className="text-[11px] font-semibold text-slate-500 uppercase">Color</label>
              <div className="flex gap-2 mt-1">
                {['#4CAF50', '#1E88E5', '#FB8C00', '#8E24AA', '#E53935', '#00ACC1', '#3949AB'].map(
                  (c) => (
                    <button
                      key={c}
                      type="button"
                      onClick={() => setNewGroupColor(c)}
                      className={`w-7 h-7 rounded-full border-2 transition-transform ${
                        newGroupColor === c ? 'scale-110 border-slate-800' : 'border-transparent'
                      }`}
                      style={{ backgroundColor: c }}
                    />
                  )
                )}
              </div>
            </div>
            <div className="flex gap-2 pt-2">
              <button
                type="button"
                onClick={() => setShowNewGroupModal(false)}
                className="flex-1 py-2 rounded-lg border border-slate-200 text-slate-600 text-xs font-semibold"
              >
                Cancel
              </button>
              <button
                type="submit"
                className="flex-1 py-2 rounded-lg bg-[#0D47A1] text-white text-xs font-bold shadow-md"
              >
                Create
              </button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
};
