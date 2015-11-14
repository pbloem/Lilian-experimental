# -*- coding: utf-8 -*-

import matplotlib as mpl
mpl.use('Agg')
import matplotlib.pyplot as p
import numpy as n
import pylab
import scipy.stats as stats
from __builtin__ import file
from matplotlib.pyplot import margins
import os.path
import json

# Load the likelihoods
likelihoods = n.genfromtxt('likelihoods.csv', delimiter=',')

fig = p.figure(figsize=(4,4))
ax = p.subplot(111)
### 1) Plot the factors

n, bins, patches = ax.hist(likelihoods, 15, histtype='stepfilled')
p.setp(patches, 'facecolor', 'b', 'linewidth', 0)

ax.spines["right"].set_visible(False)
ax.spines["top"].set_visible(False)
ax.spines["left"].set_visible(False)

xloc = p.LinearLocator(2)
ax.get_xaxis().set_major_locator(xloc)
yloc = p.LinearLocator(2)
ax.get_yaxis().set_major_locator(yloc)


ax.get_xaxis().set_tick_params(which='both', top='off')
ax.get_yaxis().set_tick_params(which='both', right='off')

p.savefig('likelihoods.png')
p.savefig('likelihoods.pdf')
