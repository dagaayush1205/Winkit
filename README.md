# WinkIT: Instant Payout in a Wink 

### **The Parametric Financial Nervous System for the Global Gig Economy**

WinkIT is a high-performance, event-driven insurance engine that automates recovery for gig workers during "Ghost Hours"—income loss caused by hyper-local climate and civic disruptions. No forms, no adjusters, just physics-verified payouts.

---

## Phase 3 Deliverables

| Artifact | Access Link | Description |
| :--- | :--- | :--- |
| **Pitch Deck** | [View Presentation](https://www.google.com/search?q=YOUR_LINK_HERE) | High-level business & technical strategy. |
| **Demo Video** | [Watch Video](https://www.google.com/search?q=YOUR_LINK_HERE) | 6-minute deep dive of App, Dashboard, & Backend. |
| **Technical Whitepaper** | [Read Deep-Dive](https://www.google.com/search?q=YOUR_LINK_HERE) | Actuarial math, H3 logic, and P&L modeling. |
| **Phase 2 Archive** | [Phase 2 README](https://www.google.com/search?q=YOUR_LINK_HERE) | Reference for project evolution. |
| **Android APK** | [Download APK](https://www.google.com/search?q=YOUR_LINK_HERE) | v3.0 Stable Release (Use on Physical Device). |
| **Admin Website** |[Visit Site](https://www.google.com/search?q=YOUR_LINK_HERE) | Admin website. |

---

## Phase 3: Major Technical Evolution

- **Spatial Authority:** Migrated from circular geofencing to **Uber H3 Hexagonal Indexing (Res 8)** for uniform risk adjacency and spillover math. 
- **The Fraud Fortress:** Implemented hardware-level **Sensor Fusion**—cross-referencing GPS velocity with raw IMU Accelerometer variance to eliminate location spoofing.  
- **Omnichannel Scaling:** Expanded beyond the app to include **WhatsApp (Conversational AI)** and **IVR Voice Gateways** for feature-phone users.  
- **Capital Solvency:** Introduced the **Hourly Drip-Feed Model** and **Asymptotic Pricing** ($20 - $49) to ensure 24% EBITDA margins.
- **Codebase Refactoring:** Restructured the codebase to significantly enhance overall readability and maintainability.
- **Optimized Weather Forecasting:** Implemented parent/child relationships within the Hex architecture to minimize redundant API calls.
- **Trigger Optimization:** Refactored the trigger code implementation, successfully reducing database calls by 50%.
- **Efficient Data Caching:** Introduced a dedicated database table to batch and store 7-day weather forecasts. Fetching a full week of data in a single request drastically reduces overall API calls to OpenWeather.
---

## Project Structure

```bash
WinkIT/
├── android-app/              # Kotlin & Jetpack Compose (The Sensor Layer)
├── comms_engine
│   ├── bots.py
│   ├── main.py
│   ├── requirements.txt
│   └── test_send.py
├── engine
│   ├── dynamic_pricing
│   │   └──pricing_engine.py  # Calculates the premium
│   └── ml_fraud
│       ├── anomaly_model.py
│       └── anomaly.py
├── scripts
│   └── seed_h3_grid.py       # Created a starter set of hex index
├── services
|   ├── civic_risk_agent.py   # calculates the civic risk
|   └── weather_api_client.py # weather forecast
├── workers
|   ├── fraud_worker.py       # Determines users gaming the system
|   ├── payout_worker.py      # Creates Payments
|   └── trigger_worker.py     # Triggers Disruption
└── winklytics-web/       # Next.js & Deck.gl (Actuarial Command Center)
```

---

## Live Implementation & Audit

WinkIT is fully deployed and available for live evaluation.

### **1. Actuarial Command Center (Web)**

- **Live URL:** https://winkitlytics.vercel.app/  
- **Note:** Auth is mocked for judging friction; bypass login to see real-time H3 grid data.

### **2. Live Backend Audit (The Engine Room)**

For a "Glass-Box" audit of our DigitalOcean Droplet and live Python daemons:

- **Browser View:** http://168.144.23.23:8000/ (Instant log inspection)  
- **Guest SFTP:**  
  ```
  sftp guest_viewer@168.144.23.23
  Password: astrobugs
  ```
 > [!NOTE]  
> SFTP access might be blocked depending on your corporate Wi-Fi or IT admin settings.


### **3. Mobile Evaluation**

- **Fast Track:** Install APK on a **Physical Device**.  
- **Login:** `9876543210`  
- **Platform ID:** `ZEP-1001` (Rahul Sharma)  
- **Developer Note:** Emulators will trigger a "Mock Location" rejection state by the Fraud Fortress.  

---

## The Tech Stack

- **Frontend:** Kotlin, Jetpack Compose, Deck.gl  
- **Backend:** Python (FastAPI), DigitalOcean Droplet  
- **Database:** Supabase (PostgreSQL), Real-time WebSockets, RLS Security  
- **Geospatial:** Uber H3 Indexing  
- **AI/Voice:** Cerebras (Llama 3.1 Inference), Twilio API  
- **Payments:** Cashfree Payouts API Integration  

---

**Built with ❤️ by Team Astrobugs for Guidewire DevTrails 2026.**
