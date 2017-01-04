
import os.path
import numpy
import scipy.io
import tarfile
import scipy.sparse
import mmap
import contextlib
import tempfile
import logging

ROOT_DIR_NAME = 'codeweb'

class FileSystem(object):
    
    # assumes that the dir codeweb is unique in the project.
    @staticmethod
    def getRootDir():
        currentDir = os.path.dirname(os.path.realpath(__file__))
        while True:
            if currentDir == '':
                raise Exception('could not find ' + ROOT_DIR_NAME)
            rootDir, topDir = os.path.split(currentDir)
            if topDir == ROOT_DIR_NAME:
                return currentDir
            currentDir = rootDir

    @staticmethod
    def getDataDir():
        return FileSystem.getRootDir() + '/data'
    
    @staticmethod
    def getGephiDir():
        dir = os.path.join(FileSystem.getResultsDir(), 'astNetworks')
        if not os.path.exists(dir):
            os.makedirs(dir)
        return dir
    
    @staticmethod
    def getSiteDir():
        return FileSystem.getRootDir() + '/site/cwsite/cwsite'    

    @staticmethod
    def getDistanceMatrixDir():
        return os.path.join(FileSystem.getDataDir(), 'matching')

    @staticmethod
    def getBinDir():
        return FileSystem.getRootDir() + '/bin'
    
    @staticmethod
    def getAstDir():
        return FileSystem.getDataDir() + '/ast'

    @staticmethod
    def getLogDir():
        return FileSystem.getRootDir() + '/log'
    
    @staticmethod
    def getExtDir():
        return FileSystem.getRootDir() + '/ext'
    
    @staticmethod 
    def getWorkingDir():
        return FileSystem.getRootDir() + '/working'
    
    @staticmethod
    def getResultsDir():
        return FileSystem.getRootDir() + '/results'

    @staticmethod
    def getMatchExecutable():
        return os.path.join(FileSystem.getBinDir(),'matching') 

    @staticmethod
    def getDBMatchExecutable():
        return os.path.join(FileSystem.getBinDir(),'db-matching') 

    @staticmethod
    def getMatchKeywordPath():
        return os.path.join(FileSystem.getDataDir(),'starter')

    @staticmethod
    def getNearestNeighborDir():
        return os.path.join(FileSystem.getDataDir(), 'nearestNeighbors')

    @staticmethod
    def getKNNdir():
        return os.path.join(FileSystem.getDataDir(), 'KNN')

    @staticmethod
    def getStarterIdentsDir():
        return os.path.join(FileSystem.getDataDir(), 'starter')
    
    @staticmethod
    def getIdentCountDir():
        return os.path.join(FileSystem.getDataDir(), 'IdentCount')

    @staticmethod
    def loadStarterIdents(part):
        path = os.path.join(FileSystem.getStarterIdentsDir(), \
                        'starter_' + str(part[0]) + '_' + str(part[1]) + '.txt')
        with open(path) as fid:
            rows = fid.readlines()
        return [r.strip() for r in rows]

    @staticmethod
    def loadIdentCounts(part):
        path = os.path.join(FileSystem.getIdentCountDir(), \
                        'IdentCount_' + str(part[0]) + \
                        '_' + str(part[1]) + '.txt')
        with open(path) as fid:
            rows = fid.readlines()
        counts = []
        for r in rows:
            rtoks = r.strip().split(' ')
            counts.append((rtoks[0], int(rtoks[1])))
        return counts

    @staticmethod
    def loadDistanceMatrix(part, loadSparse):
        hwId = str(part[0])
        partId = str(part[1])
        fileNameBase = 'dist_' + hwId + '_' + partId
        extension = '.txt'
        if loadSparse:
            extension = '.sparse10.mat'
        fileName = fileNameBase + extension
        dataDir = FileSystem.getDistanceMatrixDir()
        path = os.path.join(dataDir, fileName)
        print 'load: ' + path
        _, fileExtension = os.path.splitext(fileName)
        if 'mtx' in fileExtension:
            return mmread(path)
        elif 'mat' in fileExtension:
            matrix = scipy.io.loadmat(path, mat_dtype=True)['m']
            return matrix.tocoo()
        else:
            f = open(path, 'r')
            return mmap.mmap(f.fileno(), 0, access=mmap.ACCESS_READ)
        
    @staticmethod
    def loadSubmissionIdMap(part):
        hwId = str(part[0])
        partId = str(part[1])
        fileNameBase = 'ast_' + hwId + '_' + partId
        tarName = fileNameBase + '.tar.gz'
        dataDir = FileSystem.getAstDir()
        path = os.path.join(dataDir, tarName)
        tar = tarfile.open(path)
        file = tar.extractfile(fileNameBase + '/submissionids.dat')
        file.readline()
        file.readline()
        submissionIdMap = {}
        for line in file:
            line = line.strip()
            values = line.split(':')
            astIdsString = values[2].strip(',').split(',')
            astIds = map(int, astIdsString)
            nodeId = int(values[0])
            submissionIdMap[nodeId] = astIds
        return submissionIdMap

    @staticmethod
    def loadNearestNeighbors(part):
        dataDir = FileSystem.getNearestNeighborDir()
        path = os.path.join(dataDir, 'NNmap_' + str(part[0]) + '_' \
                            + str(part[1]) + '.txt')
        print(path)
        with open(path) as fid:
            rows = fid.readlines()
        NNmap = {}
        for r in rows:
            values = [int(x) for x in r.strip().split(',')]   
            NNmap[values[0]] = (values[1],values[2])
        return NNmap

    @staticmethod
    def loadKNN(part):
        dataDir = FileSystem.getKNNdir()
        path = os.path.join(dataDir, 'KNNmap_' + str(part[0]) + '_' \
                            + str(part[1]) + '.txt')
        print(path)
        with open(path) as fid:
            rows = fid.readlines()
        K = int(rows[0].strip())
        KNNmap = {}
        for row in rows:
            rtoks = [int(x) for x in row.strip().split(' ')]
            astId = rtoks[0]
            rowK = (len(rtoks)-1)/2
            astIdList = rtoks[1:(rowK+2)]
            distList = rtoks[(rowK+2):]
            KNNmap[astId] = zip(astIdList, distList)
        return KNNmap

    @staticmethod
    def initializeLogging(dirName, logFileName = 'log', level = 'logging.INFO'):
        logDir = os.path.join(FileSystem.getLogDir(),dirName)
        if not os.path.exists(logDir):
            os.makedirs(logDir)
        logFileName = os.path.join(logDir,logFileName)
        logging.basicConfig(filename = logFileName, \
                    format = '%(asctime)s %(message)s', \
                    datefmt = '%m/%d/%Y %I:%M:%S %p', level = logging.INFO)

    @staticmethod
    def getUnitTestDir(hw_id):
        return FileSystem.getDataDir() + '/octave_unittest/mlclass-ex' + str(hw_id)
    
    ### LOCAL: WE SHOULD SEPARATE THIS OUT 
    @staticmethod
    def getOctave():
        return '/home/jhuang11/work/code/octave-3.6.3/bin/octave-3.6.3'





