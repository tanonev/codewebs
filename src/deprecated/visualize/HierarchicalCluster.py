'''
Created on Mar 12, 2013

@author: chrispiech
'''

#import hcluster
from scipy.spatial import distance
from matplotlib import pyplot
import numpy
from PathImporter import PathImporter
from FileSystem import FileSystem

FULL_MATRIX = 'ast_1_3.txt'
SMALL_MATRIX = 'ast_1_1.txt'
SPARSE_MATRIX = 'ast_1_1.txt.sparse10.mtx'
TEST_MATRIX = 'test.txt'

MAX_VALUE = 15

class Runner(object):
    
    def run(self):
        matrix = FileSystem.loadDistanceMatrix('ast_1_3.sparse10.mat')
        print matrix.shape
        
        symMatrix = matrix + matrix.T
        symMatrix[symMatrix > 5] = MAX_VALUE
        symMatrix[symMatrix == 0] = MAX_VALUE
        numpy.fill_diagonal(symMatrix, 0)
        v = distance.squareform(symMatrix)
        
        Z = hcluster.linkage(v)
        hcluster.dendrogram(Z)
        pyplot.show()
        
        
Runner().run()