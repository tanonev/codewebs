'''
Created on Mar 19, 2013

@author: Chris
'''

from sets import Set
import random

from PathImporter import PathImporter
from FileSystem import FileSystem

NUM_ITERATIONS = 5

class KMedioids(object):
    
    def findMedioids(self, distanceMatrix):
        numItems = distanceMatrix.shape[0]
        medioids = Set([])
        clusters = Set([])
        for _ in NUM_CLUSTERS 
        
        for i in range(NUM_ITERATIONS):
            print 'iteration: ' + str(i)
            
    
    def loadMatrix(self):
        return FileSystem.loadDistanceMatrix('ast_1_3.sparse10.mat')
    
    def run(self):
        matrix = self.loadMatrix()
        self.findMedioids(matrix)
        print 'done'
        
KMedioids().run()