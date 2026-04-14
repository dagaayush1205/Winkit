import numpy as np
import pandas as pd
from sklearn.ensemble import IsolationForest
import joblib
import os

class TelemetryFraudModel:
    def __init__(self):
        self.model_path = "engine/ml_fraud/isolation_forest.pkl"
        self.model = None
        
        print("\n[🛠️ DEBUG] Initializing ML Engine...")
        if os.path.exists(self.model_path):
            self.model = joblib.load(self.model_path)
            print("  ├── 🟢 Model loaded successfully from disk.")
        else:
            print("  ├── 🟡 No model found. Triggering Cold-Start Training...")
            self._train_synthetic_model()

    def _train_synthetic_model(self):
        print("\n[🧠 DEBUG] Generating Synthetic Training Data...")
        
        normal_data = pd.DataFrame({
            'gps_speed_kmh': np.random.normal(40, 15, 1000), 
            'accel_variance': np.random.normal(2.5, 0.5, 1000), 
            'gyro_variance': np.random.normal(1.0, 0.2, 1000)   
        })
        print(f"  ├── Generated {len(normal_data)} rows of 'Normal' riding data.")
        
        spoof_data = pd.DataFrame({
            'gps_speed_kmh': np.random.normal(45, 5, 50),     
            'accel_variance': np.random.normal(0.01, 0.005, 50), 
            'gyro_variance': np.random.normal(0.01, 0.005, 50)   
        })
        print(f"  ├── Generated {len(spoof_data)} rows of 'Spoofed' (Zero Physics) data.")
        
        X_train = pd.concat([normal_data, spoof_data])
        
        print("\n[⚙️ DEBUG] Fitting Isolation Forest...")
        self.model = IsolationForest(n_estimators=100, contamination=0.05, random_state=42)
        self.model.fit(X_train)
        
        os.makedirs(os.path.dirname(self.model_path), exist_ok=True)
        joblib.dump(self.model, self.model_path)
        print("  ├── ✅ Model trained and saved to disk!")

    def evaluate_telemetry(self, speed, accel_var, gyro_var):
        print("\n[🔍 DEBUG] Evaluating New Telemetry Burst:")
        print(f"  ├── Inputs -> Speed: {speed}km/h | Accel: {accel_var} | Gyro: {gyro_var}")
        
        features = pd.DataFrame([[speed, accel_var, gyro_var]], 
                              columns=['gps_speed_kmh', 'accel_variance', 'gyro_variance'])
        
        prediction = self.model.predict(features)[0]
        raw_score = self.model.decision_function(features)[0]
        
        print(f"  ├── Raw Math -> Prediction: {prediction} (-1 is Fraud, 1 is Normal) | Raw Score: {raw_score:.4f}")
        
        fraud_probability = round((1 - (raw_score + 0.5)) * 100, 2)
        fraud_probability = max(0, min(100, fraud_probability)) 
        
        is_fraud = bool(prediction == -1)
        
        reason = "Normal physics telemetry."
        if is_fraud and accel_var < 0.5 and speed > 10:
            reason = "PHYSICS MISMATCH: High GPS speed detected with near-zero physical vibration."

        print(f"  ├── 🎯 Final Output -> Fraud: {is_fraud} | Confidence: {fraud_probability}%")
        return {
            "is_fraud": is_fraud,
            "fraud_probability_percent": fraud_probability,
            "reason": reason
        }

if __name__ == "__main__":
    print("\n" + "="*60)
    print("🚀 ASTROBUGS ML SANDBOX - ISOLATION TEST")
    print("="*60)

    tester = TelemetryFraudModel()

    print("\n" + "-"*60)
    print("🧪 TEST CASE 1: The 'Good' Gig Worker (Bumpy Road, Moving)")
    good_ride = tester.evaluate_telemetry(speed=42.5, accel_var=2.3, gyro_var=1.1)

    print("\n" + "-"*60)
    print("🧪 TEST CASE 2: The 'Spoofer' (Sitting on a couch, fake GPS)")
    spoofed_ride = tester.evaluate_telemetry(speed=60.0, accel_var=0.02, gyro_var=0.01)
    
    print("\n" + "="*60)
    print("✅ SANDBOX TEST COMPLETE.")
    print("="*60 + "\n")
