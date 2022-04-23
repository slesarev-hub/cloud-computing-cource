import matplotlib.pyplot as plt
import pandas as pd
from datetime import datetime
import sys

prices = pd.read_csv("candle.csv")

plt.figure()

width = .4
width2 = .05

up = prices[prices.close>=prices.open]
down = prices[prices.close<prices.open]

col1 = 'green'
col2 = 'red'

plt.bar(up.index,up.close-up.open,width,bottom=up.open,color=col1)
plt.bar(up.index,up.high-up.close,width2,bottom=up.close,color=col1)
plt.bar(up.index,up.low-up.open,width2,bottom=up.open,color=col1)

plt.bar(down.index,down.close-down.open,width,bottom=down.open,color=col2)
plt.bar(down.index,down.high-down.open,width2,bottom=down.open,color=col2)
plt.bar(down.index,down.low-down.close,width2,bottom=down.close,color=col2)

plt.title(sys.argv[1])
plt.xticks([i for i in range(len(prices))],[datetime.fromtimestamp(i).strftime("%B %d") for i in prices.date])

plt.savefig('plot.png')