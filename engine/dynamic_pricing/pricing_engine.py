import math
from datetime import datetime, date, timedelta, timezone
import sys
import os

current_dir = os.path.dirname(os.path.abspath(__file__))
root_dir = os.path.dirname(os.path.dirname(current_dir))
sys.path.append(root_dir)

from config import OPENWEATHER_API_KEY, DEMO_MODE, SUPABASE_DB_URL, SUPABASE_API_KEY
from services.weather_api_client import WeatherAPIClient
from services.civic_risk_agent import CivicRiskAgent, fetch_live_chennai_headlines
from supabase import create_client, Client

class DynamicPricingEngine:
    def __init__(self):
        # Initializing Services
        self.weather_client = WeatherAPIClient(api_key=OPENWEATHER_API_KEY, demo_mode=DEMO_MODE)
        self.civic_agent = CivicRiskAgent()
        
        # Initialize Supabase
        self.supabase: Client = create_client(SUPABASE_DB_URL, SUPABASE_API_KEY)

        # Actuarial Constants
        self.k_time_decay = 0.05  
        self.w_variance = 0.40    
        self.platform_fee = 10.00 
    
    def fetch_all_workers(self):
        """Fetches all active workers and their trust scores."""
        try:
            res = self.supabase.table("Workers").select("worker_id, trust_score").execute()
            return res.data or []
        except Exception as e:
            print(f"❌ Error fetching workers: {e}")
            return []

    def process_all_zones(self, dynamic_v_loss: float = 500.0):
        """The Cron Entry Point: Uses a Two-Pass architecture for extreme efficiency."""
        
        print("🌍 Fetching active zones from h3_zone_states...")
        response = self.supabase.table("h3_zone_states").select("*").execute()
        zones = response.data

        workers = self.fetch_all_workers()

        if not zones or not workers:
            print("❌ Missing zones or workers. Aborting sweep.")
            return

        print("📰 Fetching live Chennai news...")
        live_news = fetch_live_chennai_headlines(max_headlines=5)
        
        raw_weather_data = self.weather_client.get_forecast()
        daily_pop = {}
        today = date.today()
        
        for block in raw_weather_data.get('list', []):
            dt = datetime.fromtimestamp(block['dt']).date()
            pop = block.get('pop', 0.0) 
            if dt not in daily_pop or pop > daily_pop[dt]:
                daily_pop[dt] = pop

        # ==========================================
        # PASS 1: EVALUATE ZONES ONCE
        # ==========================================
        # We calculate the base environmental risk for all 7 days and store it in memory.
        # This prevents redundant API math and database writes.
        evaluated_zones = {} 

        for zone in zones:
            zone_7_day_metrics = self._evaluate_and_update_zone(
                zone, live_news, daily_pop, today, dynamic_v_loss
            )
            evaluated_zones[zone['zone_name']] = zone_7_day_metrics

        # ==========================================
        # PASS 2: CALCULATE WORKER PREMIUMS
        # ==========================================
        # Lock the current time strictly to UTC
        current_time = datetime.now(timezone.utc)
        
        # Create the hour bucket, keeping the UTC timezone attached
        hour_bucket = current_time.replace(minute=0, second=0, microsecond=0)

        for worker in workers:
            try:
                worker_id = worker["worker_id"]
                
                # Extract trust score safely, default to 50
                trust_score = float(worker.get("trust_score") or 50.0)
                
                # Map 0-100 Trust Score to a 0.30 -> 0.00 penalty
                worker_f_risk = max(0.0, (100 - trust_score) * 0.005)

                print(f"\n👤 Processing Worker: {worker_id} (Trust: {trust_score}, Penalty: +{worker_f_risk:.2f})")
                
                total_premium = 0.0

                # Loop over our pre-calculated memory matrix
                for zone_name, metrics_7_days in evaluated_zones.items():
                    for daily_el, daily_u_risk in metrics_7_days:
                        
                        # Inject the worker's personal fraud penalty here
                        raw_beta = 1.0 + daily_u_risk + worker_f_risk
                        capped_beta = min(raw_beta, 2.5)
                        
                        total_premium += (capped_beta * daily_el)

                final_gross_premium = round(total_premium + self.platform_fee, 2)

                # Upsert the final calculated premium to the worker's ledger
                self.supabase.table("worker_charges").upsert({
                    "worker_id": worker_id,
                    "created_at": current_time.isoformat(),
                    "hour_bucket": hour_bucket.isoformat(),
                    "premium": final_gross_premium
                }).execute()

                print(f"💼 Updated {worker_id} → ₹{final_gross_premium}")
            except Exception as e:
                print(f"❌ Failed to process worker {worker.get('worker_id', 'UNKNOWN')}. Error: {e}")
                continue
    def _evaluate_and_update_zone(self, zone, live_news, daily_pop, today, dynamic_v_loss):
        """Calculates environmental risk, updates DB, and returns a 7-day metric list."""
        hex_id = zone['hex_id']
        zone_name = zone['zone_name']
        
        v_zone_score = float(zone.get('v_zone_score', 0.25))
        previous_day_water_risk = float(zone.get('yesterday_water', 0.0))
        
        print(f"\n========================================")
        print(f"🔍 Analyzing Zone: {zone_name} ({hex_id})")
        print(f"========================================")

        civic_data = self.civic_agent.analyze_civic_risk(live_news, rider_zone=zone_name)
        base_p_civic = float(civic_data.get("p_civic", 0.0))

        today_effective_weather = 0.0
        today_p_union = 0.0
        
        # We will return a list of tuples: [(day1_el, day1_urisk), (day2_el, day2_urisk)...]
        metrics_7_days = []

        # The 7-Day Simulation Loop
        for t in range(7):
            target_date = today + timedelta(days=t)
            
            if target_date in daily_pop:
                raw_p_weather = daily_pop[target_date]
                last_known_pop = raw_p_weather
            else:
                raw_p_weather = last_known_pop * 0.2  
                last_known_pop = raw_p_weather

            p_civic = base_p_civic * (0.5 ** t)

            infrastructure_multiplier = 1.0 + v_zone_score
            boosted_p_weather = min(raw_p_weather * infrastructure_multiplier, 1.0)

            spillover_retention = 0.66 * v_zone_score
            waterlogging_risk = previous_day_water_risk * spillover_retention

            effective_p_weather = max(boosted_p_weather, waterlogging_risk)
            previous_day_water_risk = effective_p_weather 

            p_neither = (1.0 - effective_p_weather) * (1.0 - p_civic)
            p_union = 1.0 - p_neither

            if t == 0:
                today_effective_weather = effective_p_weather
                today_p_union = p_union

            # Calculate base math parameters (Beta is calculated later per-worker)
            expected_loss_el = p_union * dynamic_v_loss
            time_penalty = self.k_time_decay * math.sqrt(t)
            variance_penalty = self.w_variance * (p_union * (1.0 - p_union))
            u_risk = time_penalty + variance_penalty
            
            metrics_7_days.append((expected_loss_el, u_risk))

        # ==========================================
        # DATABASE WRITE OPERATIONS (Executes only ONCE per zone)
        # ==========================================
        
        self.supabase.table("h3_zone_states").update({
            "yesterday_water": round(today_effective_weather, 4)
        }).eq("hex_id", hex_id).execute()

# Lock the current time strictly to UTC
        current_time = datetime.now(timezone.utc)
        
        # Create the hour bucket, keeping the UTC timezone attached
        hour_bucket = current_time.replace(minute=0, second=0, microsecond=0)

        # Log Disruption Event if notable
        if today_p_union > 0.10: 
            event_type = "CIVIC" if base_p_civic > today_effective_weather else "WEATHER"
            event_id = f"{hex_id}-{event_type}-{hour_bucket.strftime('%Y%m%d%H')}"
            
            self.supabase.table("disruption_events").upsert({
                "event_id": event_id,
                "created_at": current_time.isoformat(),
                "hour_bucket": hour_bucket.isoformat(),
                "hex_id": hex_id,
                "event_type": event_type,
                "p_effective": round(today_effective_weather, 4),
                "p_civic": round(base_p_civic, 4),
                "combined_risk": round(today_p_union, 4)
            }).execute()
            print(f"🚨 Logged {event_type} Disruption Event: {event_id}")
        else:
            print("✅ Zone risk is low. No disruption logged.")
            
        return metrics_7_days


if __name__ == "__main__":
    engine = DynamicPricingEngine()
    engine.process_all_zones(dynamic_v_loss=500.0)
