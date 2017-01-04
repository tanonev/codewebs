#include dbsetup
from PathImporter import PathImporter
PathImporter.importPythonPaths()
import DBSetup
from models.models import *
import sys 
import json
from Lca import Lca
import os

# Change this to be the annotation task id from 
# the database! Id 2 was my test id - Chris
ANNOTATION_TASK_ID = 1
OUTPUT_ROOT = '../../data/annotationJson'

class Runner(object):

    def saveAnnotation(self, json, octave):
        dirName = 'hw' + str(octave.homework_id) + '_' + str(octave.part_id)
        outputDir = os.path.join(OUTPUT_ROOT, dirName)
        if not os.path.exists(outputDir):
            os.makedirs(outputDir)
        fileName = str(octave.ast_id) + '.json'
        path = os.path.join(outputDir, fileName)
        print 'writing: ' + path
        outputFile = open(path, 'w')
        outputFile.write(json)

    def getAstNodeForChar(self, lineNum, colNum, nodeMap):
        mapLines = nodeMap.split('\n')
        # The first line says how many lines there are
        header = mapLines[0]
        body = mapLines[1:]
        # The first char in each line says how many characters there are
        line = body[lineNum].split(' ')[1:]
        astNode = line[colNum]
        return astNode
        
    def getAstNodeIdForComment(self, comment, octave):
        nodeMap = octave.map
        octaveJson = json.loads(octave.json)
        # since line numbers are indexed by 1 in the UI
        startLineNum = comment.start_line - 1
        endLineNum = comment.end_line - 1
        startColNum = comment.start_col
        # since the end col value is the character after the end of the selection
        endColNum = comment.end_col - 1

        startNode = self.getAstNodeForChar(startLineNum, startColNum, nodeMap) 
        endNode = self.getAstNodeForChar(endLineNum, endColNum, nodeMap)
        nodeId = Lca.getLca(octaveJson, int(startNode), int(endNode))
        return nodeId

    def getCommentDict(self, comment, octave):
        commentType = comment.comment_info.type
        astNodeId = self.getAstNodeIdForComment(comment, octave)
        return {
            'ast_id' : astNodeId,
            'confidence' : 1.0,
            'type' : commentType.upper(),
            'comment_id' : comment.id
        }

    def getCommentList(self, octave):
        commentList = []
        octaveComments = Comment.objects.filter(octave=octave).order_by("-timestamp")
        for comment in octaveComments:
            print 'processing comment: ' + str(comment.id) + '...'
            commentDict = self.getCommentDict(comment, octave)
            commentList.append(commentDict)
        return commentList

    def createJsonForOctave(self, octave):
        print 'get json for octave: ' + str(octave.id) + '...'
        jsonDict = {
            'comments' : self.getCommentList(octave)
        }       
        octaveJson = json.dumps(jsonDict)
        self.saveAnnotation(octaveJson, octave)
    
    def run(self):
        print '-----------------------------------'
        print 'running...'
        task = AnnotationTask.objects.get(id=ANNOTATION_TASK_ID)
        astsAnnotated = task.completed.all()
        for octave in astsAnnotated:
            self.createJsonForOctave(octave)
        print 'done'

try:
    argstr = sys.argv[1]
    ANNOTATION_TASK_ID = int(argstr)
except:
    print('Usage: python CreateAnnotatedJson.py [task_id]')
    sys.exit(1)
Runner().run()






