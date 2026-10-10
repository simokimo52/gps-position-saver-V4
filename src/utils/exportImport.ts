import { Group, Place } from '../types';

export type ExportFormat = 'gpx' | 'kml' | 'csv' | 'json';

function escapeXml(str: string): string {
  return str
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&apos;');
}

function unescapeXml(str: string): string {
  return str
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&quot;/g, '"')
    .replace(/&apos;/g, "'")
    .replace(/&amp;/g, '&');
}

function escapeCsv(str: string): string {
  const mustQuote = str.includes(',') || str.includes('"') || str.includes('\n');
  return mustQuote ? `"${str.replace(/"/g, '""')}"` : str;
}

function parseCsvLine(line: string): string[] {
  const result: string[] = [];
  let inQuotes = false;
  let current = '';
  for (let i = 0; i < line.length; i++) {
    const c = line[i];
    if (c === '"') {
      if (inQuotes && i + 1 < line.length && line[i + 1] === '"') {
        current += '"';
        i++;
      } else {
        inQuotes = !inQuotes;
      }
    } else if (c === ',' && !inQuotes) {
      result.push(current.trim());
      current = '';
    } else {
      current += c;
    }
  }
  result.push(current.trim());
  return result;
}

export function generateGpx(places: Place[]): string {
  const isoTime = new Date().toISOString();
  let xml = `<?xml version="1.0" encoding="UTF-8"?>\n`;
  xml += `<gpx version="1.1" creator="GPS Position Saver" xmlns="http://www.topografix.com/GPX/1/1">\n`;
  xml += `  <metadata>\n    <name>Exported Waypoints</name>\n    <time>${isoTime}</time>\n  </metadata>\n`;

  for (const p of places) {
    xml += `  <wpt lat="${p.latitude}" lon="${p.longitude}">\n`;
    if (p.altitude != null) {
      xml += `    <ele>${p.altitude}</ele>\n`;
    }
    xml += `    <time>${new Date(p.createdAt).toISOString()}</time>\n`;
    xml += `    <name>${escapeXml(p.name)}</name>\n`;

    const descParts: string[] = [];
    if (p.address) descParts.push(`Address: ${p.address}`);
    if (p.note) descParts.push(`Note: ${p.note}`);
    if (p.phone) descParts.push(`Phone: ${p.phone}`);
    if (p.accuracy != null) descParts.push(`Accuracy: ±${p.accuracy}m`);

    if (descParts.length > 0) {
      xml += `    <desc>${escapeXml(descParts.join('; '))}</desc>\n`;
    }
    xml += `    <sym>Waypoint</sym>\n`;
    xml += `  </wpt>\n`;
  }

  xml += `</gpx>`;
  return xml;
}

export function generateKml(places: Place[]): string {
  let kml = `<?xml version="1.0" encoding="UTF-8"?>\n`;
  kml += `<kml xmlns="http://www.opengis.net/kml/2.2">\n`;
  kml += `  <Document>\n    <name>GPS Position Saver Places</name>\n`;

  for (const p of places) {
    kml += `    <Placemark>\n`;
    kml += `      <name>${escapeXml(p.name)}</name>\n`;

    const descParts: string[] = [];
    if (p.address) descParts.push(`Address: ${p.address}`);
    if (p.note) descParts.push(`Note: ${p.note}`);
    if (p.phone) descParts.push(`Phone: ${p.phone}`);

    if (descParts.length > 0) {
      kml += `      <description>${escapeXml(descParts.join('\n'))}</description>\n`;
    }

    kml += `      <Point>\n`;
    kml += `        <coordinates>${p.longitude},${p.latitude},${p.altitude ?? 0}</coordinates>\n`;
    kml += `      </Point>\n`;
    kml += `    </Placemark>\n`;
  }

  kml += `  </Document>\n</kml>`;
  return kml;
}

export function generateCsv(places: Place[], groups: Group[]): string {
  const groupMap = new Map(groups.map((g) => [g.id, g.name]));
  let csv = `ID,Name,Latitude,Longitude,Accuracy_Meters,Altitude_Meters,Address,Phone,Note,Category,CreatedAt\n`;

  for (const p of places) {
    const groupName = p.groupId ? groupMap.get(p.groupId) || '' : '';
    csv += [
      p.id,
      escapeCsv(p.name),
      p.latitude,
      p.longitude,
      p.accuracy ?? '',
      p.altitude ?? '',
      escapeCsv(p.address || ''),
      escapeCsv(p.phone || ''),
      escapeCsv(p.note || ''),
      escapeCsv(groupName),
      p.createdAt,
    ].join(',') + '\n';
  }

  return csv;
}

export function generateJson(places: Place[], groups: Group[]): string {
  return JSON.stringify(
    {
      version: 1,
      exportedAt: Date.now(),
      groups,
      places,
    },
    null,
    2
  );
}

export function downloadFile(content: string, filename: string, mimeType: string) {
  const blob = new Blob([content], { type: mimeType });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
}

export function parseImportContent(content: string): { places: Partial<Place>[]; groups?: Group[] } {
  const trimmed = content.trim();

  // 1. JSON
  if (trimmed.startsWith('{') && trimmed.includes('"places"')) {
    try {
      const data = JSON.parse(trimmed);
      return {
        places: Array.isArray(data.places) ? data.places : [],
        groups: Array.isArray(data.groups) ? data.groups : [],
      };
    } catch {
      // ignore
    }
  }

  // 2. GPX
  if (trimmed.includes('<gpx')) {
    const places: Partial<Place>[] = [];
    const wptRegex = /<wpt\s+lat="([^"]+)"\s+lon="([^"]+)"[^>]*>([\s\S]*?)<\/wpt>/g;
    let match: RegExpExecArray | null;

    while ((match = wptRegex.exec(trimmed)) !== null) {
      const lat = parseFloat(match[1]);
      const lon = parseFloat(match[2]);
      const inner = match[3];

      const nameMatch = /<name>([\s\S]*?)<\/name>/.exec(inner);
      const descMatch = /<desc>([\s\S]*?)<\/desc>/.exec(inner);
      const eleMatch = /<ele>([\s\S]*?)<\/ele>/.exec(inner);

      if (!isNaN(lat) && !isNaN(lon)) {
        places.push({
          name: nameMatch ? unescapeXml(nameMatch[1]) : 'Imported Waypoint',
          latitude: lat,
          longitude: lon,
          altitude: eleMatch ? parseFloat(eleMatch[1]) : undefined,
          note: descMatch ? unescapeXml(descMatch[1]) : undefined,
          createdAt: Date.now(),
          updatedAt: Date.now(),
          isFavorite: false,
        });
      }
    }
    return { places };
  }

  // 3. KML
  if (trimmed.includes('<kml')) {
    const places: Partial<Place>[] = [];
    const placemarkRegex = /<Placemark>([\s\S]*?)<\/Placemark>/g;
    let match: RegExpExecArray | null;

    while ((match = placemarkRegex.exec(trimmed)) !== null) {
      const inner = match[1];
      const nameMatch = /<name>([\s\S]*?)<\/name>/.exec(inner);
      const descMatch = /<description>([\s\S]*?)<\/description>/.exec(inner);
      const coordsMatch = /<coordinates>\s*([^,\s]+),([^,\s]+)(?:,([^\s<]+))?\s*<\/coordinates>/.exec(inner);

      if (coordsMatch) {
        const lon = parseFloat(coordsMatch[1]);
        const lat = parseFloat(coordsMatch[2]);
        const alt = coordsMatch[3] ? parseFloat(coordsMatch[3]) : undefined;

        if (!isNaN(lat) && !isNaN(lon)) {
          places.push({
            name: nameMatch ? unescapeXml(nameMatch[1]) : 'Imported KML Point',
            latitude: lat,
            longitude: lon,
            altitude: isNaN(alt ?? NaN) ? undefined : alt,
            note: descMatch ? unescapeXml(descMatch[1]) : undefined,
            createdAt: Date.now(),
            updatedAt: Date.now(),
            isFavorite: false,
          });
        }
      }
    }
    return { places };
  }

  // 4. CSV
  if (trimmed.startsWith('ID,') || trimmed.includes(',')) {
    const places: Partial<Place>[] = [];
    const lines = trimmed.split('\n').filter((l) => l.trim().length > 0);
    const startIdx = lines[0].startsWith('ID,') || lines[0].toLowerCase().includes('latitude') ? 1 : 0;

    for (let i = startIdx; i < lines.length; i++) {
      const tokens = parseCsvLine(lines[i]);
      if (tokens.length >= 4) {
        const name = tokens[1] || 'Imported Place';
        const lat = parseFloat(tokens[2]);
        const lon = parseFloat(tokens[3]);
        const acc = tokens[4] ? parseFloat(tokens[4]) : undefined;
        const alt = tokens[5] ? parseFloat(tokens[5]) : undefined;
        const address = tokens[6] || undefined;
        const phone = tokens[7] || undefined;
        const note = tokens[8] || undefined;

        if (!isNaN(lat) && !isNaN(lon)) {
          places.push({
            name,
            latitude: lat,
            longitude: lon,
            accuracy: isNaN(acc ?? NaN) ? undefined : acc,
            altitude: isNaN(alt ?? NaN) ? undefined : alt,
            address,
            phone,
            note,
            createdAt: Date.now(),
            updatedAt: Date.now(),
            isFavorite: false,
          });
        }
      }
    }
    return { places };
  }

  return { places: [] };
}
