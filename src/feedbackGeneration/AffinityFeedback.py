import os.path
import hashlib

from cwsite.models.models import *

from src.feedbackGeneration.FeedbackGenerator import FeedbackGenerator
from src.util.Matching import Matching
from src.util.Assignment import Assignment
from src.util.OctaveAST import OctaveAST
from src.util.FileSystem import FileSystem

# The main data structure in this class is:
#    self.assnExemplars
# This dictionary maps assignments to a feedback dictionary.
# The feedback dictionary maps exemplarIds to a dictionary
# of relevant information
HOMEWORK_ID = 8
PART_ID = 6
MAX_EDIT_DISTANCE = 20

class AffinityFeedback(FeedbackGenerator):

    def __init__(self):
        assignmentList = [Assignment(HOMEWORK_ID, PART_ID)]

        self.assnExemplars = {}
        for assn in assignmentList:
            feedbackDict = self._getFeedbackDict(assn)
            self.assnExemplars[assn] = feedbackDict
            
    def getFeedback(self, ast, code, charmap, assn):

        hashCode = hashlib.sha1(ast.encode('utf-8')).hexdigest()

        raise Exception(hashCode)

        self._checkAssnIsValid(assn)
        feedbackDict = self.assnExemplars[assn]

        exemplarDistances = {}
        minDistance = None
        minDistanceId = None

        queryAST = OctaveAST.createOctaveAST(ast, charmap, code)

        tmpResponse = ''
        for exemplarId in feedbackDict:
            exemplar = feedbackDict[exemplarId]['ast']
            
            if not self._passesLowerBoundMatching(queryAST, exemplarAST):
                continue
            matcher = Matching.fromOctaveAST(assn, queryAST, exemplarAST)
            matcher.runMatching()
            exemplarDistance = matcher.getScore()
            exemplarDistances[exemplarId] = exemplarDistance
            if exemplarDistance < minDistance:
                minDistance = exemplarDistance
                minDistanceId = exemplarId
            
        if len(scores) == 0:
            raise Exception('not close enough to any exemplar')
        bestExemplar = feedbackDict[minDistanceId]
        comments = bestExemplar['comments']
        if len(comments) == 0:
            raise Exception('No comment for corresponding exemplar')
    
        return comments[0].comment_info.text


    ##############################
    # Private Helper Methods     #
    ##############################

    def _passesLowerBoundMatching(self, queryAST, exemplarAST):
        queryNodes = queryAST.getNumNodes()
        exemplarNodes = exemplarAST.getNumNodes()
        return abs(queryNodes - exemplarNodes) <= MAX_EDIT_DISTANCE
        #raise Exception('not ready')

    def _getAffinityExemplarFile(self, assn):
        resultsDir = FileSystem.getResultsDir()
        fileDir =  os.path.join(resultsDir, 'clusters/affinityExemplars')
        fileName = 'exemplars_' + str(assn) + '.txt'
        exemplarFile = os.path.join(fileDir, fileName)
        return open(exemplarFile)

    def _loadAffinityExemplarIds(self, assn):
        affinityExemplarFile = self._getAffinityExemplarFile(assn)
        affinityExemplarIds = []
        for line in affinityExemplarFile:
            affinityExemplarIds.append(line.strip())
        affinityExemplarFile.close()
        return affinityExemplarIds

    def _getFeedbackDict(self, assn):
        self._checkAssnIsValid(assn)
        hw = assn.getHomework()
        part = assn.getPart()

        exemplarList = self._loadAffinityExemplarIds(assn)
            
        feedbackDict = {}
        for exemplarId in exemplarList:
            octave = Octave.objects.get(ast_id = exemplarId, homework_id = hw, part_id = part)
            exemplarAST = OctaveAST.createOctaveAST(octave.json, octave.map, octave.code)
            feedbackDict[exemplarId] = { 
                'homework_id': octave.homework_id, 
                'part_id' : octave.part_id,
                'ast_id' : octave.ast_id,
                'code' : octave.code,
                'json' : octave.json,
                'map' : octave.map,
                'comments' : Comment.objects.filter(octave_id = octave.id),
                'ast' : exemplarAST
            }  
        return feedbackDict

    def _checkAssnIsValid(self, assn):
        if assn.getTuple() != (HOMEWORK_ID, PART_ID):
            raise Exception(str(assn.getTuple()) + ' not valid')




  
