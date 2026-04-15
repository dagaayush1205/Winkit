import React, { useState, useEffect } from 'react';
import { supabase } from '../supabase'; // Make sure this points to your Supabase client

const DatabaseLedger = () => {
  const [selectedTable, setSelectedTable] = useState('claims_and_payouts');
  const [tableData, setTableData] = useState([]);
  const [isLoading, setIsLoading] = useState(true);

  // The tables you want to expose in the admin panel
  const availableTables = [
    { id: 'claims_and_payouts', label: 'Automated Claims' },
    { id: 'manual_claims', label: 'Manual Claims' },
    { id: 'weekly_policies', label: 'Active Policies' },
    { id: 'Workers', label: 'Worker Profiles' }
  ];

  useEffect(() => {
    const fetchTableData = async () => {
      setIsLoading(true);
      try {
        // Fetch the latest 50 rows from the selected table
        const { data, error } = await supabase
          .from(selectedTable)
          .select('*')
          .limit(50); // Limit to prevent crashing the browser on huge tables

        if (error) throw error;
        setTableData(data || []);
      } catch (error) {
        console.error("Error fetching ledger data:", error.message);
        setTableData([]);
      } finally {
        setIsLoading(false);
      }
    };

    fetchTableData();
  }, [selectedTable]);

  // Extract dynamic headers from the first row of data
  const headers = tableData.length > 0 ? Object.keys(tableData[0]) : [];

  return (
    <div className="flex flex-col h-full bg-[#0F0F1A] text-white p-6 rounded-xl border border-gray-800 shadow-2xl">
      {/* Header & Tabs */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center mb-6">
        <div>
          <h2 className="text-2xl font-black text-transparent bg-clip-text bg-gradient-to-r from-emerald-400 to-cyan-400">
            Database Ledger
          </h2>
          <p className="text-sm text-gray-400 mt-1">Live overview of all Supabase infrastructure tables.</p>
        </div>

        {/* Table Selector */}
        <div className="mt-4 md:mt-0 flex space-x-2 overflow-x-auto pb-2 md:pb-0">
          {availableTables.map((table) => (
            <button
              key={table.id}
              onClick={() => setSelectedTable(table.id)}
              className={`px-4 py-2 text-sm font-bold rounded-lg transition-all ${
                selectedTable === table.id
                  ? 'bg-purple-600 text-white shadow-[0_0_15px_rgba(147,51,234,0.5)]'
                  : 'bg-gray-800 text-gray-400 hover:bg-gray-700'
              }`}
            >
              {table.label}
            </button>
          ))}
        </div>
      </div>

      {/* The Data Grid */}
      <div className="flex-1 bg-[#151522] rounded-xl border border-gray-800 overflow-hidden relative">
        {isLoading ? (
          <div className="absolute inset-0 flex items-center justify-center">
            <div className="w-10 h-10 border-4 border-emerald-400 border-t-transparent rounded-full animate-spin"></div>
          </div>
        ) : tableData.length === 0 ? (
          <div className="absolute inset-0 flex items-center justify-center text-gray-500">
            No data found in {selectedTable}.
          </div>
        ) : (
          <div className="overflow-x-auto max-h-[600px] overflow-y-auto custom-scrollbar">
            <table className="w-full text-left border-collapse">
              <thead className="bg-[#1A1A2E] sticky top-0 z-10 shadow-md">
                <tr>
                  {headers.map((header) => (
                    <th key={header} className="p-4 text-xs font-bold text-gray-400 uppercase tracking-wider whitespace-nowrap border-b border-gray-800">
                      {header.replace(/_/g, ' ')}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-800">
                {tableData.map((row, rowIndex) => (
                  <tr key={rowIndex} className="hover:bg-gray-800/50 transition-colors">
                    {headers.map((header) => {
                      const cellValue = row[header];
                      // Format objects/booleans for display
                      const displayValue = typeof cellValue === 'object' 
                        ? JSON.stringify(cellValue) 
                        : String(cellValue);

                      // Neon glow logic for specific status keywords
                      const isSuccessStatus = ['ACTIVE', 'PAID', 'APPROVED'].includes(displayValue);
                      const isPendingStatus = ['PENDING_REVIEW', 'PENDING'].includes(displayValue);

                      return (
                        <td key={`${rowIndex}-${header}`} className="p-4 text-sm whitespace-nowrap text-gray-300">
                          {isSuccessStatus ? (
                            <span className="px-2 py-1 text-xs font-bold text-emerald-400 bg-emerald-400/10 rounded-md border border-emerald-400/20">
                              {displayValue}
                            </span>
                          ) : isPendingStatus ? (
                            <span className="px-2 py-1 text-xs font-bold text-amber-400 bg-amber-400/10 rounded-md border border-amber-400/20">
                              {displayValue}
                            </span>
                          ) : (
                            displayValue
                          )}
                        </td>
                      );
                    })}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};

export default DatabaseLedger;
