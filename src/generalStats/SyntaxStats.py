import sys
import os.path
sys.path.append(os.path.abspath('../../'))
sys.path.append(os.path.abspath('../../site/cwsite/'))

import numpy
import random
from sets import Set
import networkx as nx

from src.util.MLClass import MLClass
from src.util.FileSystem import FileSystem
from src.util.AstNetwork import AstNetwork
from src.util.WarningManager import WarningManager

MAX_EDIT_DISTANCE = 10
MAX_NODES_CONSIDERED = 30
HISTOGRAM_BUCKETS = 10

class SyntaxStats(object):
    
    def getDistanceMatrix(self, part):
        sparse = part != (1,1)
        return FileSystem.loadDistanceMatrix(part, False)
    
    def getSubmissionIdMap(self, part):
        return FileSystem.loadSubmissionIdMap(part)
    
    def getResultPartPath(self, statName, part):
        syntaxDir = os.path.join(FileSystem.getResultsDir(), 'syntaxStatistics')
        statDir = os.path.join(syntaxDir, statName)
        fileName = statName + '_' + str(part[0]) + '_' + str(part[1]) + '.csv'
        
        if not os.path.exists(statDir):
            os.makedirs(statDir)
        return os.path.join(statDir, fileName)
    
    def getAllParts(self):
        return [(1, 3)]
    
    def save(self, statName, astNetwork, matrix):
        path = self.getResultPartPath(statName, astNetwork.getPart())
        numpy.savetxt(path, matrix, delimiter=",")
        
    # (2) 
    def saveCoverageCount(self, astNetwork):
        print 'save coverage count'
        astCoverage = []
        for key in astNetwork.subIdMap:
            count = len(astNetwork.subIdMap[key])
            astCoverage.append([key, count])
        self.save('coverageCount', astNetwork, astCoverage)
        
    # (3) made up    
    def saveClustersVsDbSize(self, astNetwork, editDistance):
        print 'save clusters vs db size'
        if editDistance != 0:
            raise Exception('not ready for non zero edit distance...')
        statName = 'clustersVsDbSize'
        if editDistance > 0:
            statName += '_editDistance' + str(editDistance)
        inverseSubIdMap = astNetwork.getInverseSubIdMap()
        submissions = inverseSubIdMap.keys()
        random.shuffle(submissions)
        uniqueAsts = []
        astSet = Set([])
        for sub in submissions:
            astId = inverseSubIdMap[sub]
            astSet.add(astId)
            uniqueAsts.append(len(astSet))
        self.save(statName, astNetwork, uniqueAsts)
        
    # Helper for (4)
    def filterGraphByEditDistance(self, graph, maxDistance):
        filteredGraph = nx.Graph()
        for node in graph.nodes():
            filteredGraph.add_node(node)
        for edgeTuple in graph.edges():
            start = edgeTuple[0]
            end = edgeTuple[1]
            weight = graph.edge[start][end]['weight']
            if weight <= maxDistance and weight > 0:
                attributeDict = {'weight': weight}
                filteredGraph.add_edge(start, end, attributeDict)
        return filteredGraph
    
    # (4)
    def saveEditDistanceVsClusters(self, astNetwork):
        print 'save edit distance vs clusters'
        editDistanceVsClusterStats = []
        for editDistance in range(0, MAX_EDIT_DISTANCE):
            print 'edit distance: ' + str(editDistance)
            #filteredGraph = self.filterGraphByEditDistance(graph, editDistance)
            filteredGraph = astNetwork.getGraph(editDistance)
            components = nx.connected_component_subgraphs(filteredGraph)
            numComponents = len(components) 
            averageDegree = numpy.mean(filteredGraph.degree().values())
            row = [editDistance, numComponents, averageDegree]
            editDistanceVsClusterStats.append(row)
        self.save('editDistanceVsClusters', astNetwork, editDistanceVsClusterStats)
        
        
    # (5)
    def saveCoverageVsNumClusters(self, astNetwork):
        astCoverage = []
        numStudents = astNetwork.getNumStudents()
        studentsCovered = 0
        clustersConsidered = 0
        for key in astNetwork.subIdMap:
            if clustersConsidered > MAX_NODES_CONSIDERED:
                percentCovered = float(studentsCovered) / numStudents
                astNetwork.addStat('coverageAfter' + str(MAX_NODES_CONSIDERED), percentCovered)
                break
            count = len(astNetwork.subIdMap[key])
            studentsCovered += count
            clustersConsidered += 1
            percentCovered = float(studentsCovered) / numStudents
            astCoverage.append([key, percentCovered])
        self.save('coverageVsNumClusters', astNetwork, astCoverage)
    
    def countZeros(self, astNetwork):
        print 'count zeros...'
        linesRead = 0
        count = 0
        while(True):
            line = astNetwork.matrixFile.readline()
            if not line: break
            values = line.strip().split()
            WarningManager.printWarning('len of file: ' + str(len(values)))
            print linesRead
            for value in values:
                value = int(value)
                if value == 0:
                    count += 1
            linesRead += 1
            
        print 'there are ' + str(count) + ' zeros in the file'
    
    def saveSyntaxStats(self, astNetwork):
        #self.countZeros(astNetwork)
        astNetwork.saveGephi()
        #self.saveCoverageCount(astNetwork)
        #self.saveClustersVsDbSize(astNetwork, 0)
        #self.saveEditDistanceVsClusters(astNetwork)
        #self.saveCoverageVsNumClusters(astNetwork)
    
    def saveOverallStat(self, statName, values):
        dir = os.path.join(FileSystem.getResultsDir(), 'syntaxStatistics')
        dir = os.path.join(dir, 'overallStats')
        if not os.path.exists(dir):
            os.makedirs(dir)
        histogram = numpy.histogram(values, HISTOGRAM_BUCKETS, range=(0, 1))
        histogramMatrix = []
        for index in range(len(histogram[0])):
            x = histogram[1][index]
            y = histogram[0][index]
            histogramMatrix.append([x, y])
        histogramPath = os.path.join(dir, statName + 'Hist.csv')
        valuesPath = os.path.join(dir, statName + 'Values.csv')
        numpy.savetxt(valuesPath, values, delimiter=",")
        numpy.savetxt(histogramPath, histogramMatrix, delimiter=",")
    
    def run(self):
        overallStatNames = []
        astNetworkStats = []
        for part in self.getAllParts():
            distanceMatrix = self.getDistanceMatrix(part)
            subIdMap = self.getSubmissionIdMap(part)
            astNetwork = AstNetwork(part, distanceMatrix, subIdMap)
            astNetworkStats.append(astNetwork.getStats())
            self.saveSyntaxStats(astNetwork)
        overallStatNames = astNetworkStats[0].keys()
        
        for statName in overallStatNames:
            values = []
            for astNetworkStat in astNetworkStats:
                value = astNetworkStat[statName]
                values.append(value)
            self.saveOverallStat(statName, values)
                
        print 'done'

if __name__ == "__main__":
    SyntaxStats().run()
