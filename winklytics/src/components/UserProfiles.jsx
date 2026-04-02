import React, { useState, useEffect } from 'react';
import { Search, UserCheck, UserX, ChevronDown, ChevronUp, Mail, Fingerprint, Calendar, Shield, MapPin, Phone } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import { cn } from '../lib/utils';
import { supabase } from '../supabase';

export default function UserProfiles() {
  const [workers, setWorkers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [expandedId, setExpandedId] = useState(null); // Tracks which profile is open

  useEffect(() => {
    async function fetchWorkers() {
      if (!supabase) return;
      try {
        const { data, error } = await supabase
          .from('Workers')
          .select('*')
          .order('trust_score', { ascending: false });
        
        if (error) throw error;
        setWorkers(data || []);
      } catch (err) {
        console.error("Error fetching workers:", err);
      } finally {
        setLoading(false);
      }
    }

    fetchWorkers();
  }, []);

  const filteredWorkers = workers.filter(w => 
    (w.name && w.name.toLowerCase().includes(searchTerm.toLowerCase())) ||
    (w.worker_id && w.worker_id.toLowerCase().includes(searchTerm.toLowerCase())) ||
    (w.phone && w.phone.includes(searchTerm))
  );

  const toggleExpand = (id) => {
    setExpandedId(expandedId === id ? null : id);
  };

  return (
    <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-black tracking-tight text-[#0C1117] dark:text-slate-100">Rider Profiles</h2>
          <p className="text-sm text-slate-500 font-medium mt-1">Manage workforce trust scores, access, and statuses.</p>
        </div>
        <div className="flex items-center gap-4">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" size={16} />
            <input 
              type="text" 
              placeholder="Search riders..." 
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="pl-10 pr-4 py-2.5 bg-white border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-[#5B2D8E]/20 w-64 shadow-sm"
            />
          </div>
        </div>
      </div>

      <div className="bg-white border border-slate-200 rounded-[20px] overflow-hidden shadow-sm antigravity">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-slate-50 border-b border-slate-200">
                <th className="px-6 py-5 text-[10px] font-extrabold text-slate-400 uppercase tracking-widest">Worker ID</th>
                <th className="px-6 py-5 text-[10px] font-extrabold text-slate-400 uppercase tracking-widest">Name / Phone</th>
                <th className="px-6 py-5 text-[10px] font-extrabold text-slate-400 uppercase tracking-widest">Trust Score</th>
                <th className="px-6 py-5 text-[10px] font-extrabold text-slate-400 uppercase tracking-widest">Status</th>
                <th className="px-6 py-5 text-[10px] font-extrabold text-slate-400 uppercase tracking-widest">Actions</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                Array.from({ length: 5 }).map((_, i) => (
                  <tr key={i} className="animate-pulse">
                    <td colSpan={5} className="px-6 py-4 h-16 bg-slate-50/50" />
                  </tr>
                ))
              ) : filteredWorkers.length > 0 ? (
                filteredWorkers.map((worker) => (
                  <React.Fragment key={worker.worker_id}>
                    <tr className={cn(
                      "border-b border-slate-100 hover:bg-slate-50 transition-colors cursor-pointer",
                      expandedId === worker.worker_id && "bg-blue-50/30"
                    )} onClick={() => toggleExpand(worker.worker_id)}>
                      <td className="px-6 py-5 text-xs font-mono font-bold text-slate-500">{worker.worker_id}</td>
                      <td className="px-6 py-5">
                        <div className="flex flex-col">
                          <span className="text-sm font-bold text-slate-900">{worker.name}</span>
                          <span className="text-[10px] font-medium text-slate-500">{worker.phone} • {worker.gender}</span>
                        </div>
                      </td>
                      <td className="px-6 py-5">
                        <div className="flex items-center gap-2">
                          <div className="w-16 h-2 bg-slate-100 rounded-full overflow-hidden">
                            <div 
                              className={cn("h-full", worker.trust_score > 80 ? 'bg-emerald-400' : worker.trust_score > 50 ? 'bg-amber-400' : 'bg-rose-500')} 
                              style={{ width: `${worker.trust_score}%` }}
                            />
                          </div>
                          <span className="text-xs font-black">{worker.trust_score}</span>
                        </div>
                      </td>
                      <td className="px-6 py-5">
                        <span className={cn(
                          "text-[10px] font-black px-2.5 py-1 rounded-md uppercase tracking-wider flex items-center gap-1 w-max",
                          worker.status === 'ACTIVE' ? "bg-emerald-50 text-emerald-600" : "bg-rose-50 text-rose-600"
                        )}>
                          {worker.status === 'ACTIVE' ? <UserCheck size={12} /> : <UserX size={12} />}
                          {worker.status}
                        </span>
                      </td>
                      <td className="px-6 py-5">
                        <button className="flex items-center gap-1 text-[10px] font-bold text-blue-600 hover:text-blue-700 uppercase transition-colors">
                          {expandedId === worker.worker_id ? 'Close Profile' : 'View Profile'}
                          {expandedId === worker.worker_id ? <ChevronUp size={14} /> : <ChevronDown size={14} />}
                        </button>
                      </td>
                    </tr>
                    
                    {/* EXPANDABLE PROFILE SECTION */}
                    <AnimatePresence>
                      {expandedId === worker.worker_id && (
                        <tr>
                          <td colSpan={5} className="p-0 border-b border-slate-200">
                            <motion.div
                              initial={{ height: 0, opacity: 0 }}
                              animate={{ height: 'auto', opacity: 1 }}
                              exit={{ height: 0, opacity: 0 }}
                              className="overflow-hidden bg-slate-50/80 shadow-inner"
                            >
                              <div className="p-8 grid grid-cols-1 md:grid-cols-3 gap-8">
                                {/* Identity Block */}
                                <div className="space-y-4">
                                  <h4 className="text-[10px] font-extrabold text-slate-400 uppercase tracking-widest border-b border-slate-200 pb-2">Identity & Contact</h4>
                                  <div className="space-y-3">
                                    <div className="flex items-center gap-3">
                                      <Mail className="text-slate-400" size={16} />
                                      <div className="flex flex-col">
                                        <span className="text-[10px] text-slate-500 uppercase font-bold">Email Address</span>
                                        <span className="text-xs font-semibold text-slate-900">{worker.email || 'Not provided'}</span>
                                      </div>
                                    </div>
                                    <div className="flex items-center gap-3">
                                      <Phone className="text-slate-400" size={16} />
                                      <div className="flex flex-col">
                                        <span className="text-[10px] text-slate-500 uppercase font-bold">Phone</span>
                                        <span className="text-xs font-semibold text-slate-900">{worker.phone}</span>
                                      </div>
                                    </div>
                                  </div>
                                </div>

                                {/* Security Block */}
                                <div className="space-y-4">
                                  <h4 className="text-[10px] font-extrabold text-slate-400 uppercase tracking-widest border-b border-slate-200 pb-2">Security & System</h4>
                                  <div className="space-y-3">
                                    <div className="flex items-center gap-3">
                                      <Fingerprint className="text-slate-400" size={16} />
                                      <div className="flex flex-col">
                                        <span className="text-[10px] text-slate-500 uppercase font-bold">Aadhar Hash (KYC)</span>
                                        <span className="text-xs font-mono font-semibold text-slate-900 truncate w-40">{worker.aadhar_hash || 'Pending KYC'}</span>
                                      </div>
                                    </div>
                                    <div className="flex items-center gap-3">
                                      <Shield className={worker.access ? "text-emerald-500" : "text-rose-500"} size={16} />
                                      <div className="flex flex-col">
                                        <span className="text-[10px] text-slate-500 uppercase font-bold">System Access</span>
                                        <span className="text-xs font-semibold text-slate-900">{worker.access ? 'Authorized (TRUE)' : 'Revoked (FALSE)'}</span>
                                      </div>
                                    </div>
                                  </div>
                                </div>

                                {/* Operations Block */}
                                <div className="space-y-4">
                                  <h4 className="text-[10px] font-extrabold text-slate-400 uppercase tracking-widest border-b border-slate-200 pb-2">Operations</h4>
                                  <div className="space-y-3">
                                    <div className="flex items-center gap-3">
                                      <MapPin className="text-slate-400" size={16} />
                                      <div className="flex flex-col">
                                        <span className="text-[10px] text-slate-500 uppercase font-bold">Primary H3 Hex</span>
                                        <span className="text-xs font-mono font-semibold text-slate-900">{worker.primary_h3_hex || 'Unassigned'}</span>
                                      </div>
                                    </div>
                                    <div className="flex items-center gap-3">
                                      <Calendar className="text-slate-400" size={16} />
                                      <div className="flex flex-col">
                                        <span className="text-[10px] text-slate-500 uppercase font-bold">Account Created</span>
                                        <span className="text-xs font-semibold text-slate-900">{new Date(worker.created_at).toLocaleDateString()}</span>
                                      </div>
                                    </div>
                                  </div>
                                </div>

                              </div>
                              <div className="px-8 py-4 bg-slate-100/50 flex justify-end gap-3 border-t border-slate-200">
                                <button className="px-4 py-2 bg-white border border-slate-200 rounded-lg text-xs font-bold text-slate-600 hover:bg-slate-50 transition-colors shadow-sm">Audit History</button>
                                <button className={cn("px-4 py-2 rounded-lg text-xs font-bold text-white shadow-sm transition-colors", worker.access ? "bg-rose-600 hover:bg-rose-700" : "bg-emerald-600 hover:bg-emerald-700")}>
                                  {worker.access ? 'Revoke Access' : 'Restore Access'}
                                </button>
                              </div>
                            </motion.div>
                          </td>
                        </tr>
                      )}
                    </AnimatePresence>
                  </React.Fragment>
                ))
              ) : (
                <tr>
                  <td colSpan={5} className="px-6 py-8 text-center text-slate-400 text-xs italic">No riders found.</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </motion.div>
  );
}
