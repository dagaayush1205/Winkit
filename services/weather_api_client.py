import time
import requests
import os
import sys
from datetime import datetime, timedelta, timezone
import h3
from supabase import create_client, Client
import uuid

# Adjust path to import from root directory
current_dir = os.path.dirname(os.path.abspath(__file__))
root_dir = os.path.dirname(current_dir)
sys.path.append(root_dir)

from config import DEMO_MODE, SUPABASE_DB_URL, SUPABASE_API_KEY, OPENWEATHER_API_KEY

class Colors:
    HEADER = '\033[95m'
    BLUE = '\033[94m'
    CYAN = '\033[96m'
    GREEN = '\033[92m'
    WARNING = '\033[93m'
    FAIL = '\033[91m'
    ENDC = '\033[0m'
    BOLD = '\033[1m'

class WeatherCronJob:
    def __init__(self):
        print(f"{Colors.HEADER}{Colors.BOLD}☁️ STARTING WINKIT WEATHER TELEMETRY CRON{Colors.ENDC}")
        self.api_key = OPENWEATHER_API_KEY
        self.base_url = "https://api.openweathermap.org/data/2.5"
        self.demo_mode = DEMO_MODE
        self.supabase: Client = create_client(SUPABASE_DB_URL, SUPABASE_API_KEY)

    def get_hex_forecast(self, lat: float, lon: float) -> dict:
        """Hits OpenWeather for the specific coordinates of the H3 Hex."""
        if self.demo_mode:
            return self._generate_mock_forecast()

        endpoint = f"{self.base_url}/forecast?lat={lat}&lon={lon}&appid={self.api_key}&units=metric"
        response = requests.get(endpoint)
        response.raise_for_status()
        return response.json()

    def _generate_mock_forecast(self) -> dict:
        """Synthetic data for hackathon stress-testing."""
        current_time = int(time.time())
        return {
            "list": [{
                "dt": current_time + (i * 10800), # 3-hour jumps
                "main": {"temp": 28.5},
                "weather": [{"id": 501}],
                "wind": {"speed": 5.2},
                "pop": 0.85 if i < 10 else 0.10, # Heavy rain early, then clears up
                "rain": {"3h": 12.5}
            } for i in range(40)] # 5 days of mock data
        }

    def _extract_7_day_pop(self, forecast_data: dict) -> dict:
        """Extracts max PoP for the next 5 days and extrapolates days 6 and 7."""
        daily_pop = {}
        # 1. Aggregate max PoP per day from the API
        for block in forecast_data.get('list', []):
            dt = datetime.fromtimestamp(block['dt'], timezone.utc).date()
            pop = block.get('pop', 0.0)
            if dt not in daily_pop or pop > daily_pop[dt]:
                daily_pop[dt] = pop

        # 2. Map exactly 7 days from today, extrapolating missing days
        today = datetime.now(timezone.utc).date()
        final_7_days = {}
        last_known_pop = 0.0
        
        for t in range(7):
            target_date = today + timedelta(days=t)
            if target_date in daily_pop:
                final_7_days[target_date] = daily_pop[target_date]
                last_known_pop = daily_pop[target_date]
            else:
                # Decay logic for days 6 and 7 (or any missing data)
                last_known_pop = last_known_pop * 0.2  
                final_7_days[target_date] = last_known_pop
                
        return final_7_days

    def run_15min_sync(self):
        """Fetches hexes, pulls weather, and batch inserts into DB."""
        print(f"🔍 Fetching active spatial grid from {Colors.CYAN}h3_zone_states{Colors.ENDC}...")
        
        response = self.supabase.table("h3_zone_states").select("hex_id, zone_name").execute()
        zones = response.data

        if not zones:
            print(f"{Colors.FAIL}❌ No zones found. Aborting weather sync.{Colors.ENDC}")
            return

        current_weather_records = []
        future_forecast_records = []
        
        parent_weather_cache = {}
        parent_7day_cache = {}

        for zone in zones:
            hex_id = zone['hex_id']
            zone_name = zone.get('zone_name', 'Unknown')
            
            try:
                parent_hex = h3.cell_to_parent(hex_id, 7)
                
                # Fetch/Cache API Data for Macro Zone
                if parent_hex not in parent_weather_cache:
                    lat, lon = h3.cell_to_latlng(parent_hex)
                    forecast_data = self.get_hex_forecast(lat, lon)
                    parent_weather_cache[parent_hex] = forecast_data
                    
                    # Also calculate and cache the 7-day forward array
                    parent_7day_cache[parent_hex] = self._extract_7_day_pop(forecast_data)
                    print(f"   {Colors.HEADER}📡 API CALL: Fetched new data for Macro-Zone {parent_hex}{Colors.ENDC}")
                else:
                    forecast_data = parent_weather_cache[parent_hex]
                
                # 1. Prepare Current Weather Record (For the 'Weather' table)
                current_block = forecast_data.get('list', [])[0]
                pop = current_block.get('pop', 0.0)
                temp = current_block.get('main', {}).get('temp', 0.0)
                weather_code = current_block.get('weather', [{}])[0].get('id', 800)
                wind_speed_kmh = round(current_block.get('wind', {}).get('speed', 0.0) * 3.6, 2)
                rain_vol = current_block.get('rain', {}).get('3h', 0.0)
                forecast_ts = datetime.fromtimestamp(current_block.get('dt'), timezone.utc).isoformat()
                
                current_weather_records.append({
                    "telemetry_id": f"WT-{uuid.uuid4().hex[:6].upper()}",
                    "Rain": rain_vol,
                    "hex_id": hex_id,
                    "forecast_timestamp": forecast_ts,
                    "pop": pop,
                    "wind_speed_kmh": wind_speed_kmh,
                    "weather_code": weather_code,
                    "temp": temp
                })
                
                # 2. Prepare 7-Day Forecast Records (For the 'future_forecast' table)
                hex_7_day_array = parent_7day_cache[parent_hex]
                for target_date, daily_pop in hex_7_day_array.items():
                    future_forecast_records.append({
                        "hex_id": hex_id,
                        "target_date": target_date.isoformat(),
                        "pop": round(daily_pop, 4),
                        "updated_at": datetime.now(timezone.utc).isoformat()
                    })
                
                print(f"   {Colors.BLUE}🌤️  Processed {zone_name} | Current PoP: {pop*100}%{Colors.ENDC}")

            except Exception as e:
                print(f"{Colors.FAIL}❌ Failed to fetch/parse weather for {hex_id}: {e}{Colors.ENDC}")

        # Batch Insert Current Weather
        if current_weather_records:
            print(f"\n{Colors.WARNING}💾 Executing batch insert of {len(current_weather_records)} current telemetry records...{Colors.ENDC}")
            self.supabase.table("Weather").insert(current_weather_records).execute()
            
        # Batch Upsert 7-Day Forecasts
        if future_forecast_records:
            print(f"{Colors.WARNING}🔮 Executing batch UPSERT of {len(future_forecast_records)} future forecast records...{Colors.ENDC}")
            self.supabase.table("future_forecast").upsert(future_forecast_records, on_conflict="hex_id, target_date").execute()
            
        print(f"{Colors.GREEN}{Colors.BOLD}✅ Weather grid synchronized successfully!{Colors.ENDC}\n")

if __name__ == "__main__":
    cron = WeatherCronJob()
    cron.run_15min_sync()
