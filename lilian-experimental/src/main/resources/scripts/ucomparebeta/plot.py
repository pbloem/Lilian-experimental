import matplotlib as mpl
import matplotlib.pyplot as p
import numpy as n
import pylab
import scipy.stats as stats

# To be run in the workspace dir of the UCompareBeta module
barwidth = 0.3
pluswidth = 0.7

data = n.genfromtxt('numbers.csv', delimiter=',')
(nummotifs, numfeatures) = data.shape

f, (ax1, ax2) = p.subplots(2, sharex=True)

# draw the estimate profits as vertical lines
ind = n.arange(nummotifs)
ax1.bar(ind - barwidth/2.0, data[:, 0], barwidth, color='k')

# horizontal ticks to show lower bounds
ax1.hlines(data[:, 1], ind - pluswidth/2.0, ind + pluswidth/2.0)

ax1.set_xlim([0 - pluswidth, nummotifs + pluswidth])

ax1.hlines(0, - pluswidth, nummotifs - 1 + pluswidth)

ax1.get_yaxis().set_tick_params(which='both', direction='out')

ax1.spines["right"].set_visible(False)
ax1.spines["top"].set_visible(False)
ax1.spines["bottom"].set_visible(False)

ax1.get_xaxis().set_tick_params(which='both', top='off', bottom='off')
ax1.get_yaxis().tick_left()

ax1.set_ylabel('factor')

# bars for frequency

ax2.bar(ind - barwidth/2.0, data[:, 1], barwidth, color='k')
ax2.get_yaxis().set_tick_params(which='both', direction='out')

ax2.hlines(0, - pluswidth, nummotifs - 1 + pluswidth)


ax2.spines["right"].set_visible(False)
ax2.spines["top"].set_visible(False)
ax2.spines["bottom"].set_visible(False)

ax2.get_xaxis().tick_bottom()
ax2.get_yaxis().tick_left()
ax2.get_xaxis().set_tick_params(which='both', top='off', bottom='off')
ax2.get_yaxis().tick_left()

ax2.set_ylabel('frequency of motif')
ax2.set_xlabel('rank (by factor)')



p.savefig('ucomp.beta.png')
