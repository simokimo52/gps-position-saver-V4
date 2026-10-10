import { useEffect, useState, useCallback, useRef } from 'react';

export interface LocationState {
  latitude: number;
  longitude: number;
  accuracy: number;
  altitude: number | null;
  timestamp: number;
  isSimulated: boolean;
  statusText: string;
}

// Fallback when GPS is completely unavailable or pending
const DEFAULT_FALLBACK: LocationState = {
  latitude: 33.5731, // Casablanca, Morocco as realistic default for user
  longitude: -7.5898,
  accuracy: 12.0,
  altitude: 40.0,
  timestamp: Date.now(),
  isSimulated: true,
  statusText: 'Waiting for GPS Permission...',
};

export function useGeolocation() {
  const [location, setLocation] = useState<LocationState>(() => {
    const cached = localStorage.getItem('gps_last_known_location');
    if (cached) {
      try {
        const parsed = JSON.parse(cached);
        return parsed;
      } catch {
        // ignore
      }
    }
    return DEFAULT_FALLBACK;
  });

  const [isLocating, setIsLocating] = useState<boolean>(false);
  const [elapsedSeconds, setElapsedSeconds] = useState<number>(0);
  const [locatingStatus, setLocatingStatus] = useState<string>('Connecting to GPS...');
  const [permissionState, setPermissionState] = useState<'prompt' | 'granted' | 'denied'>('prompt');
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const watchIdRef = useRef<number | null>(null);

  // Update cached location
  const updateLocation = useCallback((newLoc: LocationState) => {
    setLocation(newLoc);
    try {
      localStorage.setItem('gps_last_known_location', JSON.stringify(newLoc));
    } catch {
      // ignore
    }
  }, []);

  // Monitor permission state using Permissions API if supported
  useEffect(() => {
    if ('permissions' in navigator && navigator.permissions.query) {
      navigator.permissions
        .query({ name: 'geolocation' })
        .then((permissionStatus) => {
          setPermissionState(permissionStatus.state as 'prompt' | 'granted' | 'denied');
          permissionStatus.onchange = () => {
            setPermissionState(permissionStatus.state as 'prompt' | 'granted' | 'denied');
            if (permissionStatus.state === 'granted') {
              setErrorMessage(null);
            }
          };
        })
        .catch(() => {
          // Permissions API might not support geolocation in some webviews
        });
    }
  }, []);

  // Primary method to explicitly request location from user interaction
  const requestRealGps = useCallback((): Promise<LocationState> => {
    return new Promise((resolve) => {
      if (!('geolocation' in navigator)) {
        setPermissionState('denied');
        setErrorMessage('Geolocation is not supported by your browser.');
        resolve(location);
        return;
      }

      setIsLocating(true);
      setLocatingStatus('Requesting GPS permission from Android...');

      navigator.geolocation.getCurrentPosition(
        (pos) => {
          setPermissionState('granted');
          setErrorMessage(null);
          setIsLocating(false);

          const realLoc: LocationState = {
            latitude: pos.coords.latitude,
            longitude: pos.coords.longitude,
            accuracy: pos.coords.accuracy || 4.0,
            altitude: pos.coords.altitude,
            timestamp: pos.timestamp || Date.now(),
            isSimulated: false,
            statusText: 'Live GPS Connected',
          };
          updateLocation(realLoc);
          resolve(realLoc);
        },
        (err) => {
          setIsLocating(false);
          if (err.code === 1) {
            // PERMISSION_DENIED
            setPermissionState('denied');
            setErrorMessage('Location permission was denied by Android/Chrome.');
          } else if (err.code === 2) {
            // POSITION_UNAVAILABLE
            setErrorMessage('GPS position unavailable. Please ensure GPS/Location is enabled in Android.');
          } else if (err.code === 3) {
            // TIMEOUT
            setErrorMessage('GPS request timed out. Searching for satellites...');
          } else {
            setErrorMessage('Unable to retrieve location.');
          }

          // Fallback but clearly mark as simulated
          const fallback: LocationState = {
            ...location,
            timestamp: Date.now(),
            isSimulated: true,
            statusText: err.code === 1 ? 'Permission Denied' : 'GPS Unavailable',
          };
          updateLocation(fallback);
          resolve(fallback);
        },
        {
          enableHighAccuracy: true,
          timeout: 15000,
          maximumAge: 0,
        }
      );
    });
  }, [location, updateLocation]);

  // Satellite Lock Acquisition with feedback
  const acquireGpsLock = useCallback(async (): Promise<LocationState> => {
    setIsLocating(true);
    setElapsedSeconds(0);
    setLocatingStatus('Connecting to GPS satellites...');

    const timer = setInterval(() => {
      setElapsedSeconds((prev) => {
        const next = prev + 1;
        if (next <= 2) {
          setLocatingStatus('Connecting to GPS satellites...');
        } else if (next <= 5) {
          setLocatingStatus('Calculating precise coordinates...');
        } else {
          setLocatingStatus('Refining position accuracy...');
        }
        return next;
      });
    }, 1000);

    try {
      const loc = await requestRealGps();
      clearInterval(timer);
      setIsLocating(false);
      return loc;
    } catch {
      clearInterval(timer);
      setIsLocating(false);
      return location;
    }
  }, [requestRealGps, location]);

  // Real-time GPS Watching
  useEffect(() => {
    if (!('geolocation' in navigator)) return;

    try {
      watchIdRef.current = navigator.geolocation.watchPosition(
        (pos) => {
          setPermissionState('granted');
          setErrorMessage(null);
          updateLocation({
            latitude: pos.coords.latitude,
            longitude: pos.coords.longitude,
            accuracy: pos.coords.accuracy || 4.0,
            altitude: pos.coords.altitude,
            timestamp: pos.timestamp || Date.now(),
            isSimulated: false,
            statusText: 'Live GPS Active',
          });
        },
        (err) => {
          if (err.code === 1) {
            setPermissionState('denied');
          }
        },
        {
          enableHighAccuracy: true,
          maximumAge: 2000,
          timeout: 20000,
        }
      );
    } catch {
      // ignore
    }

    return () => {
      if (watchIdRef.current !== null && 'geolocation' in navigator) {
        navigator.geolocation.clearWatch(watchIdRef.current);
      }
    };
  }, [updateLocation]);

  return {
    location,
    isLocating,
    elapsedSeconds,
    locatingStatus,
    permissionState,
    errorMessage,
    refreshLocation: requestRealGps,
    acquireGpsLock,
    updateLocation,
  };
}
