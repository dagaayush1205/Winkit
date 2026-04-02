import os
import sys
from datetime import datetime, timedelta, timezone
# Adjust path to import from root directory
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
        """Fetches disruption events logged in the last 60 minutes using explicit UTC."""
        # 1. Get the current time, explicitly locked to UTC
        now_utc = datetime.now(timezone.utc)
        
        # 2. Subtract 1 hour and format it with the timezone flag (+00:00)
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

    def get_worker_latest_hex(self, worker_id: str):
        """Finds exactly where the worker is right now."""
        response = self.supabase.table("raw_gps_telemetry") \
            .select("h3_hex_id") \
            .eq("worker_id", worker_id) \
            .order("timestamp", desc=True) \
            .limit(1) \
            .execute()
            
        if response.data:
            return response.data[0].get("h3_hex_id")
        return None

    def check_claim_exists(self, claim_id: str) -> bool:
        """Prevents double-paying a worker for the same event."""
        response = self.supabase.table("claims_and_payouts") \
            .select("claim_id") \
            .eq("claim_id", claim_id) \
            .execute()
        return len(response.data) > 0

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

        claims_generated = 0

        # Loop through every recent disaster
        for event in events:
            event_id = event['event_id']
            danger_hex = event['hex_id']
            event_type = event['event_type']
            
            print(f"\n{Colors.CYAN}--- Processing Event: {event_id} ({event_type} in {danger_hex}) ---{Colors.ENDC}")

            # Loop through everyone with insurance
            for policy in active_policies:
                worker_id = policy['worker_id']
                policy_id = policy['policy_id']
                payout_amt = policy.get('max_daily_coverage', 500.0)

                # Check if the worker is actually in the danger zone
                worker_hex = self.get_worker_latest_hex(worker_id)
                
                if worker_hex == danger_hex:
                    # Generate an Idempotent Claim ID (e.g., CLM-ZEP1001-EVT1234)
                    # We take the last 8 chars of the event_id to keep it clean
                    claim_id = f"CLM-{worker_id}-{event_id[-8:]}"
                    
                    if self.check_claim_exists(claim_id):
                        print(f"   {Colors.BLUE}⏭️  Claim already exists for {worker_id}. Skipping.{Colors.ENDC}")
                        continue
                        
                    print(f"   {Colors.WARNING}🚨 HIT! {worker_id} is in the danger zone. Generating ESCROW claim...{Colors.ENDC}")
                    
                    # 💥 THE SMART CONTRACT EXECUTION 💥
                    self.supabase.table("claims_and_payouts").insert({
                        "claim_id": claim_id,
                        "worker_id": worker_id,
                        "policy_id": policy_id,
                        "event_id": event_id,
                        "payout_amt": payout_amt,
                        "status": "ESCROW",
                        "fraud_flags_triggered": 0
                    }).execute()
                    
                    claims_generated += 1
                    print(f"   {Colors.GREEN}✅ Claim {claim_id} locked in ESCROW.{Colors.ENDC}")
                else:
                    # Worker is safe, no payout needed
                    pass 

        print(f"\n{Colors.BOLD}🏁 Sweep Complete. Generated {claims_generated} new ESCROW claims.{Colors.ENDC}")

if __name__ == "__main__":
    worker = ClaimTriggerWorker()
    worker.process_triggers()
