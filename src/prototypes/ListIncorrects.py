import sys 
import os.path
sys.path.append(os.path.abspath('../../'))
sys.path.append(os.path.abspath('../../site/cwsite'))

import src.util.DBSetup
from src.util.MLClass import MLClass
from src.util.FileSystem import FileSystem
from src.util.AstNetwork import AstNetwork
from models.models import Octave, Original_submissions
import logging

class ListIncorrect(object):

    def writeToFile(self,fname,data):
        logging.info('writeToFile(' + str(fname) + ')')
        fid = open(fname,'wt')
        for x in data:
            fid.write(str(x) + '\n')
        fid.close()
        
    def getSuff(self, pre, label,parts):
        return pre + label + '_' + str(parts[0]) + '_' + str(parts[1]) + '.txt'
    
    def parseSubmissions(self,submissionsStr):
        return [int(x) for x in submissionsStr[1:-1].split(',')]

    def getASTids(self,part,label):
        logging.info('getASTids(' + str(part) + ', ' + str(label) + ')')
        blabel = 0
        if label == 'corrects':
            blabel = 1
        octaveList = Octave.objects.filter(homework_id = part[0], \
                part_id = part[1], \
                correct = blabel).values('ast_id','coursera_submission_ids')
        astList = [ast['ast_id'] for ast in octaveList]
        submissionList = [self.parseSubmissions(ast['coursera_submission_ids']) \
                                    for ast in octaveList]
        return astList, submissionList

    def getSubmissionMap(self,part):
        logging.info('getSubmissionMap(' + str(part) + ')')
        submissionTuples = Original_submissions.objects.filter(\
                            homework_id = part[0], \
                            part_id = part[1]).values('id','proj_id')
        submissionMap = {}
        for s in submissionTuples:
            try:
                submissionMap[s['id']] = int(s['proj_id'])
            except:
                continue
        return submissionMap

    def writeASTs(self, astList, label, parts,dirName):
        logging.info('writeASTs(...'+str(label) + ', ' \
                        + str(parts) + ', ' + str(dirName)+ ')')
        suffASTs = self.getSuff('',label,parts)
        fileNameASTs = os.path.join(dirName,suffASTs)
        self.writeToFile(fileNameASTs, astList)

    def writeNumSubmissions(self, submissionsList, label, parts, dirName):
        logging.info('writeNumSubmissions(...'+str(label) + ', ' \
                        + str(parts) + ', ' + str(dirName)+ ')')
        numSubmissions = [len(x) for x in submissionsList]
        suffSubmissions = self.getSuff('numSubmissions_',label,parts)
        fileNameSubmissions = os.path.join(dirName,suffSubmissions)
        self.writeToFile(fileNameSubmissions, numSubmissions)
        
    def writeUsers(self, submissionsList, label, parts, submissionMap, dirName):
        logging.info('writeUsers(...'+str(label) + ', ' \
                        + str(parts) + ', ' + str(dirName)+ ')')
        numUniqueUsers = []
        for submissions in submissionsList:
            count = len(set([submissionMap[s] for s in submissions \
                                if s in submissionMap]))
            numUniqueUsers.append(count)   
        suffUsers = self.getSuff('numUniqueUsers_',label,parts)
        fileNameUsers = os.path.join(dirName,suffUsers)
        self.writeToFile(fileNameUsers, numUniqueUsers)

    def run(self):
        dirName = os.path.join(FileSystem.getDataDir(),'incorrects')
        if not os.path.exists(dirName):
            os.makedirs(dirName)
        logDirName = os.path.join(FileSystem.getLogDir(), 'incorrects')
        if not os.path.exists(logDirName):
            os.makedirs(logDirName)
            
        logFileName = os.path.join(logDirName,'log')
        logging.basicConfig(filename = logFileName, \
                        format = '%(asctime)s %(message)s', \
                        datefmt = '%m/%d/%Y %I:%M:%S %p', level = logging.INFO)
        logging.info('ListIncorrects()')

        for part in MLClass.allProblems():
            print(part)
            logging.info('Problem ' + str(part))
            incorrectASTs, incorrectSubmissions = \
                                self.getASTids(part, 'incorrects')
            correctASTs, correctSubmissions = self.getASTids(part, 'corrects')

            self.writeASTs(incorrectASTs, 'incorrects', part, dirName)
            self.writeASTs(correctASTs, 'corrects', part, dirName)

            self.writeNumSubmissions(incorrectSubmissions, \
                                        'incorrects', part, dirName)
            self.writeNumSubmissions(correctSubmissions, \
                                        'corrects', part, dirName)

            submissionMap = self.getSubmissionMap(part)
            self.writeUsers(incorrectSubmissions, 'incorrects', \
                                        part, submissionMap, dirName)
            self.writeUsers(correctSubmissions, 'corrects', \
                                        part, submissionMap, dirName)

if __name__ == '__main__':
    ListIncorrect().run()

