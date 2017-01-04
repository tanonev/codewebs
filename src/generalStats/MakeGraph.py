import sys
import os.path
sys.path.append(os.path.abspath('../../'))
sys.path.append(os.path.abspath('../../site/cwsite/'))

import numpy
import scipy
import mmap
#import networkx as nx
import igraph
import logging
from sets import Set

from src.util.FileSystem import FileSystem
from src.util.Distances import Distances
from src.util.Assignment import Assignment

THRESHOLD = 30

class MakeGraph(object):
    
    def getGraph(self, astSet, assn, threshold, label):
        part = assn.getTuple()
        #filteredGraph = nx.Graph()
        filteredGraph = igraph.Graph()

        distanceMatrix = FileSystem.loadDistanceMatrix(part, False)
        subIdMap = FileSystem.loadSubmissionIdMap(part)
        lookup = {}
        for key,idx in zip(subIdMap,range(len(subIdMap))):
            if int(key) in astSet:
                numStudents = len(subIdMap[key])
                #filteredGraph.add_node(key, {'weight': numStudents})
                filteredGraph.add_vertex(label=key,weight = numStudents)
                lookup[key] = filteredGraph.vs.find(label = int(key))
        row = 0
        toAdd = {}
        while True:
            logging.info('assn: ' + str(assn) + ', ' + label + ', row: ' + str(row))
            line = distanceMatrix.readline()
            if not line: break
            if not row in astSet:
                row += 1
                continue

            rowValues = map(int, line.strip().split())
            for col in range(row + 1, len(rowValues)):
                if not col in astSet:
                    continue
                value = rowValues[col]
                if value >= 0 and value <= threshold:
                    toAdd[(lookup[row], lookup[col])] = value
                    #filteredGraph.add_edge(row, col, {'edits': value})
            row += 1
        logging.info('Oh... one more thing.')
        filteredGraph.add_edges(toAdd.keys())
        filteredGraph.es['edits'] = toAdd.values()
        return filteredGraph
    
    def getOutputFilePath(self, assn, threshold, label):
        dataDir = FileSystem.getDataDir()
        outputDir = os.path.join(dataDir, 'incorrects')
        if not os.path.exists(outputDir):
            os.makedirs(outputDir)
        fileName = label+'Graph.' + str(assn) +'.sparse' + str(threshold) + '.gml'
        return os.path.join(outputDir, fileName)
    
    def getAsts(self, assn, label):
        dataDir = FileSystem.getDataDir()
        outputDir = os.path.join(dataDir, 'incorrects')
        fileName = label + '_' + str(assn) + '.txt'
        path = os.path.join(outputDir, fileName)
        astList = []
        astFile = open(path)
        for line in astFile.readlines():
            astList.append(int(line))
        return Set(astList)

    def run(self, assn, threshold):
        logDir = os.path.join(FileSystem.getLogDir(), 'MakeGraph')
        if not os.path.exists(logDir):
            os.makedirs(logDir)
        logFileName = os.path.join(logDir,'log')
        logging.basicConfig(filename = logFileName, format = '%(asctime)s %(message)s', \
                                datefmt = '%m/%d/%Y %I:%M:%S %p', level = logging.INFO)

        labels = ['incorrects','corrects']
        for label in labels:
            asts = self.getAsts(assn,label)
            graph = self.getGraph(asts, assn, threshold,label)
            outPath = self.getOutputFilePath(assn, threshold, label)
            logging.info('write graph: ' + outPath)
            graph.save(outPath)
            logging.info('done.')
        #nx.write_gml(graph, outPath)
            
if __name__ == "__main__":
    homework = 1
    numParts = 7
    for part in range(1,numParts + 1):
        assn = Assignment(homework, part)
        MakeGraph().run(assn, THRESHOLD)




