import React, { useState } from 'react';
import { motion } from 'framer-motion';
import { Settings, Save, CloudRain, Shield, Activity, Database } from 'lucide-react';
import { cn } from '../lib/utils';

export default function ConfigModule() {
  const [rainThreshold, setRainThreshold] = useState(40);
  const [fraudStrictness, setFraudStrictness] = useState('High');
  const [isSaving, setIsSaving] = useState(false);

  const handleSave = () => {
    setIsSaving(true);
    setTimeout(() => {
      setIsSaving(false);
      alert('Parametric thresholds updated successfully.');
    }, 1200);
  };

  return (
    <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} className="space-y-8 pb-20 px-8 pt-8 max-w-5xl mx-auto">
      <div>
        <h2 className="text-2xl font-black tracking-tight text-slate-900 dark:text-slate-100">System Configuration</h2>
        <p className="text-sm text-slate-500 font-medium mt-1">Manage parametric trigger thresholds and system integrations.</p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
        {/* Parametric Triggers */}
        <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl p-6 shadow-sm antigravity">
          <div className="flex items-center gap-3 mb-6">
            <div className="p-2 bg-blue-50 dark:bg-blue-900/20 text-blue-600 rounded-lg"><CloudRain size={20} /></div>
            <h3 className="font-bold text-slate-900 dark:text-slate-100 uppercase tracking-wider text-sm">Parametric Triggers</h3>
          </div>
          
          <div className="space-y-6">
            <div>
              <div className="flex justify-between items-center mb-2">
                <label className="text-xs font-bold text-slate-500 uppercase tracking-wider">Rainfall Payout Threshold</label>
                <span className="text-sm font-black text-blue-600">{rainThreshold} mm</span>
              </div>
              <input 
                type="range" min="10" max="100" step="5" value={rainThreshold} onChange={(e) => setRainThreshold(e.target.value)}
                className="w-full h-2 bg-slate-200 rounded-lg appearance-none cursor-pointer accent-blue-600"
              />
              <p className="text-[10px] text-slate-400 mt-2">Payouts auto-trigger when OpenWeatherMap reports rain &gt; {rainThreshold}mm.</p>
            </div>
          </div>
        </div>

        {/* Security & Fraud */}
        <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl p-6 shadow-sm antigravity">
          <div className="flex items-center gap-3 mb-6">
            <div className="p-2 bg-rose-50 dark:bg-rose-900/20 text-rose-600 rounded-lg"><Shield size={20} /></div>
            <h3 className="font-bold text-slate-900 dark:text-slate-100 uppercase tracking-wider text-sm">Anti-Fraud Engine</h3>
          </div>
          
          <div className="space-y-4">
            <label className="text-xs font-bold text-slate-500 uppercase tracking-wider">GPS Spoofing Strictness</label>
            <div className="grid grid-cols-3 gap-3">
              {['Low', 'Medium', 'High'].map(level => (
                <button 
                  key={level} onClick={() => setFraudStrictness(level)}
                  className={cn("py-2 px-4 rounded-xl text-xs font-bold transition-all border", fraudStrictness === level ? "bg-rose-50 border-rose-200 text-rose-600" : "bg-slate-50 border-slate-200 text-slate-500 hover:bg-slate-100")}
                >
                  {level}
                </button>
              ))}
            </div>
            <p className="text-[10px] text-slate-400">High strictness blocks payouts if Z-axis variance is less than 0.5% during movement.</p>
          </div>
        </div>

        {/* Integrations */}
        <div className="md:col-span-2 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl p-6 shadow-sm antigravity">
          <div className="flex items-center gap-3 mb-6">
            <div className="p-2 bg-emerald-50 dark:bg-emerald-900/20 text-emerald-600 rounded-lg"><Database size={20} /></div>
            <h3 className="font-bold text-slate-900 dark:text-slate-100 uppercase tracking-wider text-sm">API Integrations</h3>
          </div>
          
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div className="p-4 border border-slate-100 dark:border-slate-800 rounded-xl">
              <div className="flex justify-between items-center mb-2">
                <span className="font-bold text-sm text-slate-900 dark:text-white">Supabase Connection</span>
                <span className="text-[10px] font-bold px-2 py-1 bg-emerald-100 text-emerald-600 rounded uppercase">Connected</span>
              </div>
              <p className="text-xs font-mono text-slate-400 truncate">wss://zjx...supabase.co/realtime</p>
            </div>
            <div className="p-4 border border-slate-100 dark:border-slate-800 rounded-xl">
              <div className="flex justify-between items-center mb-2">
                <span className="font-bold text-sm text-slate-900 dark:text-white">OpenWeatherMap API</span>
                <span className="text-[10px] font-bold px-2 py-1 bg-emerald-100 text-emerald-600 rounded uppercase">Active Ping</span>
              </div>
              <p className="text-xs font-mono text-slate-400 truncate">Last poll: 2 seconds ago</p>
            </div>
          </div>
        </div>
      </div>

      <div className="flex justify-end pt-4 border-t border-slate-200 dark:border-slate-800">
        <button 
          onClick={handleSave}
          className="flex items-center gap-2 px-6 py-3 bg-blue-600 text-white rounded-xl text-sm font-bold shadow-lg shadow-blue-500/30 hover:bg-blue-700 transition-all active:scale-95"
        >
          {isSaving ? <Activity className="animate-spin" size={18} /> : <Save size={18} />}
          {isSaving ? 'Updating Engine...' : 'Save Configuration'}
        </button>
      </div>
    </motion.div>
  );
}
