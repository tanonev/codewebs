import sys
import os.path
sys.path.append(os.path.abspath('../../'))
sys.path.append(os.path.abspath('../../site/cwsite/'))

import src.util.DBSetup
from src.util.FileSystem import FileSystem
from src.util.MLClass import MLClass
from src.util.Assignment import Assignment
from cwsite.models.models import Octave
from operator import itemgetter
import logging

class Runner(object):

    def loadNumSubmissions(self, assn, path):
        logging.info('loadNumSubmissions(' + str(assn) + ')')

        asts = Octave.objects.filter(homework_id = assn.getHomework(), \
                                part_id = assn.getPart()).values('ast_id','coursera_submission_ids')

        numSubmissions = len(asts)*[0]
        cnt = 0
        for ast in asts:
            id = int(ast['ast_id'])
            assert(cnt == id)
            submissions = ast['coursera_submission_ids'][1:-1].split(',')
            numSubmissions[cnt] = len(submissions)
            cnt += 1
        with open(path,'wt') as fid:
            for num,astid in zip(numSubmissions,range(len(numSubmissions))):
                fid.write(str(astid) + ', ' + str(num) + '\n')

    def loadOutputs(self, assn, astOutputPath, mapOutputPath):
        logging.info('loadOutputs(' + str(assn) + ')')

        asts = Octave.objects.filter(homework_id = assn.getHomework(), \
                                part_id = assn.getPart()).values('output')
        outputCounts = {}
        for ast, cnt in zip(asts,range(len(asts))):
            output = ast['output']
            if cnt % 1000 == 0:
                logging.info('Counting... ' + str(cnt) + ' of ' + str(len(asts)))
            try:
                outputCounts[output] += 1
            except KeyError:
                outputCounts[output] = 1
        index, sortedOutputs = self._sortOutputs(outputCounts)
        self.writeASTs(asts, index, astOutputPath)
        self.writeMap(sortedOutputs, mapOutputPath)

    def run(self):
        dirname = 'DumpNumSubmissions'
        FileSystem.initializeLogging(dirname)
    
        outputDir = os.path.join(FileSystem.getDataDir(),dirname)
        if not os.path.exists(outputDir):
            os.makedirs(outputDir)

        for (h,p) in MLClass.allProblems():
            assn = Assignment(h,p)
            path = os.path.join(outputDir,\
                    'NumSubmissions_' + str(assn) + '.txt')
            #self.loadOutputs(assn, astOutputPath, mapOutputPath)
            self.loadNumSubmissions(assn, path)

if __name__ == '__main__':
    Runner().run()

