import os
print os.environ['PATH']

import matplotlib.pyplot as p
import numpy as n

data = n.loadtxt('../csv/data.table.csv', delimiter=',')

fig = p.figure()
ax = fig.add_subplot(111)

# sc = ax.scatter(data[:, 0], data[:, 1])
eb = ax.errorbar(data[:, 0], data[:, 2], data[:, 1], data[:, 3], ecolor='black', capsize=0, marker='None', linestyle='None', zorder=10)
sc = ax.scatter(data[:, 0], data[:, 2], c=data[:, 4], cmap='RdBu_r', zorder=100, lw=1, s=20, edgecolor='black')
p.colorbar(sc)

xmax = max(data[:, 0] + data[:, 1]);
ymax = max(data[:, 2] + data[:, 3]);

mn = min(xmax, ymax)

ax.plot( [0, mn], [0, mn], color='gray')


p.xlim([0, xmax])
p.ylim([0, ymax])

ax.set_aspect(1)


p.xlabel('model dimension')
p.ylabel('data dimension')

p.savefig('out.table.png')
p.savefig('out.table.svg')