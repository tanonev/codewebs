import os.path
import sys
sys.path.append(os.path.abspath('../../'))
sys.path.append(os.path.abspath('../../site/cwsite/'))
from cwsite.models.models import *
import json

class OctaveAST(object):
    
    def __init__(self,octave = None):
        if octave:
            self.jsonStr = octave.json
            #tree = json.loads(octave.json)
            try:
                tree = json.loads(octave.json.replace('\r', '\\r'),strict=False)
            except ValueError:
                raise
            self.json = json
            self.root = tree['root']
            self._readOctMap(octave.map)
            self.code = octave.code

    @staticmethod
    def createOctaveAST(astJson, astMap, astCode):
        tree = json.loads(astJson) 
        octaveAST = OctaveAST()
        octaveAST.jsonStr = astJson
        octaveAST.root = tree['root']   
        octaveAST._readOctMap(astMap)
        octaveAST.code = astCode
        return octaveAST

    def __str__(self):
        return self.root.__str__()

    def allNodes(self, nodeType = ''):
        allNodes = OctaveAST.allDescendantIds(self.root, nodeType)
        return allNodes

    def getNumNodes(self):
        return len(self.allNodes())

    def getNodeId(self,startLine,startCol,endLine,endCol):
        startNode = self.maplines[startLine][startCol]  
        endNode = self.maplines[endLine][endCol]
        return self.getLCA(startNode,endNode) 

	def isSibling(self, node1, node2):
		self._ensureParentMapExists()
		return self.parentMap[node1] == self.parentMap[node2]

	def isConsecutiveSibling(self, node1, node2):
		if not self.isSibling(node1, node2):
			return False
		childIds = self.getChildIds(self.getParent(node1))	
		if abs(childIds.index(node1) - childIds.index(node2)) == 1:
			return True
		return False

	def isParent(self, node1, node2):
		self._ensureParentMapExists()
		return self.parentMap[node2] == node1
	
	def isChild(self, node1, node2):
		self._ensureParentMapExists()
		return self.parentMap[node1] == node2

	def getParent(self, nodeId):
		self._ensureParentMapExists()
		return self.parentMap[nodeId]

	def getChildIds(self, nodeId):
		self._ensureChildMapExists()
		return self.childMap[nodeId]

    def getLCA(self,node1,node2):
        self._ensureParentMapExists()
        path1 = self._getPathToRoot(node1)
        path2 = self._getPathToRoot(node2)
        lca = None
        while path1 and path2:
            id1 = path1.pop()
            id2 = path2.pop()
            if id1 != id2:
                break
            lca = id1
        assert lca != None
        return lca

    # returns subtree rooted at nodeId if it exists
    @staticmethod
    def subtree(nodeId,root):
        if root['id'] == nodeId:
            return root
        for child in root['children']:
            cst = OctaveAST.subtree(nodeId,child)
            if cst != None:
                return cst
        return None
           
    @staticmethod
    def allDescendantIds(root, nodeType = ''):
        desc = []
        if nodeType == '' or nodeType == root['type']:
            desc = [root['id']]
        for child in root['children']:
            desc = desc + OctaveAST.allDescendantIds(child, nodeType)
        return desc

    def getCodeBounds(self,nodeId):
        startLine, startCol = (len(self.maplines), max([len(l) for l in self.maplines]))
        endLine, endCol = (-1,-1)
        subtree = self.subtree(nodeId,self.root)
        if not subtree:
            return startLine,startCol,endLine,endCol
        nodeIdList = self.allDescendantIds(subtree)
        startFound = False
        for line, lineNum in zip(self.maplines,range(len(self.maplines))):
            for nodeId, colNum in zip(line,range(len(line))):
                if nodeId in nodeIdList:
                    if not startFound:
                        (startLine,startCol) = (lineNum,colNum)
                        startFound = True
                    (endLine,endCol) = (lineNum,colNum)
        return (startLine, startCol, endLine, endCol)       

    def getCodeSnippet(self, startLine, startCol, endLine, endCol):
        if startLine>endLine:
            return ''
        if startLine == endLine and startCol > endCol:
            return ''
        allLines = self.code.split('\n')
        lines = allLines[startLine:(endLine+1)]
        lines[0] = startCol*' ' + lines[0][startCol:]
        lines[-1] = lines[-1][:(endCol+1)] 
        return ''.join([l + '\n' for l in lines])

    def snippetSplit(self,startLine, startCol, endLine, endCol):
        if startLine>endLine:
            return self.code,'',''
        if startLine == endLine and startCol > endCol:
            return self.code,'',''
        allLines = self.code.split('\n')

        linesPre = allLines[:(startLine+1)]
        lines = allLines[startLine:(endLine+1)]
        linesPost = allLines[endLine:]

        linesPre[-1] = linesPre[-1][:startCol]
        if startLine == endLine:
            lines[0] = lines[0][startCol:(endCol+1)]
        else:
            lines[0] = lines[0][startCol:]
            lines[-1] = lines[-1][:(endCol+1)] 
        linesPost[0] = linesPost[0][(endCol+1):]

        return (''.join([l + '\n' for l in linesPre[:-1]] + [linesPre[-1]]), \
                ''.join([l + '\n' for l in lines[:-1]] + [lines[-1]]), \
                ''.join([l + '\n' for l in linesPost]) )

    def _readOctMap(self,mapstr):
        mapLines = mapstr.split('\n')
        header = mapLines[0]
        body = mapLines[1:]
        lines = []
        for l in body:
            lines.append([int(x) for x in l.split(' ')[1:]])
        self.mapheader = header
        self.maplines = lines

    def _getPathToRoot(self,node):
        path = []
        while node != None:
            path.append(node)
            node = self.parentMap[node]
        return path

    def _ensureParentMapExists(self):
        try:
            self.parentMap
        except AttributeError:
            self.parentMap = {}
            self._populateParentMap(self.root, None)

    def _populateParentMap(self,node, parentId):
        curId = int(node['id'])
        self.parentMap[curId] = parentId;
        for child in node['children']:
            self._populateParentMap(child, curId)
	
	def _ensureChildMapExists(self):
		try:
			self.childMap 
		except AttributeError:
			self.childMap = {}
			self._populateChildMap(self.root)

	def _populateChildMap(self, node):
		curId = int(node['id'])
		self.childMap[curId] = []
		for child in node['children']:
			self.childMap[curId].append(int(child['id']))
			self._populateChildMap(child)












