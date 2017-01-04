#! /usr/bin/env python

import os,sys
sys.path.append(os.path.abspath('../../'))
from src.util.FileSystem import FileSystem
import src.util.DBSetup
from cwsite.models.models import *
from src.octaveUnitTesting.UnitTester import UnitTester
import logging
import time


class Timer:    
    def __enter__(self):
        self.start = time.clock()
        return self

    def __exit__(self, *args):
        self.end = time.clock()
        self.interval = self.end - self.start

def testProblem(hwId,partId):
    print('Unit testing homework ' + str(hwId) + ', part ' + str(partId))
    logFile = FileSystem.getLogDir() + '/octave_unittesting/log_' + str(hwId) + '_' + str(partId)
    logging.basicConfig(filename = logFile, format = '%(asctime)s %(message)s', \
                    datefmt='%m/%d/%Y %I:%M:%S %p', level=logging.DEBUG)
    
    print('Loading unit testing code')
    tester = UnitTester(hwId,partId)

    print('Loading submissions')
    Submissions = Octave.objects.filter(homework_id = hwId, part_id = partId)
    print('Unit testing started.')
    for submission,i in zip(Submissions,range(len(Submissions))):
        # run unit tests for submission i
        print('Running submission ' + str(i) + ' of ' + str(len(Submissions)))
        tester.refreshWorkingDir()
        tester.loadCode(submission.code)
        with Timer() as t:
            output,correct = tester.run()
        print('\tRequest took %.03f sec.' % t.interval)
        
        
        # commit output to db
        #submission.output = output
        #submission.correct = correct
        ######submission.save()
        logging.debug(report(hwId,partId,i,len(Submissions),correct,submission.id,t.interval))

def report(hwId,partId,progress,total,correct,astid,elapsedTime):
    rpt = '[Homework ' + str(hwId) + ', Part ' + str(partId) + ']: AST ' + str(astid) + ' (' \
            + str(progress) + ' of ' + str(total) + '), time=' + str(elapsedTime) + ', '
    if correct:
        rpt += 'Correct'
    else:
        rpt += 'Incorrect'
    return rpt

def run():
    if len(sys.argv) > 2:
        hwId = int(sys.argv[1])
        partId = int(sys.argv[2])
    else:
        print('expected input: hwId partId')
    testProblem(hwId,partId)

run()

