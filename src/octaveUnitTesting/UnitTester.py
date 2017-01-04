import os.path
import sys
sys.path.append(os.path.abspath('../../'))

import stat
import shutil
import subprocess
import logging

from src.util.FileSystem import FileSystem
from src.util.RunExternal import RunExternal

# We assume that unit tests are indeed independent of each other
# 
# the way this is written, you can't unit test
# the same problem in parallel
class UnitTester(object):
    MAXTIME = 5 # seconds before killing executable

    def __init__(self, hwId, partId):
        self.hwId = hwId
        self.partId = partId
        self.dataDir = os.path.join(FileSystem.getDataDir(),'octave_unittest/mlclass-ex' + str(self.hwId))
        #print(self.dataDir)
        assert(os.path.exists(self.dataDir))
        self._loadCorrect()
        self.workingDir = os.path.join(FileSystem.getWorkingDir(),'unitTesting_' + str(self.hwId) + '_' + str(self.partId))
        self.createWorkingDir()
        self.refreshWorkingDir()
        self._writeUnitTestScript()
        self._writeUnitTestFilesScript()
        #print(self.fileScriptName)
        self.unitTestFile = self.workingDir + '/' + self.getUnitTestFile()

    def createWorkingDir(self):
        if not os.path.exists(self.workingDir):
            os.makedirs(self.workingDir)

    def refreshWorkingDir(self):
        allFiles = os.listdir(self.dataDir)
        for f in allFiles:
            fullFile = os.path.join(self.dataDir, f)
            if (os.path.isfile(fullFile)):
                shutil.copy(fullFile, self.workingDir)
        self.isClean = True
        self.codeLoaded = False

    def loadCode(self,codeStr):
        assert(self.isClean)
        fid = open(self.unitTestFile,'wt')
        fid.write(codeStr.encode('utf8'))
        fid.close()
        self.codeLoaded = True
        self.isClean = False

    def run(self):
        assert(self.codeLoaded)
        runner = RunExternal([self.scriptName],UnitTester.MAXTIME,pipeOption = True)
        runner.go()
        if runner.killed == True:
            logging.debug('Process took too long, killed.')
            return ('',False)
        outLines = runner.outLines
        self.codeLoaded = False
        if len(outLines) < 2:
            self.codeLoaded = False
            return ('',False)
        output = outLines[-2]
        lastLine = outLines[-1]
        if lastLine != 'result':
            return ('',False)
        correct = self.checkCorrect(output)
        return output,correct
    
    def checkCorrect(self,output):
        #print(output)
        try:
            if [float(x) for x in output.rstrip(' \n').split(' ')] == self.correct:
                return True
        except:
            return False
        return False

    def _writeUnitTestScript(self):
        self.scriptName = self.workingDir + '/unittestscript.sh'
        fid = open(self.scriptName,'wt')
        fid.write('#! ' + FileSystem.getOctave() + ' -qf\n')
        fid.write('addpath(\"' + self.workingDir + '\");\n')
        fid.write('output = unittest(' + str(self.partId) + ', false);\n')
        fid.write('printf(output);\n')
        fid.write('printf(\'\\nresult\');\n')
        fid.write('return\n')
        fid.close()
        self._makeExecutable(self.scriptName)

    def _makeExecutable(self,fname):
        st = os.stat(fname)
        os.chmod(fname, st.st_mode | stat.S_IEXEC)

    def _writeUnitTestFilesScript(self):
        self.fileScriptName = self.workingDir + '/unittestfilesscript.sh'
        fid = open(self.fileScriptName,'wt')
        fid.write('#! ' + FileSystem.getOctave() + ' -qf\n')
        fid.write('addpath(\"' + self.workingDir + '\");\n')
        fid.write('fname = unittest(' + str(self.partId) + ', true);\n')
        fid.write('printf(fname);')
        fid.write('return\n')
        fid.close()
        self._makeExecutable(self.fileScriptName)
    
    def getUnitTestFile(self):
        p = subprocess.Popen(self.fileScriptName, bufsize = -1,stdout = subprocess.PIPE)
        fname = p.stdout.read()
        return fname

    def _loadCorrect(self):
        fname = self.dataDir + '/correct.txt'
        assert(os.path.exists(fname))
        fid = open(fname)
        rows = fid.readlines()
        self.correct = [float(x) for x in rows[self.partId-1].rstrip(' \n').split(' ')]
        fid.close()



