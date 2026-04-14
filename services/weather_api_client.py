import time
import requests
import os
import sys
from datetime import datetime, timezone
import h3
from supabase import create_client, Client
import uuid

# Adjust path to import from root directory (One level up from /services)
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
        """Synthetic data for hackathon stress-testing without burning API limits."""
        current_time = int(time.time())
        return {
            "list": [{
                "dt": current_time + 3600,
                "main": {"temp": 28.5},
                "weather": [{"id": 501}],
                "wind": {"speed": 5.2}, # m/s
                "pop": 0.85,            # 85% rain chance
                "rain": {"3h": 12.5}    # 12.5mm of rain
            }]
        }

    def run_15min_sync(self):
        """The main cron function. Fetches hexes, pulls weather, and batch inserts."""
        print(f"🔍 Fetching active spatial grid from {Colors.CYAN}h3_zone_states{Colors.ENDC}...")
        
        # 1. Fetch all active hexes
        response = self.supabase.table("h3_zone_states").select("hex_id, zone_name").execute()
        zones = response.data

        if not zones:
            print(f"{Colors.FAIL}❌ No zones found. Aborting weather sync.{Colors.ENDC}")
            return

        records_to_insert = []

        # 2. Iterate through the grid
        for zone in zones:
            hex_id = zone['hex_id']
            zone_name = zone.get('zone_name', 'Unknown')
            
            try:
                # Get the precise GPS center of the H3 Hexagon (Using H3 v4 syntax)
                lat, lon = h3.cell_to_latlng(hex_id)
            except Exception as e:
                print(f"{Colors.WARNING}⚠️ Invalid hex_id {hex_id}: {e}{Colors.ENDC}")
                continue
            
            try:
                forecast_data = self.get_hex_forecast(lat, lon)
                
                # Extract the very first 3-hour block (This is our "Current" operational weather)
                current_block = forecast_data.get('list', [])[0]

                # Parse Actuarial Weather Data
                pop = current_block.get('pop', 0.0)
                temp = current_block.get('main', {}).get('temp', 0.0)
                weather_code = current_block.get('weather', [{}])[0].get('id', 800)
                
                # Convert OpenWeather's m/s to km/h for the database
                wind_speed_ms = current_block.get('wind', {}).get('speed', 0.0)
                wind_speed_kmh = round(wind_speed_ms * 3.6, 2)
                
                # Rain volume (OpenWeather returns this under 'rain' -> '3h')
                rain_vol = current_block.get('rain', {}).get('3h', 0.0)
                
                forecast_ts = datetime.fromtimestamp(current_block.get('dt'), timezone.utc).isoformat()
                unique_telemetry_id = f"WT-{uuid.uuid4().hex[:6].upper()}"
                # Build the Database Row (Notice: 'created_at' is completely removed)
                records_to_insert.append({
                    "telemetry_id": unique_telemetry_id,
                    "Rain": rain_vol,
                    "hex_id": hex_id,
                    "forecast_timestamp": forecast_ts,
                    "pop": pop,
                    "wind_speed_kmh": wind_speed_kmh,
                    "weather_code": weather_code,
                    "temp": temp
                })
                
                print(f"   {Colors.BLUE}🌤️  Processed {zone_name} | PoP: {pop*100}% | Temp: {temp}°C{Colors.ENDC}")

            except Exception as e:
                print(f"{Colors.FAIL}❌ Failed to fetch/parse weather for {hex_id}: {e}{Colors.ENDC}")

        # 3. Batch Insert into the Supabase 'Weather' Table
        if records_to_insert:
            print(f"\n{Colors.WARNING}💾 Executing batch insert of {len(records_to_insert)} telemetry records into Supabase...{Colors.ENDC}")
            self.supabase.table("Weather").insert(records_to_insert).execute()
            print(f"{Colors.GREEN}{Colors.BOLD}✅ Weather grid synchronized successfully!{Colors.ENDC}\n")

if __name__ == "__main__":
    cron = WeatherCronJob()
    cron.run_15min_sync()
