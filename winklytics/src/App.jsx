import React, { useState, useEffect, useMemo } from 'react';
import { 
  Activity, AlertTriangle, ShieldCheck, Zap, TrendingUp, 
  Map as MapIcon, BarChart3, Settings, LogOut, Wallet, ShieldAlert, X
} from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import { cn } from './lib/utils';
import { supabase } from './supabase';
import DatabaseLedger from './components/DatabaseLedger';
// Components
import Sidebar from './components/Sidebar';
import Header from './components/Header';
import KPICard from './components/KPICard';
import RiskMap from './components/RiskMap';
import AuditItem from './components/AuditItem';
import FinancialBurnChart from './components/FinancialBurnChart';
import FraudTable from './components/FraudTable';
import RiskModule from './components/RiskModule';
import Login from './components/Login';
import ConfigModule from './components/ConfigModule'; 
import UserProfiles from './components/UserProfiles'; 

export default function App() {
  // THE GATEKEEPER STATE: Holds the dynamic user data
  const [user, setUser] = useState(null); 
  
  const [activeView, setActiveView] = useState('live');
  const [showNotifications, setShowNotifications] = useState(false);
  const [notifications, setNotifications] = useState([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [selectedHex, setSelectedHex] = useState(null);
  const [isSearchFocused, setIsSearchFocused] = useState(false);
  const [loading, setLoading] = useState(true);
  
  const [darkMode, setDarkMode] = useState(() => {
    if (typeof window !== 'undefined') {
      return localStorage.getItem('theme') === 'dark' || 
        (!localStorage.getItem('theme') && window.matchMedia('(prefers-color-scheme: dark)').matches);
    }
    return false;
  });

  useEffect(() => {
    if (darkMode) {
      document.documentElement.classList.add('dark');
      localStorage.setItem('theme', 'dark');
    } else {
      document.documentElement.classList.remove('dark');
      localStorage.setItem('theme', 'light');
    }
  }, [darkMode]);

  const [kpis, setKpis] = useState({ totalPremium: 0, totalPayouts: 0, activePolicies: 0, fraudBlocked: 0 });
  const [auditTrail, setAuditTrail] = useState([]);
  const [burnData, setBurnData] = useState([]);
  const [fraudData, setFraudData] = useState([]);
  const [riskMarkers, setRiskMarkers] = useState([]);

  const unreadCount = notifications.filter(n => !n.read).length;
  const markAllRead = () => setNotifications(notifications.map(n => ({ ...n, read: true })));

  const triggerMockAction = (actionName) => {
    if (actionName === 'Logout Sequence') {
      setUser(null); 
      setActiveView('live'); // Reset view for next login
    } else {
      alert(`[MOCK ACTION]: ${actionName} sequence initiated!`);
    }
    setSelectedHex(null); 
  };

  const fetchData = async () => {
    if (!supabase) {
      setLoading(false);
      return;
    }
    setLoading(true);
    try {
      const { data: premiumData } = await supabase.from('weekly_policies').select('premium_paid');
      const totalPremium = premiumData?.reduce((acc, curr) => acc + (curr.premium_paid || 0), 0) || 0;

      const { data: payoutData } = await supabase.from('claims_and_payouts').select('payout_amt').eq('status', 'AUTO_PAID');
      const totalPayouts = payoutData?.reduce((acc, curr) => acc + (curr.payout_amt || 0), 0) || 0;

      const { count: activePolicies } = await supabase.from('weekly_policies').select('*', { count: 'exact', head: true }).eq('status', 'ACTIVE');

      const { data: rejectedData } = await supabase.from('claims_and_payouts').select('payout_amt').eq('status', 'REJECTED').gt('fraud_flags_triggered', 0);
      const fraudBlocked = rejectedData?.reduce((acc, curr) => acc + (curr.payout_amt || 0), 0) || 0;

      setKpis({ totalPremium, totalPayouts, activePolicies: activePolicies || 0, fraudBlocked });

      const { data: auditData } = await supabase.from('claims_and_payouts').select('*').order('created_at', { ascending: false }).limit(10);
      setAuditTrail(auditData || []);

      const { data: burnRaw } = await supabase.from('claims_and_payouts').select('payout_amt, created_at').eq('status', 'AUTO_PAID');
      const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
      const aggregatedBurn = burnRaw?.reduce((acc, curr) => {
        const date = new Date(curr.created_at);
        const month = months[date.getMonth()];
        if (!acc[month]) acc[month] = 0;
        acc[month] += curr.payout_amt;
        return acc;
      }, {}) || {};

      const burnChartData = Object.entries(aggregatedBurn).map(([day, actual]) => ({
        day, actual, expected: actual * 0.8
      })).slice(-6);
      setBurnData(burnChartData);

      const { data: fraudRaw } = await supabase.from('claims_and_payouts').select('*, workers(name)').eq('status', 'REJECTED');
      setFraudData(fraudRaw || []);

      const { data: riskData } = await supabase.from('claims_and_payouts').select('latitude, longitude, status').not('latitude', 'is', null);
      setRiskMarkers(riskData || []);

    } catch (error) {
      console.error('Error fetching data:', error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
    if (!supabase) return;
    const channel = supabase.channel('audit-trail-changes')
      .on('postgres_changes', { event: 'INSERT', table: 'claims_and_payouts', schema: 'public' }, (payload) => {
        setAuditTrail(prev => [payload.new, ...prev].slice(0, 10));
        const newNotif = {
          id: Date.now(), title: 'New Claim Event', message: `A new claim event has been logged: ${payload.new.status}`,
          time: 'Just now', type: payload.new.status === 'REJECTED' ? 'alert' : 'info', read: false
        };
        setNotifications(prev => [newNotif, ...prev]);
      }).subscribe();
    return () => supabase.removeChannel(channel);
  }, []);

  const filteredAuditTrail = useMemo(() => {
    let filtered = auditTrail;
    if (statusFilter !== 'ALL') filtered = filtered.filter(item => item.status === statusFilter);
    if (!searchQuery) return filtered;
    return filtered.filter(item => item.status.toLowerCase().includes(searchQuery.toLowerCase()) || (item.id && item.id.toLowerCase().includes(searchQuery.toLowerCase())));
  }, [searchQuery, statusFilter, auditTrail]);

  const formatCurrency = (val) => {
    if (val >= 1000000) return `₹${(val / 1000000).toFixed(1)}M`;
    if (val >= 1000) return `₹${(val / 1000).toFixed(1)}k`;
    return `₹${val}`;
  };

  // -------------------------------------------------------------
  // THE GATEKEEPER: If not logged in, return the Login screen
  // -------------------------------------------------------------
  if (!user) {
    return <Login onLogin={setUser} />;
  }

  // -------------------------------------------------------------
  // MAIN DASHBOARD (Only visible if authenticated)
  // -------------------------------------------------------------
  return (
    <div className="flex h-screen bg-slate-50 dark:bg-slate-950 text-slate-800 dark:text-slate-200 font-sans overflow-hidden transition-colors duration-300">
      <Sidebar activeView={activeView} setActiveView={setActiveView} triggerMockAction={triggerMockAction} />

      <main className="flex-1 flex flex-col overflow-hidden relative">
        <Header 
          user={user} 
          searchQuery={searchQuery} setSearchQuery={setSearchQuery} isSearchFocused={isSearchFocused}
          setIsSearchFocused={setIsSearchFocused} filteredAuditTrail={filteredAuditTrail}
          showNotifications={showNotifications} setShowNotifications={setShowNotifications}
          unreadCount={unreadCount} notifications={notifications} markAllRead={markAllRead}
          darkMode={darkMode} setDarkMode={setDarkMode} statusFilter={statusFilter}
          setStatusFilter={setStatusFilter} triggerMockAction={triggerMockAction}
        />

        <div className="flex-1 overflow-y-auto p-8 space-y-8">
          {!supabase && (
            <div className="bg-amber-50 border border-amber-200 p-4 rounded-xl flex items-center gap-3">
              <AlertTriangle className="text-amber-500" size={20} />
              <div>
                <p className="text-sm font-bold text-amber-900">Supabase Configuration Required</p>
                <p className="text-xs text-amber-700">Please set VITE_SUPABASE_URL and VITE_SUPABASE_ANON_KEY.</p>
              </div>
            </div>
          )}
          
          {activeView === 'live' && (
            <>
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
                <KPICard title="Total Premium Pool" value={formatCurrency(kpis.totalPremium)} subtext={`Across ${kpis.activePolicies} active policies`} icon={TrendingUp} iconColor="text-blue-600" trend="+12.4%" loading={loading} />
                <KPICard title="Live Disruptions" value="2 Active" subtext="Sector 4 Flooding, GST Road Protest" icon={AlertTriangle} iconColor="text-amber-500" loading={loading} />
                <KPICard title="Total Payouts (AUTO_PAID)" value={formatCurrency(kpis.totalPayouts)} subtext="Disbursed instantly to riders" icon={Zap} iconColor="text-emerald-600" loading={loading} />
                <KPICard title="Fraud Blocked" value={formatCurrency(kpis.fraudBlocked)} subtext="Saved (Rejected with Fraud Flags)" icon={ShieldAlert} iconColor="text-rose-600" trend="-8.2%" loading={loading} />
              </div>

              <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
                <div className="lg:col-span-2 flex flex-col gap-4">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      <MapIcon size={18} className="text-slate-400" />
                      <h2 className="text-sm font-bold text-slate-900 dark:text-slate-100 uppercase tracking-wider">Live Geospatial Risk Map</h2>
                    </div>
                  </div>
                  <div className="h-[500px] relative">
                    <RiskMap onHexClick={(id) => setSelectedHex(id)} markers={riskMarkers} />
                    <AnimatePresence>
                      {selectedHex !== null && (
                        <motion.div initial={{ opacity: 0, x: 20 }} animate={{ opacity: 1, x: 0 }} exit={{ opacity: 0, x: 20 }} className="absolute top-4 right-4 w-64 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl shadow-2xl p-4 z-30">
                          <div className="flex justify-between items-center mb-4">
                            <h3 className="text-xs font-bold text-slate-900 dark:text-slate-100 uppercase tracking-wider">Hex Details: #{selectedHex.slice(0,6)}...</h3>
                            <button onClick={() => setSelectedHex(null)} className="text-slate-400 hover:text-slate-600"><X size={14} /></button>
                          </div>
                          <div className="space-y-3">
                            <button onClick={() => triggerMockAction('Deploy Emergency Agent')} className="w-full mt-2 py-2 bg-blue-600 text-white text-[10px] font-bold uppercase rounded-lg hover:bg-blue-700 transition-colors">Deploy Agent</button>
                          </div>
                        </motion.div>
                      )}
                    </AnimatePresence>
                  </div>
                </div>

                <div className="flex flex-col gap-4">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      <Activity size={18} className="text-slate-400" />
                      <h2 className="text-sm font-bold text-slate-900 dark:text-slate-100 uppercase tracking-wider">Real-Time Audit Trail</h2>
                    </div>
                    <button onClick={() => triggerMockAction('View Full Audit Log')} className="text-[10px] font-bold text-blue-600 hover:underline uppercase">View All</button>
                  </div>
                  <div className="flex-1 space-y-3 overflow-y-auto pr-2 scrollbar-hide">
                    {loading ? Array.from({ length: 5 }).map((_, i) => <div key={`skel-${i}`} className="h-20 bg-slate-100 dark:bg-slate-800 animate-pulse rounded-xl" />) 
                    : filteredAuditTrail.length > 0 ? filteredAuditTrail.map((item) => <AuditItem key={item.id || Math.random()} item={item} />) 
                    : <div className="text-slate-400 text-xs italic p-8 border-2 border-dashed border-slate-100 rounded-2xl text-center">No entries match filters</div>}
                  </div>
                </div>
              </div>
              <FinancialBurnChart data={burnData} loading={loading} />
            </>
          )}

          {/* ROUTING FOR OTHER MODULES */}
          {activeView === 'fraud' && <FraudTable data={fraudData} loading={loading} triggerMockAction={triggerMockAction} />}
          
          {/* 🔥 Your Old Page */}
          {activeView === 'risk' && <RiskModule />} 
          
          {/* 🔥 Your New Page */}
          {activeView === 'ledger' && <DatabaseLedger />} 
          
          {activeView === 'config' && <ConfigModule />}
          {activeView === 'profiles' && <UserProfiles triggerMockAction={triggerMockAction} />}          {/* FALLBACK FOR UNBUILT MODULES */}
          {activeView !== 'live' && activeView !== 'fraud' && activeView !== 'risk' && activeView !== 'config' && activeView !== 'profiles' && activeView !== 'ledger' && (
            <div className="flex flex-col items-center justify-center h-[60vh] text-center space-y-4">
              <div className="w-16 h-16 bg-slate-100 dark:bg-slate-800 rounded-full flex items-center justify-center text-slate-300"><Settings size={32} /></div>
              <div>
                <h3 className="text-lg font-bold text-slate-900 dark:text-slate-100 uppercase tracking-wider">{activeView} Module</h3>
                <p className="text-sm text-slate-400">This section is currently under development.</p>
              </div>
              <button onClick={() => setActiveView('live')} className="px-6 py-2 bg-blue-600 text-white text-xs font-bold uppercase rounded-lg">Return Home</button>
            </div>
          )}
        </div>
      </main>

      {/* Quick Actions Floating Menu */}
      <div className="fixed bottom-8 right-8 flex flex-col gap-3 items-end z-50">
        <AnimatePresence>
          {selectedHex !== null && (
            <motion.div initial={{ opacity: 0, scale: 0.8, y: 20 }} animate={{ opacity: 1, scale: 1, y: 0 }} exit={{ opacity: 0, scale: 0.8, y: 20 }} className="bg-white border border-slate-200 rounded-2xl shadow-2xl p-4 flex items-center gap-4">
              <div className="flex items-center gap-2">
                <div className="w-2 h-2 rounded-full bg-rose-500" />
                <span className="text-xs font-bold text-slate-900">Hex Selected</span>
              </div>
              <div className="h-4 w-px bg-slate-200" />
              <button onClick={() => triggerMockAction('Relocate Riders')} className="text-[10px] font-bold text-blue-600 hover:underline uppercase">Relocate Riders</button>
              <button onClick={() => triggerMockAction('Block Hex Payouts')} className="text-[10px] font-bold text-rose-600 hover:underline uppercase">Block Payouts</button>
            </motion.div>
          )}
        </AnimatePresence>
        <button onClick={() => triggerMockAction('Global Agent Override')} className="w-14 h-14 bg-blue-600 hover:bg-blue-500 text-white rounded-2xl flex items-center justify-center shadow-xl shadow-blue-200 transition-all hover:scale-110 active:scale-95 group">
          <Zap className="group-hover:fill-white transition-all" size={24} />
        </button>
      </div>
    </div>
  );
}
