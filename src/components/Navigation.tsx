import React from 'react';
import { ScreenTab } from '../types';
import { Bookmark, List, Map as MapIcon, Radio, Settings } from 'lucide-react';

interface NavigationProps {
  currentTab: ScreenTab;
  onTabChange: (tab: ScreenTab) => void;
  placesCount: number;
  hasActiveLiveSession: boolean;
}

export const Navigation: React.FC<NavigationProps> = ({
  currentTab,
  onTabChange,
  placesCount,
  hasActiveLiveSession,
}) => {
  const tabs: { key: ScreenTab; label: string; icon: React.ReactNode; badge?: string | number }[] = [
    {
      key: 'home',
      label: 'Save',
      icon: <Bookmark className="w-5 h-5" />,
    },
    {
      key: 'places',
      label: 'Places',
      icon: <List className="w-5 h-5" />,
      badge: placesCount > 0 ? placesCount : undefined,
    },
    {
      key: 'map',
      label: 'Map',
      icon: <MapIcon className="w-5 h-5" />,
    },
    {
      key: 'live',
      label: 'Live',
      icon: <Radio className={`w-5 h-5 ${hasActiveLiveSession ? 'animate-pulse text-emerald-500' : ''}`} />,
      badge: hasActiveLiveSession ? '•' : undefined,
    },
    {
      key: 'settings',
      label: 'Settings',
      icon: <Settings className="w-5 h-5" />,
    },
  ];

  return (
    <nav className="h-16 bg-white/95 backdrop-blur-md border-t border-slate-200/80 px-2 sm:px-6 flex items-center justify-around z-40 select-none shadow-md">
      {tabs.map((tab) => {
        const isActive = currentTab === tab.key;
        return (
          <button
            key={tab.key}
            onClick={() => onTabChange(tab.key)}
            className={`flex flex-col items-center justify-center py-1 px-3 rounded-2xl relative transition-all duration-150 cursor-pointer ${
              isActive
                ? 'text-[#0D47A1] font-bold'
                : 'text-slate-400 hover:text-slate-600 font-medium'
            }`}
          >
            <div className="relative">
              <div
                className={`transition-transform duration-150 ${
                  isActive ? 'scale-110' : 'scale-100'
                }`}
              >
                {tab.icon}
              </div>

              {tab.badge !== undefined && (
                <span
                  className={`absolute -top-1 -right-2 px-1 text-[10px] font-bold rounded-full ${
                    hasActiveLiveSession && tab.key === 'live'
                      ? 'bg-emerald-500 text-white w-2 h-2 p-0'
                      : 'bg-blue-100 text-[#0D47A1]'
                  } flex items-center justify-center`}
                >
                  {tab.badge}
                </span>
              )}
            </div>

            <span className="text-[11px] mt-0.5 tracking-tight">{tab.label}</span>

            {isActive && (
              <div className="w-4 h-0.5 bg-[#0D47A1] rounded-full mt-0.5" />
            )}
          </button>
        );
      })}
    </nav>
  );
};
