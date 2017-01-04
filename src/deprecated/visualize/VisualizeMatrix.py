'''
Created on Mar 12, 2013

@author: chrispiech
'''

import os
import numpy
from numpy.random import rand
from matplotlib import  pyplot
from FileSystem import FileSystem

FILE_NAME = 'ast_1_3.txt'
MAX_ROWS = 10

class Runner():
    
    def visualizeMatrix(self, matrix):
        print 'visualizing matrix'
        print matrix.shape
        print matrix
        pyplot.pcolor(matrix)
        pyplot.show()
    
    def run(self):
        toShow = FileSystem.getMatrix('simple.txt')
        self.visualizeMatrix(toShow)
        
Runner().run()