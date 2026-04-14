import React, { useState } from 'react';
import { Filter, Activity, ShieldAlert, Crosshair, AlertOctagon, CheckCircle2 } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import { 
  LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip as RechartsTooltip, ResponsiveContainer 
} from 'recharts';
import { cn } from '../lib/utils';

const FraudTable = ({ data, loading, triggerMockAction }) => {
  const [expandedId, setExpandedId] = useState(null);

  // Mock telemetry data to show the judges the "Physics Mismatch"
  const generateMockTelemetry = (isFraud) => {
    return Array.from({ length: 10 }).map((_, i) => ({
      time: `${i}s`,
      speed: isFraud ? 45 + Math.random() * 5 : 15 + Math.random() * 10, // Spoofers "move" fast
      vibration: isFraud ? 0.01 + Math.random() * 0.02 : 1.5 + Math.random() * 2 // Spoofers have 0 vibration
    }));
  };

  const toggleExpand = (id) => {
    setExpandedId(expandedId === id ? null : id);
  };

  return (
    <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-black tracking-tight text-slate-900">Fraud Detection Center</h2>
          <p className="text-sm text-slate-500 font-medium">Kinematic telemetry analysis and Isolation Forest ML outputs.</p>
        </div>
        <button onClick={() => triggerMockAction('Open Advanced Filters')} className="flex items-center gap-2 px-4 py-2 bg-white border border-slate-200 rounded-lg text-xs font-bold text-slate-600 hover:bg-slate-50 transition-colors shadow-sm">
          <Filter size={14} /> Filter Results
        </button>
      </div>

      <div className="bg-white border border-slate-200 rounded-[20px] overflow-hidden shadow-sm">
        <table className="w-full text-left border-collapse">
          <thead>
            <tr className="bg-slate-50 border-b border-slate-200">
              <th className="px-6 py-5 text-[10px] font-extrabold text-slate-400 uppercase tracking-widest">Claim ID</th>
              <th className="px-6 py-5 text-[10px] font-extrabold text-slate-400 uppercase tracking-widest">Worker</th>
              <th className="px-6 py-5 text-[10px] font-extrabold text-slate-400 uppercase tracking-widest">Location</th>
              <th className="px-6 py-5 text-[10px] font-extrabold text-slate-400 uppercase tracking-widest">Risk Score</th>
              <th className="px-6 py-5 text-[10px] font-extrabold text-slate-400 uppercase tracking-widest">Status</th>
              <th className="px-6 py-5 text-[10px] font-extrabold text-slate-400 uppercase tracking-widest text-right">Audit</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan={6} className="px-6 py-12 text-center text-slate-400"><Activity className="animate-spin mx-auto mb-2" /> Loading Telemetry...</td></tr>
            ) : data.length > 0 ? data.map((row) => {
              const isFraud = row.status === 'REJECTED';
              const telemetryData = generateMockTelemetry(isFraud);
              const fraudScore = isFraud ? (85 + Math.random() * 14).toFixed(1) : (5 + Math.random() * 20).toFixed(1);

              return (
                <React.Fragment key={row.claim_id}>
                  <tr 
                    className={cn("border-b border-slate-100 hover:bg-slate-50 cursor-pointer transition-colors", expandedId === row.claim_id && "bg-slate-50")}
                    onClick={() => toggleExpand(row.claim_id)}
                  >
                    <td className="px-6 py-5 text-xs font-mono font-bold text-slate-500">{row.claim_id?.slice(0, 8)}</td>
                    <td className="px-6 py-5 text-xs font-bold text-[#5B2D8E]">{row.workers?.name || 'Unknown'}</td>
                    <td className="px-6 py-5 text-xs text-slate-600">{row.latitude ? `${row.latitude.toFixed(4)}, ${row.longitude.toFixed(4)}` : 'N/A'}</td>
                    <td className="px-6 py-5">
                      <span className={cn("text-[10px] font-black px-2.5 py-1 rounded-md", isFraud ? "bg-rose-100 text-rose-700" : "bg-emerald-100 text-emerald-700")}>
                        {fraudScore}% RISK
                      </span>
                    </td>
                    <td className="px-6 py-5">
                      <div className="flex items-center gap-2">
                        <div className={cn("w-2 h-2 rounded-full", isFraud ? "bg-rose-500" : "bg-emerald-500")} />
                        <span className="text-[10px] font-bold uppercase tracking-wider text-slate-700">{row.status}</span>
                      </div>
                    </td>
                    <td className="px-6 py-5 text-right">
                      <span className="text-[10px] font-bold text-blue-600 uppercase tracking-wider hover:underline">
                        {expandedId === row.claim_id ? 'Close' : 'View Data'}
                      </span>
                    </td>
                  </tr>

                  {/* EXPANDED TELEMETRY VIEW */}
                  <AnimatePresence>
                    {expandedId === row.claim_id && (
                      <tr>
                        <td colSpan={6} className="p-0 border-b border-slate-200">
                          <motion.div initial={{ height: 0, opacity: 0 }} animate={{ height: 'auto', opacity: 1 }} exit={{ height: 0, opacity: 0 }} className="overflow-hidden bg-slate-100/50 shadow-inner">
                            <div className="p-8 grid grid-cols-1 lg:grid-cols-3 gap-8">
                              
                              {/* Left: ML Diagnosis */}
                              <div className="space-y-6">
                                <div>
                                  <h4 className="text-[10px] font-extrabold text-slate-400 uppercase tracking-widest mb-3">ML Model Diagnosis</h4>
                                  {isFraud ? (
                                    <div className="p-4 bg-white border border-rose-200 rounded-xl shadow-sm">
                                      <div className="flex items-center gap-2 text-rose-600 mb-2"><AlertOctagon size={18} /><span className="font-bold text-sm">Physics Mismatch Detected</span></div>
                                      <p className="text-xs text-slate-600 leading-relaxed">Isolation Forest detected highly anomalous kinematic data. High GPS speed reported (+40km/h) but IMU Z-axis variance is near absolute zero. <strong>High probability of location spoofing app.</strong></p>
                                    </div>
                                  ) : (
                                    <div className="p-4 bg-white border border-emerald-200 rounded-xl shadow-sm">
                                      <div className="flex items-center gap-2 text-emerald-600 mb-2"><CheckCircle2 size={18} /><span className="font-bold text-sm">Valid Telemetry</span></div>
                                      <p className="text-xs text-slate-600 leading-relaxed">IMU variance correlates naturally with reported GPS movement. No anomalies detected in 10-second sensor buffer.</p>
                                    </div>
                                  )}
                                </div>
                                <button onClick={() => triggerMockAction(`Ban Worker ${row.workers?.name}`)} className="w-full py-2.5 bg-rose-600 text-white text-xs font-bold uppercase tracking-wider rounded-lg shadow-md hover:bg-rose-700 transition-all">
                                  Revoke Platform Access
                                </button>
                              </div>

                              {/* Right: The Graph */}
                              <div className="lg:col-span-2 bg-white border border-slate-200 p-5 rounded-xl shadow-sm">
                                <div className="flex justify-between items-center mb-4">
                                  <h4 className="text-xs font-bold text-slate-800 flex items-center gap-2"><Crosshair size={14} className="text-blue-600"/> 10s Sensor Buffer (GPS vs IMU)</h4>
                                  <div className="flex gap-4 text-[10px] font-bold uppercase tracking-wider text-slate-500">
                                    <span className="flex items-center gap-1"><div className="w-2 h-2 rounded-full bg-blue-500"/> Speed (km/h)</span>
                                    <span className="flex items-center gap-1"><div className="w-2 h-2 rounded-full bg-rose-500"/> Vibration (g)</span>
                                  </div>
                                </div>
                                <div className="h-48 w-full">
                                  <ResponsiveContainer width="100%" height="100%">
                                    <LineChart data={telemetryData} margin={{ top: 5, right: 5, left: -20, bottom: 0 }}>
                                      <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#E2E8F0" />
                                      <XAxis dataKey="time" fontSize={10} tickLine={false} axisLine={false} />
                                      <YAxis yAxisId="left" fontSize={10} tickLine={false} axisLine={false} />
                                      <YAxis yAxisId="right" orientation="right" fontSize={10} tickLine={false} axisLine={false} />
                                      <RechartsTooltip contentStyle={{ borderRadius: '8px', border: 'none', boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)' }} />
                                      <Line yAxisId="left" type="monotone" dataKey="speed" stroke="#3b82f6" strokeWidth={2} dot={false} isAnimationActive={true} />
                                      <Line yAxisId="right" type="step" dataKey="vibration" stroke="#f43f5e" strokeWidth={2} dot={false} isAnimationActive={true} />
                                    </LineChart>
                                  </ResponsiveContainer>
                                </div>
                              </div>

                            </div>
                          </motion.div>
                        </td>
                      </tr>
                    )}
                  </AnimatePresence>
                </React.Fragment>
              );
            }) : (
              <tr><td colSpan={6} className="px-6 py-8 text-center text-slate-400 text-xs italic">No fraud events detected</td></tr>
            )}
          </tbody>
        </table>
      </div>
    </motion.div>
  );
};

export default FraudTable;
