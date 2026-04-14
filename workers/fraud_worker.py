import os
import statistics
import json
from supabase import create_client, Client
from config import SUPABASE_DB_URL
from config import SUPABASE_API_KEY
import h3

# Import our new ML Engine!
from engine.ml_fraud.anomaly_model import TelemetryFraudModel

supabase: Client = create_client(SUPABASE_DB_URL, SUPABASE_API_KEY)

# Initialize the ML Model (It will train itself on the first run)
ml_fraud_engine = TelemetryFraudModel()

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
    
    # --- LAYER 1: DETERMINISTIC HARDWARE CHECKS ---
    if payload.get("dev_settings_enabled") is True or payload.get("is_mock_location") is True:
        return True, "Hardware Flag: Mock Location or Dev Settings Enabled"

    if payload.get("os_signature_valid") is False or payload.get("play_integrity_pass") is False:
        return True, "Security Flag: OS Signature/Play Integrity Failed (Rooted Device)"

    # --- LAYER 2: ML-POWERED PHYSICS AUDIT (Replacing hardcoded IMU checks) ---
    speed = payload.get("speed_kmh") or 0.0
    # Fallbacks in case Android hasn't sent these specific fields yet
    accel_var = payload.get("imu_variance") if payload.get("imu_variance") is not None else 0.0
    gyro_var = payload.get("gyro_variance", 1.0) # Default to 1.0 (normal) if not implemented in app yet
    
    ml_result = ml_fraud_engine.evaluate_telemetry(speed, accel_var, gyro_var)
    
    if ml_result['is_fraud']:
        # We append the probability % so you can show it on Winklytics later!
        return True, f"ML Anomaly ({ml_result['fraud_probability_percent']}% Risk): {ml_result['reason']}"

    # --- LAYER 3: GNSS SENSOR CHECKS ---
    gnss_data = payload.get("raw_gnss_data")
    if not gnss_data:
        if speed > 0:
            return True, "GNSS Anomaly: Missing Satellite Data while moving."
        else:
            pass 
    elif isinstance(gnss_data, list):
        snr_values = [sat.get("snr", 0) for sat in gnss_data if "snr" in sat]
        if len(snr_values) > 1:
            snr_var = statistics.variance(snr_values)
            if snr_var < 1.0:
                return True, f"GNSS Anomaly: SNR variance mathematically impossible ({snr_var:.2f})."

    # Passed all checks - We include the ML confidence in the clean message!
    confidence = 100.0 - ml_result['fraud_probability_percent']
    return False, f"CLEAN: Hardware, Sensors & ML Verified ({confidence}% Confidence)."

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
        lat = row.get('latitude')
        lng = row.get('longitude')

        hex_id = None
        if lat is not None and lng is not None:
            try:
                # Using H3 v4 syntax to get the cell ID
                hex_id = h3.latlng_to_cell(lat, lng, 9)
            except Exception as e:
                print(f"{Colors.WARNING}   ⚠️ Invalid coordinates for Ping {ping_id}: {e}{Colors.ENDC}")
                
        # Run ML + Deterministic evaluation
        is_fraud, reason = evaluate_payload(row)
        
        # Print status
        if is_fraud:
            print(f"{Colors.FAIL}   [PING {ping_id} | {worker_id}] ❌ FRAUD: {reason}{Colors.ENDC}")
        else:
            print(f"{Colors.GREEN}   [PING {ping_id} | {worker_id}] ✅ {reason}{Colors.ENDC}")
            
        update_payload = {
            "is_flagged_fraud": is_fraud,
            "fraud_reason": reason
        }
        if hex_id:
            update_payload["h3_hex_id"] = hex_id

        # Write to Supabase
        update_res = supabase.table("raw_gps_telemetry").update(update_payload).eq("ping_id", ping_id).execute()
        
        if not update_res.data:
            print(f"{Colors.WARNING}   ⚠️ Failed to update Ping ID {ping_id} in database.{Colors.ENDC}")

    print(f"\n{Colors.CYAN}{Colors.BOLD}🏁 BATCH VERIFICATION COMPLETE.{Colors.ENDC}\n")

if __name__ == "__main__":
    process_pending_verifications()
