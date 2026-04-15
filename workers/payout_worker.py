import os
import time
import sys
import random
from supabase import create_client, Client

root_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.append(root_dir)

from config import SUPABASE_DB_URL
from config import SUPABASE_API_KEY

supabase: Client = create_client(SUPABASE_DB_URL, SUPABASE_API_KEY)


class Colors:
    HEADER = '\033[95m'
    BLUE = '\033[94m'
    CYAN = '\033[96m'
    GREEN = '\033[92m'
    WARNING = '\033[93m'
    FAIL = '\033[91m'
    ENDC = '\033[0m'
    BOLD = '\033[1m'

class WinkitPayoutWorker:
    def __init__(self):
        print(f"{Colors.HEADER}{Colors.BOLD}💸 STARTING WINKIT PAYOUT DAEMON (Razorpay Gateway){Colors.ENDC}")
        print("Listening for ESCROW claims on the database queue...\n")

    def fetch_escrow_claims(self):
        """
        Queries the actual claims table for anything sitting in ESCROW.
        """
        response = supabase.table("claims_and_payouts").select("*").eq("status", "ESCROW").execute()
        return response.data

    def run_fraud_fortress(self, claim):
        """
        Cross-references the claim with the rider's recent GPS telemetry.
        """
        worker_id = claim['worker_id']
        print(f"{Colors.CYAN}🛡️  [FRAUD FORTRESS] Analyzing Claim {claim['claim_id']} for Worker {worker_id}...{Colors.ENDC}")

        # Fetch the most recent GPS ping for this specific worker
        gps_response = supabase.table("raw_gps_telemetry") \
            .select("*") \
            .eq("worker_id", worker_id) \
            .order("timestamp", desc=True) \
            .limit(1) \
            .execute()

        latest_ping = gps_response.data

        # If no GPS data is available, leave as ESCROW
        if not latest_ping:
            print(f"   {Colors.WARNING}⚠️ PENDING: No GPS telemetry found for {worker_id}. Leaving in ESCROW.{Colors.ENDC}")
            return "NO_DATA"

        telemetry = latest_ping[0]

        # If the background Auditor flagged this rider as fraud, reject the claim
        if telemetry.get("is_flagged_fraud"):
            reason = telemetry.get("fraud_reason", "Unknown Spoofer")
            print(f"   {Colors.FAIL}❌ REJECTED: Telemetry flagged -> {reason}{Colors.ENDC}")
            return "REJECTED"
            
        # If PENDING_VERIFICATION, we wait for the cron job to finish its math
        if telemetry.get("fraud_reason") == "PENDING_VERIFICATION":
            print(f"   {Colors.WARNING}⚠️ PENDING: Telemetry math still running. Leaving in ESCROW.{Colors.ENDC}")
            return "NO_DATA"

        print(f"   {Colors.GREEN}✅ PASSED: Telemetry clean. Physics verified.{Colors.ENDC}")
        return "APPROVED"

    def execute_razorpay_transfer(self, claim):
        """
        Placeholder API integration with Razorpay Route / UPI.
        """
        print(f"\n{Colors.BLUE}🏦 [BANKING] Initiating Razorpay UPI Transfer...{Colors.ENDC}")
        time.sleep(1.5) # Simulating network request to Razorpay
        
        # Simulated Razorpay API Response (In a real app, wrap this in a try/except block)
        payout_success = True 
        
        if payout_success:
            upi_txn_id = f"pay_{random.randint(1000000, 9999999)}"
            print(f"   {Colors.GREEN}{Colors.BOLD}💸 SUCCESS: ₹{claim['payout_amt']} credited to {claim['worker_id']}{Colors.ENDC}")
            print(f"   {Colors.GREEN}├─ Gateway: Razorpay UPI{Colors.ENDC}")
            print(f"   {Colors.GREEN}└─ UTR Ref: {upi_txn_id}{Colors.ENDC}")
            return "AUTO_PAID"
        else:
            print(f"   {Colors.FAIL}❌ BANK ERROR: Razorpay API timeout. Leaving in ESCROW.{Colors.ENDC}")
            return "PAYOUT_FAILED"

    def update_claim_status(self, claim_id, new_status):
        """
        Updates the claim in the database.
        """
        supabase.table("claims_and_payouts").update({"status": new_status}).eq("claim_id", claim_id).execute()

    def process_queue(self):
        # for cron job
        try:
            claims = self.fetch_escrow_claims()
            
            if not claims:
                print(f"{Colors.GREEN}✅ No pending claims in ESCROW. Database is settled.{Colors.ENDC}\n")
                return 
            
            for claim in claims:
                print("-" * 60)
                print(f"📥 New Claim detected: {claim['claim_id']} | ₹{claim['payout_amt']}")
                
                decision = self.run_fraud_fortress(claim)
                
                if decision == "APPROVED":
                    # Proceed to Payout
                    final_status = self.execute_razorpay_transfer(claim)
                    if final_status == "AUTO_PAID":
                        self.update_claim_status(claim['claim_id'], final_status)
                        print(f"\n{Colors.HEADER}System updated database claim status to: {final_status}{Colors.ENDC}")
                    else:
                        # Payout failed, leave in ESCROW
                        print(f"\n{Colors.WARNING}System leaving claim status as: ESCROW{Colors.ENDC}")
                        
                elif decision == "REJECTED":
                    # Fraud detected, update database to REJECTED
                    self.update_claim_status(claim['claim_id'], "REJECTED")
                    print(f"\n{Colors.FAIL}System updated database claim status to: REJECTED{Colors.ENDC}")
                    
                elif decision == "NO_DATA":
                    # Missing GPS or pending audit, leave in ESCROW
                    print(f"\n{Colors.WARNING}System leaving claim status as: ESCROW{Colors.ENDC}")
                
                print("-" * 60)
                
        except Exception as e:
            print(f"\n{Colors.FAIL}🛑 Fatal Error processing claims: {e}{Colors.ENDC}")
            sys.exit(1)

if __name__ == "__main__":
    worker = WinkitPayoutWorker()
    worker.process_queue()
