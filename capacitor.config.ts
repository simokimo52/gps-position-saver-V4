import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.simodev.gpspositionsaver.web',
  appName: 'GPS Position Saver',
  webDir: 'dist',
  server: {
    cleartext: true,
  },
  plugins: {
    Geolocation: {
      permissions: ['location'],
    },
  },
};

export default config;
