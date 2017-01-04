'''
Created on Mar 12, 2013

@author: chrispiech
'''
import os
import numpy
from scipy.spatial import distance
from matplotlib import pyplot
import networkx as nx
from scipy.sparse import coo_matrix
from FileSystem import FileSystem

FULL_MATRIX_ZIP = 'ast_1_3.gz'
FULL_MATRIX = 'ast_1_3.txt'
SMALL_MATRIX = 'ast_1_1.txt'
TEST_MATRIX = 'test.txt'

class Runner(object):
    
    def getOutFilePath(self, matrixName):
        outFileDir = FileSystem.getDataDir('gephi')
        outFileName = matrixName + '.gml'
        outFilePath = os.path.join(outFileDir, outFileName)
        return outFilePath
    
    def createGraph(self, distanceMatrix, idMap):
        graph = nx.Graph()
        cx = coo_matrix(distanceMatrix)  
        for astId in idMap:
            count = len(idMap[astId])
            graph.add_node(astId,weight=count)
        for i,j,v in zip(cx.row, cx.col, cx.data):
            if v > 0:
                edgeWeight = 1.0 / 2 ** v
                graph.add_edge(i, j, weight=edgeWeight)
        return graph
    
    def createGephi(self, cuttoff):
        print 'createGephi'
        matrixName = 'ast_1_1'
        fileName = 'ast_1_1.txt'
        matrix = FileSystem.loadDistanceMatrix(fileName)
        submissionIdMap = FileSystem.loadSubmissionIdMap('ast_1_1')
        graph = self.createGraph(matrix, submissionIdMap)
        outPath = self.getOutFilePath(matrixName + '_' + str(cuttoff))
        print 'write gephi: ' + outPath
        nx.write_gml(graph,outPath)
    
    def run(self):
        self.createGephi(5)
        print'done'
    
if __name__ == '__main__': 
    Runner().run() 