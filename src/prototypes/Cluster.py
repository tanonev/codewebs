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
import igraph
import numpy as np
from sets import Set

THRESHOLD = 30
FILTERTHRESHOLD = 2
NUMCLUSTERS = 10
NUMASTS = 10

class Cluster(object):

    def loadGraph(self, assn, threshold, label):
        logging.info('Loading graph')
        filename = self.getGraphPath(assn, threshold, label)    
        return igraph.Graph.Read_GML(filename)
        
    def getGraphPath(self, assn, threshold, label):
        dataDir = FileSystem.getDataDir()
        incorrectsDir = os.path.join(dataDir,'incorrects')
        filename = label + 'Graph.' + str(assn) + '.sparse' + str(threshold) + '.gml'
        return os.path.join(incorrectsDir,filename)

    def getAllParts(self):
        return [Assignment(1,1)]
        return [Assignment(1,1),\
                Assignment(1,2),\
                Assignment(1,3),\
                Assignment(1,4),\
                Assignment(1,5),\
                Assignment(1,6),\
                Assignment(1,7)]

    # remove all edges with length greater than something
    def filterEdges(self,G,threshold):
        logging.info('Filtering graph with ' + str(len(G.es)) + ' edges...')
        toRemove = G.es.select(lambda edge: edge['edits'] > threshold)
        G.delete_edges(toRemove)
        logging.info('Edges remaining: ' + str(len(G.es)))
        return G
    
    def numSubmissionsInCluster(self, C):
        return sum(C.vs['weight'])

    def commonClusters(self, G, numClusters):
        logging.info('Clustering graph...')
        clusters = G.components().subgraphs()
        clusterSizeTuples = [(c,self.numSubmissionsInCluster(c)) for c in clusters]
        sortedClusters = sorted(clusterSizeTuples, key = itemgetter(1), reverse = True)
        logging.info('Number of connected components found: ' + str(len(sortedClusters)))
        numClusters = min(numClusters, len(sortedClusters))
        return [cluster for (cluster,clusterSize) in sortedClusters[:numClusters]]
    
    def sortByConnectivity(self, C):
        logging.info('Sorting nodes within each cluster by connectivity')
        astList = []
        for ast in C.vs:
            astValence = 0.0
            for nbr in C.neighborhood(ast):
                astValence += C.vs[nbr]['weight']
            astValence *= ast['weight']
            astList.append((ast,astValence))
        sortedASTs = sorted(astList, key = itemgetter(1), reverse = True)
        return [int(ast['label']) for (ast, astValence) in sortedASTs]

    def getOutputPath(self, assn, Cidx, label):
        resultsDir = FileSystem.getResultsDir()
        clustersDir = os.path.join(resultsDir,'clusters')
        if not os.path.exists(clustersDir):
            os.makedirs(clustersDir)
        connectedCompDir = os.path.join(clustersDir,'connectedComponents')
        if not os.path.exists(connectedCompDir):
            os.makedirs(connectedCompDir)
        filename = label + 'cluster_' + str(assn) + '_' + str(Cidx) + '.txt'
        return os.path.join(connectedCompDir,filename)

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

    # find nearest neighbors of asts in source in astsTarget
    def findNearestCorrects(self,assn,asts, numASTs,label):
        nn = {}
        if label == 'corrects':
            for ast in asts:
                nn[ast] = ast
            return nn
        corrects = self.getAsts(assn, 'corrects')
        sources = asts[:numASTs]
        distanceMatrix = FileSystem.loadDistanceMatrix(assn.getTuple(),False)
        subIdMap = FileSystem.loadSubmissionIdMap(assn.getTuple()) 
        astNetwork = AstNetwork(assn.getTuple, distanceMatrix, subIdMap)
        D = astNetwork.getDistanceList(sources,corrects)
        for s in D:
            nn[s] = D[s][np.argmin([y for (x,y) in D[s]])][0]
        return nn

    # writeResults expects a list of ast Ids sorted by prototypicalness
    def writeResults(self, assn, Cidx, asts, numASTs,numSubmissions,nn,label):
        filename = self.getOutputPath(assn,Cidx,label)
        fid = open(filename,'wt')
        fid.write('Number of submissions to cluster: ' \
                + str(numSubmissions) + '\n')
        for astId in asts[:numASTs]:
            fid.write(str(astId) + ' ' + str(nn[astId]) + '\n')

    def run(self):
        logDir = os.path.join(FileSystem.getLogDir(), 'cluster')
        if not os.path.exists(logDir):
            os.makedirs(logDir)
        logFileName = os.path.join(logDir,'log')
        logging.basicConfig(filename = logFileName, \
                    format = '%(asctime)s %(message)s', \
                    datefmt = '%m/%d/%Y %I:%M:%S %p', level = logging.INFO)
        labels = ['corrects','incorrects']
        for assn in self.getAllParts():
            for label in labels:
                logging.info('Cluster.run(): (hw,part): ' \
                    + str(assn) + ', ' + label)
                G = self.loadGraph(assn, THRESHOLD, label)
                Gfilt = self.filterEdges(G, FILTERTHRESHOLD)
                clusters = self.commonClusters(Gfilt,NUMCLUSTERS)
                for C,Cidx in zip(clusters,range(len(clusters))):
                    asts = self.sortByConnectivity(C)
                    numSubmissions = self.numSubmissionsInCluster(C)
                    logging.info('--------------------')
                    logging.info('Clustersize: ' + str(len(C.vs)) + \
                            ' ' + str(len(C.es)) + ' ' + str(numSubmissions))
                    #print(asts)
                    logging.info('Finding nearest corrects.')
                    nn = self.findNearestCorrects(assn,asts,NUMASTS,label)
                    self.writeResults(assn,Cidx,asts,NUMASTS, \
                            numSubmissions,nn,label)
        logging.info('Done.')
                
if __name__ == '__main__':
    Cluster().run()

