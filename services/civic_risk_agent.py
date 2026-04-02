import os
import sys
import json
import requests
import feedparser
from config import GEMINI_API
# Adjust path to import from config.py in the root directory
current_dir = os.path.dirname(os.path.abspath(__file__))
root_dir = os.path.dirname(current_dir)
sys.path.append(root_dir)

try:
    from config import GEMINI_API
except ImportError:
    print("⚠️ Error: Could not find GEMINI_API in config.py")
    sys.exit(1)
try:
    from config import GEMINI_API, TOMTOM, DEMO_MODE
except ImportError:
    print("⚠️ Error: Check your config.py imports")
    sys.exit(1)
def fetch_live_chennai_headlines(max_headlines: int = 5) -> list:
    """Fetches the latest breaking news headlines for Chennai."""
    rss_url = "https://timesofindia.indiatimes.com/rssfeeds/2950623.cms"
    
    try:
        feed = feedparser.parse(rss_url)
        headlines = []
        for entry in feed.entries[:max_headlines]:
            clean_summary = entry.summary.split('<')[0] # Strips HTML tags
            headlines.append(f"{entry.title} - {clean_summary}")
        return headlines
    except Exception as e:
        print(f"Failed to fetch live news: {e}")
        return ["No local news available at this time."]

class CivicRiskAgent:
    def __init__(self, api_key: str = GEMINI_API):
        self.api_key = api_key
        # Using Gemini 2.5 Flash for high-speed, cost-effective reasoning
        self.endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"
        
        # THE OBJECTIVE ACTUARIAL MATRIX
        self.civic_matrix = {
            "TOTAL_SHUTDOWN": 1.0,     # Curfew, Section 144, Internet ban
            "ARTERIAL_BLOCKAGE": 0.75, # Riots, major protests blocking highways
            "LOCALIZED_FRICTION": 0.30,# VIP movement, festival crowds, peaceful rallies
            "NORMAL": 0.0              # Standard traffic, minor accidents
        }

    def analyze_civic_risk(self, news_headlines: list, rider_zone: str) -> dict:
        """Passes local news to the LLM Classifier and applies Actuarial Logic."""
        
        prompt = (
            "You are a strict data-extraction AI for a Q-Commerce insurance platform.\n"
            f"Review these local news headlines for the city containing the zone '{rider_zone}': {news_headlines}\n"
            "Your ONLY job is to classify the most severe civic event mentioned into one of four exact categories:\n"
            "1. 'TOTAL_SHUTDOWN' (Section 144, Curfew, City-wide strike, Police Lockdown)\n"
            "2. 'ARTERIAL_BLOCKAGE' (Riots, major protests blocking highways/GST road)\n"
            "3. 'LOCALIZED_FRICTION' (VIP movement, localized festival crowds, peaceful rallies)\n"
            "4. 'NORMAL' (Standard traffic, minor accidents, weather events, or no relevant news)\n\n"
            'You MUST respond in strict JSON format ONLY: {"classification": "THE_CATEGORY", "event_location": "Name of specific neighborhood/road or None", "reason": "1-sentence justification"}'
        )

        headers = {"Content-Type": "application/json"}
        payload = {"contents": [{"parts": [{"text": prompt}]}]}

        try:
            # 1. Call Gemini to Classify the Event
            response = requests.post(
                f"{self.endpoint}?key={self.api_key}",
                headers=headers,
                json=payload
            )
            response.raise_for_status()
            
            result = response.json()
            content = result["candidates"][0]["content"]["parts"][0]["text"]
            clean_content = content.replace("```json", "").replace("```", "").strip()
            raw_data = json.loads(clean_content)
            
            classification = raw_data.get("classification", "NORMAL")
            event_location = raw_data.get("event_location", "").lower()
            rider_zone_lower = rider_zone.lower()

            # 2. Map Classification to Objective Probability
            p_base = self.civic_matrix.get(classification, 0.0)

            # 3. Epicenter / Distance Decay Logic
            # If it's a city-wide shutdown, everyone is at risk (1.0).
            if classification == "TOTAL_SHUTDOWN":
                p_civic = p_base
            # If the rider is exactly where the protest is, full risk.
            elif rider_zone_lower in event_location or event_location in rider_zone_lower:
                p_civic = p_base
            # If there's a protest, but it's 15km away from the rider, apply a 80% decay damper.
            else:
                p_civic = p_base * 0.20 

            # 4. Traffic Telemetry Validation (The Reality Check)
            # We will build this out next!
            is_traffic_validated = self.verify_with_traffic_telemetry(rider_zone, classification)
            
            if not is_traffic_validated and p_civic > 0:
                print(f"🚦 Traffic API Override: LLM claimed {classification}, but traffic is flowing normally. Downgrading risk.")
                p_civic = 0.0
                raw_data["reason"] += " (OVERRIDDEN: Traffic telemetry shows normal speeds)."

            # 5. Finalize Payload
            raw_data["p_civic"] = round(p_civic, 3)
            raw_data["zone_evaluated"] = rider_zone
            
            return raw_data
            
        except Exception as e:
            print(f"⚠️ Agentic AI Error: {e}")
            return {"p_civic": 0.0, "classification": "ERROR", "event_location": "None", "reason": "Failed to parse civic risk. Defaulting to 0."}

    def _get_zone_coordinates(self, zone_name: str) -> tuple:
        """
        Helper function to map zone names to GPS coordinates for the Traffic API.
        In a full database, you'd query PostGIS. For the demo, we use a fast dictionary.
        """
        # Format: (Latitude, Longitude)
        zone_map = {
            "potheri": (12.8236, 80.0435),          # SRM University GST Road
            "koramangala": (12.9352, 77.6245),      # BLR
            "andheri": (19.1136, 72.8697),          # MUM
            # Default fallback to Chennai center if zone not found
            "default": (13.0827, 80.2707)           
        }
        
        # Search for keyword matches (e.g. "potheri" in "potheri / srm area")
        for key, coords in zone_map.items():
            if key in zone_name.lower():
                return coords
        return zone_map["default"]

    def verify_with_traffic_telemetry(self, zone: str, llm_classification: str) -> bool:
        """
        Queries the live TomTom Traffic API. If the LLM claims a blockage,
        we check if actual vehicle speeds have dropped below 40% of normal.
        """
        # 1. No need to waste API calls if the LLM says everything is fine
        if llm_classification == "NORMAL":
            return True
            
        # 2. Safety fallback for testing without an API key
        if not TOMTOM:
            print("⚠️ TomTom API Key missing. Skipping live traffic validation.")
            return True

        # 3. Fetch coordinates and hit the TomTom API
        lat, lng = self._get_zone_coordinates(zone)
        url = f"https://api.tomtom.com/traffic/services/4/flowSegmentData/absolute/10/json?point={lat},{lng}&key={TOMTOM}"

        try:
            response = requests.get(url, timeout=5)
            response.raise_for_status()
            data = response.json()
            
            flow_data = data.get('flowSegmentData', {})
            current_speed = flow_data.get('currentSpeed', 1)
            free_flow_speed = flow_data.get('freeFlowSpeed', 1)
            
            # 4. The Mathematical Reality Check
            # Ratio of 1.0 means perfect traffic. 0.10 means dead gridlock.
            flow_ratio = current_speed / free_flow_speed
            print(f"   [Traffic API] {zone.title()} Flow Ratio: {flow_ratio:.2f} (Current: {current_speed}km/h | Free: {free_flow_speed}km/h)")
            
            # If the LLM claims an Arterial Blockage or Shutdown, the physical speed MUST be terribly slow (< 40%).
            if llm_classification in ["TOTAL_SHUTDOWN", "ARTERIAL_BLOCKAGE"]:
                if flow_ratio > 0.40:
                    return False # LLM hallucinated. Traffic is moving fine.
            
            # If the LLM claims Localized Friction, traffic should be somewhat slow (< 75%).
            if llm_classification == "LOCALIZED_FRICTION":
                if flow_ratio > 0.75:
                    return False # LLM hallucinated. Traffic is smooth.
                    
            return True # The traffic data confirms the LLM's classification!
            
        except requests.exceptions.RequestException as e:
            print(f"⚠️ Traffic API Error: {e}")
            # Fail-open: If the traffic API crashes, trust the LLM so the insurance system doesn't break
            return True


# --- LOCAL TESTING ---
if __name__ == "__main__":
    agent = CivicRiskAgent()
    target_zone = "Potheri"
    
    print("📡 Fetching LIVE news from Chennai RSS Feed...")
    live_news = fetch_live_chennai_headlines(max_headlines=4)
    for i, headline in enumerate(live_news):
        print(f"  {i+1}. {headline}")
        
    print(f"\n🧠 Analyzing live news for civic risk in {target_zone}...")
    risk_assessment = agent.analyze_civic_risk(live_news, rider_zone=target_zone)
    
    print("\n✅ AI Actuarial Extraction Complete:")
    print(f"📍 Zone Evaluated: {risk_assessment.get('zone_evaluated')}")
    print(f"📊 Classification: {risk_assessment.get('classification')}")
    print(f"🗺️ Event Location: {risk_assessment.get('event_location')}")
    print(f"🚨 Final p_civic:  {risk_assessment.get('p_civic')}")
    print(f"📝 Reasoning:      {risk_assessment.get('reason')}")
    print("-" * 50)
