import React from 'react';
import { Filter } from 'lucide-react';
import { motion } from 'framer-motion';
import { cn } from '../lib/utils';

const FraudTable = ({ data, loading, triggerMockAction }) => {
  return (
    <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-bold text-slate-900 dark:text-slate-100">Fraud Detection Center</h2>
          <p className="text-sm text-slate-500 dark:text-slate-400">Monitoring GPS spoofing and coordinated market crash attempts.</p>
        </div>
        <button onClick={() => triggerMockAction('Open Advanced Filters')} className="flex items-center gap-2 px-4 py-2 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-lg text-xs font-bold text-slate-600 dark:text-slate-400 hover:bg-slate-50 dark:hover:bg-slate-800 transition-colors">
          <Filter size={14} /> Filter Results
        </button>
      </div>

      <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl overflow-hidden shadow-sm transition-colors duration-300">
        <table className="w-full text-left border-collapse">
          <thead>
            <tr className="bg-slate-50 dark:bg-slate-800/50 border-b border-slate-200 dark:border-slate-800">
              <th className="px-6 py-4 text-[10px] font-bold text-slate-400 uppercase tracking-widest">Claim ID</th>
              <th className="px-6 py-4 text-[10px] font-bold text-slate-400 uppercase tracking-widest">Worker Name</th>
              <th className="px-6 py-4 text-[10px] font-bold text-slate-400 uppercase tracking-widest">Location</th>
              <th className="px-6 py-4 text-[10px] font-bold text-slate-400 uppercase tracking-widest">Fraud Flags</th>
              <th className="px-6 py-4 text-[10px] font-bold text-slate-400 uppercase tracking-widest">Payout Amt</th>
              <th className="px-6 py-4 text-[10px] font-bold text-slate-400 uppercase tracking-widest">Status</th>
              <th className="px-6 py-4 text-[10px] font-bold text-slate-400 uppercase tracking-widest">Actions</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              Array.from({ length: 4 }).map((_, i) => (
                <tr key={i} className="animate-pulse"><td colSpan={7} className="px-6 py-4 h-12 bg-slate-50/50 dark:bg-slate-800/20" /></tr>
              ))
            ) : data.length > 0 ? data.map((row) => (
              <tr key={row.claim_id} className="border-b border-slate-100 dark:border-slate-800 hover:bg-slate-50/50 dark:hover:bg-slate-800/50 transition-colors">
                <td className="px-6 py-4 text-xs font-mono text-slate-600 dark:text-slate-400">{row.claim_id?.slice(0, 8)}</td>
                <td className="px-6 py-4 text-xs font-bold text-slate-900 dark:text-slate-100">{row.workers?.name || 'Unknown'}</td>
                <td className="px-6 py-4 text-xs text-slate-600 dark:text-slate-400">{row.latitude ? `${row.latitude.toFixed(4)}, ${row.longitude.toFixed(4)}` : 'N/A'}</td>
                <td className="px-6 py-4">
                  <span className="text-[10px] font-bold px-2 py-0.5 rounded bg-rose-50 dark:bg-rose-900/20 text-rose-600 dark:text-rose-400 uppercase">{row.fraud_flags_triggered} Flags</span>
                </td>
                <td className="px-6 py-4 text-xs font-bold text-slate-900 dark:text-slate-100">₹{row.payout_amt}</td>
                <td className="px-6 py-4">
                  <div className="flex items-center gap-2">
                    <div className={cn("w-1.5 h-1.5 rounded-full", row.status === 'REJECTED' ? "bg-rose-500" : "bg-amber-500")} />
                    <span className="text-xs font-medium text-slate-700 dark:text-slate-300">{row.status}</span>
                  </div>
                </td>
                <td className="px-6 py-4">
                  <button onClick={() => triggerMockAction(`Manual Review for ${row.claim_id}`)} className="text-blue-600 dark:text-blue-400 hover:underline text-[10px] font-bold uppercase">Review</button>
                </td>
              </tr>
            )) : (
              <tr><td colSpan={7} className="px-6 py-8 text-center text-slate-400 text-xs italic">No fraud events detected</td></tr>
            )}
          </tbody>
        </table>
      </div>
    </motion.div>
  );
};

export default FraudTable;
