import time
import requests
import os
import sys
current_dir = os.path.dirname(os.path.abspath(__file__))
root_dir = os.path.dirname(os.path.dirname(current_dir))
sys.path.append(root_dir)

# Ensure you add your Supabase keys to config.py
from config import DEMO_MODE
class WeatherAPIClient:
    def __init__(self, api_key: str, lat: float = 12.8259, lon: float = 80.0395, demo_mode=DEMO_MODE):
        self.api_key = api_key
        self.base_url = "https://api.openweathermap.org/data/2.5"
        self.lat = lat
        self.lon = lon
        self.demo_mode = demo_mode

    def get_forecast(self) -> dict:
        """Fetches the 5-day/3-hour forecast (used by the Pricing Engine)."""
        if self.demo_mode:
            print("DEMO MODE ACTIVE: Injecting synthetic forecast data for testing.")
            return self._generate_mock_forecast(simulated_pop=0.20)

        endpoint = f"{self.base_url}/forecast?lat={self.lat}&lon={self.lon}&appid={self.api_key}&units=metric"
        response = requests.get(endpoint)
        response.raise_for_status()
        return response.json()

    def get_live_weather(self) -> dict:
        """Fetches the current weather (used by the Trigger Worker)."""
        endpoint = f"{self.base_url}/weather?lat={self.lat}&lon={self.lon}&appid={self.api_key}&units=metric"
        response = requests.get(endpoint)
        response.raise_for_status()
        return response.json()
    
    def _generate_mock_forecast(self, simulated_pop: float) -> dict:
        mock_list = []
        current_time = int(time.time())
        
        # Generate 40 blocks (5 days * 8 blocks of 3-hours)
        for i in range(40):
            mock_list.append({
                "dt": current_time + (i * 10800), # Add 3 hours in seconds
                "pop": simulated_pop  # Inject your desired probability here
            })
            
        return {"list": mock_list}
