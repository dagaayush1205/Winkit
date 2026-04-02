import React, { useEffect, useState } from "react";
import { MapContainer, TileLayer, Marker, Polygon, useMap } from "react-leaflet";
import L from "leaflet";
import "leaflet/dist/leaflet.css";
import { supabase } from "../supabase";
import { cellToBoundary, cellToLatLng, cellToParent } from "h3-js";

const customIcon = L.divIcon({
  className: "custom-pulse-marker",
  html: `<div style="width: 16px; height: 16px; background: #EF4444; border-radius: 50%; border: 3px solid white; box-shadow: 0 0 10px rgba(239,68,68,0.8);"></div>`,
  iconSize: [16, 16], iconAnchor: [8, 8],
});

function FitBounds({ zones }) {
  const map = useMap();
  const [hasInitialized, setHasInitialized] = useState(false);

  useEffect(() => {
    if (!zones || zones.length === 0) return;
    const bounds = [];
    zones.forEach(z => {
      if (z.hex_id) {
        try {
          const [lat, lng] = cellToLatLng(z.hex_id);
          bounds.push([lat, lng]);
        } catch(e) {}
      }
    });

    if (bounds.length > 0) {
      if (!hasInitialized) {
        map.fitBounds(bounds, { padding: [50, 50], animate: false });
        setHasInitialized(true);
      } else {
        map.flyToBounds(bounds, { padding: [50, 50], duration: 1.5 });
      }
    }
  }, [zones, map, hasInitialized]);
  return null;
}

export default function RiskMap({ onHexClick }) {
  const [zones, setZones] = useState([]);
  const [events, setEvents] = useState([]);
  const [selected, setSelected] = useState(null);
  const [isLoading, setIsLoading] = useState(true); 

  useEffect(() => {
    async function fetchData() {
      try {
        const { data: zoneData } = await supabase.from("h3_zone_states").select("*");
        const { data: eventData } = await supabase.from("disruption_events").select("*");
        setZones(zoneData || []);
        setEvents(eventData || []);
      } catch (err) {
        console.error("Map fetch error:", err);
      } finally {
        setIsLoading(false); 
      }
    }
    fetchData();
    const channel = supabase.channel('map-updates')
      .on('postgres_changes', { event: '*', schema: 'public', table: 'h3_zone_states' }, () => fetchData()
    ).subscribe();
    return () => supabase.removeChannel(channel);
  }, []);

  return (
    <div className="relative w-full h-[600px] rounded-[20px] overflow-hidden border border-slate-200 shadow-sm antigravity bg-slate-50">
      <div className="absolute top-4 left-4 z-[400] bg-white/90 backdrop-blur-md p-4 rounded-xl border border-slate-200 shadow-lg pointer-events-none">
        <h3 className="text-[11px] font-extrabold uppercase tracking-[0.2em] text-slate-500 mb-2">Live Sat-Link</h3>
        <div className="flex items-center gap-3">
           <div className="flex items-center gap-1.5 text-xs font-bold text-slate-700">
             <div className="w-3 h-3 rounded bg-[#34D399] opacity-50 border border-[#10B981]"></div>
             Monitored Zone
           </div>
           <div className="flex items-center gap-1.5 text-xs font-bold text-slate-700">
             <div className="w-3 h-3 rounded bg-[#FF3366] opacity-60 border border-[#E11A50]"></div>
             Active Payout Risk
           </div>
        </div>
      </div>

      {isLoading ? (
        <div className="w-full h-full flex flex-col items-center justify-center">
          <div className="w-10 h-10 border-4 border-[#2563EB]/20 border-t-[#2563EB] rounded-full animate-spin mb-4"></div>
          <p className="text-[11px] font-extrabold text-slate-400 uppercase tracking-[0.2em] animate-pulse">Acquiring Geospatial Lock...</p>
        </div>
      ) : (
        <MapContainer center={[0, 0]} zoom={2} style={{ width: "100%", height: "100%", zIndex: 10 }} zoomControl={false}>
          <TileLayer url="https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png" attribution='&copy; CartoDB' />
          <FitBounds zones={zones} />

          {/* DRAWING THE ACTUAL H3 HEXAGONS */}
          {zones.map((z, i) => {
            if (!z.hex_id) return null;
            try {
              const massiveHexId = cellToParent(z.hex_id, 7); 
              
              const boundary = cellToBoundary(massiveHexId);
              const isHighRisk = z.v_zone_score > 0.6;
              
              return (
                <Polygon
                  key={i}
                  positions={boundary}
                  pathOptions={{
                    color: isHighRisk ? "#FF3366" : "#10B981", // Brighter borders
                    fillColor: isHighRisk ? "#FF3366" : "#34D399",
                    fillOpacity: isHighRisk ? 0.5 : 0.2, // Made it slightly more opaque so it pops
                    weight: 3, // Thicker border for that enterprise look
                  }}
                  eventHandlers={{ 
                    click: () => {
                      setSelected(z);
                      if (onHexClick) onHexClick(z.hex_id); 
                    } 
                  }}
                />
              );
            } catch (error) { return null; }
          })}        </MapContainer>
      )}
    </div>
  );
}
