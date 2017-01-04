import networkx as nx
import os.path
import scipy.sparse 
from numpy import log

import logging
from src.util.FileSystem import FileSystem
from cwsite.models.models import Octave

class AstNetwork(object):
    
    def __init__(self, part, matrixFile, idMap):
        self.matrixFile = matrixFile
        self.subIdMap = idMap
        self.part = part
        self.stats = {}
        logDir = os.path.join(FileSystem.getLogDir(),'astnetwork')
        if not os.path.exists(logDir):
            os.makedirs(logDir)
        logFileName = os.path.join(logDir,'log')
        logging.basicConfig(filename = logFileName, format = '%(asctime)s %(message)s', \
                                datefmt = '%m/%d/%Y %I:%M:%S %p', level = logging.INFO)
        logging.info('AstNetwork Initialization: (hw,part): ' + str(self.part))
        
    def getHomeworkName(self):
        hwId = str(self.part[0])
        partId = str(self.part[1])
        return hwId + '_' + partId
        
    def getPart(self):
        return self.part
    
    def getNumAsts(self):
        return distanceMatrix.shape[0]
    
    def getNumStudents(self):
        numStudents = 0
        for sub in self.subIdMap:
            numStudents += len(self.subIdMap[sub])
        return numStudents
    
    def getInverseSubIdMap(self):
        inverseMap = {}
        for key in self.subIdMap:
            values = self.subIdMap[key]
            for value in values:
                inverseMap[value] = key
        return inverseMap
    
    def getFullGraph(self):
        return self.getGraph(30)
    
    def addStat(self, statName, value):
        self.stats[statName] = value
        
    def getStatNames(self):
        return self.stats.keys()
        
    def getStats(self):
        return self.stats
    
    def saveGephi(self, withUnitTests = False):
        if withUnitTests == True:
            graph = self.getGraphWithUnitTests(5)
        else:
            graph = self.getGraph(5)
        outPath = self._getGephiFilePath()
        print 'write gephi: ' + outPath
        nx.write_gml(graph, outPath)
    
    def _getGephiFilePath(self):
        fileDir = FileSystem.getGephiDir()
        fileName = 'gephi_' + self.getHomeworkName() + '.gml'
        return os.path.join(fileDir, fileName)
    
    def _getNodeMap(self, components):
        nodeMap = {}
        reverseMap = {}
        idCounter = 0
        for component in components:
            nodeList = []
            for node in component.nodes():
                nodeList.append(node)
                reverseMap[node] = idCounter
            nodeMap[idCounter] = nodeList
            idCounter += 1
        return nodeMap, reverseMap
    
    def streamedMatrixExample(self):
        row = 0
        self.matrixFile.seek(0)
        while True:
            line = self.matrixFile.readline()
            if not line: break
            rowValues = map(int, line.strip().split())
            reducedGraph.add_node(row)
            for col in range(row + 1, len(rowValues)):
                value = rowValues[col]
                # do something with the value
                # ... anything :)
            row += 1

    # outputs indexed by asts
    def getOutputMap(self):    
        logging.info('AstNetwork.getOutputMap()')
        astList = Octave.objects.filter(homework_id = self.part[0], \
                part_id = self.part[1]).values('ast_id','output','correct')
        outputs = {}
        for ast in astList:
            outputs[ast['ast_id']] = (ast['correct'],ast['output'])
        return outputs
        
    # asts indexed by output
    def getInverseOutputMap(self):
        logging.info('AstNetwork.getOutputMap()')
        astList = Octave.objects.filter(homework_id = self.part[0], \
                part_id = self.part[1]).values('ast_id','output','correct')
        asts = {}
        for ast in astList:
            try:
                asts[ast['output']].append((ast['ast_id'],ast['correct']))
            except KeyError:
                asts[ast['output']] = [(ast['ast_id'],ast['correct'])]
        return asts

    # return list of correct ast_ids
    def getCorrectASTids(self):
        logging.info('AstNetwork.getCorrectASTids()')
        astList = Octave.objects.filter(homework_id = self.part[0], \
                part_id = self.part[1]).values('ast_id','correct')
        corrects = [ast['ast_id'] for ast in astList if bool(ast['correct']) == True]
        return corrects

    # return list of incorrect ast_ids
    def getIncorrectASTids(self):
        logging.info('AstNetwork.getIncorrectASTids()')
        astList = Octave.objects.filter(homework_id = self.part[0], \
                part_id = self.part[1]).values('ast_id','correct')
        incorrects = [ast['ast_id'] for ast in astList if bool(ast['correct']) == False]
        return incorrects

    def getGraph(self, maxCuttoff):
        graph = nx.Graph()
        for key in self.subIdMap:
            numStudents = len(self.subIdMap[key])
            graph.add_node(key, {'weight': numStudents})
        
        row = 0
        self.matrixFile.seek(0)
        while(True): 
            line = self.matrixFile.readline()
            if not line: break
            rowValues = map(int, line.strip().split())
            for col in range(row + 1, len(rowValues)):
                value = rowValues[col]
                if value >= 0 and value <= maxCuttoff:
                    dissimilarity = value + 1
                    weight = 1.0 / (dissimilarity * dissimilarity)
                    graph.add_edge(row, col, {'weight':weight})
            row += 1
        return graph

    def getGraphWithUnitTests(self, maxCutoff):
        logging.info('AstNetwork.getGraphWithUnitTests(' + str(maxCutoff) + ')')
        graph = nx.Graph()
        outputs = self.getOutputMap()
        for key in self.subIdMap:
            numStudents = len(self.subIdMap[key])
            graph.add_node(key, {'weight': numStudents, \
                                'logWeight' : log(float(numStudents)), \
                                'output': outputs[key][1], \
                                'correct': outputs[key][0]})
            logging.info('\tastId ' + str(key) + ' of ' + str(len(self.subIdMap)))
        row = 0
        self.matrixFile.seek(0)
        logging.info('\treading matrix...')
        while(True):
            line = self.matrixFile.readline()
            if not line: break
            rowValues = map(int, line.strip().split())
            for col in range(row + 1, len(rowValues)):
                value = rowValues[col]
                if value >= 0 and value <= maxCutoff:
                    dissimilarity = value + 1
                    weight = 1.0 / (dissimilarity * dissimilarity)
                    graph.add_edge(row, col, {'weight':weight})
            row += 1
            logging.info('\trow ' + str(row) + ' of ' + str(len(self.subIdMap)))
        return graph   

    def getGraphOld(self, minCuttoff, maxCuttoff):
        # later I should allow non zero minCuttoffs. I will need to
        # update the update of clusteredGraph edges to be the max
        # of edges seen so far between ASTs in different clusteredNodes
        assert minCuttoff == 0
        
        print 'create reduced graph'
        row = 0
        reducedGraph = nx.Graph()
        self.matrixFile.seek(0)
        while(True):
            line = self.matrixFile.readline()
            if not line: break
            rowValues = map(int, line.strip().split())
            reducedGraph.add_node(row)
            for col in range(row + 1, len(rowValues)):
                value = rowValues[col]
                if value == 0:
                    reducedGraph.add_edge(row, col)
            row += 1
        components = nx.connected_component_subgraphs(reducedGraph)
        nodeMap, reverseMap = self._getNodeMap(components)
        
        print 'created clustered graph nodes'
        clusteredGraph = nx.Graph()
        for nodeId in nodeMap:
            numStudents = 0
            for node in nodeMap[nodeId]:
                count = len(self.subIdMap[node])
                numStudents += count
            clusteredGraph.add_node(nodeId, {'weight':numStudents})
        
        self.matrixFile.seek(0)
        print 'create clustered graph edges'
        row = 0
        while(True): 
            line = self.matrixFile.readline()
            if not line: break
            rowValues = map(int, line.strip().split())
            for col in range(row + 1, len(rowValues)):
                value = rowValues[col]
                if value >= 0 and value <= maxCuttoff:
                    editDistance = value
                    node1 = reverseMap[row]
                    node2 = reverseMap[col]
                    if node1 == node2: continue
                    weight = 1.0 / (editDistance * editDistance)
                    clusteredGraph.add_edge(node1, node2, {'weight':weight})
            row += 1
        return clusteredGraph
    
    
    def _getGraphDist(self, node1, node2, nodeMap):
        return 1
    
    
