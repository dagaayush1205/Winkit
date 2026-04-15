import React from 'react';
import { Activity, BarChart3, ShieldCheck, Settings, LogOut, Zap, Users, Database } from 'lucide-react'; // 🔥 Added Database import
import SidebarItem from './SidebarItem';

const Sidebar = ({ activeView, setActiveView, triggerMockAction }) => {
  return (
    <nav className="w-20 flex flex-col border-r border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 z-50 transition-colors duration-300">
      <div className="h-20 flex items-center justify-center border-b border-slate-100 dark:border-slate-800">
        <div className="w-10 h-10 bg-blue-600 rounded-xl flex items-center justify-center shadow-lg shadow-blue-200 dark:shadow-blue-900/20 cursor-pointer" onClick={() => setActiveView('live')}>
          <Zap className="text-white fill-white" size={20} />
        </div>
      </div>
      <div className="flex-1 flex flex-col py-4">
        <SidebarItem icon={Activity} label="Live" active={activeView === 'live'} onClick={() => setActiveView('live')} />
        
        {/* 🔥 YOUR OLD PAGE: Now correctly labeled as Risk/Claims */}
        <SidebarItem icon={BarChart3} label="Risk" active={activeView === 'risk'} onClick={() => setActiveView('risk')} />
        
        {/* 🔥 YOUR NEW PAGE: The God Mode Database Ledger */}
        <SidebarItem icon={Database} label="Ledger" active={activeView === 'ledger'} onClick={() => setActiveView('ledger')} />
        
        <SidebarItem icon={Users} label="Profiles" active={activeView === 'profiles'} onClick={() => setActiveView('profiles')} />
        <SidebarItem icon={ShieldCheck} label="Fraud" active={activeView === 'fraud'} onClick={() => setActiveView('fraud')} />
      </div>
      <div className="border-t border-slate-100 dark:border-slate-800 py-4">
        <SidebarItem icon={Settings} label="Config" active={activeView === 'config'} onClick={() => setActiveView('config')} />
        <SidebarItem icon={LogOut} label="Exit" onClick={() => triggerMockAction('Logout Sequence')} />
      </div>
    </nav>
  );
};

export default Sidebar;
