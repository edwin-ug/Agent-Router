import requests
import json
import time
import yfinance as yf
import pandas as pd
from openai import OpenAI

# LOCAL MODEL SETUP (Ollama)
local_client = OpenAI(
    base_url="http://localhost:11434/v1",
    api_key="ollama" 
)

# BACKUP CLOUD SETUP (Groq)
groq_client = OpenAI(
    api_key="groq-api-key-placeholder",
    base_url="https://api.groq.com/openai/v1"
)

ROUTER_URL = "http://localhost:8080/api/v1/intents/submit"
http_session = requests.Session()

# MEMORY: Keep track of the last trade to prevent spamming the same price
last_sent_price = 0.0

def get_market_data():
    """Pulls live ETH data and pre-computes strict math signals, including Dynamic ATR."""
    try:
        eth = yf.Ticker("ETH-USD")
        data = eth.history(period="5d", interval="5m") 
        
        if data.empty or len(data) < 200:
            raise ValueError("Yahoo Finance returned empty data or insufficient history.")
        
        # Calculate 50 EMA & 200 EMA for Trend Detection
        data['EMA_50'] = data['Close'].ewm(span=50, adjust=False).mean()
        data['EMA_200'] = data['Close'].ewm(span=200, adjust=False).mean()
        
        # Calculate RSI using Wilder's Smoothing
        delta = data['Close'].diff()
        gain = (delta.where(delta > 0, 0))
        loss = (-delta.where(delta < 0, 0))
        
        avg_gain = gain.ewm(alpha=1/14, min_periods=14, adjust=False).mean()
        avg_loss = loss.ewm(alpha=1/14, min_periods=14, adjust=False).mean()
        rs = avg_gain / (avg_loss + 1e-9) 
        data['RSI'] = 100 - (100 / (1 + rs))

        # Calculate ATR (Average True Range) for Volatility
        high_low = data['High'] - data['Low']
        high_close = (data['High'] - data['Close'].shift()).abs()
        low_close = (data['Low'] - data['Close'].shift()).abs()
        true_range = pd.concat([high_low, high_close, low_close], axis=1).max(axis=1)
        data['ATR'] = true_range.rolling(14).mean()

        # Drop NA rows so we have clean latest data
        data = data.dropna()
        
        current_price = round(float(data['Close'].iloc[-1]), 2)
        ema_50 = round(float(data['EMA_50'].iloc[-1]), 2)
        ema_200 = round(float(data['EMA_200'].iloc[-1]), 2)
        rsi = round(float(data['RSI'].iloc[-1]), 2)
        atr = round(float(data['ATR'].iloc[-1]), 2)
        
        # MACRO TREND LOGIC
        if ema_50 > ema_200:
            trend = "Strong_Uptrend"
        elif ema_50 < ema_200:
            trend = "Strong_Downtrend"
        else:
            trend = "Choppy"
        
        action_signal = "HOLD"
        dynamic_stop_loss = 0.0

        if trend == "Strong_Uptrend" and rsi < 45.00:
            action_signal = "BUY"
            dynamic_stop_loss = current_price - (atr * 4.0) # 4x ATR Stop Loss
        elif trend == "Strong_Downtrend" and rsi > 55.00:
            action_signal = "SELL"
            dynamic_stop_loss = current_price + (atr * 4.0) # 4x ATR Stop Loss
            
        return {
            'asset': 'ETH/USD',
            'current_price': current_price,
            'ema_50': ema_50,
            'ema_200': ema_200,
            'trend': trend,
            'rsi': rsi,
            'atr': atr,
            'math_signal': action_signal,
            'dynamic_stop_loss': round(dynamic_stop_loss, 2)
        }
    except Exception as e:
        print(f"Market Data Error: {e}")
        return None

def call_llm_for_decision(market_data):
    """The AI acts as the Risk Officer, packaging Python's math signal into JSON."""
    print(f"🤖 AI is analyzing: {market_data}")
    
    system_prompt = f"""
    You are a Senior Quantitative Analyst. Analyze the data and return JSON ONLY.

    ### THE STRATEGY SIGNAL:
    The quantitative engine has already calculated the strict mathematical signal: {market_data['math_signal']}

    YOUR JOB:
    1. If the math_signal is "BUY" or "SELL", validate it and output the intent with 95 confidence.
    2. If the math_signal is "HOLD", output "HOLD" with 0 confidence.

    OUTPUT FORMAT:
    {{
      "action": "{market_data['math_signal']}",
      "amount": 1000.0,
      "confidence": int,
      "priceAtEntry": {market_data['current_price']},
      "stopLoss": {market_data['dynamic_stop_loss'] if market_data['math_signal'] != 'HOLD' else 0.0},
      "assetPair": "ETH/USD",
      "reasoning": "Explain the {market_data['trend']}, RSI of {market_data['rsi']}, and ATR volatility."
    }}
    """

    try:
        response = local_client.chat.completions.create(
            model="llama3.2",
            messages=[{"role": "system", "content": system_prompt}],
            temperature=0.0,
            response_format={"type": "json_object"}
        )
        return json.loads(response.choices[0].message.content.strip())
    except Exception:
        # Fallback to smart cloud model if local logic is failing
        try:
            response = groq_client.chat.completions.create(
                model="llama-3.3-70b-versatile",
                messages=[{"role": "system", "content": system_prompt}],
                temperature=0.0,
                response_format={"type": "json_object"}
            )
            return json.loads(response.choices[0].message.content.strip())
        except Exception:
            return {"action": "HOLD", "confidence": 0, "reasoning": "AI Offline"}

def send_intent_to_router(trade_intent):
    """Sends the AI's decision to Spring Boot."""
    global last_sent_price
    current_price = trade_intent.get('priceAtEntry', 0.0)
    
    # SPAM PREVENTION: Don't send if price hasn't moved much since last signal
    if abs(current_price - last_sent_price) < 5.0:
        print(f" Skipping duplicate intent. Price ${current_price} is too close to last signal.")
        return

    print(f"Sending Intent: {trade_intent.get('action')} @ ${current_price} (Stop Loss: ${trade_intent.get('stopLoss')})")
    try:
        response = http_session.post(ROUTER_URL, json=trade_intent, timeout=5)
        if response.status_code == 200:
            last_sent_price = current_price
            print(f"🛡️ Router Status: {response.json().get('riskStatus')}")
        else:
            print(f"Server Error: {response.status_code}")
    except Exception as e:
        print(f"Could not reach Spring Boot: {e}")

if __name__ == "__main__":
    print("Starting Trading Agent")
    print("-" * 50)
    
    try:
        while True:
            data = get_market_data()
            if data:
                decision = call_llm_for_decision(data)
                
                # Only proceed if it's a high-confidence trade
                if decision.get("action") in ["BUY", "SELL"] and decision.get("confidence", 0) >= 85:
                    send_intent_to_router(decision)
                else:
                    action = decision.get('action', 'HOLD')
                    conf = decision.get('confidence', 0)
                    reason = decision.get('reasoning', 'Wait')
                    print(f"⚖️ AI Decision: {action} (Conf: {conf}%) - {reason}")
            
            print("-" * 50)
            time.sleep(30) 
            
    except KeyboardInterrupt:
        print("\nAgent gracefully shut down.")