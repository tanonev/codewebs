'''
Created on Mar 12, 2013

@author: chrispiech
'''

from matplotlib.pyplot import show

import hcluster 
import numpy
from numpy.random import rand

class Runner():
    
    
    def drawDendrogram(self, dist):
        Z = hcluster.linkage(dist)
        hcluster.dendrogram(Z)
        show()
    
    def run(self):
        print 'hello world'
        features = self.getRandomFeatures()
        dist = hcluster.pdist(features)
        print len(dist)
        
        self.drawDendrogram(dist)

Runner().run()