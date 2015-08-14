import matplotlib as mpl
import matplotlib.pyplot as p
import numpy as n
import pylab
import scipy.stats as stats
import networkx as nwx
import glob
from __builtin__ import file
from matplotlib.pyplot import margins


margin = 0.05
extra = 0.05

row1height = 0.6
row2height = 0.2
row3height = 0.2

# To be run in the workspace dir of the UCompareBeta module
barwidth = 0.3
pluswidth = 0.7

data = n.genfromtxt('numbers.csv', delimiter=',')
(nummotifs, numfeatures) = data.shape

clip = 15
if nummotifs > clip:
    data = data[0:clip,:]
    (nummotifs, numfeatures) = data.shape

fig = p.figure()

ax1 = fig.add_axes([0.0 + margin + extra, row3height + row2height + margin, 1.0 - 2.0 * margin- extra, row1height - 2.0 * margin]); 

### 1) Draw the estimate profits as vertical lines
ind = n.arange(nummotifs)

ax1.bar(ind - barwidth/2.0, data[:, 0], barwidth, color='k')

# horizontal ticks to show lower bounds
ax1.hlines(data[:, 1], ind - pluswidth/2.0, ind + pluswidth/2.0)

ax1.set_xlim([0 - pluswidth, nummotifs - 1 + pluswidth])

ax1.hlines(0, - pluswidth, nummotifs - 1 + pluswidth)

ax1.get_yaxis().set_tick_params(which='both', direction='out')

ax1.spines["right"].set_visible(False)
ax1.spines["top"].set_visible(False)
ax1.spines["bottom"].set_visible(False)
ax1.spines["left"].set_visible(False)

ax1.get_xaxis().set_tick_params(which='both', top='off', bottom='off', labelbottom='off')
ax1.get_yaxis().tick_left()

top = n.max(data[:, 0])
if n.min(data[:, 0]) < - top:
   ax1.set_ylim(bottom=-top)

ax1.set_ylabel('factor')

### 2) Plot the small graphs

graphs = []
bottom = margin
height = row2height - margin

side = pluswidth - 0.5
width = (1.0 - 2.0 * margin - extra) / (nummotifs + 2.0 * side)

i = 0
for path in glob.glob('motif.*.edgelist'):
    axsmall = fig.add_axes([margin + extra + side*width + width * i, bottom, width, height])
    axsmall.axis('off')
    
    graph = nwx.read_edgelist(path)
    graphs.append(graph)
    
    nwx.draw_spectral(graphs[i], ax=axsmall, node_color='k', node_size=20)
    i = i + 1


### 3)  Frequency graph

ax3 = fig.add_axes([0.0 + margin + extra, row2height + margin, 1.0 - 2.0 * margin - extra, row3height - margin]) 

ax3.bar(ind - barwidth/2.0, data[:, 2], barwidth, color='k')

ax3.get_yaxis().set_tick_params(which='both', direction='out')

ax3.set_xlim([0 - pluswidth, nummotifs - 1 + pluswidth])

ax3.spines["right"].set_visible(False)
ax3.spines["top"].set_visible(False)
ax3.spines["left"].set_visible(False)

ax3.get_xaxis().tick_bottom()
ax3.get_xaxis().set_tick_params(which='both', top='off', bottom='off', right='off', labelbottom='off')
ax3.get_yaxis().tick_left()

ax3.set_ylabel('freq.')

p.savefig('ucomp.beta.png')
