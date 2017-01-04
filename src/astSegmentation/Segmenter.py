import sys
import os.path
sys.path.append(os.path.abspath('../../'))
sys.path.append(os.path.abspath('../../site/cwsite/'))

import src.util.DBSetup
from src.util.FileSystem import FileSystem
from cwsite.models.models import Octave
from src.util.MLClass import MLClass
from src.util.Assignment import Assignment
from src.util.OctaveAST import OctaveAST
from src.util.Matching import Matching
from operator import itemgetter

class Segmenter(object):

	def __init__(self, hwId, partId, astId, numASTs = 500):
		octave = Octave.objects.get(homework_id = hwId, part_id, ast_id = astId)
		self.assn = Assignment(hwId, partId)
		self.astId = astId
		self.ast = OctaveAST(octave)
		self.statementListIds = self.ast.allNodes('STATEMENT_LIST')
		self.numASTs = numASTs
		self._runMatchings()

	def _runMatchings(self):
		self.totalMatch = {}
		self.consecutiveMatch = {}
		self.normalized = {}
		for i in range(self.numASTs):
			srcAST = self.ast
			targetAST = Octave.objects.get(homework_id = self.assn.getHomework(), \
												part_id = self.assn.getPart(), \
												ast_id = i)
			matcher = Matching.fromOctaveAST(self.assn, srcAST, targetAST)	
			for stmtListId in self.statementListIds:
				srcStatementIds = self.ast.getChildIds(stmtListId)
				targetStatementIds = [matcher.getDest(i) for i in srcStatementIds]
				for currSrc, nextSrc, currTarget, nextTarget in zip(srcStatementIds[:-1], srcStatementIds[1:], \
																	targetStatementIds[:-1], targetStatementIds[1:]):
					if currTarget != None and nextTarget != None:
						self.totalMatch[(currSrc, nextSrc)] += 1
						if targetAST.isConsecutiveSibling(currTarget, nextTarget):
							self.consecutiveMatch[(currSrc, nextSrc)] += 1
		for k in self.consecutiveMatch:
			self.normalized[k] = self.consecutiveMatch[k] / float(self.totalMatch[k])
	
	def topConsecutive(self, k = 5):	
		sortedPairs = sorted(self.consecutiveMatch.iteritems(), key = itemgetter(1), reverse = True)
		topPairs = [pair for (pair,val) in sortedPairs[:k]]
		return topPairs

	def topNormalizedConsecutive(self, k = 5):					
		sortedPairs = sorted(self.normalized.iteritems(), key = itemgetter(1), reverse = True)
		topPairs = [pair for (pair,val) in sortedPairs[:k]]
		return topPairs




