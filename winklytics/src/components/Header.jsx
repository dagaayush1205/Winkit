import React from 'react';
import { Search, Bell, Sun, Moon } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import { cn } from '../lib/utils';

const Header = ({ 
  user, // <--- THIS WAS MISSING!
  searchQuery, 
  setSearchQuery, 
  isSearchFocused, 
  setIsSearchFocused, 
  filteredAuditTrail, 
  showNotifications, 
  setShowNotifications, 
  unreadCount, 
  notifications, 
  markAllRead,
  darkMode,
  setDarkMode,
  statusFilter,
  setStatusFilter,
  triggerMockAction
}) => {
  return (
    <header className="h-20 border-b border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 flex items-center justify-between px-8 z-40 transition-colors duration-300">
      <div className="flex items-center gap-6">
        <div>
          <h1 className="text-xl font-bold text-slate-900 dark:text-slate-100 tracking-tight">Winklytics</h1>
          <p className="text-xs text-slate-400 font-medium">Winkit Parametric Platform • Enterprise v4.2</p>
        </div>
        <div className="h-8 w-px bg-slate-200 dark:bg-slate-800" />
        <div className="flex items-center gap-2 bg-emerald-50 dark:bg-emerald-900/20 px-3 py-1.5 rounded-full border border-emerald-100 dark:border-emerald-900/30">
          <div className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse" />
          <span className="text-[10px] font-bold text-emerald-700 dark:text-emerald-400 uppercase tracking-widest">System Healthy</span>
        </div>
      </div>
      
      <div className="flex items-center gap-4">
        {/* Status Filter */}
        <div className="flex items-center gap-1 bg-slate-50 dark:bg-slate-800 p-1 rounded-lg border border-slate-200 dark:border-slate-700">
          {['ALL', 'AUTO_PAID', 'REJECTED', 'PENDING'].map((status) => (
            <button
              key={status}
              onClick={() => setStatusFilter(status)}
              className={cn(
                "px-2 py-1 text-[9px] font-bold rounded transition-all",
                statusFilter === status 
                  ? "bg-white dark:bg-slate-700 text-blue-600 dark:text-blue-400 shadow-sm" 
                  : "text-slate-400 hover:text-slate-600 dark:hover:text-slate-300"
              )}
            >
              {status.replace('_', ' ')}
            </button>
          ))}
        </div>

        <div className="relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" size={16} />
          <input 
            type="text" 
            placeholder="Search audit trail..." 
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            onFocus={() => setIsSearchFocused(true)}
            onBlur={() => setTimeout(() => setIsSearchFocused(false), 200)}
            className="pl-10 pr-4 py-2 bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500/20 transition-all w-48 dark:text-slate-200"
          />
          {isSearchFocused && searchQuery && (
            <div className="absolute top-full left-0 right-0 mt-2 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl shadow-xl p-2 z-50">
              <p className="text-[10px] text-slate-400 font-bold uppercase px-2 mb-2">Search Results</p>
              {filteredAuditTrail.length > 0 ? (
                filteredAuditTrail.map(item => (
                  <div key={item.id} onClick={() => triggerMockAction(`View Claim ${item.id}`)} className="p-2 hover:bg-slate-50 dark:hover:bg-slate-800 rounded-lg cursor-pointer flex items-center gap-3">
                    <span className="text-xs text-slate-700 dark:text-slate-300 truncate">{item.status} - {item.id?.slice(0, 8)}</span>
                  </div>
                ))
              ) : (
                <p className="text-xs text-slate-500 p-2 italic">No matches found.</p>
              )}
            </div>
          )}
        </div>
        
        <div className="flex items-center gap-2">
          <button 
            onClick={() => setDarkMode(!darkMode)}
            className="p-2 rounded-lg text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition-all"
          >
            {darkMode ? <Sun size={20} /> : <Moon size={20} />}
          </button>

          <div className="relative">
            <button 
              onClick={() => setShowNotifications(!showNotifications)}
              className={cn(
                "p-2 rounded-lg transition-colors relative",
                showNotifications ? "bg-slate-100 dark:bg-slate-800 text-blue-600" : "text-slate-400 hover:text-slate-600 dark:hover:text-slate-200"
              )}
            >
              <Bell size={20} />
              {unreadCount > 0 && (
                <div className="absolute top-2 right-2 w-4 h-4 bg-rose-500 rounded-full border-2 border-white dark:border-slate-900 flex items-center justify-center">
                  <span className="text-[8px] text-white font-bold">{unreadCount}</span>
                </div>
              )}
            </button>

            <AnimatePresence>
              {showNotifications && (
                <motion.div 
                  initial={{ opacity: 0, y: 10, scale: 0.95 }}
                  animate={{ opacity: 1, y: 0, scale: 1 }}
                  exit={{ opacity: 0, y: 10, scale: 0.95 }}
                  className="absolute top-full right-0 mt-2 w-80 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl shadow-2xl z-50 overflow-hidden"
                >
                  <div className="p-4 border-b border-slate-100 dark:border-slate-800 flex justify-between items-center bg-slate-50/50 dark:bg-slate-800/50">
                    <h3 className="text-sm font-bold text-slate-900 dark:text-slate-100">Notifications</h3>
                    <button onClick={markAllRead} className="text-[10px] font-bold text-blue-600 dark:text-blue-400 hover:underline uppercase">Mark all read</button>
                  </div>
                  <div className="max-h-96 overflow-y-auto">
                    {notifications.length > 0 ? notifications.map(n => (
                      <div key={n.id} onClick={() => triggerMockAction('View Notification')} className={cn("p-4 border-b border-slate-50 dark:border-slate-800 hover:bg-slate-50 dark:hover:bg-slate-800 transition-colors cursor-pointer", !n.read && "bg-blue-50/30 dark:bg-blue-900/10")}>
                        <div className="flex justify-between items-start mb-1">
                          <span className={cn(
                            "text-[10px] font-bold uppercase px-1.5 py-0.5 rounded",
                            n.type === 'success' ? "bg-emerald-50 dark:bg-emerald-900/20 text-emerald-600 dark:text-emerald-400" : 
                            n.type === 'alert' ? "bg-rose-50 dark:bg-rose-900/20 text-rose-600 dark:text-rose-400" : "bg-blue-50 dark:bg-blue-900/20 text-blue-600 dark:text-blue-400"
                          )}>
                            {n.type}
                          </span>
                          <span className="text-[10px] text-slate-400">{n.time}</span>
                        </div>
                        <h4 className="text-xs font-bold text-slate-900 dark:text-slate-100 mb-1">{n.title}</h4>
                        <p className="text-[11px] text-slate-500 dark:text-slate-400 leading-normal">{n.message}</p>
                      </div>
                    )) : (
                      <p className="text-xs text-slate-400 p-8 text-center italic">No new notifications</p>
                    )}
                  </div>
                  <div className="p-3 bg-slate-50 dark:bg-slate-800/50 text-center border-t border-slate-100 dark:border-slate-800">
                    <button onClick={() => triggerMockAction('View All Notifications')} className="text-[10px] font-bold text-slate-500 hover:text-slate-900 dark:hover:text-slate-200 uppercase">View all notifications</button>
                  </div>
                </motion.div>
              )}
            </AnimatePresence>
          </div>
        </div>

        <div className="h-8 w-px bg-slate-200 dark:bg-slate-800" />
        <div className="flex items-center gap-3 pl-2 cursor-pointer group" onClick={() => triggerMockAction('View Profile')}>
          <div className="text-right">
            {/* DYNAMIC USER DATA FROM LOGIN */}
            <p className="text-xs font-bold text-slate-900 dark:text-slate-100 group-hover:text-blue-600 transition-colors">
              {user?.name || 'Admin'}
            </p>
            <p className="text-[10px] text-slate-400 font-medium">
              {user?.role || 'Risk Manager'}
            </p>
          </div>
          <div className="w-10 h-10 rounded-full bg-blue-100 dark:bg-blue-900/30 text-blue-600 border border-blue-200 dark:border-blue-800 overflow-hidden group-hover:border-blue-500 transition-all flex items-center justify-center font-black text-lg shadow-sm">
            {/* DYNAMIC INITIAL */}
            {user?.name?.charAt(0).toUpperCase() || 'A'}
          </div>
        </div>
      </div>
    </header>
  );
};

export default Header;
