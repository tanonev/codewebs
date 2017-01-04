#! /usr/bin/env python

import os,sys
sys.path.append(os.path.abspath('../../../../'))
from src.util.FileSystem import FileSystem
from src.util.RunExternal import RunExternal
from src.unitTestServer.CodewebsUnitTestClient import CodewebsUnitTestClient

INJECTPATH = '/home/jhuang11/work/code/codeweb/src/Instrumentation/src'
INJECTEXEC = 'Inject'
UNITTESTERHOST = 'inference.stanford.edu'
HW = 1
PART = 3

class InstrumentedCode(object):

    def instrument(self, fname, outputPath):
        path = os.path.join(FileSystem.getWorkingDir(), self.projectName, 'tmp.m')
        cmd = ['java','-cp',INJECTPATH,INJECTEXEC, fname, path, outputPath]
        injectCmd = RunExternal(cmd, 60, True)
        injectCmd.run()
        errorCode = injectCmd.getErrorCode()
        if errorCode != 0:
            raise Exception('Instrumentation Error!')        
        return path

    def run(self, hw, part):
        self.projectName = 'Instrumentation'
        self.testClient = CodewebsUnitTestClient(UNITTESTERHOST)

        astdir = os.path.join(FileSystem.getAstDir(),'ast_' + str(hw) + '_' + str(part))
        outputDir = os.path.join(FileSystem.getDataDir(),\
                'Instrumentation',str(hw) + '_' + str(part))

        srcFiles = [fname for fname in os.listdir(astdir) if fname[-5:] == '.code']
        for idx, fname in enumerate(srcFiles):
            if idx % 100 == 0:
                print(str(idx) + ' of ' + str(len(srcFiles)))

            astId = fname[4:-5]
            outputPath = os.path.join(outputDir,'ast_' + astId + '.trace')
            fullname = os.path.join(astdir, fname)
            try:
                instrumentedCodePath = self.instrument(fullname, outputPath)
            except:
                print('Instrumentation Error')
                continue
            
            with open(instrumentedCodePath) as fid:
                code = fid.read()
                self.testClient.call(code)
                testResult = self.testClient.wait()
                
if __name__ == '__main__':
    InstrumentedCode().run(HW,PART)



