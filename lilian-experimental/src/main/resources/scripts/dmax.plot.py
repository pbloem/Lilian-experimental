import os
print os.environ['PATH']

import matplotlib.pyplot as p
import numpy as n

data = n.loadtxt('../csv/data.result.csv', delimiter=',')

runs = data.shape[1] -  1
print data.shape

max = max(max(data[:, 0]), max(data[:, 1]))

fig = p.figure()
ax = fig.add_subplot(111)

sc = ax.scatter(data[:, 0], data[:, 1], c=data[:, 2], cmap='jet')
ax.plot( [0, max], [0, max], color='gray')

ax.set_aspect(1)
ax.set_xlim(0, max)
ax.set_ylim(0, max)

fig.colorbar(sc)

p.savefig('out.png')
p.savefig('out.svg')
