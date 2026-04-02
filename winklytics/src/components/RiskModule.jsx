import React, { useState, useEffect } from 'react';
import { 
  Search, 
  Filter, 
  Edit2, 
  Save, 
  X, 
  Trash2, 
  Plus, 
  BarChart3, 
  Table as TableIcon,
  Check,
  AlertCircle,
  Activity,    
  ShieldCheck  
} from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import { 
  BarChart, 
  Bar, 
  XAxis, 
  YAxis, 
  CartesianGrid, 
  Tooltip as RechartsTooltip, 
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell
} from 'recharts';
import { cn } from '../lib/utils';
import { supabase } from '../supabase';

const RiskModule = () => {
  const [activeTab, setActiveTab] = useState('claims');
  const [claims, setClaims] = useState([]);
  const [policies, setPolicies] = useState([]);
  const [loading, setLoading] = useState(true);
  const [editingId, setEditingId] = useState(null);
  const [editForm, setEditForm] = useState({});
  const [searchTerm, setSearchTerm] = useState('');
  const [message, setMessage] = useState(null);
  const [showAddModal, setShowAddModal] = useState(false);
  const [newRecord, setNewRecord] = useState({});

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    if (!supabase) return;
    setLoading(true);
    try {
      const { data: claimsData } = await supabase.from('claims_and_payouts').select('*').order('created_at', { ascending: false });
      const { data: policiesData } = await supabase.from('weekly_policies').select('*').order('created_at', { ascending: false });
      setClaims(claimsData || []);
      setPolicies(policiesData || []);
    } catch (error) {
      console.error('Error fetching risk data:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleAdd = async () => {
    if (!supabase) return;
    const table = activeTab === 'claims' ? 'claims_and_payouts' : 'weekly_policies';
    try {
      const { error } = await supabase.from(table).insert([newRecord]);
      if (error) throw error;
      setMessage({ type: 'success', text: 'Record added successfully' });
      setShowAddModal(false);
      setNewRecord({});
      fetchData();
    } catch (error) {
      setMessage({ type: 'error', text: error.message });
    }
    setTimeout(() => setMessage(null), 3000);
  };

  const handleEdit = (item) => {
    const id = activeTab === 'claims' ? item.claim_id : item.policy_id;
    setEditingId(id);
    setEditForm({ ...item });
  };

  const handleCancel = () => {
    setEditingId(null);
    setEditForm({});
  };

  const handleSave = async (table) => {
    if (!supabase) return;
    try {
      const primaryKeyColumn = table === 'claims_and_payouts' ? 'claim_id' : 'policy_id';
      
      const { error } = await supabase
        .from(table)
        .update(editForm)
        .eq(primaryKeyColumn, editingId);

      if (error) throw error;

      setMessage({ type: 'success', text: 'Record updated successfully' });
      fetchData();
      setEditingId(null);
    } catch (error) {
      setMessage({ type: 'error', text: error.message });
    }
    setTimeout(() => setMessage(null), 3000);
  };

  const handleDelete = async (table, id) => {
    if (!supabase || !window.confirm('Are you sure you want to delete this record?')) return;
    try {
      const primaryKeyColumn = table === 'claims_and_payouts' ? 'claim_id' : 'policy_id';
      const { error } = await supabase.from(table).delete().eq(primaryKeyColumn, id);
      if (error) throw error;
      setMessage({ type: 'success', text: 'Record deleted' });
      fetchData();
    } catch (error) {
      setMessage({ type: 'error', text: error.message });
    }
    setTimeout(() => setMessage(null), 3000);
  };

  // Analytics Data
  const claimStatusStats = claims.reduce((acc, curr) => {
    acc[curr.status] = (acc[curr.status] || 0) + 1;
    return acc;
  }, {});

  const pieData = Object.entries(claimStatusStats).map(([name, value]) => ({ name, value }));
  const COLORS = ['#2563EB', '#10B981', '#FBBF24', '#DC2626']; 

  const filteredClaims = claims.filter(c => 
    (c.claim_id && c.claim_id.toLowerCase().includes(searchTerm.toLowerCase())) ||
    (c.worker_id && c.worker_id.toLowerCase().includes(searchTerm.toLowerCase())) ||
    (c.status && c.status.toLowerCase().includes(searchTerm.toLowerCase()))
  );

  const filteredPolicies = policies.filter(p => 
    (p.policy_id && p.policy_id.toLowerCase().includes(searchTerm.toLowerCase())) ||
    (p.worker_id && p.worker_id.toLowerCase().includes(searchTerm.toLowerCase())) ||
    (p.status && p.status.toLowerCase().includes(searchTerm.toLowerCase()))
  );

  return (
    <motion.div 
      initial={{ opacity: 0, y: 20 }} 
      animate={{ opacity: 1, y: 0 }} 
      className="space-y-8 pb-20 px-8 pt-8"
    >
      {/* Header & Stats */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl font-black tracking-tight text-[#0C1117] dark:text-slate-100">Database Management</h2>
          <p className="text-sm text-slate-500 font-medium mt-1">Comprehensive view of all parametric claims and active policies.</p>
        </div>
        <div className="flex items-center gap-3">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" size={16} />
            <input 
              type="text" 
              placeholder="Search records..." 
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="pl-10 pr-4 py-2.5 bg-white border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-[#5B2D8E]/20 w-64 shadow-sm"
            />
          </div>
          <button 
            onClick={() => {
              setNewRecord(activeTab === 'claims' ? { status: 'PENDING', payout_amt: 0, fraud_flags_triggered: 0 } : { status: 'ACTIVE', premium_paid: 0 });
              setShowAddModal(true);
            }} 
            className="flex items-center gap-2 px-5 py-2.5 bg-[#2563EB] text-white rounded-xl text-xs font-bold uppercase tracking-wider hover:bg-blue-700 transition-colors shadow-md"
          >
            <Plus size={16} />
            Add Record
          </button>
          <button onClick={fetchData} className="p-2.5 bg-white border border-slate-200 rounded-xl text-slate-500 hover:text-[#2563EB] transition-colors shadow-sm">
            <Activity size={18} className={cn(loading && "animate-spin")} />
          </button>
        </div>
      </div>

      {/* Analytics Row */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 bg-white border border-slate-200 rounded-[20px] p-6 shadow-sm antigravity">
          <div className="flex items-center gap-2 mb-6">
            <BarChart3 size={18} className="text-[#2563EB]" />
            <h3 className="text-[11px] font-extrabold uppercase tracking-[0.25em] text-slate-400">Claim Volume by Status</h3>
          </div>
          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={pieData}>
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#E2E8F0" />
                <XAxis dataKey="name" axisLine={false} tickLine={false} fontSize={11} tick={{fill: '#94A3B8'}} />
                <YAxis axisLine={false} tickLine={false} fontSize={11} tick={{fill: '#94A3B8'}} />
                <RechartsTooltip 
                  contentStyle={{ backgroundColor: '#0C1117', border: 'none', borderRadius: '12px', color: '#fff' }}
                  itemStyle={{ color: '#fff' }}
                />
                <Bar dataKey="value" radius={[6, 6, 0, 0]}>
                  {pieData.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>
        
        <div className="bg-white border border-slate-200 rounded-[20px] p-6 shadow-sm antigravity">
          <div className="flex items-center gap-2 mb-6">
            <BarChart3 size={18} className="text-[#10B981]" />
            <h3 className="text-[11px] font-extrabold uppercase tracking-[0.25em] text-slate-400">Distribution</h3>
          </div>
          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={pieData}
                  innerRadius={60}
                  outerRadius={80}
                  paddingAngle={5}
                  dataKey="value"
                  stroke="none"
                >
                  {pieData.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                  ))}
                </Pie>
                <RechartsTooltip contentStyle={{ backgroundColor: '#0C1117', border: 'none', borderRadius: '12px', color: '#fff' }} />
              </PieChart>
            </ResponsiveContainer>
          </div>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex items-center gap-1 bg-slate-100 p-1.5 rounded-xl w-fit">
        <button 
          onClick={() => setActiveTab('claims')}
          className={cn(
            "flex items-center gap-2 px-6 py-2.5 rounded-lg text-xs font-bold transition-all uppercase tracking-wider",
            activeTab === 'claims' ? "bg-white text-[#2563EB] shadow-sm" : "text-slate-500 hover:text-slate-800"
          )}
        >
          <TableIcon size={16} />
          Claims & Payouts
        </button>
        <button 
          onClick={() => setActiveTab('policies')}
          className={cn(
            "flex items-center gap-2 px-6 py-2.5 rounded-lg text-xs font-bold transition-all uppercase tracking-wider",
            activeTab === 'policies' ? "bg-white text-[#2563EB] shadow-sm" : "text-slate-500 hover:text-slate-800"
          )}
        >
          <ShieldCheck size={16} />
          Weekly Policies
        </button>
      </div>

      {/* Main Table Container */}
      <div className="bg-white border border-slate-200 rounded-[20px] overflow-hidden shadow-sm antigravity">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-slate-50 border-b border-slate-200">
                {activeTab === 'claims' ? (
                  <>
                    <th className="px-6 py-5 text-[10px] font-extrabold text-slate-400 uppercase tracking-widest">Claim ID</th>
                    <th className="px-6 py-5 text-[10px] font-extrabold text-slate-400 uppercase tracking-widest">Worker ID</th>
                    <th className="px-6 py-5 text-[10px] font-extrabold text-slate-400 uppercase tracking-widest">Status</th>
                    <th className="px-6 py-5 text-[10px] font-extrabold text-slate-400 uppercase tracking-widest">Payout</th>
                    <th className="px-6 py-5 text-[10px] font-extrabold text-slate-400 uppercase tracking-widest">Fraud Flags</th>
                    <th className="px-6 py-5 text-[10px] font-extrabold text-slate-400 uppercase tracking-widest">Date</th>
                    <th className="px-6 py-5 text-[10px] font-extrabold text-slate-400 uppercase tracking-widest">Actions</th>
                  </>
                ) : (
                  <>
                    <th className="px-6 py-5 text-[10px] font-extrabold text-slate-400 uppercase tracking-widest">Policy ID</th>
                    <th className="px-6 py-5 text-[10px] font-extrabold text-slate-400 uppercase tracking-widest">Worker ID</th>
                    <th className="px-6 py-5 text-[10px] font-extrabold text-slate-400 uppercase tracking-widest">Status</th>
                    <th className="px-6 py-5 text-[10px] font-extrabold text-slate-400 uppercase tracking-widest">Premium</th>
                    <th className="px-6 py-5 text-[10px] font-extrabold text-slate-400 uppercase tracking-widest">Start Date</th>
                    <th className="px-6 py-5 text-[10px] font-extrabold text-slate-400 uppercase tracking-widest">End Date</th>
                    <th className="px-6 py-5 text-[10px] font-extrabold text-slate-400 uppercase tracking-widest">Actions</th>
                  </>
                )}
              </tr>
            </thead>
            <tbody>
              {loading ? (
                Array.from({ length: 5 }).map((_, i) => (
                  <tr key={i} className="animate-pulse">
                    {/* Updated colSpan to 7 since we added Worker ID */}
                    <td colSpan={7} className="px-6 py-4 h-16 bg-slate-50/50" />
                  </tr>
                ))
              ) : activeTab === 'claims' ? (
                filteredClaims.map((item) => (
                  <tr key={item.claim_id} className="border-b border-slate-100 hover:bg-slate-50 transition-colors">
                    <td className="px-6 py-5 text-xs font-mono font-bold text-slate-500">{item.claim_id}</td>
                    
                    {/* ADDED WORKER ID */}
                    <td className="px-6 py-5 text-xs font-bold text-[#5B2D8E]">{item.worker_id || 'Unknown'}</td>
                    
                    <td className="px-6 py-5">
                      {editingId === item.claim_id ? (
                        <select 
                          value={editForm.status} 
                          onChange={(e) => setEditForm({ ...editForm, status: e.target.value })}
                          className="bg-white border border-slate-200 rounded px-2 py-1 text-xs"
                        >
                          <option value="PENDING">PENDING</option>
                          <option value="AUTO_PAID">AUTO_PAID</option>
                          <option value="REJECTED">REJECTED</option>
                        </select>
                      ) : (
                        <span className={cn(
                          "text-[10px] font-black px-2.5 py-1 rounded-md uppercase tracking-wider",
                          item.status === 'AUTO_PAID' ? "bg-emerald-50 text-emerald-600" : 
                          item.status === 'REJECTED' ? "bg-rose-50 text-rose-600" : "bg-amber-50 text-amber-600"
                        )}>
                          {item.status}
                        </span>
                      )}
                    </td>
                    <td className="px-6 py-5 text-sm font-black text-[#0C1117]">
                      {editingId === item.claim_id ? (
                        <input 
                          type="number" 
                          value={editForm.payout_amt} 
                          onChange={(e) => setEditForm({ ...editForm, payout_amt: parseFloat(e.target.value) })}
                          className="w-20 bg-white border border-slate-200 rounded px-2 py-1 text-xs"
                        />
                      ) : (
                        `₹${item.payout_amt}`
                      )}
                    </td>
                    <td className="px-6 py-5 text-xs font-bold text-slate-600">{item.fraud_flags_triggered}</td>
                    <td className="px-6 py-5 text-xs font-medium text-slate-500">{new Date(item.created_at).toLocaleDateString()}</td>
                    <td className="px-6 py-5">
                      <div className="flex items-center gap-3">
                        {editingId === item.claim_id ? (
                          <>
                            <button onClick={() => handleSave('claims_and_payouts')} className="text-emerald-600 hover:text-emerald-700 transition-colors"><Save size={16} /></button>
                            <button onClick={handleCancel} className="text-slate-400 hover:text-slate-600 transition-colors"><X size={16} /></button>
                          </>
                        ) : (
                          <>
                            <button onClick={() => handleEdit(item)} className="text-[#2563EB] hover:text-blue-700 transition-colors"><Edit2 size={16} /></button>
                            <button onClick={() => handleDelete('claims_and_payouts', item.claim_id)} className="text-rose-600 hover:text-rose-700 transition-colors"><Trash2 size={16} /></button>
                          </>
                        )}
                      </div>
                    </td>
                  </tr>
                ))
              ) : (
                filteredPolicies.map((item) => (
                  <tr key={item.policy_id} className="border-b border-slate-100 hover:bg-slate-50 transition-colors">
                    <td className="px-6 py-5 text-xs font-mono font-bold text-slate-500">{item.policy_id}</td>
                    
                    {/* ADDED WORKER ID */}
                    <td className="px-6 py-5 text-xs font-bold text-[#5B2D8E]">{item.worker_id || 'Unknown'}</td>

                    <td className="px-6 py-5">
                      {editingId === item.policy_id ? (
                        <select 
                          value={editForm.status} 
                          onChange={(e) => setEditForm({ ...editForm, status: e.target.value })}
                          className="bg-white border border-slate-200 rounded px-2 py-1 text-xs"
                        >
                          <option value="ACTIVE">ACTIVE</option>
                          <option value="EXPIRED">EXPIRED</option>
                          <option value="CANCELLED">CANCELLED</option>
                        </select>
                      ) : (
                        <span className={cn(
                          "text-[10px] font-black px-2.5 py-1 rounded-md uppercase tracking-wider",
                          item.status === 'ACTIVE' ? "bg-emerald-50 text-emerald-600" : "bg-slate-100 text-slate-500"
                        )}>
                          {item.status}
                        </span>
                      )}
                    </td>
                    <td className="px-6 py-5 text-sm font-black text-[#0C1117]">
                      {editingId === item.policy_id ? (
                        <input 
                          type="number" 
                          value={editForm.premium_paid} 
                          onChange={(e) => setEditForm({ ...editForm, premium_paid: parseFloat(e.target.value) })}
                          className="w-20 bg-white border border-slate-200 rounded px-2 py-1 text-xs"
                        />
                      ) : (
                        `₹${item.premium_paid}`
                      )}
                    </td>
                    <td className="px-6 py-5 text-xs font-medium text-slate-500">{new Date(item.week_start_date).toLocaleDateString()}</td>
                    <td className="px-6 py-5 text-xs font-medium text-slate-500">{new Date(item.week_end_date).toLocaleDateString()}</td>
                    <td className="px-6 py-5">
                      <div className="flex items-center gap-3">
                        {editingId === item.policy_id ? (
                          <>
                            <button onClick={() => handleSave('weekly_policies')} className="text-emerald-600 hover:text-emerald-700 transition-colors"><Save size={16} /></button>
                            <button onClick={handleCancel} className="text-slate-400 hover:text-slate-600 transition-colors"><X size={16} /></button>
                          </>
                        ) : (
                          <>
                            <button onClick={() => handleEdit(item)} className="text-[#2563EB] hover:text-blue-700 transition-colors"><Edit2 size={16} /></button>
                            <button onClick={() => handleDelete('weekly_policies', item.policy_id)} className="text-rose-600 hover:text-rose-700 transition-colors"><Trash2 size={16} /></button>
                          </>
                        )}
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </motion.div>
  );
};

export default RiskModule;
