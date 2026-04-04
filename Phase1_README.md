# Winkit — Providing instant BlinkSurance


https://github.com/user-attachments/assets/aea4e84e-063c-46f7-a50c-9844131bd26d

## UI_DOCS
https://drive.google.com/file/d/1-nBodyZMe44uFsX5o8ZkT82nYkDGd6q3/view?usp=sharing

### Guidewire DevTrails 2026 Submission
## Introduction

Winkit is a **parametric micro-insurance platform** designed for India's gig-economy delivery workforce **(Blinkit/Zepto)**.
Winkit automatically compensates gig workers for income loss caused by external disruptions, such as extreme weather and civic unrest, using **real-time data** and **zero-touch smart contracts**. We are deploying the platform in strategic stages. The first stage establishes a secure foundation by focusing on **data collection, anomaly detection, and algorithmic fraud prevention**. This proprietary **data moat powers** our second stage: implementing advanced AI and **H3 Hexagonal Hierarchical Geospatial Indexing to hyper-localize risk across the operational zone**. By continuously **analyzing real-time news, weather updates, and traffic telemetry** within these spatial grids, Winkit maps the exact **physical reality of the streets** to **instantly trigger zero-touch claims.**

# Target Persona- Q-Commerce Delivery Rider
Our target use is Q-commerce delivery rider operating on variable and shift-based earnings. If a sudden disruption occurs—like severe flooding or a government-mandated curfew—their income drops to zero. Traditional insurance cannot serve this demographic because the administrative cost of processing a ₹500 missed-shift claim is higher than the payout itself.

## The Scenario
A rider operating near Potheri, Chennai, plans to work a 6-hour shift. The weather forecast is clear, but a sudden political protest severely blocks the main GST Road. The rider is physically unable to safely deliver orders.

## End-to-End Workflow
1. **App Initialization:** The rider opens the Winkit app. The backend calculates their dynamic "Earning Velocity" (e.g., ₹150/hr) based on historical delivery ledger data.

2. **Real-Time Risk Assessment:** The system instantly queries Weather APIs and our Agentic LLM (which reads live local RSS news feeds) to assess the probability of disruption.

3. **Smart Quote:** The rider purchases a risk-adjusted micro-policy for a week.

4. **Parametric Trigger:** Later that day, the Agentic AI confirms via news APIs that the road is fully blocked, hitting the disruption threshold.

5. **Zero-Touch Payout:** Because the parametric condition is mathematically met, the system monitors the rider. Based on the number of deliveries that the rider received for that area and the amount of time that was required to take de-tour. By the end of shift, Winkit instantly deposits the ₹150 (1 hours × ₹150) coverage into the rider's wallet. No claims adjusters, no manual verification.

---
## 📑 Table of Contents

- [Target Persona](#target-persona)
- [The Scenario](#the-scenario)
- [End-to-End Workflow](#end-to-end-workflow)
- [Unicorn Ideas](#unicorn-ideas)
- [Pricing & Risk Model](#pricing--risk-model)
- [AI/ML Integration](#aiml-integration)
- [Events Coverage](#events-coverage)
- [Platform Justification (Mobile-First)](#platform-justification-mobile-first)
- [Tech Stack](#tech-stack)
- [System Architecture Overview](#system-architecture-overview)
- [Implementation](#implementation)
- [Market Crash](#market-crash)
- [Development Roadmap](#development-roadmap)
- [Team](#team)


# Unicorn Ideas 

Winkit goes beyond traditional parametric insurance by introducing **system-level innovations** that improve both worker outcomes and insurer efficiency.

## 1. Paid Relocation Engine (A* Path Planning)

### The Problem

When a **single dark store goes offline** due to a localized disruption (power outage, protest, roadblock), delivery riders assigned to that store instantly lose income for the entire shift.

Traditional insurance would:

- pay full income loss (₹500–₹800)
- increase insurer losses
- create inefficiency in capital usage

### The Winkit Approach

Instead of paying for inactivity, Winkit enables **paid relocation to a nearby operational zone**.

### How It Works

1. A disruption is detected at a specific dark store
2. The system queries nearby **active stores**
3. A graph is constructed using road networks
4. The **A\* path planning algorithm** computes the fastest route
5. Transit time is calculated dynamically using traffic data
6. Rider is offered a **paid relocation option**

### Example
<img width="1076" height="579" alt="Screenshot from 2026-03-19 10-37-32" src="https://github.com/user-attachments/assets/72f488e8-b2b6-4825-8d71-d9f56a01699b" />

### Why This is Powerful

- Reduces insurer payout (₹150 vs ₹600 full shift loss)
- Keeps the rider **earning instead of idle**
- Improves **system efficiency and capital sustainability**
- Introduces **optimization into insurance**, not just compensation

---
# Pricing & Risk Model
Winkit eliminates manual claim processing by tying payouts to objective, third-party data thresholds. The basic formula for this insurance policy is

$$
\text{Premium} = [1 - (1 - p_{weather})(1 - p_{civic})] \times L \times \beta + platform fee
$$

where, 

$p_{weather}$ : the probability of precipitation or harsh weather  \
$p_{civic}$ : riot, political movement ans other similar event \
$L$ : payout \
$\beta$ : risk multiplier

Here to calculate the $p_{weather}$ \
**Infrastructure Water Boosting($p_{boosted}$):** By mapping zones using Uber H3 Hexagons, the system tracks historical delivery failure rates to assign a $V_{zone}$​ vulnerability score. $V_{zone}$​ acts as an infrastructure multiplier on the weather itself.

$$
p_{boosted​}=min(p_{weather}​×(1.0+V_{zone}​),1.0)
$$

**Spillover Retention ($p_{spillover​}$):** Weather risk doesn't reset at midnight. A massive storm on Monday creates a residual disruption risk on a perfectly sunny Tuesday. The system calculates standing water retention based on the zone's specific drainage capacity.

$$
p_{spillover}​=p_{yesterday}​×(0.66×V_{zone}​)
$$

**The Effective Weather Risk:** The final probability fed into the Union Probability pricing engine is simply whichever danger is higher—today's boosted rain falling from the sky, or yesterday's standing water trapped on the street:

$$
p_{effective}​=max(p_{boosted​}, p_{spillover}​)
$$

To implement the model, we have divided our implementation in 2 stages.
## Stage 1 - C.O.L.D. Start
Stage 1 establishes the security layer for the Winkit ecosystem through robust anomaly and fraud detection. By implementing a Risk Multiplier, we create a data-driven barrier against bad actors attempting to exploit threshold-based payouts. This "Cold Start" integrity is a prerequisite for the scaling logic introduced in Stage 2.

$$
\beta = 1.0 + U_{weather} + F_{risk}
$$

where, \
$U_{weather}$ : unpredictability of weather \
$F_{risk}$ : game risk, where all the users are penalised based on history. New users are not penalized. \


### $U_{weather}$
This is dependent on 2 values, binary variance and time decay. 
We calculate the binary variance using bernoulli distribution where,

**Binary Variance** 

$variance = p(1-p)$

This value peaks to 0.25 when p = 0.5.

**Time Decay**

A forecast for tomorrow is highly reliable but the same could not be said for 5 days in future. We are calculating with with the help of theta decay = $\ k&radic;t \$

Finally, 

$$U_{weather} = k&radic;t + w.p(1-p)$$ 

here, \
$t$ = no. of days in the future \
$k$ = time constant(0.05) \
$p$ = pop(probability of precipitation) \
$w$ = variance weight constant 0.05 \
If a pre-agreed API threshold is crossed the policy executes automatically.

## Stage 2 - Hyper-Localization & AI Data Moat
As the platform scales, the β multiplier transitions from static data to an empirical ML feedback loop.
- H3 Spatial Mapping: By mapping operational zones using Uber H3 Hexagonal Hierarchical Geospatial Indexing to hyper-localize risk across the operational zone, the system tracks historical delivery failure rates. If a specific street floods repeatedly, the algorithm automatically spikes the Vzone​ premium for that exact grid.
  
- Agentic AI: Weather provides structured probabilities, but civic risk (riots, curfews) does not. We integrate a lightweight Agentic AI layer utilizing the Gemini 2.5 Flash API. By parsing live local RSS news feeds through strict zero-shot prompts, the LLM returns a deterministic JSON probability of civic disruption to feed the math engine.

New users for whom no data is available are given a default score of the zone average.

# AI/ML Integration
Winkit utilizes AI/ML across three distinct layers of the architecture to ensure accurate pricing and eliminate systemic abuse:

- **Dynamic Premium Calculation (The β Multiplier):** The premium relies on β=1.0+Urisk​+Frisk​+Vzone​. As the platform scales, Vzone​ (geospatial penalty) transitions from static data to an empirical ML feedback loop. By mapping operational zones using Uber H3 Hexagonal Grids, the system tracks historical delivery failure rates per hexagon. If a specific street floods repeatedly, the algorithm automatically spikes the Vzone​ premium for that exact grid.

- **Automated Fraud Detection ($F_{risk}$​):** Because the platform is 100% parametric, individual "fake claims" are impossible (a rider cannot claim it rained if the API says it is sunny). However, to protect the capital pool from systemic platform abuse, we include $F_{risk}$​, a dynamic fraud buffer that algorithmically scales based on suspicious user clustering or historically anomalous API data.

- **Agentic AI for Unstructured Risk:** Weather provides structured probabilities, but civic risk (riots, curfews) does not. We integrate a lightweight Agentic AI layer utilizing the Gemini 2.5 Flash API. By parsing live local RSS news feeds through strict zero-shot prompts, the LLM acts as an extraction agent, returning a deterministic JSON probability of civic disruption to feed the math engine.

  
# Events Coverage
## Events Covered

| Category | Event Types |
| :--- | :--- |
| **Environmental** | Heavy rainfall, Cyclones, Severe thunderstorms, Flooding, Low visibility (Fog / Smog) |
| **Traffic / Urban** | Major road closures, Accident hotspots, Severe congestion, Construction blockages, VIP movement restrictions |
| **Regulatory** | Section 144 curfews, Emergency lockdowns, Election restrictions, Festival crowd control zones, Protest zones |

- Regulatory events that have been informed well in time, before 48 hours of the disruption will not be liable in the policy
- If unplanned events continue for more than 1 week. The policy for next week will be on hold and not issued for the next week.
  
## Events NOT Covered

To prevent **moral hazard**, the following disruptions are excluded:

- vendor strikes
- supply chain stockouts
- demand collapse
- voluntary worker absence

# Platform Justification (Mobile-First)
Winkit is deployed as a native Android Mobile Application (Kotlin + Jetpack Compose). A web app cannot support our hyper-local risk engine, which requires continuous GPS Telemetry and Background location access to accurately map the rider to a specific hexagonal grid for dynamic geospatial pricing. The users are payed in their UPI bank account.

### Insurer Dashboard

Built with **Next.js + React**.

Features include:

- disruption analytics
- payout monitoring
- geospatial event visualization
- fraud detection insights

# Tech Stack

| Layer | Technology |
|---|---|
Mobile | Kotlin + Jetpack Compose |
Backend | FastAPI |
Database | PostgreSQL + PostGIS |
ML | Scikit-Learn |
AI Agents | Gemini 2.5 Flash API (Direct Structured Outputs) |
Routing Engine | NetworkX |
Web Dashboard | Next.js |
Maps | Mapbox / Leaflet |
Notifications | Firebase Cloud Messaging |
Payments | Razorpay Sandbox |


Example reasoning pipeline:
`User Location
↓
Check GeoJSON Government Restriction Zones
↓
Verify Weather and Traffic Data
↓
Validate Disruption Event
↓
Trigger Automatic Payout to register UPI`

---
# System Architecture Overview
<img width="4300" height="2000" alt="Screenshot from 2026-03-19 10-45-40" src="https://github.com/user-attachments/assets/73c2bb01-d976-4eb4-9805-fd9fd85ded56" />  

# Premium Calculation Architecture
 <img width="2739" height="2422" alt="mermaid-diagram-2026-03-19-180219" src="https://github.com/user-attachments/assets/b21fe5ed-e79b-4b87-bc1f-92203344bb89" />

# Implementation
Follow these steps to implement our code in your local desktop.

Clone the repo and setup the environment.
```
git clone https://github.com/your-username/Winkit.git
cd Winkit

# Create the virtual environment
python -m venv .venv

# Activate the virtual environment
# On macOS/Linux:
source .venv/bin/activate
# On Windows:
# .venv\Scripts\activate

# Install dependencies
pip install -r requirements.txt
```
Create `config.py` to store API keys

```
GEMINI_API="your_google_gemini_api_key"
OPENWEATHER_API_KEY="your_openweather_api_key"


# Setting this to TRUE to API calls and simulate a Cyclone in Chennai.
# This will allow you to test the Stateful Waterlogging Math without needing live API keys.
DEMO_MODE=True
```

finally run the test script
```
python test_flow.py
```

<img width="936" height="732" alt="image" src="https://github.com/user-attachments/assets/4520c54e-fd69-4ae5-9d54-3da0b214c111" />

This is the prototype and basic working of the engine when a new user triggers the system.

<img width="936" height="732" alt="image" src="https://github.com/user-attachments/assets/928c2995-8246-4719-936a-c1dd24cfaaeb" />

This is the prototype and basic working of the engine when a veteran user triggers the system.

# Flow Architecture
<img width="1052" height="845" alt="Screenshot from 2026-03-19 10-47-12" src="https://github.com/user-attachments/assets/21ea8edf-b234-4e85-84de-33d9e0969cd2" />


The structure of our code base:
```
├── app.db
├── core-backend
│   ├── database.py
│   ├── __pycache__
│   └── worker_profile.py
├── engine
│   └── dynamic_pricing
├── frontend
│   ├── app
│   ├── build.gradle.kts
│   ├── gradle
│   ├── gradle.properties
│   ├── gradlew
│   ├── gradlew.bat
│   └── settings.gradle.kts
├── gig_workers_db.csv
├── README.md
├── requirements.txt 
├── services
│   ├── civic_risk_agent.py
│   ├── location_risk_service.py
│   └── weather_api_client.py
├── test_flow.py
├── trigger-workers
│   └── event_evaluator.py
└── WinkIt

```

**Core Risk Engine**: The heart of the program is `\engine`. It ingests real-time data to solve the β multiplier equation. We are yet to add the H3 Integration.

**Agentic AI Layer**: To solve the problem of unstructured civic risk, we utilize Gemini 2.5 Flash as a "Reasoning Agent."

- `civic_risk_agent.py`: This service fetches local RSS feeds and news snippets. It uses structured output (JSON) to convert news headlines into a deterministic pcivic​ value between 0.0 and 1.0.
- `weather_api_client.py`: Interacts with OpenWeather/Weatherstack to provide the $p_{weather​}$ grounding data.

**Parametric Trigger & Evaluator**: This is the zero-touch layer.

- `event_evaluator.py`: A background worker that constantly compares the pre-agreed policy thresholds against live data feeds. When a condition is met within a specific H3 hexagon, it flags all active policies in that grid for payout.

**Backend & Data Persistence**

- `database.py`: Manages the PostgreSQL/PostGIS instance.
- `worker_profile.py`: Maintains the "Earning Velocity" and $F_{risk}​ (Fraud Score) for each rider, ensuring that payouts are proportional to actual historical performance.

**Mobile Client**: Built with Kotlin and Jetpack Compose, the mobile app acts as the primary data sensor. It handles:
 - Background Telemetry: Periodically pings the backend with encrypted location data to verify the rider is within their insured risk zone.
 - Instant Wallet: Displays real-time policy status and immediate payout notifications via Firebase.

# Market Crash
## The Problem
A sophisticated syndicate of 500 delivery workers in a tier-1 city has successfully exploited a beta parametric insurance platform. Organizing via localized Telegram groups, they are using advanced GPS-spoofing applications to fake their locations. While resting safely at home, they are tricking the system into believing they are trapped in a severe, red-alert weather zone, triggering mass false payouts and instantly draining the liquidity pool.

## Solution 
TO solve this we deciding to implement multiple security measures and checks.
In the current market there are 2 types of widely used for OS for phones, Android and iOS.
The market share for Android users is 96% and only 4% for iOS users according to a study conducted by Incognia. 

### Android Users
We perform a series of checks and evaluation to prevent and detect location spoofing and other similar activities to the game the system.
- **Developer Settings:** This is one of the most basic methods of perform location spoofing. For our app to work, it will need a basic requirement that developer setting remain disabled.
- **Location Flags:** Check for mock location flags
- **OS Signature:** Another method is to jail-break or rooting the mobile phone. We verify the cryptographic OS signature.
- **Play integrity Attestation**
- **IMU and GPS co-relation ** — the tool moves the GPS pin but cannot move the phone's accelerometer. GPS says moving at 25 km/h. So we can cross verify this data point.
- **GPS Satellites Verification:** A phone typically utilizes 8 to 12 satellites at a given time to give GPS coordinates. For this verification, we take the raw GPS data input which includes:

  - **PRN:** The ID of the satellite.

  - **Azimuth:** The compass direction of the satellite.

  - **Elevation:** The angle of the satellite above the horizon.

  - **SNR / C/N0:** The signal-to-noise ratio.
Using this data and cross-verifying with the actual position of the satellites we are able to determine the integerity of the data and GPS data

### iOS Users
OS protocols here are different. Possible places where spoofing can be done is tempering with iTunes protocols, using another devices with tools like iAnyGo, 3uTools, Dr.Fone and others. We cannot perform many tests that we did for Android users but since the security of iOS is already strong. These softwares are only able to inject 2D coordinates (altitude is hardcoded). Possible detection and prevention methods are:
- **Z-axis flatlines:** Real GPS altitude fluctuates by ±5 metres even standing still
- **Perfect coordinate smoothness:** movement paths are mathematically interpolated. No road jitter, no signal multipath, no micro-deceleration at turns.
- **IMU and GPS co-relation:** The tool moves the GPS pin but cannot move the phone's accelerometer. GPS says moving at 25 km/h. So we can cross verify this data point.
---

## Development Roadmap

### Phase 1: Foundation & Core Architecture (Weeks 1–2) ✅
*Focus: Mathematical modeling, database scaffolding, and system design.*

* **Persona & Scope:** Defined target Q-Commerce persona (shift-based, 2km radius) and mapped binary income loss scenarios (Environmental vs. Civic).
* **Risk Math Engine:** Established the dynamic β multiplier logic and union probability models for unstructured civic risk and deterministic weather data.
* **Backend Infrastructure:** Deployed the core FastAPI structure, established PostgreSQL/PostGIS database schemas for geospatial queries, and created the foundational API routing (`database.py`, `worker_profile.py`).
* **Market Crash Strategy:** Architected the algorithmic defense against GPS spoofing syndicates (Z-axis flatline detection, GPS/IMU telemetry correlation).
* **Hackathon Assets:** Completed the enterprise-focused README, Mermaid.js system architecture diagram, native UI sandbox, and the Phase 1 pitch video.

### Phase 2: Agentic AI & Geospatial Engine (Weeks 3–4) 
*Focus: Bringing the "Brain" and the Insurer Dashboard to life.*

* **Agentic Risk Engine:** Implement the Gemini 2.5 Flash API service (`civic_risk_agent.py`) to parse live RSS news feeds through zero-shot prompts, outputting deterministic JSON risk scores for civic disruptions.
* **ML Pricing Model:** Train the Scikit-Learn engine to calculate custom, shift-weighted weekly premiums based on historical disruption data and user risk vectors.
* **Insurer Command Center:** Develop the Next.js + Tremor UI web dashboard for actuaries. Integrate Uber H3 Hexagonal mapping to visualize live risk zones and monitor the platform's financial exposure.
* **Native Mobile MVP:** Connect the Kotlin + Jetpack Compose UI screens to the FastAPI backend, finalizing the onboarding, shift-selection, and dynamic policy checkout flows.

### Phase 3: Telemetry Defense & Zero-Touch Payouts (Weeks 5–6) 🏁
*Focus: Anti-spoofing hardware integration, automated routing, and the final financial triggers.*

* **Hardware Defense Layer:** Finalize the native Android sensor-fusion logic. Cross-reference raw IMU (accelerometer/gyroscope) data with GPS NMEA telemetry to actively block spoofing apps and ensure absolute location integrity.
* **Active Mitigation Engine:** Build out the NetworkX A* path planning logic to calculate paid relocation transit routes for riders trapped in localized blackout zones.
* **Zero-Touch Escrow & Payouts:** Map the trigger evaluation worker (`event_evaluator.py`) directly to the Smart Escrow Wallet. Integrate the Razorpay sandbox to simulate instant UPI disbursements upon Agentic AI approval.
* **Final Pitch Production:** Record the final end-to-end technical demo showcasing a localized disruption, the native app telemetry defense, the Next.js tracking dashboard, and the instant zero-touch payout.
---
## What Makes Winkit Different
_Five Innovations No Competitor Has Combined_

1. **Uber H3 Hexagonal Hyper-Localization:** Every competing platform defines risk at the district or pin-code level. Winkit maps every operational zone using **Uber's H3 hexagonal grid at resolution 9** each hexagon covering ~0.1 km². We price each one independently. Why hexagons? Unlike square grids, every H3 cell has equidistant neighbors no corner-adjacency distortion. A flood affecting one hex affects its 6 neighbors with mathematically equal proximity. This geometric property makes spatial risk propagation clean and unambiguous.

2. **$V_{zone}$ (The Infrastructure Vulnerability Score):** Each H3 hexagon accumulates a **$V_{zone}$ score** derived from historical delivery failure events within that exact cell. If a specific hex floods repeatedly, its V_zone rises automatically, no manual recalibration. This is Winkit's data moat: every disruption event makes pricing more accurate for that exact street. 

3. **$p_{boosted}$ (Infrastructure-Weighted Weather Risk):** Standard parametric platforms ask: *"What is the probability of rain?"* Winkit asks: *"What is the probability of rain **causing income loss in this specific hex**?"* These are not the same question.

4. **$p_{spillover}$ (Risk Doesn't Reset at Midnight):** **This is our most distinctive actuarial insight and no competitor has it.** Every parametric weather platform resets its risk model at midnight. Winkit does not. A massive storm on Monday creates standing water on Tuesday — even under a clear sky. Chennai's drainage infrastructure in low-lying zones retains water for 18–36 hours. A rider working Tuesday morning is still exposed to Monday's storm.

5. **Satellite Ephemeris Verification: Fraud-Proof Physics:** GPS spoofing apps inject fake coordinates at the OS level. They cannot fake the physical geometry of satellites in orbit. Every GPS satellite broadcasts its position and those positions are **mathematically predictable to centimetre accuracy** from NASA and ISRO's NavIC ephemeris data. When a claim is filed, Winkit queries the raw `GnssStatus` API to capture the exact satellite constellation the device reports: PRN IDs, azimuth angles, elevation angles, and SNR values. Our backend computes the *expected sky plot* for the claimed coordinates at that exact timestamp.
---
# Team

**Astro Bugs**

| Member | Role |
|---|---|
Pavithra | Mobile Architecture |
Ayush | AI/ML & Risk Modeling |
Nikhil | Web Dashboard |
Amman | Product Strategy |

---
