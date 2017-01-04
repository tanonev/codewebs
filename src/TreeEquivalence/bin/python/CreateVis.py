import sys
import os.path
sys.path.append(os.path.abspath('../../../../'))
import re
from src.util.MLClass import MLClass
from src.util.FileSystem import FileSystem
from src.util.Assignment import Assignment

class CreateVis(object):

    def __init__(self, assn):
        self.equivDir = os.path.join(FileSystem.getDataDir(), \
                    'equivalence','equivalence_' + str(assn))
        self.levels = []
        for d in os.listdir(self.equivDir):
            try:
                self.levels.append(int(d))
            except ValueError:
                pass
        self.classes = {}
        self.levelMap = {}
        for level in self.levels:
            for d in os.listdir(os.path.join(self.equivDir,str(level))):
                prefix = d.split('.')[0]
                self.levelMap[int(prefix)] = level
                try:
                    self.classes[level].append(int(prefix))      
                except KeyError:
                    self.classes[level] = [int(prefix)]
                except ValueError:
                    pass

    def readEquivalenceClass(self, level, classId):
        path = os.path.join(self.equivDir, str(level), str(classId) + '.txt')
        subtrees = []
        with open(path) as fid:
            blocks = fid.read().split('------------\n')
            for block in blocks[1:]:
                lines = block.split('\n')
                info = [int(x) for x in lines[0].split(' ')]
                code = ''.join([s+'\n' for s in lines[1:]])
                subtrees.append({'info': info, 'code': code})
        return subtrees

    def codeSplit(self,code):
        last = 0
        segments = []
        for f in re.finditer('({[0-9]*})',code):
            segments.append(code[last:f.start(1)])
            segments.append(code[f.start(1):f.end(1)])
            last = f.end(1)
        segments.append(code[last:])
        return segments

    def getClassRefs(self, code):
        segments = self.codeSplit(code)
        classRefs = []
        for segIdx in range(1,len(segments),2):
            segment = segments[segIdx]
            classRefs.append(int(segment[1:-1]))
        return list(set(classRefs))

    def readTemplate(self):
        path = os.path.join(FileSystem.getDataDir(), 'equivalence', 'templates', 'template.html')
        with open(path) as fid:
            template = fid.read() 
        (before,after) = template.split('{{hierarchy}}')
        (middle,after) = after.split('{{script}}')
        self.template = {'before': before,'middle': middle, 'after': after}

    def loadAllClasses(self):
        self.subtrees = {}
        for level in self.levels:
            for classId in self.classes[level]:
                self.subtrees[(level,classId)] = self.readEquivalenceClass(level,classId)

    def getOutputDir(self):
        path = os.path.join(FileSystem.getDataDir(), 'equivalence', 'html_' + str(assn))
        if not os.path.exists(path):
            os.makedirs(path)
        return path

    def writeToFile(self,level, classId, hierarchy, script):
        path = os.path.join(self.getOutputDir(), str(level) + '_' + str(classId) + '.html')
        with open(path,'wt') as fid:
            fid.write(self.template['before'])
            fid.write(hierarchy)
            fid.write(self.template['middle'])
            fid.write(script)
            fid.write(self.template['after'])

    def startDiv(self,indent, divId = '', divClass = ''):
        if len(divId) > 0:
            divId = ' id="' + divId + '"'
        if len(divClass) > 0:
            divClass = ' class="' + divClass + '"'
        return indent*'\t' + '<div' + divId + divClass + '>\n'

    def endDiv(self,indent):
        return indent*'\t' + '</div>\n'
    
    def badge(self, indent, classId):
        return indent*'\t' + '<span class="badge badge-success">' + str(classId) + '</span>\n'

    def renderCode(self, code):    
        s = ''
        segments = self.codeSplit(code)
        for segment, segIdx in zip(segments,range(len(segments))):
            if segIdx % 2 == 0:
                s += segment
            else:
                s += '<a href="javascript: void(0);" class="showClass' \
                        + segment[1:-1] + '">' + segment + '</a>'
        return s + '\n'

    def scriptSegment(self, hoverId, targetId):
        s = '$("' + hoverId + '").hover(function () {\n$(' \
            + targetId + ').fadeTo("fast",1.0);\n}, function () {\n$(' \
            + targetId + ').fadeTo("fast",0.75);\n})\n\n'
        return s

    def getClassSets(self, level, classId):
        classSets = []
        for i in range(level+1):   
            classSets.append([])
        classSets[level].append(classId)
        for currLevel in range(level,0,-1):
            classSets[currLevel] = list(set(classSets[currLevel]))
            for classId in classSets[currLevel]:
                subtrees = self.readEquivalenceClass(currLevel,classId)
                for subtree in subtrees:
                    classRefs = self.getClassRefs(subtree['code'])
                    for classIdRef in classRefs:
                        levelRef = self.levelMap[classIdRef]
                        classSets[levelRef].append(classIdRef)
        classSets[0] = list(set(classSets[0]))
        return classSets

    def genCode(self, level, classId):
        s = ''
        divLevel = 1
        classSets = self.getClassSets(level, classId)
        classIdList = []
        for divLevel in range(0,level+1):
            currLevel = level - divLevel
            if len(classSets[currLevel]) == 0:
                continue
            if divLevel == 0:
                s += self.startDiv(1, 'level' + str(divLevel), 'levelzero')
            else:
                s += self.startDiv(1, 'level' + str(divLevel), 'levelmorethanzero')
            for classId in classSets[currLevel]:
                subtrees = self.readEquivalenceClass(currLevel,classId)
                divId = 'class_' + str(classId)
                classIdList.append(classId)
                s += self.startDiv(2, divId, 'equivClass well')
                s += self.startDiv(3, '', 'classId')
                s += self.badge(4, classId)
                s += self.endDiv(3)
                for subtree in subtrees:
                    # subtree['info'], subtree['code']
                    s += self.startDiv(3, 'class_' + str(level) + '_' + str(classId),'')
                    s += self.renderCode(subtree['code'])
                    s += self.endDiv(3)
                s += self.endDiv(2)
            s += self.endDiv(1)
        hierarchy = s
        script = self.scriptSegment('.equivClass','this')
        for classId in classIdList:
            script += self.scriptSegment('.showClass' + str(classId), \
                        '"#class_' + str(classId) + '"')
        return (hierarchy, script)

    def run(self):
        self.readTemplate()
        self.loadAllClasses()
        for level in self.levels:
            print('Level ' + str(level))
            for classId in self.classes[level]:
                (hierarchy, script) = self.genCode(level,classId)
                self.writeToFile(level,classId,hierarchy,script)

if __name__ == '__main__':
    assn = Assignment(1,3)
    CV = CreateVis(assn)
    CV.run()


