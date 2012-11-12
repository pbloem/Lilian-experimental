import os
print os.environ['PATH']

import matplotlib.pyplot as p
import numpy as n

data = n.loadtxt('../csv/data.table.csv', delimiter=',')

fig = p.figure()
ax = fig.add_subplot(111)

fit = -data[:, 4]
fit = (fit - min(fit)) * (1 / (max(fit) - min(fit)))
fit = fit * 60 + 10
#print fit

for i in range(0, 3):
    sub = data[(12*i):(12*(i+1)),:]
    
    eb = ax.errorbar(sub[:, 0], sub[:, 2], sub[:, 1], sub[:, 3], ecolor='black', capsize=0, marker='None', linestyle='None', zorder=10)
    
    marker = 'o'
    if(i is 1):
        marker = '^'
    if(i is 2):
        marker = 's'


    # score = sub[:, 2]
    score = n.argsort(sub[:, 2])
    # score = (score - min(score)) / (max(score) - min(score))

    sc = ax.scatter(sub[:, 0], sub[:, 2], c=score, cmap='RdBu_r', zorder=100, lw=1, marker=marker, s=fit, edgecolor='black')

# p.colorbar(sc)

xmax = max(data[:, 0] + data[:, 1]);
ymax = max(data[:, 2] + data[:, 3]) + 0.3;

mn = min(xmax, ymax)

ax.plot( [0, mn], [0, mn], color='gray', zorder=0)

y = 1.26
ax.plot( [0, xmax], [y, y], color='gray', zorder=1)
y = 1.7
ax.plot( [0, xmax], [y, y], color='gray', zorder=1)
y = 2.01
ax.plot( [0, xmax], [y, y], color='gray', zorder=1)



p.xlim([0, xmax])
p.ylim([0, ymax])

ax.set_aspect(1)


p.xlabel('model dimension')
p.ylabel('data dimension')

p.savefig('out.table.png')
p.savefig('out.table.svg')