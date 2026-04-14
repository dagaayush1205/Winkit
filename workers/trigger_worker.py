import os
import sys
from datetime import datetime, timedelta, timezone
from collections import defaultdict
import h3

current_dir = os.path.dirname(os.path.abspath(__file__))
root_dir = current_dir if "config.py" in os.listdir(current_dir) else os.path.dirname(current_dir)
sys.path.append(root_dir)

from config import SUPABASE_DB_URL, SUPABASE_API_KEY
from supabase import create_client, Client

class Colors:
    HEADER = '\033[95m'
    BLUE = '\033[94m'
    CYAN = '\033[96m'
    GREEN = '\033[92m'
    WARNING = '\033[93m'
    FAIL = '\033[91m'
    ENDC = '\033[0m'
    BOLD = '\033[1m'

class ClaimTriggerWorker:
    def __init__(self):
        print(f"{Colors.HEADER}{Colors.BOLD}⚡ STARTING WINKIT SMART CONTRACT EXECUTOR{Colors.ENDC}")
        self.supabase: Client = create_client(SUPABASE_DB_URL, SUPABASE_API_KEY)

    def fetch_recent_disruptions(self):
        """Fetches disruption events logged in the last 60 minutes."""
        now_utc = datetime.now(timezone.utc)
        one_hour_ago = (now_utc - timedelta(hours=1)).isoformat()
        
        response = self.supabase.table("disruption_events") \
            .select("*") \
            .gte("created_at", one_hour_ago) \
            .execute()
        return response.data or []

    def fetch_active_policies(self):
        """Fetches all workers who currently have an active insurance policy."""
        response = self.supabase.table("weekly_policies") \
            .select("*") \
            .eq("status", "ACTIVE") \
            .execute()
        return response.data or []

    def bulk_fetch_latest_telemetry(self) -> dict:
        """Fetches all recent telemetry and creates an O(1) lookup map of Worker -> Latest Hex."""
        one_hour_ago = (datetime.now(timezone.utc) - timedelta(hours=1)).isoformat()
        
        # Order by ascending so the loop naturally overwrites older pings, leaving the newest in the dict
        response = self.supabase.table("raw_gps_telemetry") \
            .select("worker_id, h3_hex_id, timestamp") \
            .gte("timestamp", one_hour_ago) \
            .order("timestamp", desc=False) \
            .execute()
            
        worker_hex_map = {}
        for row in (response.data or []):
            if row.get("h3_hex_id"):
                worker_hex_map[row["worker_id"]] = row["h3_hex_id"]
                
        return worker_hex_map

    def bulk_fetch_today_payouts(self) -> dict:
        """Creates an O(1) lookup map of Worker -> Total Amount Paid Today."""
        today_start = datetime.now(timezone.utc).replace(hour=0, minute=0, second=0, microsecond=0).isoformat()
        
        response = self.supabase.table("claims_and_payouts") \
            .select("worker_id, payout_amt") \
            .gte("created_at", today_start) \
            .execute()
            
        paid_today_map = defaultdict(float)
        for row in (response.data or []):
            paid_today_map[row["worker_id"]] += float(row.get("payout_amt", 0.0))
            
        return paid_today_map

    def bulk_fetch_existing_claims(self) -> set:
        """Creates an O(1) lookup SET of claim IDs generated in the last few hours."""
        few_hours_ago = (datetime.now(timezone.utc) - timedelta(hours=3)).isoformat()
        
        response = self.supabase.table("claims_and_payouts") \
            .select("claim_id") \
            .gte("created_at", few_hours_ago) \
            .execute()
            
        return {row["claim_id"] for row in (response.data or [])}


    def process_triggers(self):
        """The main chronological sweep."""
        print("🔍 Sweeping for recent Disruption Events...")
        events = self.fetch_recent_disruptions()

        if not events:
            print(f"{Colors.GREEN}✅ No recent disruptions. The grid is stable.{Colors.ENDC}")
            return

        print(f"⚠️ Found {len(events)} recent disruption(s). Cross-referencing Active Policies...")
        active_policies = self.fetch_active_policies()

        if not active_policies:
            print(f"{Colors.WARNING}⚠️ No active policies found. No coverage to trigger.{Colors.ENDC}")
            return

        # 🚀 LOAD BULK DATA INTO MEMORY (Bye bye N+1!)
        print("📥 Caching global fleet telemetry and financial state into memory...")
        worker_hex_map = self.bulk_fetch_latest_telemetry()
        paid_today_map = self.bulk_fetch_today_payouts()
        existing_claims_set = self.bulk_fetch_existing_claims()

        claims_generated = 0
        claims_to_insert = [] # We will batch insert the new claims!

        # Loop through every recent disaster
        for event in events:
            event_id = event['event_id']
            danger_hex = event['hex_id']
            event_type = event['event_type']
            
            print(f"\n{Colors.CYAN}--- Processing Event: {event_id} ({event_type} in {danger_hex}) ---{Colors.ENDC}")

            # 💥 THE H3 UPGRADE: Create a 1-Hex Blast Radius around the danger zone
            # This returns a set containing the danger_hex AND its 6 immediate neighbors
            try:
                blast_radius_hexes = h3.grid_disk(danger_hex, 1)
            except Exception as e:
                print(f"{Colors.FAIL}❌ Invalid Event Hex {danger_hex}: {e}{Colors.ENDC}")
                continue

            # Loop through everyone with insurance
            for policy in active_policies:
                worker_id = policy['worker_id']
                policy_id = policy['policy_id']
                
                # 1. Check if the worker is actually in the danger zone using O(1) Dictionary Lookup
                worker_hex = worker_hex_map.get(worker_id)
                
                if not worker_hex:
                    continue # No recent GPS data for this worker
                
                # Check if worker is ANYWHERE inside the blast radius
                if worker_hex in blast_radius_hexes:
                    
                    daily_max = float(policy.get('max_daily_coverage', 500.0))
                    hourly_payout = float(policy.get('hourly_rate', daily_max / 10.0))
                    
                    # 2. Prevent over-paying if they hit their daily cap using O(1) Dictionary Lookup
                    today_paid = paid_today_map.get(worker_id, 0.0)
                    
                    if today_paid >= daily_max:
                        print(f"   {Colors.WARNING}⏭️  {worker_id} maxed out daily coverage (₹{daily_max}). Skipping.{Colors.ENDC}")
                        continue
                        
                    actual_payout = min(hourly_payout, daily_max - today_paid)
                    
                    # 3. Check for double payments using O(1) Set Lookup
                    claim_id = f"CLM-{worker_id}-{event_id[-8:]}"
                    
                    if claim_id in existing_claims_set:
                        print(f"   {Colors.BLUE}⏭️  Claim already exists for {worker_id} this hour. Skipping.{Colors.ENDC}")
                        continue
                        
                    print(f"   {Colors.WARNING}🚨 HIT! {worker_id} in danger zone. Drip-feeding ₹{actual_payout:.2f}...{Colors.ENDC}")
                    
                    # Append to our batch insert list
                    claims_to_insert.append({
                        "claim_id": claim_id,
                        "worker_id": worker_id,
                        "policy_id": policy_id,
                        "event_id": event_id,
                        "payout_amt": round(actual_payout, 2),
                        "status": "ESCROW",
                        "fraud_flags_triggered": 0
                    })
                    
                    # Update local state to prevent duplicate processing in the same run
                    existing_claims_set.add(claim_id)
                    paid_today_map[worker_id] += actual_payout
                    claims_generated += 1

        # 💥 BATCH INSERT ALL CLAIMS AT ONCE 💥
        if claims_to_insert:
            print(f"\n{Colors.WARNING}💾 Executing batch insert of {len(claims_to_insert)} smart contract claims into ESCROW...{Colors.ENDC}")
            self.supabase.table("claims_and_payouts").insert(claims_to_insert).execute()
            
        print(f"\n{Colors.BOLD}🏁 Sweep Complete. Generated {claims_generated} new ESCROW claims.{Colors.ENDC}")

if __name__ == "__main__":
    worker = ClaimTriggerWorker()
    worker.process_triggers()
