import sys
import os.path
sys.path.append(os.path.abspath('../../'))
sys.path.append(os.path.abspath('../../site/cwsite/'))

import numpy
import random
from sets import Set
import networkx as nx
import logging
from operator import itemgetter

from src.util.MLClass import MLClass
from src.util.FileSystem import FileSystem
from src.util.AstNetwork import AstNetwork
from cwsite.models.models import *

MAX_EDIT_DISTANCE = 10
MAX_NODES_CONSIDERED = 50
HISTOGRAM_BUCKETS = 10
NUM_OUTPUTS_CONSIDERED = 50

# This class is focused on mapping out the
# interplay between sylistic and functional
# differences
class InterplayStats(object):
    
    def saveAstsWithSameFunctionality(self):
        raise Exception('not done')
    
    # this function should generate stats for
    # each astNetwork passed (which represents one
    # homework part). Any stat that you want to collect
    # across all homework parts should be saved via the
    # astNetwork.addStat function
    def saveInterplayStats(self, astNetwork):
        #self.saveDistVsOutputSimilarity(astNetwork,30)
        #self.saveBetweenOutputClassDistances(astNetwork, 500,weighted=True)
        self.saveBetweenOutputClassDistances(astNetwork, 500,weighted=False)
        #astNetwork.saveGephi(withUnitTests = True)
        self.postProcessOutputClassDistances(astNetwork, 500)

    #######################################
    # Helper
    ######################################
    
    def getDistanceMatrix(self, part):
        sparse = part != (1,1)
        return FileSystem.loadDistanceMatrix(part, False)
    
    def getSubmissionIdMap(self, part):
        return FileSystem.loadSubmissionIdMap(part)
    
    def getAllParts(self):
        #return [(1,1)]
        return [(1,3)]
    
    def getPopularOutputs(self,astNetwork,k):
        logging.info('InterplayStats.getPopularOutputs')
        asts = Octave.objects.filter(homework_id = astNetwork.part[0], \
                    part_id = astNetwork.part[1]).values('output','ast_id')
        D = {}
        for s in asts:
            numSubmissions = len(astNetwork.subIdMap[s['ast_id']])
            try:
                D[s['output']] += numSubmissions
            except KeyError:
                D[s['output']] = numSubmissions
        sortedOutputs = sorted(D.iteritems(), key = itemgetter(1), reverse = True)
        return [x[0] for x in sortedOutputs[:k]]

    def saveDistVsOutputSimilarity(self, astNetwork, maxDist):
        row = 0
        astNetwork.matrixFile.seek(0)
        outputs = astNetwork.getOutputMap()
        results = numpy.zeros((maxDist,2))
        while True:
            line = astNetwork.matrixFile.readline()
            if not line: break
            rowValues = map(int, line.strip().split())
            rowOutput = outputs[row][0]
            numSubmissionsRow = len(astNetwork.subIdMap[row])
            for col in range(row + 1, len(rowValues)):
                dist = rowValues[col]
                colOutput = outputs[col][0]
                numSubmissionsCol = len(astNetwork.subIdMap[col])
                if dist < maxDist:
                    results[dist][int(rowOutput == colOutput)] += \
                            numSubmissionsRow*numSubmissionsCol
            row += 1
            logging.info('\trow ' + str(row) + ' of ' + str(len(astNetwork.subIdMap)))
        self.save('distanceVsOutputSimilarity', astNetwork, results)

    def saveBetweenOutputClassDistances(self, astNetwork, maxDist, weighted = False):
        if weighted == True:
            self.betweenOutputClassDistances = 'betweenOutputClassDistances_weighted'
        else:
            self.betweenOutputClassDistances = 'betweenOutputClassDistances'
        try:
            self.load(self.betweenOutputClassDistances,astNetwork)
            return
        except:
            pass
        logging.info('InterplayStats.saveBetweenOutputClassDistances')
        topOutputs = self.getPopularOutputs(astNetwork,NUM_OUTPUTS_CONSIDERED)
        outputs = astNetwork.getOutputMap()
        results = {}
        for i in range(NUM_OUTPUTS_CONSIDERED):
            for j in range(i,NUM_OUTPUTS_CONSIDERED):
                results[(i,j)] = maxDist*[0]
        row = 0
        astNetwork.matrixFile.seek(0)
        while True:
            line = astNetwork.matrixFile.readline()
            if not line: break
            rowValues = map(int, line.strip().split())
            rowOutput = outputs[row][1]
            if rowOutput not in topOutputs:
                row += 1
                continue
            rowIndex = topOutputs.index(rowOutput)
            numSubmissionsRow = len(astNetwork.subIdMap[row])
            for col in range(row, len(rowValues)):
                colOutput = outputs[col][1]
                if colOutput not in topOutputs:
                    continue
                colIndex = topOutputs.index(colOutput)
                dist = rowValues[col]
                numSubmissionsCol = len(astNetwork.subIdMap[col])
                if dist < maxDist:
                    key = tuple(sorted((rowIndex,colIndex)))
                    if weighted == True:
                        results[key][dist] += numSubmissionsRow*numSubmissionsCol
                    else:
                        results[key][dist] += 1
            row += 1
            logging.info('\trow ' + str(row) + ' of ' + str(len(astNetwork.subIdMap)))
        resultsMat = []
        for i in range(NUM_OUTPUTS_CONSIDERED):
            for j in range(i,NUM_OUTPUTS_CONSIDERED):
                tally = sum(results[(i,j)])
                try:
                    dist = [x/float(tally) for x in results[(i,j)]]
                    resultsMat.append([i,j,tally]+dist)
                except ZeroDivisionError:
                    logging.info('Error: ' + str((i,j)) +':'+  str(results[(i,j)]))
        self.save(self.betweenOutputClassDistances, astNetwork, resultsMat)
        
    def postProcessOutputClassDistances(self, astNetwork, maxDist):
        resultsBig = self.load(self.betweenOutputClassDistances, astNetwork)
        dist = numpy.zeros((2,resultsBig.shape[1]-3))
        for i in range(resultsBig.shape[0]):
            output1 = resultsBig[i,0]
            output2 = resultsBig[i,1]
            sameOutputs = int(output1 == output2)
            dist[sameOutputs,:] += resultsBig[i,3:]
        for i in range(2):
            dist[i,:] = dist[i,:]/numpy.sum(dist[i,:])
        self.save(self.betweenOutputClassDistances + '_post', \
                astNetwork, numpy.transpose(dist))

    def save(self, statName, astNetwork, matrix):
        syntaxDir = os.path.join(FileSystem.getResultsDir(), 'interplayStatistics')
        statDir = os.path.join(syntaxDir, statName)
        fileName = statName + '_' + str(astNetwork.part[0]) + '_' + str(astNetwork.part[1]) + '.csv'
        if not os.path.exists(statDir):
            os.makedirs(statDir)
        path = os.path.join(statDir, fileName)
        numpy.savetxt(path, matrix, delimiter=",")

    def load(self, statName, astNetwork):
        syntaxDir = os.path.join(FileSystem.getResultsDir(), 'interplayStatistics')
        statDir = os.path.join(syntaxDir, statName)
        fileName = statName + '_' + str(astNetwork.part[0]) + '_' + str(astNetwork.part[1]) + '.csv'
        path = os.path.join(statDir, fileName)
        if not os.path.exists(path):
            raise Exception('File not found')
        return numpy.loadtxt(path, delimiter=",")

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
            self.saveInterplayStats(astNetwork)
        overallStatNames = astNetworkStats[0].keys()
        
        for statName in overallStatNames:
            values = []
            for astNetworkStat in astNetworkStats:
                value = astNetworkStat[statName]
                values.append(value)
            self.saveOverallStat(statName, values)
                
        print 'done'

if __name__ == "__main__":
    InterplayStats().run()
