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
    # VOICE (IVR)
    # ------------------------------------------------------------------

    def generate_welcome_ivr(self) -> str:
        """
        TwiML for the initial greeting when someone calls.
        Gathers a single digit and posts it to /voice/menu-handler.
        """
        response = VoiceResponse()

        gather = Gather(
            num_digits=1,
            action=f"{self.base_url}/voice/menu-handler",  # Must be absolute URL
            method="POST",
            timeout=5
        )
        gather.say(
            "Welcome to Wink IT. "
            "Press 1 for your latest claim status. "
            "Press 2 for your trust score.",
            voice="Polly.Aditi",
            language="en-IN"
        )
        response.append(gather)

        # Fallback if caller doesn't press anything
        response.say(
            "We did not receive any input. Please call again. Goodbye.",
            voice="Polly.Aditi",
            language="en-IN"
        )
        return str(response)

    def generate_menu_response(self, digit: str, phone: str) -> str:
        """
        TwiML response after the caller presses a digit.
        """
        response = VoiceResponse()
        clean_phone = self.normalize_phone(phone)

        # --- Look up worker ---
        try:
            worker_res = (
                self.supabase.table("Workers")
                .select("worker_id, trust_score")   # FIX: was 'trust', correct column is 'trust_score'
                .eq("phone", clean_phone)
                .execute()
            )
        except Exception as e:
            logger.error(f"[Voice] Workers lookup failed for {clean_phone}: {e}")
            response.say(
                "A system error occurred. Please try again later.",
                voice="Polly.Aditi", language="en-IN"
            )
            return str(response)

        if not worker_res.data:
            response.say(
                "Account not recognised. Please register on the WinkIT app.",
                voice="Polly.Aditi", language="en-IN"
            )
            return str(response)

        worker_id = worker_res.data[0]["worker_id"]

        # --- Route digit ---
        if digit == "1":
            try:
                res = (
                    self.supabase.table("claims_and_payouts")
                    .select("status")
                    .eq("worker_id", worker_id)
                    .order("created_at", desc=True)
                    .limit(1)
                    .execute()
                )
                status = res.data[0]["status"] if res.data else "not available"
            except Exception as e:
                logger.error(f"[Voice] claims_and_payouts lookup failed for {worker_id}: {e}")
                status = "unavailable due to a system error"

            response.say(
                f"Your latest claim status is {status}.",
                voice="Polly.Aditi", language="en-IN"
            )

        elif digit == "2":
            trust_score = worker_res.data[0].get("trust_score", 0)  # FIX: correct column name
            response.say(
                f"Your trust score is {trust_score}.",
                voice="Polly.Aditi", language="en-IN"
            )

        else:
            response.say(
                "Invalid option. Please call again and press 1 or 2.",
                voice="Polly.Aditi", language="en-IN"
            )

        response.say("Stay safe. Goodbye.", voice="Polly.Aditi", language="en-IN")
        return str(response)
