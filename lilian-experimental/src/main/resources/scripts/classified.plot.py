import os
print os.environ['PATH']

import matplotlib.pyplot as p
import numpy as n

for name in ['reduced-ifs', 'reduced-pca']:
    data = n.loadtxt('../csv/data.'+name+'.csv', delimiter=',')
    
    fig = p.figure()
    ax = fig.add_subplot(111)
    
    sc = ax.scatter(data[:, 0], data[:, 1], c=data[:, 2], cmap='jet', alpha=0.2)

    ax.set_aspect(1)
    
    p.savefig('out.'+name+'.png')
    p.savefig('out.'+name+'.svg')
