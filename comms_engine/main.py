from fastapi import FastAPI, Form, Response
from bots import WinkitBotEngine

app = FastAPI(title="WinkIT Comms Engine")
bot = WinkitBotEngine()

@app.post("/whatsapp")
async def whatsapp_webhook(From: str = Form(...), Body: str = Form(...)):
    """Twilio hits this when someone sends a WhatsApp to your sandbox."""
    phone = From.replace("whatsapp:", "")
    reply_text = bot.process_whatsapp_input(phone, Body)
    
    # We send the reply back using the bot helper
    bot.send_whatsapp(phone, reply_text)
    return {"status": "sent"}

@app.post("/voice/incoming")
async def voice_incoming():
    """Twilio hits this when someone calls your number."""
    twiml_content = bot.generate_welcome_ivr()
    return Response(content=twiml_content, media_type="text/xml")

@app.post("/voice/menu-handler")
async def voice_menu(Digits: str = Form(...), From: str = Form(...)):
    """Handles logic after the user interacts with the voice menu."""
    # Twilio sends 'From' as '+91XXXXXXXXXX'
    twiml_content = bot.generate_menu_response(Digits, From)
    return Response(content=twiml_content, media_type="text/xml")
