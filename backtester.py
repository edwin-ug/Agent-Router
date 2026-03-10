import requests
import pandas as pd
import time
from datetime import datetime

def fetch_binance_data(symbol="ETHUSDT", interval="5m", start_str="2025-01-01", end_str="2025-02-01"):
    print(f"Downloading Binance {interval} data from {start_str} to {end_str}...")
    
    start_ts = int(datetime.strptime(start_str, "%Y-%m-%d").timestamp() * 1000)
    end_ts = int(datetime.strptime(end_str, "%Y-%m-%d").timestamp() * 1000)
    
    all_klines = []
    limit = 1000 
    
    while start_ts < end_ts:
        url = f"https://api.binance.com/api/v3/klines?symbol={symbol}&interval={interval}&startTime={start_ts}&endTime={end_ts}&limit={limit}"
        response = requests.get(url)
        data = response.json()
        
        if not data or type(data) is dict:
            break
            
        all_klines.extend(data)
        start_ts = data[-1][0] + 1 
        time.sleep(0.1) 
        
    if not all_klines:
        return pd.DataFrame()

    df = pd.DataFrame(all_klines, columns=[
        'timestamp', 'Open', 'High', 'Low', 'Close', 'Volume', 
        'close_time', 'qav', 'num_trades', 'taker_base_vol', 'taker_quote_vol', 'ignore'
    ])
    
    df['timestamp'] = pd.to_datetime(df['timestamp'], unit='ms')
    df.set_index('timestamp', inplace=True)
    
    for col in ['Open', 'High', 'Low', 'Close', 'Volume']:
        df[col] = df[col].astype(float)
        
    print(f"Successfully downloaded {len(df)} candles!")
    return df

def run_backtest(start_date, end_date, interval="5m", rsi_buy_threshold=35.0, rsi_sell_threshold=65.0):
    
    data = fetch_binance_data(symbol="ETHUSDT", interval=interval, start_str=start_date, end_str=end_date)
    
    if data.empty:
        print("Failed to download data.")
        return

    print("Calculating Indicators (EMA 50, EMA 200, RSI & ATR Volatility)...")
    data['EMA_50'] = data['Close'].ewm(span=50, adjust=False).mean()
    data['EMA_200'] = data['Close'].ewm(span=200, adjust=False).mean() 
    
    # Wilder's RSI Calculation
    delta = data['Close'].diff()
    gain = (delta.where(delta > 0, 0))
    loss = (-delta.where(delta < 0, 0))
    avg_gain = gain.ewm(alpha=1/14, min_periods=14, adjust=False).mean()
    avg_loss = loss.ewm(alpha=1/14, min_periods=14, adjust=False).mean()
    rs = avg_gain / (avg_loss + 1e-9) 
    data['RSI'] = 100 - (100 / (1 + rs))

    # ATR (Average True Range)
    high_low = data['High'] - data['Low']
    high_close = (data['High'] - data['Close'].shift()).abs()
    low_close = (data['Low'] - data['Close'].shift()).abs()
    true_range = pd.concat([high_low, high_close, low_close], axis=1).max(axis=1)
    data['ATR'] = true_range.rolling(14).mean()
    
    data = data.dropna()

    print("Starting Simulation...\n")
    print("-" * 50)
    
    in_position = False
    position_type = None 
    entry_price = 0.0
    stop_loss = 0.0
    take_profit = 0.0
    
    wins = 0
    losses = 0
    total_trades = 0
    profit_dollar = 0.0 
    
    cooldown_timer = 0 

    for index, row in data.iterrows():
        current_price = row['Close']
        ema_50 = row['EMA_50']
        ema_200 = row['EMA_200']
        rsi = row['RSI']
        atr = row['ATR']
        time_str = index.strftime("%Y-%b-%d %H:%M")

        if in_position:
            if position_type == "BUY":
                if row['Low'] <= stop_loss:
                    print(f"🔴 [{time_str}] LOSS: Stopped out of LONG at ${stop_loss:.2f}")
                    losses += 1
                    profit_dollar -= 1000 * (abs(entry_price - stop_loss) / entry_price)
                    in_position = False
                    cooldown_timer = 12 # WAIT 60 MINS AFTER A LOSS
                elif row['High'] >= take_profit:
                    print(f"🟢 [{time_str}] WIN: Take profit hit on LONG at ${take_profit:.2f}")
                    wins += 1
                    profit_dollar += 1000 * (abs(take_profit - entry_price) / entry_price)
                    in_position = False
                    cooldown_timer = 3 # WAIT 15 MINS AFTER A WIN
            
            elif position_type == "SELL":
                if row['High'] >= stop_loss:
                    print(f"🔴 [{time_str}] LOSS: Stopped out of SHORT at ${stop_loss:.2f}")
                    losses += 1
                    profit_dollar -= 1000 * (abs(stop_loss - entry_price) / entry_price)
                    in_position = False
                    cooldown_timer = 12 # WAIT 60 MINS AFTER A LOSS
                elif row['Low'] <= take_profit:
                    print(f"🟢 [{time_str}] WIN: Take profit hit on SHORT at ${take_profit:.2f}")
                    wins += 1
                    profit_dollar += 1000 * (abs(entry_price - take_profit) / entry_price)
                    in_position = False
                    cooldown_timer = 3 # WAIT 15 MINS AFTER A WIN
            
            continue 

        if cooldown_timer > 0:
            cooldown_timer -= 1
            continue

        if ema_50 > ema_200:
            trend = "Strong_Uptrend"
        elif ema_50 < ema_200:
            trend = "Strong_Downtrend"
        else:
            trend = "Choppy"
        
        # BUY SIGNAL
        if trend == "Strong_Uptrend" and rsi < rsi_buy_threshold:
            in_position = True
            position_type = "BUY"
            entry_price = current_price
            
            stop_loss = current_price - (atr * 4.0)   
            take_profit = current_price + (atr * 6.0) 
            
            total_trades += 1
            print(f"🔵 [{time_str}] OPEN LONG @ ${entry_price:.2f} (RSI: {rsi:.2f}, ATR: {atr:.2f})")
            
        # SELL SIGNAL
        elif trend == "Strong_Downtrend" and rsi > rsi_sell_threshold:
            in_position = True
            position_type = "SELL"
            entry_price = current_price
            
            stop_loss = current_price + (atr * 4.0)   
            take_profit = current_price - (atr * 6.0) 
            
            total_trades += 1
            print(f"🟣 [{time_str}] OPEN SHORT @ ${entry_price:.2f} (RSI: {rsi:.2f}, ATR: {atr:.2f})")

    print("-" * 50)
    print(f"BACKTEST RESULTS ({start_date} to {end_date} | {interval} Chart)")
    print("-" * 50)
    print(f"Total Trades Taken : {total_trades}")
    print(f"Winning Trades     : {wins}")
    print(f"Losing Trades      : {losses}")
    
    if (wins + losses) > 0:
        win_rate = (wins / (wins + losses)) * 100
        print(f"Win Rate           : {win_rate:.2f}%")
        print(f"Est. Net P&L       : ${profit_dollar:.2f} (Assuming $1k per trade)")
    else:
        print("Win Rate           : N/A (No closed trades)")
        
    if in_position:
        print(f"\nNote: 1 trade is currently still OPEN ({position_type} @ ${entry_price:.2f})")

if __name__ == "__main__":
    # Testing February 2025 with highly optimized settings
    run_backtest(
        start_date="2025-02-01", 
        end_date="2025-02-28", 
        interval="5m",           
        rsi_buy_threshold=35.0,
        rsi_sell_threshold=65.0
    )