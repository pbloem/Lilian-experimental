# -*- coding: utf-8 -*-

import matplotlib as mpl
from PIL.FontFile import WIDTH
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
ll = n.genfromtxt('likelihoods.csv', delimiter=',')
(num, width) = ll.shape

fig = p.figure(figsize=(4,4))
ax = p.subplot(111)
### 1) Plot the factors

ax.scatter(n.random.normal(1.0, 0.01, num), ll[:,0], s=2, linewidth=0, c='k')
ax.scatter(n.random.normal(2.0, 0.01, num), ll[:,1], s=2, linewidth=0, c='r')
ax.scatter(n.random.normal(3.0, 0.01, num), ll[:,2], s=2, linewidth=0, c='b')

x = [1, 2, 3]
labels = ['iso', 'mog', 'ifs']
p.xticks(x, labels, rotation='vertical')
p.margins(0.2)
p.subplots_adjust(bottom=0.30)

ax.spines["right"].set_visible(False)
ax.spines["top"].set_visible(False)
ax.spines["left"].set_visible(False)

yloc = p.LinearLocator(2)
ax.get_yaxis().set_major_locator(yloc)

ax.get_xaxis().set_tick_params(which='both', top='off')
ax.get_yaxis().set_tick_params(which='both', right='off')

fig.tight_layout()

p.savefig('likelihoods.png')
p.savefig('likelihoods.pdf')
