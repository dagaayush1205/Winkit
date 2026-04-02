import React from 'react';
import { cn } from '../lib/utils';

const KPICard = ({ title, value, subtext, icon: Icon, iconColor, trend, onClick, loading }) => (
  <div 
    onClick={onClick}
    className={cn(
      "bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-5 rounded-xl shadow-sm hover:shadow-md transition-all duration-200",
      onClick ? "cursor-pointer hover:-translate-y-1" : ""
    )}
  >
    <div className="flex justify-between items-start mb-4">
      <div className={cn("p-2.5 rounded-lg bg-slate-50 dark:bg-slate-800", iconColor)}>
        <Icon size={20} />
      </div>
      {trend && !loading && (
        <span className={cn(
          "text-[10px] font-bold px-2 py-0.5 rounded-full",
          trend.startsWith('+') ? "bg-emerald-50 dark:bg-emerald-900/20 text-emerald-600 dark:text-emerald-400" : "bg-rose-50 dark:bg-rose-900/20 text-rose-600 dark:text-rose-400"
        )}>
          {trend}
        </span>
      )}
    </div>
    <div>
      <p className="text-slate-500 dark:text-slate-400 text-xs font-semibold uppercase tracking-wider mb-1">{title}</p>
      {loading ? (
        <div className="h-8 w-24 bg-slate-100 dark:bg-slate-800 animate-pulse rounded" />
      ) : (
        <h3 className="text-2xl font-bold text-slate-900 dark:text-slate-100 tracking-tight">{value}</h3>
      )}
      <p className="text-slate-400 dark:text-slate-500 text-[11px] mt-1">{subtext}</p>
    </div>
  </div>
);

export default KPICard;
