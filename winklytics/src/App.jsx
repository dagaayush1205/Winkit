import { BrowserRouter, Routes, Route } from 'react-router-dom';
import Layout from './components/Layout';
import Dashboard from './pages/Dashboard';
import RiskMap from './pages/RiskMap';
import Ledger from './pages/Ledger';
import Fraud from './pages/Fraud';

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Layout />}>
          <Route index element={<Dashboard />} />
          <Route path="map" element={<RiskMap />} />
          <Route path="ledger" element={<Ledger />} />
          <Route path="fraud" element={<Fraud />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}

export default App;
