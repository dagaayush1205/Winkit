"""
This script is performing the following functions:
The main objective is to create an agent that can analyze local news headlines
for a specific zone in Chennai and classify the civic risk level based on the
severity of events mentioned in the news. The agent uses a Large Language Model
(LLM) to classify the events into predefined categories and then applies
actuarial logic to determine a probability of civic disruption (p_civic).
Additionally, it validates the LLM's classification against live traffic
telemetry from the TomTom Traffic API to ensure accuracy.
"""

import os
import sys
import json
import requests
import feedparser
from openai import OpenAI  

current_dir = os.path.dirname(os.path.abspath(__file__))
root_dir = os.path.dirname(current_dir)
sys.path.append(root_dir)

try:
    from config import TOMTOM, DEMO_MODE, CEREBRAS_API_KEY
except ImportError:
    print("⚠️ Error: Check your config.py imports. Make sure CEREBRAS_API_KEY is defined.")
    sys.exit(1)


    """ Fetching news headlines"""
def fetch_live_chennai_headlines(max_headlines: int = 5) -> list:
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
    def __init__(self):
        self.client = OpenAI(
            api_key=CEREBRAS_API_KEY,
            base_url="https://api.cerebras.ai/v1"
        )
        
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

        try:
            #  Call Cerebras to Classify the Event
            response = self.client.chat.completions.create(
                model="llama3.1-8b", # Using Cerebras's Llama 3.1 70B model
                messages=[{"role": "user", "content": prompt}],
                response_format={"type": "json_object"}  # Enforces JSON output natively
            )
            
            # Extract the string content from the response object
            content = response.choices[0].message.content
            
            # Clean up potential markdown formatting just in case
            clean_content = content.replace("```json", "").replace("```", "").strip()
            raw_data = json.loads(clean_content)
            
            classification = raw_data.get("classification", "NORMAL")
            event_location = raw_data.get("event_location", "").lower()
            rider_zone_lower = rider_zone.lower()

            # Map Classification to Objective Probability
            p_base = self.civic_matrix.get(classification, 0.0)

            # Epicenter / Distance Decay Logic
            if classification == "TOTAL_SHUTDOWN":
                p_civic = p_base
            elif rider_zone_lower in event_location or event_location in rider_zone_lower:
                p_civic = p_base
            else:
                p_civic = p_base * 0.20 

            # Traffic Telemetry Validation (The Reality Check)
            is_traffic_validated = self.verify_with_traffic_telemetry(rider_zone, classification)
            
            if not is_traffic_validated and p_civic > 0:
                print(f"🚦 Traffic API Override: LLM claimed {classification}, but traffic is flowing normally. Downgrading risk.")
                p_civic = 0.0
                raw_data["reason"] += " (OVERRIDDEN: Traffic telemetry shows normal speeds)."

            # Finalize Payload
            raw_data["p_civic"] = round(p_civic, 3)
            raw_data["zone_evaluated"] = rider_zone
            
            return raw_data
            
        except Exception as e:
            print(f"⚠️ Agentic AI Error: {e}")
            return {"p_civic": 0.0, "classification": "ERROR", "event_location": "None", "reason": "Failed to parse civic risk. Defaulting to 0."}

    def _get_zone_coordinates(self, zone_name: str) -> tuple:
        """Helper function to map zone names to GPS coordinates for the Traffic API."""
        zone_map = {
            "potheri": (12.8236, 80.0435),          # SRM University GST Road
            "koramangala": (12.9352, 77.6245),      # BLR
            "andheri": (19.1136, 72.8697),          # MUM
            "default": (13.0827, 80.2707)           
        }
        
        for key, coords in zone_map.items():
            if key in zone_name.lower():
                return coords
        return zone_map["default"]

    def verify_with_traffic_telemetry(self, zone: str, llm_classification: str) -> bool:
        """Queries the live TomTom Traffic API."""
        if llm_classification == "NORMAL":
            return True
            
        if not TOMTOM:
            print("⚠️ TomTom API Key missing. Skipping live traffic validation.")
            return True

        lat, lng = self._get_zone_coordinates(zone)
        url = f"https://api.tomtom.com/traffic/services/4/flowSegmentData/absolute/10/json?point={lat},{lng}&key={TOMTOM}"

        try:
            response = requests.get(url, timeout=5)
            response.raise_for_status()
            data = response.json()
            
            flow_data = data.get('flowSegmentData', {})
            current_speed = flow_data.get('currentSpeed', 1)
            free_flow_speed = flow_data.get('freeFlowSpeed', 1)
            
            flow_ratio = current_speed / free_flow_speed
            print(f"   [Traffic API] {zone.title()} Flow Ratio: {flow_ratio:.2f} (Current: {current_speed}km/h | Free: {free_flow_speed}km/h)")
            
            if llm_classification in ["TOTAL_SHUTDOWN", "ARTERIAL_BLOCKAGE"]:
                if flow_ratio > 0.40:
                    return False # Traffic is moving fine
            
            if llm_classification == "LOCALIZED_FRICTION":
                if flow_ratio > 0.75:
                    return False # Traffic is smooth
                    
            return True 
            
        except requests.exceptions.RequestException as e:
            print(f"⚠️ Traffic API Error: {e}")
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
