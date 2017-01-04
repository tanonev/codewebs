#! /usr/bin/env python

from PathImporter import PathImporter
PathImporter.importPythonPaths()

import DBSetup
from models.models import *
from OctaveAST import OctaveAST
import sys
from termcolor import colored

# get all submissions for a particular hw problem
hwId = 1
partId = 3
astId = 0
octave = Octave.objects.filter(homework_id = hwId, part_id = partId, ast_id = astId)[0]

#print(octave.code)
#print(octave.json)
#print(octave.map)
ast = OctaveAST(octave)

#nodeIdList = [15,16,17]
for nodeId in range(45,59):
    (startLine,startCol,endLine,endCol) = ast.getCodeBounds(nodeId)
    presnip,snip,postsnip = ast.snippetSplit(startLine,startCol,endLine,endCol)
    
    cpre = colored(presnip,'blue')
    csnip = colored(snip,'green')
    cpost = colored(postsnip,'blue')
    print('Code Snippet:')
    print('.............\n'+cpre+csnip+cpost+'\n..............')

