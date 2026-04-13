import os
import re
from twilio.rest import Client
from twilio.twiml.voice_response import VoiceResponse, Gather
from supabase import create_client
from dotenv import load_dotenv

load_dotenv()

class WinkitBotEngine:
    def __init__(self):
        self.client = Client(os.getenv("TWILIO_ACCOUNT_SID"), os.getenv("TWILIO_AUTH_TOKEN"))
        self.wa_num = os.getenv("TWILIO_WHATSAPP_NUMBER")
        self.supabase = create_client(os.getenv("SUPABASE_URL"), os.getenv("SUPABASE_KEY"))

    def send_whatsapp(self, to_phone: str, body: str):
        """Safety check: Don't send empty bodies to Twilio."""
        if not body or body.strip() == "":
            body = "🤖 WinkIT: System is processing your request. Please try again."
        
        self.client.messages.create(
            from_=f"whatsapp:{self.wa_num}",
            body=body,
            to=f"whatsapp:{to_phone}"
        )

    def process_whatsapp_input(self, phone: str, text: str):
        """Handles user-sent WhatsApp messages."""
        cmd = text.strip().lower()
        clean_phone = phone[-10:] # Keeps 9538132358
        
        # 1. FIND WORKER ID
        worker_res = self.supabase.table("Workers").select("worker_id").eq("phone", clean_phone).execute()
        
        if not worker_res.data:
            return f"❌ Worker not found for number {clean_phone}. Please check the WinkIT app."
        
        worker_id = worker_res.data[0]['worker_id']

        # 2. STATUS COMMAND
        if cmd == "status":
            # Note: lowercase 'claims_and_payouts' as per your sidebar
            res = self.supabase.table("claims_and_payouts") \
                .select("*") \
                .eq("worker_id", worker_id) \
                .order("created_at", desc=True) \
                .limit(1) \
                .execute()
            
            if res.data:
                c = res.data[0]
                # Match these keys to your exact DB columns
                return (f"📋 *Latest Claim Found*\n"
                        f"Status: {c.get('status', 'N/A')}\n"
                        f"Amount: ₹{c.get('payout_amt', 0)}\n"
                        f"Ref: {c.get('payout_id', 'Processing')}")
            return "❌ No claims found in your record yet."
        
        elif "pay" in cmd:
            return "✅ *Manual Claim Filed*\nOur engine is auditing the H3 cell for spatial disruptions."
        
        else:
            return "🤖 *WinkIT Assistant*\nReply with:\n- *STATUS* to check latest payout\n- *PAY* to file a manual claim"

    # --- VOICE (IVR) HELPERS ---
    def generate_welcome_ivr(self):
        response = VoiceResponse()
        response.say("Welcome to Wink IT. Press 1 for status, 2 for balance.", voice='Polly.Aditi', language='en-IN')
        gather = Gather(num_digits=1, action="/voice/menu-handler")
        response.append(gather)
        return str(response)

    def generate_menu_response(self, digit: str, phone: str):
        response = VoiceResponse()
        clean_phone = phone[-10:]
        
        # Voice also needs the Worker ID lookup!
        worker_res = self.supabase.table("Workers").select("worker_id", "trust").eq("phone", clean_phone).execute()
        
        if not worker_res.data:
            response.say("Account not recognized.", voice='Polly.Aditi', language='en-IN')
            return str(response)
            
        worker_id = worker_res.data[0]['worker_id']

        if digit == "1":
            res = self.supabase.table("claims_and_payouts").select("status").eq("worker_id", worker_id).limit(1).execute()
            status = res.data[0]['status'] if res.data else "unknown"
            response.say(f"Your claim status is {status}.", voice='Polly.Aditi', language='en-IN')
        elif digit == "2":
            trust = worker_res.data[0].get('trust', 0)
            response.say(f"Your trust score is {trust}.", voice='Polly.Aditi', language='en-IN')
        
        response.say("Stay safe. Goodbye.", voice='Polly.Aditi', language='en-IN')
        return str(response)
