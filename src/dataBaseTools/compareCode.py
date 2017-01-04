#! /usr/bin/env python

import os,sys
sys.path.append(os.path.abspath('../../'))

import src.util.DBSetup
from src.util.MLClass import MLClass
from src.util.FileSystem import FileSystem
from src.util.Matching import Matching
from src.util.Printer import Printer
from src.util.Assignment import Assignment

from cwsite.models.models import Octave

def run(assn, sourceId, targetId):
    sourceResult = Octave.objects.get(homework_id = hwId, part_id = partId, \
            ast_id = sourceId).correct
    targetResult = Octave.objects.get(homework_id = hwId, part_id = partId, \
            ast_id = targetId).correct
    M = Matching.fromASTids(assn, sourceId, targetId)

    print('score: '+str(M.score))
    diffSourceMap = M.diffSourceMap()
    diffTargetMap = M.diffTargetMap()

    resultstr = ['incorrect','correct']
    print('Problem: ' + str(assn) + ', AST #' +  str(sourceId))
    print('Result: ' + resultstr[sourceResult])
    print('------------------------')
    Printer.mask(M.source, diffSourceMap)
    print('\nProblem: ' + str(assn) + ', AST #' +  str(targetId))
    print('Result: ' + resultstr[targetResult])
    print('------------------------')
    Printer.mask(M.target, diffTargetMap)

if __name__ == '__main__':
    try:
        hwId = int(sys.argv[1])
        partId = int(sys.argv[2])
        sourceId = int(sys.argv[3])
        if len(sys.argv) > 4:
            targetId = int(sys.argv[4])
        else:
            print('asdf')
            NNmap = FileSystem.loadNearestNeighbors((hwId,partId)) 
            targetId = NNmap[sourceId][0]  
    except:
        print('Usage: python compareCode.py hwId partId sourceId [targetId]')
        sys.exit(1)
    
    run(Assignment(hwId,partId), sourceId, targetId)

#hwId = 3
#partId = 3
#sourceId = 0
#targetId = 7221

