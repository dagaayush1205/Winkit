import React from 'react';
import { cn } from '../lib/utils';

const SidebarItem = ({ icon: Icon, label, active = false, onClick }) => (
  <div 
    onClick={onClick}
    className={cn(
      "flex flex-col items-center justify-center p-4 cursor-pointer transition-all duration-200 group relative",
      active 
        ? "text-blue-600 bg-blue-50/50 dark:bg-blue-900/20" 
        : "text-slate-400 hover:text-slate-600 dark:hover:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800"
    )}
  >
    <Icon size={20} className={cn("mb-1", active ? "text-blue-600 dark:text-blue-400" : "")} />
    <span className="text-[10px] font-bold tracking-wider uppercase">{label}</span>
    {active && <div className="absolute right-0 top-1/4 bottom-1/4 w-1 bg-blue-600 dark:bg-blue-400 rounded-l-full" />}
  </div>
);

export default SidebarItem;
