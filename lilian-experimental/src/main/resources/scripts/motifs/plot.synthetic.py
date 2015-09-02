# -*- coding: utf-8 -*-

import matplotlib as mpl
from _socket import NI_DGRAM
mpl.use('Agg')
import matplotlib.pyplot as p
import numpy as n
import pylab
import scipy.stats as stats
import networkx as nwx
import glob
from __builtin__ import file
from matplotlib.pyplot import margins
import os.path
import json

margin = 0.05
extra = 0.05

row1height = 0.6
row2height = 0.2
row3height = 0.2

# To be run in the workspace dir of the UCompareBeta module
barwidth = 0.3
pluswidth = 0.45

# Load experiment metadata
with open('metadata.json') as mdfile:
    metadata = json.load(mdfile)

sub_index = metadata["subindex"]
nums_instances = metadata["nums instances"]
directed = False

ni = len(nums_instances)

# Load the frequencies and factors
frequencies = n.genfromtxt('frequencies.csv', delimiter=',')
(nummotifs, width) = frequencies.shape
factors = n.genfromtxt('factors.csv', delimiter=',')
means = n.genfromtxt('means.csv', delimiter=',')

runs = width/ni

freqMeans = means[:,0:ni]
factMeans = means[:,ni:2*ni]

fig = p.figure(figsize=(16,9))

### 1) Plot the factors
ax1 = fig.add_axes([0.0 + margin + extra, row3height + row2height + margin, 1.0 - 2.0 * margin- extra, row1height - 2.0 * margin]); 

ind = n.arange(nummotifs)

bw = barwidth/ni
for i in range(ni):
    # the means as bars
    ax1.bar(ind - barwidth/2.0 + i * bw, factMeans[:, i], bw, color='k', zorder=1)
for i in range(ni):
    # the data as scatter
    for s in range(nummotifs):
        ax1.scatter((ind[s] - barwidth/2.0 + (i+0.5) * bw) * n.ones(runs), factors[s,i*runs:(i+1)*runs], s=5, color='r', zorder=3)
    
ax1.set_xlim([0 - pluswidth, nummotifs - 1 + pluswidth])

ax1.hlines(0, - pluswidth, nummotifs - 1 + pluswidth)

yloc = p.MaxNLocator(7)
ax1.get_yaxis().set_major_locator(yloc)

ax1.get_yaxis().set_tick_params(which='both', direction='out')

ax1.spines["right"].set_visible(False)
ax1.spines["top"].set_visible(False)
ax1.spines["bottom"].set_visible(False)
ax1.spines["left"].set_visible(False)

ax1.get_xaxis().set_tick_params(which='both', top='off', bottom='off', labelbottom='off')
ax1.get_yaxis().set_tick_params(which='both', left='off', right='off')

# top = n.max(factor)
# if n.min(factor) < - top and top > 0:
#   ax1.set_ylim(bottom=-top)
   
# negative grid (white lines over the bars)   
ticks = ax1.get_yaxis().get_majorticklocs()   
ticks = n.delete(ticks, n.where(n.logical_and(ticks < 0.00001, ticks > -0.00001)))
ax1.hlines(ticks, - pluswidth, nummotifs - 1 + pluswidth, color='w', zorder=2)

ax1.legend()
ax1.set_ylabel('factor (bits)')

### 2) Plot the motifs

bottom = margin
height = row2height - margin

side = pluswidth - 0.5
width = (1.0 - 2.0 * margin - extra) / (nummotifs + 2.0 * side)

i = 0
for path in glob.glob('motif.*.edgelist')[:nummotifs]:
    axsmall = fig.add_axes([margin + extra + side*width + width * i, bottom, width, height])
    axsmall.axis('off')
    
    graph = nwx.read_edgelist(path,create_using=(nwx.DiGraph() if directed else nwx.Graph()))
    
    pos = nwx.spring_layout(graph)
    nodes = nwx.draw_networkx_nodes(graph, pos, ax=axsmall, node_size=12)
    if nodes != None:
        nodes.set_edgecolor('red')
        nodes.set_color('red')
    edges = nwx.draw_networkx_edges(graph, pos, alpha=0 if directed else 1)
    if directed: # draw proper arrows
        for s in edges.get_segments():
            x = s[0, 0]
            y = s[0, 1]
            xto = s[1, 0]
            yto = s[1, 1]
            dx = xto - x
            dy = yto - y
            color = 'r' if (i == sub_index) else 'k' 
            p.arrow(x, y, dx, dy, head_width=0.05, head_length=0.1, length_includes_head=True, fc=color, ec=color)
    i = i + 1
    
### 3)  Frequency graph

ax3 = fig.add_axes([0.0 + margin + extra, row2height + margin, 1.0 - 2.0 * margin - extra, row3height - margin]) 

# ax3.bar(ind - barwidth/2.0, freq, barwidth, color='k')
for i in range(ni):
    # the means as bars
    ax3.bar(ind - barwidth/2.0 + i * bw, freqMeans[:, i], bw, color='k', zorder=1)
# the data as scatter
for i in range(ni):
    for s in range(nummotifs):
        ax3.scatter((ind[s] - barwidth/2.0 + (i+0.5) * bw) * n.ones(runs), frequencies[s,i*runs:(i+1)*runs], s=5, color='r', zorder=3)

ax3.get_yaxis().set_tick_params(which='both', direction='out')

ax3.set_xlim([0 - pluswidth, nummotifs - 1 + pluswidth])

# reduce the number of ticks
yloc = p.MaxNLocator(4)
ax3.yaxis.set_major_locator(yloc)

ax3.spines["right"].set_visible(False)
ax3.spines["top"].set_visible(False)
ax3.spines["left"].set_visible(False)

ax3.get_xaxis().tick_bottom()
ax3.get_xaxis().set_tick_params(which='both', top='off', bottom='off', right='off', labelbottom='off')
ax3.get_yaxis().set_tick_params(which='both', left='off', right='off')

ax3.set_ylim([0, ax3.get_ylim()[1]])

ticks = ax3.get_yaxis().get_majorticklocs()   
ticks = n.delete(ticks, n.where(n.logical_and(ticks < 0.00001, ticks > -0.00001)))
ax3.hlines(ticks, - pluswidth, nummotifs - 1 + pluswidth, color='w', zorder=2)

ax3.set_ylabel('freq.')

p.savefig('synthetic.plot.png')
p.savefig('synthetic.plot.pdf')
