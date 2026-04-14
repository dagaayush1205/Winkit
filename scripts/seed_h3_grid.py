import h3
import os
import sys

# Adjust path if needed
current_dir = os.path.dirname(os.path.abspath(__file__))
if current_dir not in sys.path:
    sys.path.append(current_dir)

from supabase import create_client, Client
from config import SUPABASE_DB_URL, SUPABASE_API_KEY

supabase: Client = create_client(SUPABASE_DB_URL, SUPABASE_API_KEY)

# 1. The Core Anchors
landmarks = {
    "Guduvanchery": (12.8449, 80.0503),
    "Potheri / SRM": (12.8236, 80.0435),
    "Maraimalai Nagar": (12.7935, 80.0242)
}

RESOLUTION = 9
RADIUS = 2 # A radius of 2 generates 19 hexagons per anchor point

all_hexes = set()
hex_mapping = {}

print("🗺️ Generating H3 Spatial Grid for GST Corridor...")

# 2. Math: Generate the Honeycomb
for name, coords in landmarks.items():
    try:
        # Get the center hex
        center_hex = h3.latlng_to_cell(coords[0], coords[1], RESOLUTION)
        
        # Generate the surrounding cluster (Blast Radius)
        cluster = h3.grid_disk(center_hex, RADIUS)
        
        for hx in cluster:
            if hx not in all_hexes:
                all_hexes.add(hx)
                hex_mapping[hx] = name
    except Exception as e:
        print(f"⚠️ Error generating cluster for {name}: {e}")

print(f"✅ Generated {len(all_hexes)} unique, contiguous hexagons.")
print("💾 Pushing spatial data to Supabase...")

# 3. Database Write
inserted_count = 0
for hex_id, zone_name in hex_mapping.items():
    try:
        # Upsert ensures we don't duplicate existing hexes
        supabase.table("h3_zone_states").upsert({
            "hex_id": hex_id,
            "zone_name": f"{zone_name} Sector",
            "v_zone_score": 0.20, # Default healthy infrastructure
            "yesterday_water": 0.0
        }).execute()
        inserted_count += 1
    except Exception as e:
        print(f"Failed to insert {hex_id}: {e}")

print(f"🚀 Successfully seeded {inserted_count} zones! The grid is online.")
