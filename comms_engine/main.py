import logging
from fastapi import FastAPI, Form, Response
from bots import WinkitBotEngine

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(title="WinkIT Comms Engine")
bot = WinkitBotEngine()


@app.post("/whatsapp")
async def whatsapp_webhook(From: str = Form(...), Body: str = Form(...)):
    """
    Twilio calls this endpoint when someone sends a WhatsApp message to your number.

    IMPORTANT:
    - Return TwiML XML directly — this is how Twilio reads the reply.
    - Do NOT call bot.send_whatsapp() here; that creates a separate outbound
      message and causes double-sends. Use send_whatsapp() only for proactive
      notifications outside of a webhook context.
    """
    reply_text = bot.process_whatsapp_input(From, Body)
    twiml = bot.build_whatsapp_reply(reply_text)
    return Response(content=twiml, media_type="text/xml")


@app.post("/voice/incoming")
async def voice_incoming():
    """
    Twilio calls this when someone dials your number.
    Returns the welcome IVR TwiML.
    """
    twiml = bot.generate_welcome_ivr()
    return Response(content=twiml, media_type="text/xml")


@app.post("/voice/menu-handler")
async def voice_menu(Digits: str = Form(...), From: str = Form(...)):
    """
    Twilio posts here after the caller presses a digit in the IVR menu.
    'From' comes in as '+91XXXXXXXXXX' from Twilio.
    """
    twiml = bot.generate_menu_response(Digits, From)
    return Response(content=twiml, media_type="text/xml")


@app.get("/health")
async def health():
    """Simple health check endpoint."""
    return {"status": "ok"}
