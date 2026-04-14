import math
from datetime import datetime, date, timedelta, timezone
from collections import defaultdict
import sys
import os

current_dir = os.path.dirname(os.path.abspath(__file__))
root_dir = os.path.dirname(os.path.dirname(current_dir))
sys.path.append(root_dir)

# Notice: We removed OpenWeather API imports! The engine is now 100% internal database driven.
from config import DEMO_MODE, SUPABASE_DB_URL, SUPABASE_API_KEY
from services.civic_risk_agent import CivicRiskAgent, fetch_live_chennai_headlines
from supabase import create_client, Client

class DynamicPricingEngine:
    """
    Core Actuarial Engine for Winkit.
    Evaluates environmental/civic risk strictly from internal databases and calculates
    pure, uncapped micro-premiums utilizing risk pooling and asymptotic decay.
    """
    
    def __init__(self):
        self.civic_agent = CivicRiskAgent()
        self.supabase: Client = create_client(SUPABASE_DB_URL, SUPABASE_API_KEY)

        # ACTUARIAL & FINANCIAL CONSTANTS
        self.k_time_decay = 0.05        
        self.w_variance = 0.05          
        self.premium_floor = 20.00      
        self.premium_cap = 49.99        
    
    def fetch_all_workers(self) -> list:
        try:
            workers_res = self.supabase.table("Workers").select("worker_id, trust_score, primary_h3_hex").execute()
            policies_res = self.supabase.table("weekly_policies").select("worker_id, max_daily_coverage").eq("status", "ACTIVE").execute()
            
            workers = workers_res.data or []
            policies = {p["worker_id"]: p["max_daily_coverage"] for p in (policies_res.data or [])}
            
            for w in workers:
                w["coverage_limit"] = policies.get(w["worker_id"], 500.0) 
                
            return workers
        except Exception as e:
            print(f"❌ Error fetching workers: {e}")
            return []

    def bulk_fetch_future_forecasts(self, today: date) -> dict:
        """Fetches the 7-day PoP forecast for ALL hexes in one fast query."""
        today_str = today.isoformat()
        response = self.supabase.table("future_forecast").select("*").gte("target_date", today_str).execute()
        
        # Structure: hex_forecasts['hex_id'][date_object] = pop
        hex_forecasts = defaultdict(dict)
        for row in (response.data or []):
            row_date = date.fromisoformat(row['target_date'])
            hex_forecasts[row['hex_id']][row_date] = row['pop']
            
        return hex_forecasts

    def process_all_zones(self):
        print("🌍 Fetching active zones from h3_zone_states...")
        response = self.supabase.table("h3_zone_states").select("*").execute()
        zones = response.data
        workers = self.fetch_all_workers()

        if not zones or not workers:
            print("❌ Missing zones or workers. Aborting actuarial sweep.")
            return

        print("📰 Fetching live Chennai news...")
        live_news = fetch_live_chennai_headlines(max_headlines=5)
        
        today = datetime.now(timezone.utc).date()
        
        # 🚀 THE UPGRADE: Load the entire 7-day forecast for the city from the database!
        global_forecast_map = self.bulk_fetch_future_forecasts(today)

        # ==========================================
        # PASS 1: SPATIAL RISK EVALUATION
        # ==========================================
        evaluated_zones = {} 

        for zone in zones:
            # Extract just the 7-day dictionary for this specific hex
            zone_forecast = global_forecast_map.get(zone['hex_id'], {})
            
            zone_7_day_metrics = self._evaluate_and_update_zone(
                zone, live_news, zone_forecast, today
            )
            evaluated_zones[zone['hex_id']] = zone_7_day_metrics

        # ==========================================
        # PASS 2: WORKER PREMIUM CALCULATION
        # ==========================================
        current_time = datetime.now(timezone.utc)
        hour_bucket = current_time.replace(minute=0, second=0, microsecond=0)

        for worker in workers:
            try:
                worker_id = worker["worker_id"]
                primary_hex = worker.get("primary_h3_hex")
                
                trust_score = float(worker.get("trust_score") or 50.0)
                worker_f_risk = max(0.0, (100 - trust_score) * 0.005)

                if not primary_hex or primary_hex not in evaluated_zones:
                    final_gross_premium = self.premium_floor
                    print(f"\n👤 Processing {worker_id} (No active zone. Base fee applied: ₹{final_gross_premium})")
                else:
                    print(f"\n👤 Processing {worker_id} in {primary_hex} (Trust Penalty: +{worker_f_risk:.2f})")
                    metrics_7_days = evaluated_zones[primary_hex]
                    
                    cumulative_x = 0.0
                    for p_union, daily_u_risk in metrics_7_days:
                        raw_beta = 1.0 + daily_u_risk + worker_f_risk
                        capped_beta = min(raw_beta, 2.5)
                        cumulative_x += (p_union * capped_beta) * 1.5 

                    asymptotic_premium = self.premium_floor + (29.99 * (1.0 - math.exp(-cumulative_x)))
                    final_gross_premium = round(asymptotic_premium, 2)

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

    def _evaluate_and_update_zone(self, zone, live_news, zone_forecast, today):
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
        metrics_7_days = []

        # 7-Day Forward Simulation Loop
        for t in range(7):
            target_date = today + timedelta(days=t)
            
            # Pull directly from the database dictionary passed in
            raw_p_weather = zone_forecast.get(target_date, 0.0)

            p_civic = base_p_civic * (0.5 ** t)

            # --- THE WINKIT SPATIAL MATH ---
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

            time_penalty = self.k_time_decay * math.sqrt(t)
            variance_penalty = self.w_variance * (p_union * (1.0 - p_union))
            u_risk = time_penalty + variance_penalty
            
            metrics_7_days.append((p_union, u_risk))

        # DATABASE WRITE & STATE MANAGEMENT
        new_v_zone = v_zone_score
        
        if today_p_union < 0.10:
            new_v_zone = max(v_zone_score - 0.01, 0.20) 

        self.supabase.table("h3_zone_states").update({
            "yesterday_water": round(today_effective_weather, 4),
            "v_zone_score": round(new_v_zone, 4)
        }).eq("hex_id", hex_id).execute()

        current_time = datetime.now(timezone.utc)
        hour_bucket = current_time.replace(minute=0, second=0, microsecond=0)

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
            print("✅ Zone risk is low. No disruption logged. (V_zone healing active)")
            
        return metrics_7_days

if __name__ == "__main__":
    engine = DynamicPricingEngine()
    engine.process_all_zones()
