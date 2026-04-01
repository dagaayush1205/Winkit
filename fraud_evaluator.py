import os
import statistics
import json
from supabase import create_client, Client
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

def evaluate_payload(payload: dict) -> tuple[bool, str]:
    
    # Hardware check
    if payload.get("dev_settings_enabled") is True or payload.get("is_mock_location") is True:
        return True, "Hardware Flag: Mock Location or Dev Settings Enabled"

    # verifying the os_signature_valid and play_integrity_pass flags to detect rooted devices. If either of these checks fails, it indicates that the device's security integrity has been compromised, which is a strong indicator of potential fraud or manipulation.
    if payload.get("os_signature_valid") is False or payload.get("play_integrity_pass") is False:
        return True, "Security Flag: OS Signature/Play Integrity Failed (Rooted Device)"

    # IMU-Inertial Measurement Unit check - if the device is moving at a certain speed but the IMU variance is extremely low, it could indicate that the location is being spoofed without actual movement (teleportation).
    speed = payload.get("speed_kmh") or 0.0
    imu = payload.get("imu_variance")
    
    if speed > 5.0 and imu is not None and imu < 0.1:
        return True, f"Physics Anomaly: Moving at {speed}km/h but IMU variance is {imu} (Teleportation)"

    # Space Sensors (GNSS)
    gnss_data = payload.get("raw_gnss_data")
    if not gnss_data:
        # If speed > 0 but there is NO satellite data, that's highly suspicious (software bot)
        if speed > 0:
            return True, "GNSS Anomaly: Missing Satellite Data while moving."
        else:
            # Could just be indoor scanning, we pass it but it might be flagged later
            pass 
    elif isinstance(gnss_data, list):
        snr_values = [sat.get("snr", 0) for sat in gnss_data if "snr" in sat]
        if len(snr_values) > 1:
            snr_var = statistics.variance(snr_values)
            if snr_var < 1.0:
                return True, f"GNSS Anomaly: SNR variance mathematically impossible ({snr_var:.2f})."

    # Passed all checks
    return False, "CLEAN: Hardware, Physics, and Sensors Verified."

def process_pending_verifications():
    print(f"\n{Colors.CYAN}{Colors.BOLD}🔍 SWEEPING FOR PENDING VERIFICATIONS...{Colors.ENDC}")
    
    # Fetch PENDING_VERIFICATION
    response = supabase.table("raw_gps_telemetry").select("*").eq("fraud_reason", "PENDING_VERIFICATION").execute()
    
    pending_rows = response.data
    
    if not pending_rows:
        print(f"{Colors.GREEN}└─ ✅ No pending records found. Database is fully verified.{Colors.ENDC}\n")
        return

    print(f"{Colors.BLUE}├─ Found {len(pending_rows)} records to evaluate.{Colors.ENDC}\n")

    # 2. Evaluate and Update each row
    for row in pending_rows:
        ping_id = row['ping_id']
        worker_id = row['worker_id']
        
        # Run evaluation
        is_fraud, reason = evaluate_payload(row)
        
        # Print status
        if is_fraud:
            print(f"{Colors.FAIL}   [PING {ping_id} | {worker_id}] ❌ FRAUD: {reason}{Colors.ENDC}")
        else:
            print(f"{Colors.GREEN}   [PING {ping_id} | {worker_id}] ✅ CLEAN{Colors.ENDC}")
            
        # Write to Supabase
        update_res = supabase.table("raw_gps_telemetry").update({
            "is_flagged_fraud": is_fraud,
            "fraud_reason": reason
        }).eq("ping_id", ping_id).execute()
        
        if not update_res.data:
            print(f"{Colors.WARNING}   ⚠️ Failed to update Ping ID {ping_id} in database.{Colors.ENDC}")

    print(f"\n{Colors.CYAN}{Colors.BOLD}🏁 BATCH VERIFICATION COMPLETE.{Colors.ENDC}\n")

if __name__ == "__main__":
    process_pending_verifications()
