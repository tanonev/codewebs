import sys
import os.path
sys.path.append(os.path.abspath('../../'))
sys.path.append(os.path.abspath('../../site/cwsite'))

import src.util.DBSetup
from src.util.MLClass import MLClass
from src.util.FileSystem import FileSystem
from src.util.Assignment import Assignment
from models.models import Octave

class ClusterViewer(object):

    def GetClusterPath(self,assn, clusterId, label):
        resultsDir = FileSystem.getResultsDir()
        connectedCompDir = os.path.join(resultsDir, 'clusters', 'connectedComponents')
        filename = label + 'cluster_' + str(assn) + '_' + str(clusterId) + '.txt'
        fullpath = os.path.join(connectedCompDir,filename)
        if not os.path.exists(fullpath):
            print('Error: file was not found')
            print('\tpath: ' + fullpath)
            sys.exit(1)
        else:
            return fullpath

    def loadClusterFile(self,clusterPath):
        fid = open(clusterPath)
        lines = fid.read()
        fid.close()
        with open(clusterPath) as fid: 
            return [int(x) for x in fid.read().rstrip().split('\n')[1:]]

    def printAST(self,astId,assn):
        octave = Octave.objects.filter(homework_id = assn.getHomework(), \
                                    part_id = assn.getPart(), \
                                    ast_id = astId)[0]
        print('-------------------------------------------------')
        print(octave.code)
        print('-------------------------------------------------')
        print('AST Id: ' + str(astId))
        print('Number of submissions: ' + str(octave.numSubmissions()))

    def run(self,assn, clusterId, label):
        clusterPath = self.GetClusterPath(assn,clusterId,label)
        astIds = self.loadClusterFile(clusterPath)
        for astId in astIds:
            os.system('clear')
            self.printAST(astId,assn)
            raw_input('Press key.')

if __name__ == '__main__':
    if len(sys.argv) == 5:
        label = sys.argv[1] ## this is `corrects' or 'incorrects'
        hwId = int(sys.argv[2])
        partId = int(sys.argv[3])
        clusterId = int(sys.argv[4])
    else:
        print('Usage: python ViewClusters.py [label] [hwId] [partId] [clusterId]')
        sys.exit(1)
    assn = Assignment(hwId,partId)
    ClusterViewer().run(assn,clusterId,label)

