import React from 'react';
import { CheckCircle2, ShieldAlert, AlertTriangle, Info } from 'lucide-react';
import { cn } from '../lib/utils';

const AuditItem = ({ item }) => {
  const getIcon = (type) => {
    switch (type) {
      case 'AUTO_PAID': return { icon: CheckCircle2, color: 'text-emerald-500', bg: 'bg-emerald-50' };
      case 'REJECTED': return { icon: ShieldAlert, color: 'text-rose-500', bg: 'bg-rose-50' };
      case 'PENDING': return { icon: AlertTriangle, color: 'text-amber-500', bg: 'bg-amber-50' };
      default: return { icon: Info, color: 'text-blue-500', bg: 'bg-blue-50' };
    }
  };

  const { icon: Icon, color, bg } = getIcon(item.status);
  const itemId = item.id || 'N/A';

  return (
    <div className="flex gap-4 p-4 bg-white dark:bg-slate-800 border border-slate-100 dark:border-slate-700 rounded-xl shadow-sm hover:shadow-md transition-all duration-200">
      <div className={cn("w-10 h-10 rounded-lg flex items-center justify-center shrink-0", bg)}>
        <Icon size={18} className={color} />
      </div>
      <div className="flex-1 min-w-0">
        <div className="flex justify-between items-start mb-0.5">
          <h4 className="text-xs font-bold text-slate-900 dark:text-slate-100 truncate">
            {item.status === 'AUTO_PAID' ? 'Escrow Unlocked' : item.status === 'REJECTED' ? 'Anti-Spoofing' : 'Claim Logged'}
          </h4>
          <span className="text-[10px] text-slate-400 font-medium">
            {new Date(item.created_at).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
          </span>
        </div>
        <p className="text-[11px] text-slate-500 dark:text-slate-400 leading-normal">
          {item.status === 'AUTO_PAID' 
            ? `Payout of ₹${item.payout_amt} authorized for claim #${itemId.slice(0, 8)}.`
            : item.status === 'REJECTED'
            ? `Fraud flagged for claim #${itemId.slice(0, 8)}. Payout Denied.`
            : `New claim request for ₹${item.payout_amt} received.`}
        </p>
      </div>
    </div>
  );
};

export default AuditItem;
