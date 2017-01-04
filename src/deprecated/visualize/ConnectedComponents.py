'''
Created on Mar 12, 2013

@author: chrispiech
'''
import os
import numpy
import matplotlib
from pylab import *
import networkx as nx
from scipy.sparse import coo_matrix
from scipy.sparse import lil_matrix
from scipy.io import mmread, mmwrite
import itertools

from FileSystem import FileSystem

FULL_MATRIX_ZIP = 'ast_1_3.gz'
FULL_MATRIX = 'sparse10.mat'
SMALL_MATRIX = 'ast_1_1.txt.sparse10.mtx'
TEST_MATRIX = 'test.txt'

class Runner(object):
    
    def createGraph(self, distanceMatrix):
        graph = nx.Graph()
        cx = coo_matrix(distanceMatrix)  
        for i,j,v in zip(cx.row, cx.col, cx.data):
            if v > 0:
                graph.add_edge(i, j, weight=v)
        return graph
            
    
    def filterBySimilarity(self, graph, maxDistance):
        filteredGraph = nx.Graph()
        for node in graph.nodes():
            filteredGraph.add_node(node)
        for edgeTuple in graph.edges():
            start = edgeTuple[0]
            end = edgeTuple[1]
            weight = graph.edge[start][end]['weight']
            if weight <= maxDistance and weight > 0:
                attrDict = {'weight': weight}
                filteredGraph.add_edge(start, end, attrDict)
        return filteredGraph
    
    def graphConnectedComponentsVsCutoff(self):
        print 'graph connected components'
        distanceMatrix = FileSystem.loadDistanceMatrix(FULL_MATRIX)
        graph = self.createGraph(distanceMatrix)
        for i in range(11, -1, -1):
            filteredGraph = self.filterBySimilarity(graph, i)
            components = nx.number_connected_components(filteredGraph)
            print str(i) + '\t' + str(components)
            
    def getAverageDegree(self, graph):
        degrees = graph.degree()
        return numpy.mean(degrees.values())
    
    def getComponentSizes(self, components, submissionIdMap):
        sizes = []
        for component in components:
            size = 0
            for node in component.nodes():
                numAsts = len(submissionIdMap[node])
                size += numAsts
            sizes.append(size)
        return sorted(sizes, reverse=True)
            
    def getStats(self):
        print 'graph stats'
        distanceMatrix = FileSystem.loadDistanceMatrix('ast_1_3.sparse10.mat')
        submissionIdMap = FileSystem.loadSubmissionIdMap('ast_1_3')
        graph = self.createGraph(distanceMatrix)
        for i in range(11):
            filteredGraph = self.filterBySimilarity(graph, i)
            components = nx.connected_component_subgraphs(filteredGraph)
            componentSizes = self.getComponentSizes(components, submissionIdMap)
            
            numComponents = len(components)
            degree = self.getAverageDegree(filteredGraph)
            edges = nx.number_of_edges(filteredGraph)
            toPrint = []
            toPrint.append(i)
            toPrint.append(numComponents)
            toPrint.append(degree)
            toPrint.append(edges)
            toPrint.append(componentSizes[0])
            toPrint.append(componentSizes[1])
            toPrint.append(componentSizes[2])
            string = ''
            for elem in toPrint:
                string += str(elem) + '\t'
            print string
        
    def run(self):
        self.getStats()
        print'done'
    
if __name__ == '__main__': 
    Runner().run() 