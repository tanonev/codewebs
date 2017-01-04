#include dbsetup
from PathImporter import PathImporter
PathImporter.importPythonPaths()
import DBSetup
from models.models import *
from cwsite.ast.OctaveAST import OctaveAST
import sys 
import json
from subprocess import call
from Lca import Lca
import os
from operator import itemgetter

# Change this to be the annotation directory you
# want to upload
DATA_ROOT = '../../data/annotationJson/propagated/hw1_3'
HOMEWORK_ID = 1
PART_ID = 3
MAX_ASTS_TO_UPLOAD = 50

class Runner(object):

    def getOctave(self, astId):
        octave = Octave.objects.get( \
            homework_id = HOMEWORK_ID, \
            part_id = PART_ID, \
            ast_id = int(astId) \
        )
        return octave

    def uploadComment(self, commentDict, octave):
        oldCommentId = commentDict['comment_id']
        oldComment = Comment.objects.get(id=oldCommentId)
        newComment = Comment()
        newComment.octave = octave
        newComment.comment_info = oldComment.comment_info
        newComment.human_label = False
        newComment.confidence = commentDict['confidence']
     
        ## need to check to make sure that this works
        nodeId = commentDict['ast_id']
        octaveast = OctaveAST(octave)
        (startLine,startCol,endLine,endCol) = octaveast.getCodeBounds(nodeId)
        newComment.start_line = startLine
        newComment.start_col = startCol
        newComment.end_line = endLine
        newComment.end_col = endCol
        newComment.codesnippet = octaveast.getCodeSnippet( \
                    startLine,startCol,endLine,endCol)
        
        newComment.save()

    def uploadComments(self, jsonFile, octave):
        commentsJson = json.loads(jsonFile.read())
        for comment in commentsJson['comments']:
            self.uploadComment(comment, octave)
        print octave.id

    def createPropagationEvaluation(self, propagatedOctaves):
        task = EvaluationTask.objects.create()
        for octave in propagatedOctaves:
            task.todo.add(octave)
        task.save()
        print 'created evaluation task id: ' + str(task.id)

    def run(self):
        print '-----------------------------------'
        print 'running...'
        files= os.listdir(DATA_ROOT)
        sortedfiles = sorted([(f,int(f.split('.')[0])) for f in files],key = itemgetter(1))
        propagatedOctaves = []
        for fileName,dummy in sortedfiles:
            print dummy
            path = os.path.join(DATA_ROOT, fileName)
            jsonFile= open(path)
            astId = fileName.split('.')[0]
            octave = self.getOctave(astId)
            self.uploadComments(jsonFile, octave)
            propagatedOctaves.append(octave)
            if len(propagatedOctaves) >= MAX_ASTS_TO_UPLOAD:
                break
        self.createPropagationEvaluation(propagatedOctaves)
        print 'done'

Runner().run()
