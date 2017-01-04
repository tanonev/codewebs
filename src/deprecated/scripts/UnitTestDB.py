#! /usr/bin/env python

from PathImporter import PathImporter
PathImporter.importPythonPaths()

from FileSystem import FileSystem
import DBSetup
from models.models import *
from UnitTester import UnitTester
import logging
import sys

def testProblem(hwId,partId):
    print('Unit testing homework ' + str(hwId) + ', part ' + str(partId))
    logFile = FileSystem.getLogDir() + '/octave_unittesting/log_' + str(hwId) + '_' + str(partId)
    logging.basicConfig(filename = logFile, format = '%(asctime)s %(message)s', \
                    datefmt='%m/%d/%Y %I:%M:%S %p', level=logging.DEBUG)
    
    print('Loading unit testing code')
    tester = UnitTester(hwId,partId)

    print('Loading submissions')
    Submissions = Octave.objects.filter(homework_id = hwId, part_id = partId)
    for submission,i in zip(Submissions,range(len(Submissions))):
        # run unit tests for submission i
        print('Running submission ' + str(i) + ' of ' + str(len(Submissions)))
        tester.refreshWorkingDir()
        tester.loadCode(submission.code)
        output,correct = tester.run()
        
        # commit output to db
        submission.output = output
        submission.correct = correct
        ######submission.save()
        logging.debug(report(hwId,partId,i,len(Submissions),correct,submission.id))

def report(hwId,partId,progress,total,correct,astid):
    rpt = '[Homework ' + str(hwId) + ', Part ' + str(partId) + ']: AST ' + str(astid) + ' (' \
            + str(progress) + ' of ' + str(total) + '), '
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

