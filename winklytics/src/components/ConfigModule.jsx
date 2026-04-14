import React, { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Settings, Save, CloudRain, Shield, Activity, Database, Zap, AlertTriangle } from 'lucide-react';
import { cn } from '../lib/utils';

export default function ConfigModule() {
  const [rainThreshold, setRainThreshold] = useState(40);
  const [fraudStrictness, setFraudStrictness] = useState('High');
  const [isSaving, setIsSaving] = useState(false);
  const [isSimulating, setIsSimulating] = useState(false);
  const [surgeActive, setSurgeActive] = useState(false);

  const handleSave = () => {
    setIsSaving(true);
    setTimeout(() => {
      setIsSaving(false);
      alert('Parametric thresholds updated successfully.');
    }, 1200);
  };

  const triggerDisaster = () => {
    setIsSimulating(true);
    // Simulate the engine calculating risk
    setTimeout(() => {
      setIsSimulating(false);
      setSurgeActive(true);
    }, 2000);
  };

  const resetSimulation = () => setSurgeActive(false);

  return (
    <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} className="space-y-8 pb-20 px-8 pt-8 max-w-5xl mx-auto">
      <div>
        <h2 className="text-2xl font-black tracking-tight text-slate-900">System Configuration & War Room</h2>
        <p className="text-sm text-slate-500 font-medium mt-1">Manage thresholds, integrations, and run actuarial simulations.</p>
      </div>

      {/* NEW: THE WAR ROOM SIMULATOR */}
      <div className={cn("border-2 rounded-[20px] p-6 transition-all duration-700 relative overflow-hidden", surgeActive ? "bg-rose-50 border-rose-200" : "bg-white border-slate-200 shadow-sm")}>
        {surgeActive && <div className="absolute top-0 left-0 w-full h-1 bg-rose-500 animate-pulse" />}
        
        <div className="flex items-center justify-between mb-6">
          <div className="flex items-center gap-3">
            <div className={cn("p-2 rounded-lg", surgeActive ? "bg-rose-200 text-rose-700" : "bg-slate-100 text-slate-600")}><AlertTriangle size={20} /></div>
            <h3 className="font-bold text-slate-900 uppercase tracking-wider text-sm">Solvency Stress Test (Simulation)</h3>
          </div>
          {surgeActive && <span className="px-3 py-1 bg-rose-600 text-white text-[10px] font-black uppercase tracking-widest rounded-full animate-pulse">Category 4 Cyclone Active</span>}
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 items-center">
          <div className="md:col-span-2 space-y-2">
            <p className="text-xs text-slate-600 leading-relaxed">Test the Dynamic Pricing Engine's response to catastrophic weather events. This simulates a massive spike in H3 Hex risk scores across Chennai, triggering instant premium recalculation to maintain portfolio solvency.</p>
          </div>
          
          <div className="flex flex-col gap-3 items-end">
            {!surgeActive ? (
              <button onClick={triggerDisaster} disabled={isSimulating} className="w-full flex items-center justify-center gap-2 px-6 py-3 bg-slate-900 text-white rounded-xl text-xs font-bold shadow-lg hover:bg-slate-800 transition-all disabled:opacity-70">
                {isSimulating ? <Activity className="animate-spin" size={16} /> : <Zap size={16} />}
                {isSimulating ? 'Running Models...' : 'Simulate Chennai Floods'}
              </button>
            ) : (
              <button onClick={resetSimulation} className="w-full px-6 py-3 bg-white border border-slate-300 text-slate-700 rounded-xl text-xs font-bold shadow-sm hover:bg-slate-50 transition-all">
                Reset Environment
              </button>
            )}
          </div>
        </div>

        {/* Dynamic Pricing Output Panel */}
        <AnimatePresence>
          {surgeActive && (
            <motion.div initial={{ opacity: 0, height: 0 }} animate={{ opacity: 1, height: 'auto' }} exit={{ opacity: 0, height: 0 }} className="mt-6 pt-6 border-t border-rose-200/50 grid grid-cols-3 gap-6">
              <div className="bg-white p-4 rounded-xl border border-rose-100 shadow-sm">
                <p className="text-[10px] font-bold text-slate-400 uppercase tracking-wider">Avg Hex Risk Score</p>
                <p className="text-2xl font-black text-rose-600 mt-1">89.4%</p>
                <p className="text-[10px] text-rose-500 font-bold mt-1">↑ +72.1% spike</p>
              </div>
              <div className="bg-white p-4 rounded-xl border border-rose-100 shadow-sm">
                <p className="text-[10px] font-bold text-slate-400 uppercase tracking-wider">Est. Payout Liability</p>
                <p className="text-2xl font-black text-slate-900 mt-1">₹4.2M</p>
                <p className="text-[10px] text-rose-500 font-bold mt-1">Threshold exceeded</p>
              </div>
              <div className="bg-rose-600 p-4 rounded-xl shadow-lg shadow-rose-500/30">
                <p className="text-[10px] font-bold text-rose-200 uppercase tracking-wider">New Base Premium</p>
                <div className="flex items-baseline gap-2 mt-1">
                  <p className="text-3xl font-black text-white">₹249</p>
                  <p className="text-xs text-rose-200 line-through">₹49</p>
                </div>
                <p className="text-[10px] text-white font-bold mt-1 tracking-wide">SURGE PRICING ENGAGED</p>
              </div>
            </motion.div>
          )}
        </AnimatePresence>
      </div>

      {/* KEEP YOUR EXISTING PARAMETRIC TRIGGERS, FRAUD, AND API INTEGRATION CARDS BELOW THIS */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
        {/* ... (Your existing Code for Triggers, Fraud, and DB Connections) ... */}
      </div>

    </motion.div>
  );
}
