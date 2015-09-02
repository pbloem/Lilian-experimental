# -*- coding: utf-8 -*-

import matplotlib as mpl
mpl.use('Agg')
import matplotlib.pyplot as p
import numpy as n
import pylab
import scipy.stats as stats
import networkx as nwx

fig = p.figure(figsize=(12,6))

ax1 = fig.add_subplot(121)
ax2 = fig.add_subplot(122)

graph = nwx.read_edgelist('graph.edgelist', create_using=nwx.Graph())
print len(graph.nodes())

nc = n.genfromtxt('graph-colors.csv', delimiter=',')
print len(nc)
nwx.draw_spring(graph, with_labels=False, ax=ax1, node_color=nc, node_size=20);

subbed = nwx.read_edgelist('subbed.edgelist', create_using=nwx.Graph())

sc = n.genfromtxt('subbed-colors.csv', delimiter=',')
print len(sc)
nwx.draw_spring(subbed, with_labels=False, ax=ax2, node_color=sc, node_size=20);

p.savefig('graphs.png')
