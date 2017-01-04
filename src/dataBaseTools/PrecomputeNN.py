import sys
import os.path
sys.path.append(os.path.abspath('../../'))
sys.path.append(os.path.abspath('../../site/cwsite'))

import src.util.DBSetup
from src.util.MLClass import MLClass
from src.util.FileSystem import FileSystem
from src.util.AstNetwork import AstNetwork
from src.util.Assignment import Assignment
from models.models import Octave

from operator import itemgetter
import logging
from sets import Set

class PrecomputeNN(object):

    def getASTs(self, assn, label):
        dataDir = FileSystem.getDataDir()
        outputDir = os.path.join(dataDir, 'incorrects')
        fileName = label + '_' + str(assn) + '.txt'
        path = os.path.join(outputDir, fileName)
        astList = []
        astFile = open(path)
        for line in astFile.readlines():
            astList.append(int(line))
        return Set(astList)

    def getAllParts(self):
        return [(4,1), (4,2), (4,3), (4,4), (4,5)]

    def getNN(self, corrects, incorrects, astNetwork):
        NNmap = {}
        numASTs = len(corrects) + len(incorrects)
        row = 0
        astNetwork.matrixFile.seek(0)
        while(True):
            if row % 100 == 0:
                logging.info(str(row) + ' of ' + str(numASTs))
            line = astNetwork.matrixFile.readline()
            if not line: break
            rowValues = map(int, line.strip().split())
            for col in range(row+1, len(rowValues)):
                value = rowValues[col]
                if value == -1:
                    continue
                if col in corrects:
                    try:
                        if value < NNmap[row][1]:
                            NNmap[row] = (col, value)
                    except KeyError:
                        NNmap[row] = (col, value)
                if row in corrects:
                    try:
                        if value < NNmap[col][1]:
                            NNmap[col] = (row, value)
                    except KeyError:
                        NNmap[col] = (row, value)
            row += 1
        return NNmap

    def writeNN(self, path, NNmap):
        fid = open(path,'wt')
        NNmaptuples = sorted(NNmap.iteritems(), key = itemgetter(0))
        for t in NNmaptuples:
            fid.write(str(t[0]) + ', ' + str(t[1][0]) + ', ' + str(t[1][1]) + '\n')
        fid.close()

    def initializeLog(self):
        logDir = os.path.join(FileSystem.getLogDir(),'PrecomputeNN')
        if not os.path.exists(logDir):
            os.makedirs(logDir)
        logFileName = os.path.join(logDir,'log')
        logging.basicConfig(filename = logFileName, format = '%(asctime)s %(message)s', \
                                datefmt = '%m/%d/%Y %I:%M:%S %p', level = logging.INFO)

    def run(self):
        self.initializeLog()
        for (h,p) in self.getAllParts():
            assn = Assignment(h,p)
            logging.info('PrecomputeNN (hw,part): ' + str(assn))
            corrects = self.getASTs(assn, 'corrects')   
            incorrects = self.getASTs(assn, 'incorrects')
            distanceMatrix = FileSystem.loadDistanceMatrix(assn.getTuple(),False)
            subIdMap = FileSystem.loadSubmissionIdMap(assn.getTuple())  
            astNetwork = AstNetwork(assn.getTuple(), distanceMatrix, subIdMap)
            NNmap = self.getNN(corrects, incorrects, astNetwork)
            
            outputDir = os.path.join(FileSystem.getDataDir(), 'nearestNeighbors')
            if not os.path.exists(outputDir):
                os.makedirs(outputDir)
            outputPath = os.path.join(outputDir, 'NNmap_' + str(assn) + '.txt')
            self.writeNN(outputPath, NNmap)
            
      
if __name__ == '__main__':
	PrecomputeNN().run()



