#! /usr/bin/env python 

import sys
import os.path
sys.path.append(os.path.abspath('../../'))
sys.path.append(os.path.abspath('../../site/cwsite/'))

import src.util.DBSetup
from src.util.FileSystem import FileSystem
from cwsite.models.models import Octave
from src.util.OctaveAST import OctaveAST
from src.util.Matching import Matching
from src.util.Printer import Printer

HWID = 1
PARTID = 3 
ASTID = 1
NUMASTS = 100 

k = 5

def report(node1, node2):
	octave = Octave.objects.get(homework_id = HWID, \
					part_id = PARTID, ast_id = ASTID)
	print(str(node1) + ', ' + str(node2))
	print('')
	Printer.highlightNode(octave,node1)
	print('')
	Printer.highlightNode(octave,node2)

def run():
	segmenter = Segmenter(HWID, PARTID, ASTID, NUMASTS)
	topPairs = segmenter.topNormalizedConsecutive(k)
	
	for (node1, node2) in topPairs:
		print('-------------------------------------------\n')
		report(node1, node2)
		print('-------------------------------------------\n\n\n')

if __name__ == '__main__':
	run()



