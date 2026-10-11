import { useEffect, useState, useCallback, useRef } from 'react';
import { Capacitor } from '@capacitor/core';
import { Geolocation, PermissionStatus } from '@capacitor/geolocation';

export interface LocationState {
  latitude: number;
  longitude: number;
  accuracy: number;
  altitude: number | null;
  timestamp: number;
  isSimulated: boolean;
  statusText: string;
}

const DEFAULT_FALLBACK: LocationState = {
  latitude: 33.5731,
  longitude: -7.5898,
  accuracy: 12.0,
  altitude: 40.0,
  timestamp: Date.now(),
  isSimulated: true,
  statusText: 'Waiting for GPS Permission...',
};

const isNative = Capacitor.isNativePlatform();

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
  const nativeWatchIdRef = useRef<string | null>(null);

  const updateLocation = useCallback((newLoc: LocationState) => {
    setLocation(newLoc);
    try {
      localStorage.setItem('gps_last_known_location', JSON.stringify(newLoc));
    } catch {
      // ignore
    }
  }, []);

  useEffect(() => {
    if (!isNative) {
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
    } else {
      Geolocation.checkPermissions().then((perm: PermissionStatus) => {
        setPermissionState(perm.location as 'prompt' | 'granted' | 'denied');
      }).catch(() => {});
    }
  }, [isNative]);

  const requestRealGps = useCallback(async (): Promise<LocationState> => {
    if (isNative) {
      return requestNativeGps();
    }
    return requestWebGps();
  }, [isNative]);

  const requestNativeGps = async (): Promise<LocationState> => {
    setIsLocating(true);
    setLocatingStatus('Requesting GPS permission from Android...');
    setErrorMessage(null);

    try {
      const perm = await Geolocation.requestPermissions();
      if (perm.location !== 'granted') {
        setPermissionState('denied');
        setErrorMessage('Location permission was denied by Android. Please enable it in Settings > Apps > GPS Position Saver > Permissions > Location.');
        setIsLocating(false);
        const fallback: LocationState = {
          ...location,
          timestamp: Date.now(),
          isSimulated: true,
          statusText: 'Permission Denied',
        };
        updateLocation(fallback);
        return fallback;
      }

      setPermissionState('granted');
      setLocatingStatus('Acquiring GPS satellites...');

      const position = await Geolocation.getCurrentPosition({
        enableHighAccuracy: true,
        timeout: 15000,
        maximumAge: 0,
      });

      setIsLocating(false);
      setErrorMessage(null);

      const realLoc: LocationState = {
        latitude: position.coords.latitude,
        longitude: position.coords.longitude,
        accuracy: position.coords.accuracy || 4.0,
        altitude: position.coords.altitude ?? null,
        timestamp: position.timestamp,
        isSimulated: false,
        statusText: 'Live GPS Connected',
      };
      updateLocation(realLoc);
      return realLoc;
    } catch (err: unknown) {
      setIsLocating(false);
      const error = err as Error & { code?: number; message?: string };
      if (error.code === 1 || error.message?.includes('PERMISSION_DENIED') || error.message?.includes('permission')) {
        setPermissionState('denied');
        setErrorMessage('Location permission was denied by Android. Please enable it in Settings > Apps > GPS Position Saver > Permissions > Location.');
      } else if (error.code === 2 || error.message?.includes('POSITION_UNAVAILABLE') || error.message?.includes('unavailable')) {
        setErrorMessage('GPS position unavailable. Please ensure GPS/Location is enabled in Android settings.');
      } else if (error.code === 3 || error.message?.includes('TIMEOUT') || error.message?.includes('timeout')) {
        setErrorMessage('GPS request timed out. Searching for satellites...');
      } else {
        setErrorMessage(`Unable to retrieve location: ${error.message || 'Unknown error'}`);
      }

      const fallback: LocationState = {
        ...location,
        timestamp: Date.now(),
        isSimulated: true,
        statusText: error.code === 1 || error.message?.includes('PERMISSION_DENIED') ? 'Permission Denied' : 'GPS Unavailable',
      };
      updateLocation(fallback);
      return fallback;
    }
  };

  const requestWebGps = (): Promise<LocationState> => {
    return new Promise((resolve) => {
      if (!('geolocation' in navigator)) {
        setPermissionState('denied');
        setErrorMessage('Geolocation is not supported by your browser.');
        resolve(location);
        return;
      }

      setIsLocating(true);
      setLocatingStatus('Requesting GPS permission from browser...');

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
            setPermissionState('denied');
            setErrorMessage('Location permission was denied by browser. Please allow location access in browser settings.');
          } else if (err.code === 2) {
            setErrorMessage('GPS position unavailable. Please ensure GPS/Location is enabled.');
          } else if (err.code === 3) {
            setErrorMessage('GPS request timed out. Searching for satellites...');
          } else {
            setErrorMessage('Unable to retrieve location.');
          }

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
  };

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

  useEffect(() => {
    if (isNative) {
      let watchId: string | null = null;
      (async () => {
        try {
          watchId = await Geolocation.watchPosition(
            {
              enableHighAccuracy: true,
              timeout: 20000,
              maximumAge: 2000,
            },
            (position, err) => {
              if (err) {
                if (err.code === 1) {
                  setPermissionState('denied');
                }
                return;
              }
              if (position) {
                setPermissionState('granted');
                setErrorMessage(null);
                updateLocation({
                  latitude: position.coords.latitude,
                  longitude: position.coords.longitude,
                  accuracy: position.coords.accuracy || 4.0,
                  altitude: position.coords.altitude ?? null,
                  timestamp: position.timestamp,
                  isSimulated: false,
                  statusText: 'Live GPS Active',
                });
              }
            }
          );
          nativeWatchIdRef.current = watchId;
        } catch {
          // ignore
        }
      })();

      return () => {
        if (nativeWatchIdRef.current) {
          Geolocation.clearWatch({ id: nativeWatchIdRef.current });
        }
      };
    } else {
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
    }
  }, [isNative, updateLocation]);

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
