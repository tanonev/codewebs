import os.path
import sys
sys.path.append(os.path.abspath('../../'))

from tempfile import mkstemp
from src.util.Assignment import Assignment
from src.util.RunExternal import RunExternal
from FileSystem import FileSystem
from cwsite.models.models import Octave
from src.util.OctaveAST import OctaveAST

class Matching(object):

    @staticmethod
    def fromOctaveAST(assn, sourceAST, targetAST, matchFile = ''):
        matcher = Matching()    
        matcher.assn = assn
        matcher.sourceAST = sourceAST
        matcher.targetAST = targetAST
        matcher.timeOut = 5
        matcher.keywordFile = os.path.join(FileSystem.getMatchKeywordPath(),'starter_' + str(assn) + '.txt')
        if len(matchFile) == 0:
            matcher.runMatching()
        else:
            matcher.loadMatching(matchFile)
        return matcher

    # the source and target ids are ast ids
    @staticmethod
    def fromASTids( assn, sourceId, targetId, matchFile = ''):
        matcher = Matching()
        matcher.assn = assn
        matcher.sourceId = sourceId
        matcher.targetId = targetId
        matcher.source = matcher._getOctave(matcher.sourceId)
        matcher.target = matcher._getOctave(matcher.targetId)
        matcher.sourceAST = OctaveAST(matcher.source)
        matcher.targetAST = OctaveAST(matcher.target)
        matcher.timeOut = 5
        matcher.keywordFile = os.path.join(FileSystem.getMatchKeywordPath(),'starter_' + str(assn) + '.txt')
        if len(matchFile) == 0:
            matcher.runMatching()
        else:
            matcher.loadMatching(matchFile)
        return matcher

    @staticmethod
    def againstMultiple(assn, sourceAST, targetIds):
        matcher = Matching()
        matcher.assn = assn
        matcher.timeOut = 5
        matcher.keywordFile = os.path.join(FileSystem.getMatchKeywordPath(),'starter_' + str(assn) + '.txt')
        matcher.sourceAST = sourceAST
        matchResult = Matching._dbMatcher(matcher.assn, \
                    sourceAST, targetIds, matcher.keywordFile)
        # run andy's db code
        # set up sourceAST and targetASTs

    # this function returns a list that corresponds to the map for the source code
    # with a 1 in every character position that matches to something in target 
    def diffSourceMap(self):
        diffMap = []
        sourceSide = [x for x,y in self.match]
        for line in self.sourceAST.maplines:
            diffMap.append([])
            for char in line:
                if char in sourceSide:
                    diffMap[-1].append(1)
                else:
                    diffMap[-1].append(0)
        return diffMap

    # this function returns a list that corresponds to the map for the target code
    # with a 1 in every character position that matches to something in source 
    def diffTargetMap(self):
        diffMap = []
        targetSide = [y for x,y in self.match]
        for line in self.targetAST.maplines:
            diffMap.append([])
            for char in line:
                if char in targetSide:
                    diffMap[-1].append(1)
                else:
                    diffMap[-1].append(0)
        return diffMap

	def getDest(self, node):
		self.forwardMap[node]

	def getSource(self, node):
		self.backwardMap[node]

    @staticmethod
    def _cleanUp(filePaths):
        for filePath in filePaths:
            os.remove(filePath)

    def runMatching(self):
        dummy1, tmpSource = mkstemp()
        dummy2, tmpTarget = mkstemp()
        tmpPaths = [tmpSource, tmpTarget]
        try:
            with open(tmpSource,'wt') as fidSource:
                fidSource.write(self.sourceAST.jsonStr)
            with open(tmpTarget,'wt') as fidTarget:
                fidTarget.write(self.targetAST.jsonStr)

            matchpath = FileSystem.getMatchExecutable()
            matchExec = RunExternal([matchpath, tmpSource, tmpTarget, self.keywordFile], \
                             self.timeOut, pipeOption = True)
            matchExec.runWithPipe()
        except:
            Matching._cleanUp(tmpPaths)
            raise
        Matching._cleanUp(tmpPaths)
        self._parseLines(matchExec.outLines)

    def loadMatching(self,matchFile):
        if not os.path.exists(matchFile):
            print('File ' + matchFile + ' does not exist!')
            sys.exit(1)
        fid = open(matchFile)
        lines = fid.readlines()
        fid.close()
        self._parseLines(lines)

    #@staticmethod
    #def _dbMatcher(assn, sourceAST, targetIds, keywordFile):
    #    try:
    #        dbMatchPath = FileSystem.getDBMatchExecutable()
    #        matchExec = RunExternal([], self.timeOut, pipeOption = True)
    #        matchExec.setPipeIn(sourceAST.code)a
    #        matchExec.runWithPipe()
    #    except:
    #        raise
    #    firstLine = [int(x) for x in matchExec.outLines[0].rstrip().split(' ')]
    #    score = firstLine[0]
    #    return matchResult

    def getScore(self):
        return self.score

    def _parseLines(self,lines):
        firstLine = [int(x) for x in lines[0].rstrip().split(' ')]
        self.score = firstLine[0]
        self.match = []
        for line in lines[1:]:
            self.match.append([int(x) for x in line.rstrip().split(' ')])
        self._createMaps()
        #self.match =[]
        #for line in lines:
        #    l = line.rstrip().split(' ')
        #    self.match.append([int(l[0]), int(l[2])])

	def _createMaps(self):
		self.forwardMap = {}
		self.backwardMap = {}
		for i,j in self.match:
			self.forwardMap[i] = j
			self.backwardMap[j] = i

    def _getOctave(self,astId):
        return Octave.objects.filter(homework_id = self.assn.getHomework(), \
                                part_id = self.assn.getPart(), \
                                ast_id = astId)[0]

    

