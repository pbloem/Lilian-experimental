import matplotlib as mpl
import matplotlib.pyplot as p
import numpy as n
import pylab
import scipy.stats as stats

def plot(ax, index, means, data, width, marker, name):
    ax.hlines(data, index-width/2, index+width/2, color='black', linestyles='solid')
    ax.plot(index, means, marker, markersize= 1, markeredgewidth = 0)
    ax.hlines(regEst, 0, maxIndex, color='black', linestyles='dotted')
    ax.set_xscale('log')
    ax.set_title(name)
    
    ax.set_xlim([0, n.amax(index)])

dir = '/Users/Peter/Experiments/platform test/samples/workspace/sampling/0/'

# Plot the samples

data = n.genfromtxt(dir+'single-sample.csv', delimiter=',')

avg = n.mean(data)
std = n.std(data)

lnEst = avg + 0.5 * std
regEst = float('-inf')
for v in data:
    regEst = n.logaddexp2(regEst, v)
regEst = regEst - n.log2(len(data))


f, (ax1, ax2) = p.subplots(2)

ax1.hist(data, 100, normed=1)
ax1.set_title('samples (mean estimate: '+str(regEst)+', ln est: '+str(lnEst)+')')
stats.probplot(data, plot=ax2)

f.savefig(dir+'single-sample.png')

# Plot the convergence

data = n.genfromtxt(dir+'convergence.csv', delimiter=',')

useEffectiveSampleSize = False;

if useEffectiveSampleSize:
    index = data[:, 1]
else:
    index = data[:, 0]
    
maxIndex = n.amax(index)

f, (ax1, ax2, ax3, ax4) = p.subplots(4, sharex=True)

# ymin = min(n.amin(data[:,2]), n.amin(data[:,4:7]))
# ymax = max(n.amax(data[:,2]), n.amax(data[:,4:7]))
# 
# ax1.set_ylim([ymin, ymax])
# ax2.set_ylim([ymin, ymax])
# ax3.set_ylim([ymin, ymax])
# ax4.set_ylim([ymin, ymax])

width = 10

# standard
plot(ax1, index, data[:, 2], data[:, 4], width, 'ko', 'Standard')
plot(ax2, index, data[:, 3], data[:, 5], width, 'ro', 'Log-normal')
plot(ax3, index, data[:, 2], data[:, 6], width, 'bo', 'Percentile')
plot(ax4, index, data[:, 2], data[:, 7], width, 'go', 'BCa')

p.savefig(dir+'convergence.png')
