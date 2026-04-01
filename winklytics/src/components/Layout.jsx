import { Link, useLocation, Outlet } from 'react-router-dom';
import { LayoutDashboard, Map, FileText, ShieldAlert, Bell, User } from 'lucide-react';

export default function Layout() {
  const location = useLocation();

  const navItems = [
    { name: 'Overview', path: '/', icon: <LayoutDashboard size={20} /> },
    { name: 'Risk Map', path: '/map', icon: <Map size={20} /> },
    { name: 'Ledger', path: '/ledger', icon: <FileText size={20} /> },
    { name: 'Fraud Watch', path: '/fraud', icon: <ShieldAlert size={20} /> },
  ];

  return (
    <div className="flex h-screen bg-[#F8F9FA] font-sans">
      
      {/* SIDEBAR */}
      <div className="w-64 bg-[#1A1A2E] text-white flex flex-col">
        <div className="h-20 flex items-center px-8 border-b border-gray-800">
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 rounded bg-[#00E5A0] flex items-center justify-center text-[#1A1A2E] font-bold text-xl">W</div>
            <span className="text-xl font-bold tracking-wider">Winklytics</span>
          </div>
        </div>
        
        <nav className="flex-1 py-6 px-4 space-y-2">
          {navItems.map((item) => {
            const isActive = location.pathname === item.path;
            return (
              <Link
                key={item.name}
                to={item.path}
                className={`flex items-center gap-3 px-4 py-3 rounded-xl transition-all duration-200 ${
                  isActive 
                    ? 'bg-[#5B2D8E] text-white shadow-lg' 
                    : 'text-gray-400 hover:bg-gray-800 hover:text-white'
                }`}
              >
                {item.icon}
                <span className="font-medium">{item.name}</span>
              </Link>
            );
          })}
        </nav>

        {/* Bottom Sidebar User Profile */}
        <div className="p-4 border-t border-gray-800">
          <div className="flex items-center gap-3 px-4 py-2">
            <div className="w-10 h-10 rounded-full bg-gray-700 flex items-center justify-center">
              <User size={20} />
            </div>
            <div>
              <p className="text-sm font-medium text-white">Admin Team</p>
              <p className="text-xs text-gray-400">Risk & Compliance</p>
            </div>
          </div>
        </div>
      </div>

      {/* MAIN CONTENT WRAPPER */}
      <div className="flex-1 flex flex-col overflow-hidden">
        
        {/* TOP NAVBAR */}
        <header className="h-20 bg-white border-b border-gray-200 flex items-center justify-between px-8 shadow-sm z-10">
          <h2 className="text-xl font-semibold text-gray-800">
            {navItems.find(item => item.path === location.pathname)?.name || 'Dashboard'}
          </h2>
          <div className="flex items-center gap-4">
            <button className="p-2 text-gray-400 hover:text-[#5B2D8E] transition-colors relative">
              <Bell size={24} />
              <span className="absolute top-1 right-2 w-2.5 h-2.5 bg-red-500 rounded-full border-2 border-white"></span>
            </button>
          </div>
        </header>

        {/* DYNAMIC PAGE CONTENT GOES HERE */}
        <main className="flex-1 overflow-y-auto p-8">
          <Outlet /> 
        </main>
      </div>
    </div>
  );
}
