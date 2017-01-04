'''
Created on Mar 12, 2013

@author: chrispiech
'''
import os
import numpy
from scipy.sparse import lil_matrix
from scipy.io import mmread, mmwrite

from PathImporter import PathImporter
from FileSystem import FileSystem

FULL_MATRIX_ZIP = 'ast_1_3.gz'
FULL_MATRIX = 'ast_1_3.txt'
SMALL_MATRIX = 'ast_1_1.txt'
TEST_MATRIX = 'test.txt'

CUTTOFF = 5

class Runner(object):
    
    def loadDistanceMatrix(self, fileName):
        dataDir = FileSystem.getDataDir('distanceMatrix')
        path = os.path.join(dataDir, fileName)
        print 'load matrix: ' + path
        return numpy.loadtxt(path)
    
    def getOutFilePath(self, matrixName):
        outFileDir = FileSystem.getDataDir('distanceMatrix')
        outFileName = matrixName + '.sparse' + str(CUTTOFF)
        outFilePath = os.path.join(outFileDir, outFileName)
        #return open(outFilePath, 'w')
        return outFilePath

    def saveSparseMatrix(self, matrixName):
        print 'graph connected components'
        distanceMatrix = self.loadDistanceMatrix(matrixName)
        distanceMatrix[distanceMatrix > CUTTOFF] = 0
        sparseMatrix = lil_matrix(distanceMatrix)
        outFilePath = self.getOutFilePath(matrixName)
        mmwrite(outFilePath, sparseMatrix)
        
    def run(self):
        self.saveSparseMatrix(SMALL_MATRIX)
        print'done'
    
if __name__ == '__main__': 
    Runner().run() 