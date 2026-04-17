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

* **Actuarial Solvency Engine:** Designed an **Asymptotic Pricing Curve** ($20–$49 bounds) and an **Hourly Drip-Feed Payout Model**, mathematically capping Maximum Probable Loss (MPL) to secure projected 24% EBITDA margins.<br><br>
* **Spatial Risk Architecture:** Migrated from legacy circular geofencing to **Uber H3 Hexagonal Indexing (Resolution 8)**, enabling high-precision, uniform risk adjacency and contiguous spillover modeling.<br><br>
* **"Fraud Fortress" Telemetry Audit:** Engineered hardware-level **Sensor Fusion**, cross-referencing GPS velocity against raw IMU accelerometer variance to deterministically block location-spoofing attacks.  <br><br>
* **System Scalability & Optimization:** Overhauled backend orchestration by implementing parent-child H3 hex grouping and batch-caching 7-day weather forecasts. This heavily reduced external API dependency and optimized trigger logic to slash internal database calls by 50%.<br><br>
* **Omnichannel Accessibility:** Expanded platform reach beyond standard mobile apps by deploying **WhatsApp Conversational AI** and **IVR Voice Gateways**, ensuring seamless smart-contract access for feature-phone users.<br><br>
---

## Project Structure

```bash
WinkIT/
├── android-app/              # Kotlin & Jetpack Compose (The Sensor Layer)
├── comms_engine              # Handles the whatsapp integration
│   ├── bots.py                
│   ├── main.py
│   ├── requirements.txt
│   └── test_send.py
├── engine
│   ├── dynamic_pricing
│   │   └──pricing_engine.py  # Calculates the premium
│   └── ml_fraud              # Handles fraud detection using isolation forest
│       ├── anomaly_model.py
│       └── anomaly.py
├── scripts
│   └── seed_h3_grid.py       # Created a starter set of hex index
├── services
│   ├── civic_risk_agent.py   # calculates the civic risk
│   └── weather_api_client.py # weather forecast
├── workers
│   ├── fraud_worker.py       # Determines users gaming the system
│   ├── payout_worker.py      # Creates Payments
│   └── trigger_worker.py     # Triggers Disruption
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

| Category | Technologies |
| :--- | :--- |
| **Frontend** | Kotlin, Jetpack Compose, Deck.gl |
| **Backend** | Python (FastAPI), DigitalOcean Droplet |
| **Database** | Supabase (PostgreSQL), Real-time WebSockets, RLS Security |
| **Geospatial** | Uber H3 Indexing |
| **AI/Voice** | Cerebras (Llama 3.1 Inference), Twilio API |
| **Payments** | Cashfree Payouts API Integration |

---

**Built with ❤️ by Team Astrobugs for Guidewire DevTrails 2026.**
