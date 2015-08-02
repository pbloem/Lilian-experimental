import matplotlib as mpl
import matplotlib.pyplot as p
import numpy as n
import pylab
import scipy.stats as stats

dir = '/Users/Peter/Experiments/platform test/samples/workspace/sampling/0/'

# Plot the samples

data = n.genfromtxt(dir+'single-sample.csv', delimiter=',')

f, (ax1, ax2) = p.subplots(2)

ax1.hist(data, 100, normed=1)
stats.probplot(data, plot=ax2)

f.savefig(dir+'single-sample.png')

# Plot the convergence

data = n.genfromtxt(dir+'convergence.csv', delimiter=',')

useEffectiveSampleSize = False;

if useEffectiveSampleSize:
    index = data[:, 1]
else:
    index = data[:, 0]

f, (ax1, ax2, ax3, ax4) = p.subplots(4, sharex=True)

width = 10

# standard
ax1.hlines(data[:, 4], index-width/2, index+width/2, color='black', linestyles='solid')
ax1.plot(index, data[:, 2], 'ko', markersize= 1, markeredgewidth = 0)
ax1.set_title('standard')

# log-normal
ax2.hlines(data[:, 5], index-width/2, index+width/2, color='black', linestyles='solid')
ax2.plot(index, data[:, 2], 'ro', markersize= 1, markeredgewidth = 0)
ax2.set_title('log-normal')

#standard
ax3.hlines(data[:, 6], index-width/2, index+width/2, color='black', linestyles='solid')
ax3.plot(index, data[:, 2], 'bo', markersize= 1, markeredgewidth = 0)
ax3.set_title('percentile')

#standard
ax4.hlines(data[:, 7], index-width/2, index+width/2, color='black', linestyles='solid')
ax4.plot(index, data[:, 2], 'go', markersize= 1, markeredgewidth = 0)
ax4.set_title('BCa')
 
p.savefig(dir+'convergence.png')