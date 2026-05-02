import os
import logging
from twilio.rest import Client
from twilio.twiml.voice_response import VoiceResponse, Gather
from twilio.twiml.messaging_response import MessagingResponse
from supabase import create_client
from dotenv import load_dotenv

load_dotenv()
logger = logging.getLogger(__name__)


class WinkitBotEngine:
    def __init__(self):
        self.client = Client(
            os.getenv("TWILIO_ACCOUNT_SID"),
            os.getenv("TWILIO_AUTH_TOKEN")
        )
        self.wa_num = os.getenv("TWILIO_WHATSAPP_NUMBER")   # e.g. +14155238886
        self.base_url = os.getenv("BASE_URL", "").rstrip("/")  # e.g. https://yourapp.ngrok.io
        self.supabase = create_client(
            os.getenv("SUPABASE_URL"),
            os.getenv("SUPABASE_KEY")
        )

    # ------------------------------------------------------------------
    # PHONE NORMALIZATION
    # ------------------------------------------------------------------

    def normalize_phone(self, raw: str) -> str:
        """
        Strips 'whatsapp:' prefix and country code, returning a 10-digit number.
        Matches the format stored in the Workers table (e.g. 9538132358).

        Input examples:
          'whatsapp:+919538132358'  -> '9538132358'
          '+919538132358'           -> '9538132358'
          '9538132358'              -> '9538132358'
        """
        phone = raw.replace("whatsapp:", "").strip()
        if phone.startswith("+91"):
            phone = phone[3:]
        elif phone.startswith("91") and len(phone) == 12:
            phone = phone[2:]
        return phone[-10:]

    # ------------------------------------------------------------------
    # WHATSAPP
    # ------------------------------------------------------------------

    def send_whatsapp(self, to_phone: str, body: str):
        """
        Proactively send an outbound WhatsApp message (e.g. scheduled alerts).
        Do NOT call this inside a webhook — use build_whatsapp_reply() there.
        to_phone should be a 10-digit number; country code is added here.
        """
        if not body or not body.strip():
            body = "🤖 WinkIT: System is processing your request. Please try again."

        self.client.messages.create(
            from_=f"whatsapp:{self.wa_num}",
            body=body,
            to=f"whatsapp:+91{to_phone}"
        )

    def build_whatsapp_reply(self, reply_text: str) -> str:
        """
        Build a TwiML <Message> XML response for replying inside a webhook.
        This is the correct and efficient way to reply to an incoming WhatsApp message.
        """
        if not reply_text or not reply_text.strip():
            reply_text = "🤖 WinkIT: Could not process your request. Please try again."
        resp = MessagingResponse()
        resp.message(reply_text)
        return str(resp)

    def process_whatsapp_input(self, phone: str, text: str) -> str:
        cmd = text.strip().lower()
        clean_phone = self.normalize_phone(phone)

        # --- 1. Global Worker Lookup (SAFE VERSION) ---
        try:
            # Removed upi_id to prevent the 400 error
            worker_res = (
                self.supabase.table("Workers")
                .select("worker_id, name, trust_score") 
                .eq("phone", clean_phone)
                .execute()
            )
        except Exception as e:
            logger.error(f"[WhatsApp] Workers lookup failed: {e}")
            return "⚠️ Database sync error. Please try again later."

        if not worker_res.data:
            return f"❌ Number *{clean_phone}* not recognized. Register on the app first!"

        worker = worker_res.data[0]
        worker_id = worker["worker_id"]

        # --- 2. Route Commands ---

        # WALLET (Handling missing upi_id gracefully)
# COMMAND: WALLET (Handling Enum mismatch gracefully)
        if cmd == "wallet":
            try:
                # Changed "PAID" to "APPROVED" to match your DB Enum!
                res = self.supabase.table("claims_and_payouts").select("payout_amt").eq("worker_id", worker_id).eq("status", "APPROVED").execute()
                
                total = sum(row['payout_amt'] for row in res.data) if res.data else 0
                upi = worker.get("upi_id", "Not Linked ⚠️") 
                
                return f"💰 *WinkIT Wallet*\n\nTotal Earned: *₹{total}*\nLinked UPI: {upi}\n\n_Funds are settled instantly via Razorpay._"
                
            except Exception as e:
                logger.error(f"[WhatsApp] Wallet fetch failed: {e}")
                return "⚠️ Could not load wallet balance. Please ensure your claims are processed."        # TRUST
        
        elif cmd == "trust":
            # Using .get() prevents KeyError if trust_score is null
            score = worker.get("trust_score", 0) or 0 
            level = "💎 Elite" if score > 80 else "🛠️ Rising"
            return f"🌟 *Trust Score: {score}/100*\nLevel: {level}\n\n_Keep your app active to boost your reliability._"

        # STATUS
        elif cmd == "status":
            try:
                res = (
                    self.supabase.table("claims_and_payouts")
                    .select("claim_id, status, payout_amt")
                    .eq("worker_id", worker_id)
                    .order("created_at", desc=True)
                    .limit(1)
                    .execute()
                )
            except Exception as e:
                logger.error(f"[WhatsApp] claims_and_payouts lookup failed for {worker_id}: {e}")
                return "⚠️ Could not fetch your claims right now. Please try again later."
 
            if res.data:
                c = res.data[0]
                return (
                    "📋 *Latest Claim*\n"
                    f"Status: {c.get('status', 'N/A')}\n"
                    f"Amount: ₹{c.get('payout_amt', 0)}\n"
                    f"Ref: {c.get('claim_id', 'Processing')}"
                )
            return "❌ No claims found in your record yet."

        # PAY (Keep using manual_claims since we know it works)
        elif "pay" in cmd:
            try:
                manual_entry = {
                    "worker_id": worker_id,
                    "latitude": 12.9815, 
                    "longitude": 80.2230,
                    "hazard_type": "Manual Report",
                    "status": "PENDING_REVIEW"
                }
                self.supabase.table("manual_claims").insert(manual_entry).execute()
                return "✅ *Manual Claim Filed*\nOur engine is auditing your H3 cell for disruptions."
            except Exception as e:
                return "⚠️ Failed to file claim. Try again via App."

        # DEFAULT MENU
        else:
            return (
                f"🤖 *WinkIT Assistant: Hello {worker['name']}!*\n\n"
                "Try these commands:\n"
                "• *STATUS* – Latest payout\n"
                "• *WALLET* – Total earnings\n"
                "• *TRUST* – Reliability score\n"
                "• *PAY* – File a claim"
            )
    # ------------------------------------------------------------------
    # VOICE (IVR) - POLISHED
    # ------------------------------------------------------------------

    def generate_welcome_ivr(self) -> str:
        response = VoiceResponse()

        # Twilio needs the absolute URL to know where to send the digits
        menu_url = f"{self.base_url}/voice/menu-handler"
        
        gather = Gather(
            num_digits=1,
            action=menu_url,
            method="POST",
            timeout=5
        )
        # Using a slightly more 'welcoming' script for the demo
        gather.say(
            "Namaste. Welcome to the Wink IT automated assistant. "
            "To check your latest claim status, press 1. "
            "To hear your current trust score, press 2. ",
            voice="Polly.Aditi",
            language="en-IN"
        )
        response.append(gather)

        # If they sit in silence for 5 seconds
        response.say("We did not receive an input. Thank you for using Wink IT. Goodbye.")
        return str(response)

    def generate_menu_response(self, digit: str, phone: str) -> str:
        response = VoiceResponse()
        clean_phone = self.normalize_phone(phone)

        # 1. Look up worker
        try:
            worker_res = (
                self.supabase.table("Workers")
                .select("worker_id, name, trust_score") 
                .eq("phone", clean_phone)
                .execute()
            )
        except Exception as e:
            logger.error(f"[Voice] DB Worker Lookup Error: {e}")
            response.say("I am having trouble accessing the database. Please try again later.", voice="Polly.Aditi", language="en-IN")
            return str(response)

        if not worker_res.data:
            response.say("Account not recognized. Please register on the app.", voice="Polly.Aditi", language="en-IN")
            return str(response)

        worker = worker_res.data[0]
        worker_id = worker["worker_id"]
        worker_name = worker.get("name", "User")

        # 2. Route Digit
        if digit == "1":
            try:
                res = (
                    self.supabase.table("claims_and_payouts")
                    .select("status, payout_amt")
                    .eq("worker_id", worker_id)
                    .order("created_at", desc=True)
                    .limit(1)
                    .execute()
                )
                
                if res.data:
                    db_status = res.data[0].get("status", "PENDING")
                    amt = res.data[0].get("payout_amt", 0)
                    
                    # Human-friendly mapping
                    status_map = {
                        "AUTO_PAID": f"has been settled instantly. A payout of {amt} rupees is in your wallet.",
                        "APPROVED": f"is approved. Your payout of {amt} rupees is on the way.",
                        "ESCROW": "is in escrow and being dripped to your wallet hourly.",
                        "REJECTED": "was rejected by the Fraud Fortress due to a telemetry mismatch."
                    }
                    
                    speech_status = status_map.get(db_status, f"is currently {db_status}.")
                    response.say(f"Hello {worker_name}. Your latest claim {speech_status}", voice="Polly.Aditi", language="en-IN")
                else:
                    response.say(f"Hello {worker_name}. You have no recent claims found.", voice="Polly.Aditi", language="en-IN")
            
            except Exception as e:
                # 🔥 THIS IS THE KEY: Look at your terminal if this fails!
                logger.error(f"[Voice] Claim Lookup Error for {worker_id}: {e}")
                response.say("I encountered an error fetching your claim status. Please try again.", voice="Polly.Aditi", language="en-IN")

        elif digit == "2":
            score = worker.get("trust_score", 0)
            response.say(f"Your Wink IT trust score is {score} out of 100. Keep up the good work!", voice="Polly.Aditi", language="en-IN")

        else:
            response.say("That is an invalid option.", voice="Polly.Aditi", language="en-IN")

        # Final goodbye
        response.say("Stay safe on the roads. Goodbye.", voice="Polly.Aditi", language="en-IN")
        return str(response)
