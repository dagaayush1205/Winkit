# comms_engine/test_send.py
from bots import WinkitBotEngine
import os
from dotenv import load_dotenv

load_dotenv()

def test_heartbeat():
    bot = WinkitBotEngine()
    
    # REPLACEME: Put your verified personal phone number here (with +91)
    MY_PHONE = "+91 9538132358" 
    
    print(f"🚀 Sending Phase 3 Heartbeat to {MY_PHONE}...")
    
    try:
        bot.send_whatsapp(
            to_phone=MY_PHONE,
            body="🟢 *WinkIT System Online*\n\nComms Engine is officially 'Soaring'. Your H3 Spatial Oracle and Payout Engine are now connected via WhatsApp."
        )
        print("✅ Message sent! Check your WhatsApp.")
    except Exception as e:
        print(f"❌ Error: {e}")

if __name__ == "__main__":
    test_heartbeat()
