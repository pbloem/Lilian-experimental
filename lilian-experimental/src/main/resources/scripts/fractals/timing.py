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

def clean(ax):
    ax.spines["right"].set_visible(False)
    ax.spines["top"].set_visible(False)
    ax.spines["left"].set_visible(False)

    # yloc = p.LinearLocator(2)
    # ax.get_yaxis().set_major_locator(yloc)

    ax.get_xaxis().set_tick_params(which='both', top='off')
    ax.get_yaxis().set_tick_params(which='both', right='off')
    
def clip_off(e):
    for b in e[1]:
        b.set_clip_on(False)
    for b in e[2]:
        b.set_clip_on(False)


# Load the likelihoods
size = n.genfromtxt('size.csv', delimiter=',')
(sizeNum, width) = size.shape

dimension = n.genfromtxt('dimension.csv', delimiter=',')
(dimNum, width) = dimension.shape

depth = n.genfromtxt('depth.csv', delimiter=',')
(depthNum, width) = depth.shape


fig = p.figure(figsize=(12,4))
ax1 = fig.add_subplot(131)
ax2 = fig.add_subplot(132)
ax3 = fig.add_subplot(133)

### 1) Plot the factors

exp = ax1.errorbar(size[:, 0], size[:,1], yerr= 1.96 * size[:, 2], capsize=0)
max = ax1.errorbar(size[:, 0], size[:,3], yerr= 1.96 * size[:, 4], capsize=0)
clip_off(exp)
clip_off(max)
ax1.set_xlabel('data size')
ax1.set_ylabel('time (s)')


exp = ax2.errorbar(dimension[:, 0], dimension[:,1], yerr= 1.96 * dimension[:, 2])
max = ax2.errorbar(dimension[:, 0], dimension[:,3], yerr= 1.96 *  dimension[:, 4])
clip_off(exp)
clip_off(max)
ax2.set_xlabel('dimension')
ax2.set_ylabel('time (s)')


exp = ax3.errorbar(depth[:, 0], depth[:,1], yerr= 1.96 * depth[:, 2], capsize=0)
max = ax3.errorbar(depth[:, 0], depth[:,3], yerr= 1.96 * depth[:, 4], capsize=0)
clip_off(exp)
clip_off(max)
ax3.set_xlabel('maximum depth')
ax3.set_ylabel('time (s)')

exp.set_label('expectation')
max.set_label('maximization')

ax3.legend(bbox_to_anchor=(0.7, 1.0), frameon=False, fontsize='medium')

clean(ax1)
clean(ax2)
clean(ax3)

#p.margins(0.2)
#p.subplots_adjust(bottom=0.15)

fig.tight_layout()

p.savefig('likelihoods.png')
p.savefig('likelihoods.pdf')
