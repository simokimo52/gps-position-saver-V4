import React, { useState } from 'react';
import { AppSettings, CoordinateFormat, Group, Place, UnitSystem } from '../types';
import {
  formatCoordinates,
  formatAccuracy,
} from '../utils/geoUtils';
import {
  generateCsv,
  downloadFile,
} from '../utils/exportImport';
import {
  Settings as SettingsIcon,
  ListOrdered,
  Ruler,
  FolderPlus,
  ShieldCheck,
  Trash2,
  Check,
  ChevronRight,
  Plus,
  X,
  AlertTriangle,
  Info,
  FileSpreadsheet,
  Download,
  Smartphone,
  Loader2,
  QrCode,
  ExternalLink,
} from 'lucide-react';
import QRCode from 'qrcode';

interface SettingsScreenProps {
  settings: AppSettings;
  groups: Group[];
  places: Place[];
  onUpdateSettings: (newSettings: AppSettings) => void;
  onCreateGroup: (name: string, colorHex: string) => Group;
  onDeleteGroup: (id: number) => void;
  onClearAllData: () => void;
}

const COORD_FORMATS: { key: CoordinateFormat; displayName: string; example: string }[] = [
  {
    key: 'DECIMAL_DEGREES',
    displayName: 'Decimal Degrees (DD)',
    example: '33.573100° N, 7.589800° W',
  },
  {
    key: 'DEGREES_MINUTES_SECONDS',
    displayName: 'Degrees, Minutes, Seconds (DMS)',
    example: `33° 34' 23.2" N, 7° 35' 23.3" W`,
  },
  {
    key: 'DEGREES_DECIMAL_MINUTES',
    displayName: 'Degrees & Decimal Minutes (DDM)',
    example: `33° 34.386' N, 7° 35.388' W`,
  },
  {
    key: 'UTM',
    displayName: 'Universal Transverse Mercator (UTM)',
    example: 'Zone 29N 445250m E 3715060m N',
  },
];

const UNIT_OPTIONS: { key: UnitSystem; displayName: string; caption: string }[] = [
  {
    key: 'METRIC',
    displayName: 'Metric',
    caption: 'Meters (m), Kilometers (km)',
  },
  {
    key: 'IMPERIAL',
    displayName: 'Imperial',
    caption: 'Feet (ft), Miles (mi)',
  },
];

export const SettingsScreen: React.FC<SettingsScreenProps> = ({
  settings,
  groups,
  places,
  onUpdateSettings,
  onCreateGroup,
  onDeleteGroup,
  onClearAllData,
}) => {
  const [showFormatModal, setShowFormatModal] = useState(false);
  const [showUnitModal, setShowUnitModal] = useState(false);
  const [showDefaultGroupModal, setShowDefaultGroupModal] = useState(false);
  const [showClearConfirm, setShowClearConfirm] = useState(false);
  const [showNewGroupModal, setShowNewGroupModal] = useState(false);
  const [newGroupName, setNewGroupName] = useState('');
  const [newGroupColor, setNewGroupColor] = useState('#1E88E5');
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [showAppQr, setShowAppQr] = useState(false);
  const [appQrDataUrl, setAppQrDataUrl] = useState<string | null>(null);

  const showToast = (msg: string) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(null), 3000);
  };

  const handleCopyLink = async () => {
    try {
      await navigator.clipboard.writeText(window.location.origin);
      showToast('App link copied to clipboard!');
    } catch {
      showToast('Open this page in phone browser');
    }
  };

  const handleShowAppQr = async () => {
    setShowAppQr(true);
    if (!appQrDataUrl) {
      try {
        const fullUrl = window.location.origin;
        const dataUrl = await QRCode.toDataURL(fullUrl, {
          margin: 2,
          width: 256,
          color: {
            dark: '#0D47A1',
            light: '#FFFFFF',
          },
        });
        setAppQrDataUrl(dataUrl);
      } catch (e) {
        console.error(e);
      }
    }
  };

  const handleCreateGroupSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newGroupName.trim()) return;
    onCreateGroup(newGroupName.trim(), newGroupColor);
    setNewGroupName('');
    setShowNewGroupModal(false);
    showToast('Category created');
  };

  const handleExportCsv = () => {
    if (places.length === 0) {
      showToast('No saved places to export');
      return;
    }
    const csv = generateCsv(places, groups);
    const dateStr = new Date().toISOString().slice(0, 10);
    const timeStr = new Date().toTimeString().slice(0, 8).replace(/:/g, '');
    const filename = `gps_places_${dateStr}_${timeStr}.csv`;
    downloadFile(csv, filename, 'text/csv');
    showToast(`Exported ${places.length} places to CSV!`);
  };

  const currentFormatObj =
    COORD_FORMATS.find((f) => f.key === settings.coordinateFormat) || COORD_FORMATS[0];
  const currentUnitObj =
    UNIT_OPTIONS.find((u) => u.key === settings.unitSystem) || UNIT_OPTIONS[0];
  const defaultGroup = groups.find((g) => g.id === settings.defaultGroupId);

  return (
    <div className="flex flex-col h-[calc(100vh-64px)] bg-[#F8F9FF] overflow-y-auto p-4 max-w-xl mx-auto space-y-6 select-none">
      {/* Toast */}
      {toastMessage && (
        <div className="fixed top-4 right-4 z-50 bg-slate-800 text-white text-xs font-semibold px-4 py-2.5 rounded-xl shadow-xl flex items-center gap-2 animate-in fade-in">
          <Check className="w-4 h-4 text-emerald-400" />
          <span>{toastMessage}</span>
        </div>
      )}

      {/* Screen Title */}
      <div className="pt-2 flex items-center gap-3">
        <img
          src="/app_logo_pin_1791573831039.jpg"
          alt="GPS Logo"
          className="w-10 h-10 rounded-xl object-cover shadow-sm border border-slate-200/80"
          onError={(e) => {
            (e.target as HTMLElement).style.display = 'none';
          }}
        />
        <div>
          <h2 className="text-lg font-bold text-slate-800 leading-tight">Settings & Privacy</h2>
          <p className="text-xs text-slate-500 mt-0.5">Customize display formats and local data</p>
        </div>
      </div>

      {/* SECTION 1: COORDINATES & UNITS */}
      <div className="space-y-2">
        <span className="text-[11px] font-bold text-slate-400 uppercase tracking-wider px-1">
          Coordinates & Units
        </span>

        <div className="bg-white rounded-3xl p-2 shadow-xs border border-slate-200/80 divide-y divide-slate-100">
          {/* Coordinate Format Row */}
          <button
            onClick={() => setShowFormatModal(true)}
            className="w-full p-3.5 flex items-center justify-between text-left hover:bg-slate-50 rounded-2xl transition-colors cursor-pointer"
          >
            <div className="flex items-center gap-3">
              <div className="w-9 h-9 rounded-xl bg-blue-50 text-[#0D47A1] flex items-center justify-center shrink-0">
                <ListOrdered className="w-4 h-4" />
              </div>
              <div>
                <div className="text-xs font-bold text-slate-800">Coordinate Format</div>
                <div className="text-[11px] text-slate-500">{currentFormatObj.displayName}</div>
                <div className="text-[10px] text-slate-400 font-mono mt-0.5">
                  {currentFormatObj.example}
                </div>
              </div>
            </div>
            <ChevronRight className="w-4 h-4 text-slate-400 shrink-0" />
          </button>

          {/* Unit System Row */}
          <button
            onClick={() => setShowUnitModal(true)}
            className="w-full p-3.5 flex items-center justify-between text-left hover:bg-slate-50 rounded-2xl transition-colors cursor-pointer"
          >
            <div className="flex items-center gap-3">
              <div className="w-9 h-9 rounded-xl bg-cyan-50 text-cyan-700 flex items-center justify-center shrink-0">
                <Ruler className="w-4 h-4" />
              </div>
              <div>
                <div className="text-xs font-bold text-slate-800">Distance & Altitude Units</div>
                <div className="text-[11px] text-slate-500">{currentUnitObj.displayName}</div>
                <div className="text-[10px] text-slate-400">{currentUnitObj.caption}</div>
              </div>
            </div>
            <ChevronRight className="w-4 h-4 text-slate-400 shrink-0" />
          </button>

          {/* Default Category Row */}
          <button
            onClick={() => setShowDefaultGroupModal(true)}
            className="w-full p-3.5 flex items-center justify-between text-left hover:bg-slate-50 rounded-2xl transition-colors cursor-pointer"
          >
            <div className="flex items-center gap-3">
              <div className="w-9 h-9 rounded-xl bg-indigo-50 text-indigo-700 flex items-center justify-center shrink-0">
                <FolderPlus className="w-4 h-4" />
              </div>
              <div>
                <div className="text-xs font-bold text-slate-800">Default Category on Save</div>
                <div className="text-[11px] text-slate-500">
                  {defaultGroup ? defaultGroup.name : 'None (Uncategorized)'}
                </div>
              </div>
            </div>
            <ChevronRight className="w-4 h-4 text-slate-400 shrink-0" />
          </button>
        </div>
      </div>

      {/* SECTION 2: CATEGORY MANAGEMENT */}
      <div className="space-y-2">
        <div className="flex items-center justify-between px-1">
          <span className="text-[11px] font-bold text-slate-400 uppercase tracking-wider">
            Category Management ({groups.length})
          </span>
          <button
            onClick={() => setShowNewGroupModal(true)}
            className="text-xs text-[#0D47A1] font-bold flex items-center gap-1 hover:underline"
          >
            <Plus className="w-3.5 h-3.5" /> Add Category
          </button>
        </div>

        <div className="bg-white rounded-3xl p-3 shadow-xs border border-slate-200/80 space-y-1.5">
          {groups.map((group) => {
            const count = places.filter((p) => p.groupId === group.id).length;
            return (
              <div
                key={group.id}
                className="p-2.5 rounded-2xl hover:bg-slate-50 flex items-center justify-between transition-colors"
              >
                <div className="flex items-center gap-2.5">
                  <div
                    className="w-3.5 h-3.5 rounded-full shrink-0 shadow-2xs"
                    style={{ backgroundColor: group.colorHex }}
                  />
                  <span className="text-xs font-semibold text-slate-800">{group.name}</span>
                  <span className="text-[10px] text-slate-400">({count} places)</span>
                </div>

                <button
                  onClick={() => {
                    onDeleteGroup(group.id);
                    showToast(`Deleted category ${group.name}`);
                  }}
                  className="p-1.5 text-slate-300 hover:text-rose-600 rounded-lg transition-colors"
                  title="Delete category"
                >
                  <Trash2 className="w-3.5 h-3.5" />
                </button>
              </div>
            );
          })}
        </div>
      </div>

      {/* SECTION 3: BACKUP & DATA EXPORT */}
      <div className="space-y-2">
        <span className="text-[11px] font-bold text-slate-400 uppercase tracking-wider px-1">
          Backup & Export
        </span>

        <div className="bg-white rounded-3xl p-3.5 shadow-xs border border-slate-200/80 space-y-3">
          {/* Mobile App Installation Card */}
          <div className="p-4 bg-emerald-50/80 rounded-2xl border border-emerald-200/80 space-y-3">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
              <div className="flex items-start gap-3 min-w-0">
                <div className="w-10 h-10 rounded-xl bg-emerald-600 text-white flex items-center justify-center shrink-0 shadow-xs mt-0.5 sm:mt-0">
                  <Smartphone className="w-5 h-5" />
                </div>
                <div className="min-w-0 flex-1">
                  <div className="text-xs font-bold text-slate-800 flex items-center gap-2 flex-wrap">
                    <span>Install on Phone (Mobile App)</span>
                    <span className="text-[10px] px-2 py-0.5 bg-emerald-200/80 text-emerald-950 rounded-full font-bold">
                      Offline-First • Real GPS
                    </span>
                  </div>
                  <div className="text-[11px] text-slate-600 mt-0.5">
                    Fastest way to install without APK certificate errors
                  </div>
                </div>
              </div>

              <div className="flex items-center gap-2 shrink-0 self-end sm:self-auto">
                <button
                  onClick={handleShowAppQr}
                  title="Scan QR Code to open on phone"
                  className="px-3 py-2 bg-white text-slate-700 hover:text-[#0D47A1] hover:bg-blue-50 rounded-xl border border-slate-200 text-xs font-bold transition-colors flex items-center gap-1.5 cursor-pointer shadow-2xs"
                >
                  <QrCode className="w-4 h-4 text-[#0D47A1]" />
                  <span>QR Code</span>
                </button>

                <button
                  onClick={handleCopyLink}
                  className="px-3.5 py-2 bg-emerald-600 hover:bg-emerald-700 active:scale-95 text-white text-xs font-bold rounded-xl shadow-xs transition-all flex items-center gap-1.5 cursor-pointer"
                >
                  <Download className="w-3.5 h-3.5" />
                  <span>Copy App Link</span>
                </button>
              </div>
            </div>

            <div className="p-2.5 bg-white/90 rounded-xl border border-emerald-200/60 text-[11px] text-slate-600 space-y-1.5">
              <div className="font-semibold text-emerald-800 flex items-center gap-1.5">
                <Check className="w-3.5 h-3.5 text-emerald-600 shrink-0" />
                <span>1-Tap Instant Installation on Android:</span>
              </div>
              <div className="text-slate-500 leading-relaxed">
                1. Open this app link in <strong>Chrome</strong> on your phone (scan QR or paste link).
              </div>
              <div className="text-slate-500 leading-relaxed">
                2. Tap the Chrome menu (<strong>⋮</strong> three dots) and select <strong>&ldquo;Add to Home screen&rdquo;</strong> or <strong>&ldquo;Install app&rdquo;</strong>.
              </div>
              <div className="text-slate-500 leading-relaxed">
                3. The app is permanently installed on your home screen with offline maps, GPS lock, and zero errors!
              </div>
            </div>
          </div>

          {/* CSV Export Card */}
          <div className="p-4 bg-amber-50/70 rounded-2xl border border-amber-200/70 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
            <div className="flex items-center gap-3 min-w-0">
              <div className="w-10 h-10 rounded-xl bg-amber-100 text-amber-700 flex items-center justify-center shrink-0 shadow-2xs">
                <FileSpreadsheet className="w-5 h-5" />
              </div>
              <div className="min-w-0">
                <div className="text-xs font-bold text-slate-800 flex items-center gap-2 flex-wrap">
                  <span>Export Places as CSV</span>
                  <span className="text-[10px] px-2 py-0.5 bg-amber-200/70 text-amber-900 rounded-full font-semibold">
                    {places.length} {places.length === 1 ? 'place' : 'places'}
                  </span>
                </div>
                <div className="text-[11px] text-slate-500 mt-0.5">
                  Spreadsheet format for Excel, Google Sheets & GIS
                </div>
              </div>
            </div>

            <button
              onClick={handleExportCsv}
              disabled={places.length === 0}
              className="px-3.5 py-2 bg-amber-600 hover:bg-amber-700 active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed text-white text-xs font-bold rounded-xl shadow-xs transition-all shrink-0 flex items-center justify-center gap-1.5 cursor-pointer self-end sm:self-auto"
            >
              <Download className="w-3.5 h-3.5" />
              <span>Export CSV</span>
            </button>
          </div>

          <p className="text-[11px] text-slate-400 px-1 leading-relaxed">
            Exports tabular data with coordinates, altitude, accuracy, address, phone, notes, category, and timestamps.
          </p>
        </div>
      </div>

      {/* SECTION 4: PRIVACY & DATA SAFETY */}
      <div className="space-y-2">
        <span className="text-[11px] font-bold text-slate-400 uppercase tracking-wider px-1">
          Privacy & Storage
        </span>

        <div className="bg-white rounded-3xl p-4 shadow-xs border border-slate-200/80 space-y-3.5">
          <div className="flex items-start gap-3">
            <ShieldCheck className="w-5 h-5 text-emerald-600 shrink-0 mt-0.5" />
            <div>
              <div className="text-xs font-bold text-slate-800">100% On-Device Privacy</div>
              <p className="text-[11px] text-slate-500 leading-relaxed mt-0.5">
                All saved waypoints, notes, coordinates, and images remain strictly on your local
                device storage. No analytics, tracking, or remote database synchronization.
              </p>
            </div>
          </div>

          <div className="pt-3 border-t border-slate-100 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
            <div>
              <div className="text-xs font-bold text-rose-600">Clear All Data</div>
              <div className="text-[10px] text-slate-400 mt-0.5">
                Permanently delete all {places.length} saved waypoints
              </div>
            </div>

            <button
              onClick={() => setShowClearConfirm(true)}
              className="px-4 py-2 bg-rose-50 hover:bg-rose-100 text-rose-600 text-xs font-bold rounded-xl border border-rose-200 transition-colors self-end sm:self-auto cursor-pointer"
            >
              Clear All
            </button>
          </div>
        </div>
      </div>

      {/* App Info Footer & User Signature */}
      <div className="text-center py-6 text-slate-400 text-[11px] space-y-2">
        <div className="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-full bg-blue-50 border border-blue-100 text-[#0D47A1] text-xs font-bold shadow-2xs">
          <span>Dev By TZE45 with love ❤️</span>
        </div>
        <div className="text-slate-400 text-[11px]">GPS Position Saver • Web Edition v3.0</div>
        <div className="text-slate-400 text-[10px]">Offline Satellite Navigation & Direct Waypoint Logger</div>
      </div>

      {/* Format Modal */}
      {showFormatModal && (
        <div className="fixed inset-0 z-60 flex items-center justify-center p-4 bg-black/60 backdrop-blur-2xs">
          <div className="bg-white rounded-3xl p-5 max-w-sm w-full shadow-2xl space-y-3">
            <div className="flex items-center justify-between border-b border-slate-100 pb-2">
              <h3 className="text-sm font-bold text-slate-800">Coordinate Format</h3>
              <button onClick={() => setShowFormatModal(false)} className="text-slate-400">
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="space-y-2">
              {COORD_FORMATS.map((fmt) => {
                const isSelected = settings.coordinateFormat === fmt.key;
                return (
                  <button
                    key={fmt.key}
                    onClick={() => {
                      onUpdateSettings({ ...settings, coordinateFormat: fmt.key });
                      setShowFormatModal(false);
                      showToast(`Format set to ${fmt.displayName}`);
                    }}
                    className={`w-full p-3 rounded-2xl text-left border transition-all ${
                      isSelected
                        ? 'border-[#0D47A1] bg-blue-50/60 shadow-xs'
                        : 'border-slate-200 hover:bg-slate-50'
                    }`}
                  >
                    <div className="flex items-center justify-between">
                      <span className="text-xs font-bold text-slate-800">{fmt.displayName}</span>
                      {isSelected && <Check className="w-4 h-4 text-[#0D47A1]" />}
                    </div>
                    <div className="text-[11px] font-mono text-slate-500 mt-1">{fmt.example}</div>
                  </button>
                );
              })}
            </div>
          </div>
        </div>
      )}

      {/* Units Modal */}
      {showUnitModal && (
        <div className="fixed inset-0 z-60 flex items-center justify-center p-4 bg-black/60 backdrop-blur-2xs">
          <div className="bg-white rounded-3xl p-5 max-w-sm w-full shadow-2xl space-y-3">
            <div className="flex items-center justify-between border-b border-slate-100 pb-2">
              <h3 className="text-sm font-bold text-slate-800">Unit System</h3>
              <button onClick={() => setShowUnitModal(false)} className="text-slate-400">
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="space-y-2">
              {UNIT_OPTIONS.map((opt) => {
                const isSelected = settings.unitSystem === opt.key;
                return (
                  <button
                    key={opt.key}
                    onClick={() => {
                      onUpdateSettings({ ...settings, unitSystem: opt.key });
                      setShowUnitModal(false);
                      showToast(`Units set to ${opt.displayName}`);
                    }}
                    className={`w-full p-3 rounded-2xl text-left border transition-all ${
                      isSelected
                        ? 'border-[#0D47A1] bg-blue-50/60 shadow-xs'
                        : 'border-slate-200 hover:bg-slate-50'
                    }`}
                  >
                    <div className="flex items-center justify-between">
                      <span className="text-xs font-bold text-slate-800">{opt.displayName}</span>
                      {isSelected && <Check className="w-4 h-4 text-[#0D47A1]" />}
                    </div>
                    <div className="text-[11px] text-slate-500 mt-0.5">{opt.caption}</div>
                  </button>
                );
              })}
            </div>
          </div>
        </div>
      )}

      {/* Default Category Modal */}
      {showDefaultGroupModal && (
        <div className="fixed inset-0 z-60 flex items-center justify-center p-4 bg-black/60 backdrop-blur-2xs">
          <div className="bg-white rounded-3xl p-5 max-w-sm w-full shadow-2xl space-y-3">
            <div className="flex items-center justify-between border-b border-slate-100 pb-2">
              <h3 className="text-sm font-bold text-slate-800">Default Category</h3>
              <button onClick={() => setShowDefaultGroupModal(false)} className="text-slate-400">
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="space-y-2">
              <button
                onClick={() => {
                  onUpdateSettings({ ...settings, defaultGroupId: null });
                  setShowDefaultGroupModal(false);
                  showToast('Default category cleared');
                }}
                className={`w-full p-3 rounded-2xl text-left border transition-all ${
                  settings.defaultGroupId === null
                    ? 'border-[#0D47A1] bg-blue-50/60'
                    : 'border-slate-200 hover:bg-slate-50'
                }`}
              >
                <span className="text-xs font-bold text-slate-800">None (Uncategorized)</span>
              </button>

              {groups.map((group) => {
                const isSelected = settings.defaultGroupId === group.id;
                return (
                  <button
                    key={group.id}
                    onClick={() => {
                      onUpdateSettings({ ...settings, defaultGroupId: group.id });
                      setShowDefaultGroupModal(false);
                      showToast(`Default category set to ${group.name}`);
                    }}
                    className={`w-full p-3 rounded-2xl text-left border flex items-center justify-between transition-all ${
                      isSelected
                        ? 'border-[#0D47A1] bg-blue-50/60'
                        : 'border-slate-200 hover:bg-slate-50'
                    }`}
                  >
                    <div className="flex items-center gap-2">
                      <span
                        className="w-3 h-3 rounded-full"
                        style={{ backgroundColor: group.colorHex }}
                      />
                      <span className="text-xs font-bold text-slate-800">{group.name}</span>
                    </div>
                    {isSelected && <Check className="w-4 h-4 text-[#0D47A1]" />}
                  </button>
                );
              })}
            </div>
          </div>
        </div>
      )}

      {/* Clear All Confirmation Modal */}
      {showClearConfirm && (
        <div className="fixed inset-0 z-60 flex items-center justify-center p-4 bg-black/60 backdrop-blur-2xs">
          <div className="bg-white rounded-3xl p-6 max-w-sm w-full shadow-2xl space-y-4">
            <div className="w-12 h-12 rounded-full bg-rose-100 text-rose-600 flex items-center justify-center mx-auto">
              <AlertTriangle className="w-6 h-6" />
            </div>
            <div className="text-center">
              <h3 className="text-base font-bold text-slate-800">Clear All Saved Data?</h3>
              <p className="text-xs text-slate-500 mt-1">
                Warning: This will permanently erase all saved locations, notes, and local photo
                attachments. Are you sure?
              </p>
            </div>
            <div className="flex gap-2 pt-2">
              <button
                onClick={() => setShowClearConfirm(false)}
                className="flex-1 py-2.5 rounded-2xl border border-slate-200 text-slate-700 text-xs font-bold hover:bg-slate-50"
              >
                Cancel
              </button>
              <button
                onClick={() => {
                  onClearAllData();
                  setShowClearConfirm(false);
                  showToast('All saved data erased');
                }}
                className="flex-1 py-2.5 rounded-2xl bg-rose-600 text-white text-xs font-bold hover:bg-rose-700 shadow-md"
              >
                Clear Everything
              </button>
            </div>
          </div>
        </div>
      )}

      {/* New Group Modal */}
      {showNewGroupModal && (
        <div className="fixed inset-0 z-60 flex items-center justify-center p-4 bg-black/60 backdrop-blur-2xs">
          <form
            onSubmit={handleCreateGroupSubmit}
            className="bg-white rounded-3xl p-5 max-w-xs w-full shadow-2xl space-y-4"
          >
            <h3 className="text-sm font-bold text-slate-800">New Category</h3>
            <div>
              <label className="text-[11px] font-semibold text-slate-500 uppercase">Name</label>
              <input
                type="text"
                autoFocus
                value={newGroupName}
                onChange={(e) => setNewGroupName(e.target.value)}
                placeholder="e.g. Scenery"
                className="w-full px-3 py-2 rounded-xl border border-slate-200 text-xs font-medium focus:ring-2 focus:ring-[#0D47A1]"
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
                className="flex-1 py-2 rounded-xl border border-slate-200 text-slate-600 text-xs font-semibold"
              >
                Cancel
              </button>
              <button
                type="submit"
                className="flex-1 py-2 rounded-xl bg-[#0D47A1] text-white text-xs font-bold shadow-md"
              >
                Create
              </button>
            </div>
          </form>
        </div>
      )}

      {/* App Link QR Code Modal */}
      {showAppQr && (
        <div className="fixed inset-0 z-60 flex items-center justify-center p-4 bg-black/60 backdrop-blur-2xs">
          <div className="bg-white rounded-3xl p-6 max-w-xs w-full shadow-2xl text-center space-y-4">
            <div className="flex items-center justify-between pb-2 border-b border-slate-100">
              <div className="text-xs font-bold text-slate-800 flex items-center gap-1.5">
                <Smartphone className="w-4 h-4 text-emerald-600" />
                <span>Scan to Open on Phone</span>
              </div>
              <button onClick={() => setShowAppQr(false)} className="text-slate-400 hover:text-slate-600">
                <X className="w-4 h-4" />
              </button>
            </div>

            {appQrDataUrl ? (
              <div className="p-3 bg-white rounded-2xl border border-slate-200 inline-block shadow-xs">
                <img src={appQrDataUrl} alt="App Link QR Code" className="w-48 h-48 mx-auto" />
              </div>
            ) : (
              <div className="w-48 h-48 mx-auto flex items-center justify-center bg-slate-50 rounded-2xl">
                <Loader2 className="w-6 h-6 animate-spin text-[#0D47A1]" />
              </div>
            )}

            <p className="text-[11px] text-slate-500 leading-relaxed">
              Scan with your phone&rsquo;s camera to open the application in Chrome and install it with 1 tap.
            </p>
            <div className="p-2 bg-amber-50 rounded-xl border border-amber-200/80 text-[10px] text-amber-800 text-left">
              💡 <strong>403 Access Error?</strong> Log into Chrome on your phone with your AI Studio Google account, or click <strong>Share</strong> in the top AI Studio bar.
            </div>

            <button
              onClick={() => setShowAppQr(false)}
              className="w-full py-2.5 rounded-xl bg-[#0D47A1] text-white text-xs font-bold shadow-md cursor-pointer"
            >
              Done
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
