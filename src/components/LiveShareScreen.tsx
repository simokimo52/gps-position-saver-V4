import React, { useState, useEffect } from 'react';
import QRCode from 'qrcode';
import { ShareSession } from '../types';
import {
  Radio,
  Clock,
  Copy,
  Share2,
  Square,
  History,
  CheckCircle2,
  ExternalLink,
  ShieldCheck,
  Check,
} from 'lucide-react';

interface LiveShareScreenProps {
  currentLat: number;
  currentLng: number;
  activeSession: ShareSession | null;
  sessionsHistory: ShareSession[];
  onStartSession: (durationMinutes: number) => void;
  onStopSession: () => void;
}

const DURATION_OPTIONS = [
  { label: '15 min', minutes: 15 },
  { label: '30 min', minutes: 30 },
  { label: '1 hour', minutes: 60 },
  { label: '4 hours', minutes: 240 },
  { label: '8 hours', minutes: 480 },
];

export const LiveShareScreen: React.FC<LiveShareScreenProps> = ({
  currentLat,
  currentLng,
  activeSession,
  sessionsHistory,
  onStartSession,
  onStopSession,
}) => {
  const [selectedDuration, setSelectedDuration] = useState<number>(30);
  const [remainingSeconds, setRemainingSeconds] = useState<number>(0);
  const [qrDataUrl, setQrDataUrl] = useState<string | null>(null);
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  const showToast = (msg: string) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(null), 3000);
  };

  // Timer countdown
  useEffect(() => {
    if (!activeSession || !activeSession.isActive) {
      setRemainingSeconds(0);
      return;
    }

    const updateRemaining = () => {
      const now = Date.now();
      const diff = Math.max(0, Math.floor((activeSession.expiresAt - now) / 1000));
      setRemainingSeconds(diff);
      if (diff === 0) {
        onStopSession();
      }
    };

    updateRemaining();
    const interval = setInterval(updateRemaining, 1000);
    return () => clearInterval(interval);
  }, [activeSession, onStopSession]);

  // Generate QR Code for sharing
  useEffect(() => {
    if (activeSession && activeSession.shareUrl) {
      QRCode.toDataURL(activeSession.shareUrl, { width: 220, margin: 1 })
        .then((url) => setQrDataUrl(url))
        .catch(() => setQrDataUrl(null));
    } else {
      setQrDataUrl(null);
    }
  }, [activeSession]);

  const handleCopyLink = () => {
    if (activeSession) {
      navigator.clipboard.writeText(activeSession.shareUrl);
      showToast('Live link copied to clipboard!');
    }
  };

  const handleSendLink = async () => {
    if (!activeSession) return;
    const shareData = {
      title: 'My Live GPS Location',
      text: `Track my live location for the next ${activeSession.durationMinutes} minutes:`,
      url: activeSession.shareUrl,
    };

    if (navigator.share) {
      try {
        await navigator.share(shareData);
        return;
      } catch {
        // fallback
      }
    }
    handleCopyLink();
  };

  const formatCountdown = (totalSecs: number) => {
    const hours = Math.floor(totalSecs / 3600);
    const mins = Math.floor((totalSecs % 3600) / 60);
    const secs = totalSecs % 60;
    if (hours > 0) {
      return `${hours}h ${String(mins).padStart(2, '0')}m ${String(secs).padStart(2, '0')}s`;
    }
    return `${String(mins).padStart(2, '0')}m ${String(secs).padStart(2, '0')}s`;
  };

  return (
    <div className="flex flex-col h-[calc(100vh-64px)] bg-[#F8F9FF] overflow-y-auto p-4 max-w-xl mx-auto space-y-4 select-none">
      {/* Toast */}
      {toastMessage && (
        <div className="fixed top-4 right-4 z-50 bg-slate-800 text-white text-xs font-semibold px-4 py-2.5 rounded-xl shadow-xl flex items-center gap-2 animate-in fade-in">
          <Check className="w-4 h-4 text-emerald-400" />
          <span>{toastMessage}</span>
        </div>
      )}

      {/* Screen Header */}
      <div className="text-center pt-2">
        <h2 className="text-lg font-bold text-slate-800">Live Location Sharing</h2>
        <p className="text-xs text-slate-500 mt-0.5">
          Generate an encrypted temporary link to share your position in real-time
        </p>
      </div>

      {activeSession && activeSession.isActive ? (
        /* ACTIVE SESSION CARD */
        <div className="bg-white rounded-3xl p-5 shadow-lg border border-emerald-100 space-y-4 animate-in zoom-in-95 duration-200">
          {/* Status Badge */}
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2 bg-emerald-50 px-3 py-1.5 rounded-full border border-emerald-200">
              <span className="w-2.5 h-2.5 rounded-full bg-emerald-500 animate-ping" />
              <span className="text-xs font-bold text-emerald-800">LIVE SHARING ACTIVE</span>
            </div>

            <span className="text-xs font-mono font-semibold text-slate-400">
              ID: {activeSession.sessionId.slice(0, 8)}...
            </span>
          </div>

          {/* Countdown timer */}
          <div className="bg-slate-50 rounded-2xl p-4 text-center border border-slate-100">
            <span className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider block">
              Time remaining before link expires
            </span>
            <div className="text-2xl font-mono font-black text-[#0D47A1] mt-1">
              {formatCountdown(remainingSeconds)}
            </div>
          </div>

          {/* Live QR Code */}
          {qrDataUrl && (
            <div className="flex flex-col items-center justify-center p-3 bg-white rounded-2xl border border-slate-100">
              <img
                src={qrDataUrl}
                alt="Live Location QR"
                className="w-40 h-40 rounded-xl shadow-xs"
              />
              <span className="text-[11px] text-slate-400 mt-2">
                Scan with any smartphone camera to open map
              </span>
            </div>
          )}

          {/* Share Link Box */}
          <div className="p-3 bg-slate-50 rounded-2xl border border-slate-200 flex items-center justify-between gap-2">
            <span className="text-xs font-mono text-slate-600 truncate">
              {activeSession.shareUrl}
            </span>
            <div className="flex items-center gap-1 shrink-0">
              <button
                onClick={handleCopyLink}
                className="p-2 bg-white hover:bg-slate-100 border border-slate-200 rounded-xl text-slate-700 text-xs font-bold transition-colors"
                title="Copy Link"
              >
                <Copy className="w-3.5 h-3.5" />
              </button>
              <button
                onClick={handleSendLink}
                className="p-2 bg-[#0D47A1] hover:bg-blue-900 text-white rounded-xl text-xs font-bold shadow-xs transition-colors"
                title="Send Link"
              >
                <Share2 className="w-3.5 h-3.5" />
              </button>
            </div>
          </div>

          {/* Live Coordinates */}
          <div className="text-xs text-slate-500 flex items-center justify-between px-1">
            <span>
              Live Position: {currentLat.toFixed(6)}, {currentLng.toFixed(6)}
            </span>
            <span>Duration: {activeSession.durationMinutes} min</span>
          </div>

          {/* Stop Button */}
          <button
            onClick={onStopSession}
            className="w-full py-3 bg-rose-600 hover:bg-rose-700 text-white font-bold text-xs rounded-2xl shadow-md active:scale-98 transition-all flex items-center justify-center gap-2"
          >
            <Square className="w-3.5 h-3.5 fill-current" />
            <span>STOP SHARING NOW</span>
          </button>
        </div>
      ) : (
        /* START LIVE SHARING SETUP CARD */
        <div className="bg-white rounded-3xl p-5 shadow-lg border border-slate-200/80 space-y-5">
          <div className="flex items-center gap-3">
            <div className="w-12 h-12 rounded-2xl bg-blue-50 text-[#0D47A1] flex items-center justify-center shrink-0">
              <Radio className="w-6 h-6 animate-pulse" />
            </div>
            <div>
              <h3 className="text-sm font-bold text-slate-800">Start Live GPS Broadcast</h3>
              <p className="text-xs text-slate-500">
                Share a live updating waypoint link with family, travel companions, or colleagues.
              </p>
            </div>
          </div>

          {/* Duration Selector */}
          <div className="space-y-2">
            <label className="text-xs font-bold text-slate-600 uppercase tracking-wider block">
              Session Duration
            </label>
            <div className="grid grid-cols-3 sm:grid-cols-5 gap-2">
              {DURATION_OPTIONS.map((opt) => {
                const isSelected = selectedDuration === opt.minutes;
                return (
                  <button
                    key={opt.minutes}
                    type="button"
                    onClick={() => setSelectedDuration(opt.minutes)}
                    className={`py-2 px-1 text-xs font-bold rounded-xl transition-all ${
                      isSelected
                        ? 'bg-[#0D47A1] text-white shadow-md'
                        : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                    }`}
                  >
                    {opt.label}
                  </button>
                );
              })}
            </div>
          </div>

          {/* Privacy Guarantee Note */}
          <div className="p-3.5 bg-blue-50/60 rounded-2xl border border-blue-100 flex items-start gap-2.5">
            <ShieldCheck className="w-4 h-4 text-[#0D47A1] shrink-0 mt-0.5" />
            <p className="text-xs text-slate-600 leading-relaxed">
              No registration or account required. The session link expires automatically and cannot
              be tracked after the timer ends.
            </p>
          </div>

          {/* Start Button */}
          <button
            onClick={() => onStartSession(selectedDuration)}
            className="w-full py-3.5 bg-[#0D47A1] hover:bg-[#002F6E] text-white font-bold text-xs sm:text-sm rounded-2xl shadow-lg shadow-blue-900/20 active:scale-98 transition-all flex items-center justify-center gap-2 cursor-pointer"
          >
            <Radio className="w-4 h-4" />
            <span>START LIVE SHARING</span>
          </button>
        </div>
      )}

      {/* Session History */}
      {sessionsHistory.length > 0 && (
        <div className="bg-white rounded-3xl p-5 shadow-sm border border-slate-200/80 space-y-3">
          <div className="flex items-center gap-2 text-slate-700">
            <History className="w-4 h-4 text-slate-400" />
            <h3 className="text-xs font-bold uppercase tracking-wider">Past Sessions History</h3>
          </div>

          <div className="divide-y divide-slate-100">
            {sessionsHistory.slice(0, 5).map((s) => (
              <div key={s.sessionId} className="py-2.5 flex items-center justify-between text-xs">
                <div>
                  <span className="font-semibold text-slate-800">
                    {s.durationMinutes} min session
                  </span>
                  <div className="text-[10px] text-slate-400">
                    {new Date(s.startTime).toLocaleString()}
                  </div>
                </div>
                <span className="text-[11px] text-slate-400 font-mono">
                  {s.sessionId.slice(0, 8)}...
                </span>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};
