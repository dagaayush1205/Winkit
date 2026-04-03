import React, { useEffect, useState } from "react";
import DeckGL from '@deck.gl/react';
import { H3HexagonLayer } from '@deck.gl/geo-layers';
import { Map } from 'react-map-gl/maplibre';
import 'maplibre-gl/dist/maplibre-gl.css';
import { supabase } from "../supabase";

export default function RiskMap({ onHexClick }) {
  const [hexData, setHexData] = useState([]);
  const [hoverInfo, setHoverInfo] = useState(null);
  const [isLoading, setIsLoading] = useState(true);

  // Default camera angle: Looking at Chennai with a 45-degree 3D pitch
  const initialViewState = {
    longitude: 80.0435, // Potheri Longitude
    latitude: 12.8236,  // Potheri Latitude
    zoom: 12,
    pitch: 45,
    bearing: 0
  };

  const fetchData = async () => {
    if (!supabase) {
      setIsLoading(false);
      return;
    }
    try {
      // 1. Fetch Zone Infrastructure Risk
      const { data: zones } = await supabase.from("h3_zone_states").select("*");
      
      // 2. Fetch Live Rider Telemetry to count active drivers per hex
      const { data: telemetry } = await supabase.from("raw_gps_telemetry").select("h3_hex_id");

      // Count drivers in each hex
      const driverCounts = {};
      if (telemetry) {
        telemetry.forEach(t => {
          if (t.h3_hex_id) {
            driverCounts[t.h3_hex_id] = (driverCounts[t.h3_hex_id] || 0) + 1;
          }
        });
      }

      // Merge data for the WebGL Layer
      const mergedData = (zones || []).map(z => ({
        hex_id: z.hex_id,
        zone_name: z.zone_name,
        v_zone_score: z.v_zone_score || 0,
        yesterday_water: z.yesterday_water || 0,
        active_drivers: driverCounts[z.hex_id] || 0
      }));

      setHexData(mergedData);
    } catch (err) {
      console.error("Map fetch error:", err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
    
    if (!supabase) return;

    // Listen for real-time updates when the Python Pricing Engine updates a zone
    // or when a new GPS ping hits the telemetry table!
    const channel = supabase.channel('map-updates')
      .on('postgres_changes', { event: '*', schema: 'public', table: 'h3_zone_states' }, () => fetchData())
      .on('postgres_changes', { event: 'INSERT', schema: 'public', table: 'raw_gps_telemetry' }, () => fetchData())
      .subscribe();
      
    return () => supabase.removeChannel(channel);
  }, []);

  // Configure the 3D H3 Layer
  const layer = new H3HexagonLayer({
    id: 'h3-hexagon-layer',
    data: hexData,
    pickable: true,
    wireframe: false,
    filled: true,
    extruded: true,
    elevationScale: 20, // Multiplier for the 3D height
    getHexagon: d => d.hex_id,
    
    // COLOR LOGIC: Green (Safe) to Red (High Risk) based on v_zone_score
    getFillColor: d => {
      const risk = Math.min(d.v_zone_score, 1.0);
      return [
        Math.round(16 + (225 - 16) * risk),  // R
        Math.round(185 + (26 - 185) * risk), // G
        Math.round(129 + (80 - 129) * risk), // B
        200 // Alpha Transparency
      ];
    },
    
    // 3D ELEVATION LOGIC: Taller hexes = More active drivers in that zone
    getElevation: d => (d.active_drivers * 10) + 5, 
    
    onHover: info => setHoverInfo(info),
    onClick: info => {
      if (info.object && onHexClick) onHexClick(info.object.hex_id);
    }
  });

  return (
    <div className="relative w-full h-[600px] rounded-[20px] overflow-hidden border border-slate-200 dark:border-slate-800 shadow-sm bg-slate-900">
      
      {/* Map Legend */}
      <div className="absolute top-4 left-4 z-[400] bg-slate-900/90 backdrop-blur-md p-4 rounded-xl border border-slate-700 shadow-lg pointer-events-none">
        <h3 className="text-[10px] font-extrabold uppercase tracking-[0.2em] text-slate-400 mb-3">Live Sat-Link</h3>
        <div className="space-y-2">
           <div className="flex items-center gap-2 text-xs font-bold text-slate-200">
             <div className="w-3 h-3 rounded bg-emerald-500 opacity-80"></div>
             Infrastructure Stable
           </div>
           <div className="flex items-center gap-2 text-xs font-bold text-slate-200">
             <div className="w-3 h-3 rounded bg-rose-500 opacity-80"></div>
             High Risk (Flooding/Civic)
           </div>
           <div className="flex items-center gap-2 text-xs font-bold text-slate-400 mt-2 pt-2 border-t border-slate-700">
             <span>📏 3D Height = Active Riders</span>
           </div>
        </div>
      </div>

      {isLoading && (
        <div className="absolute inset-0 z-50 bg-slate-900/50 flex flex-col items-center justify-center backdrop-blur-sm">
          <div className="w-10 h-10 border-4 border-blue-500/30 border-t-blue-500 rounded-full animate-spin mb-4"></div>
          <p className="text-[11px] font-extrabold text-blue-400 uppercase tracking-[0.2em] animate-pulse">Rendering 3D Grid...</p>
        </div>
      )}

      <DeckGL
        initialViewState={initialViewState}
        controller={true}
        layers={[layer]}
      >
        {/* THE FREE MAPLIBRE CARTOCDN LAYER */}
        <Map
          mapStyle="https://basemaps.cartocdn.com/gl/dark-matter-gl-style/style.json"
        />

        {/* Custom Hover Tooltip */}
        {hoverInfo && hoverInfo.object && (
          <div 
            className="absolute z-[500] pointer-events-none bg-slate-900/95 border border-slate-700 text-white p-3 rounded-xl shadow-2xl transform -translate-x-1/2 -translate-y-[120%]"
            style={{ left: hoverInfo.x, top: hoverInfo.y }}
          >
            <h4 className="text-xs font-black uppercase mb-1">{hoverInfo.object.zone_name || 'H3 Zone'}</h4>
            <div className="grid grid-cols-2 gap-x-4 gap-y-1 text-[10px]">
              <span className="text-slate-400">Risk Score:</span>
              <span className="font-bold text-right text-rose-400">{(hoverInfo.object.v_zone_score * 100).toFixed(0)}%</span>
              <span className="text-slate-400">Standing Water:</span>
              <span className="font-bold text-right text-blue-400">{(hoverInfo.object.yesterday_water * 100).toFixed(0)}%</span>
              <span className="text-slate-400">Active Riders:</span>
              <span className="font-bold text-right text-emerald-400">{hoverInfo.object.active_drivers}</span>
            </div>
          </div>
        )}
      </DeckGL>
    </div>
  );
}
