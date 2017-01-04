#! /usr/bin/env python

from PathImporter import PathImporter
PathImporter.importPythonPaths()

import DBSetup
from MLClass import MLClass
from models.models import *
import os,sys
import operator
import logging

resultsDir = '../results/generalStatistics/'
logDir = '../log/generalStats'

def tallyLines(hw_id,p_id):
    octaveids = [x['id'] for x in \
        Octave.objects.filter(homework_id = hw_id, part_id = p_id).values('id')]
    totalLines = 0
    totalSubmissions = 0
    for idx,cnt in zip(octaveids,range(len(octaveids))):
        if cnt % 100 == 0:
            logging.info('\ttallyLines(hw ' +str(hw_id)+ ',part '+str(p_id)+ '): on AST ' \
                    + str(cnt) + ' of ' + str(len(octaveids)))
        ast = Octave.objects.get(id = idx)
        numLines = len(ast.code.split('\n'))
        numSubmissions = len(ast.coursera_submission_ids[1:-1].split(','))
        totalSubmissions += numSubmissions
        totalLines += numSubmissions*numLines
    return float(totalLines) / totalSubmissions

def HPtableStr(allprobs,T):
    s = ''
    for (h,p) in allprobs:
        s += str(h) + ' & ' + str(p) + ' & ' + str(T[(h,p)]) + ' \\\\\n' 
    return s

def run():

    allprobs = MLClass.allProblems()
    logFileName = os.path.join(logDir,'linecountlog')
    logging.basicConfig(filename = logFileName, format = '%(asctime)s %(message)s', \
                    datefmt = '%m/%d/%Y %I:%M:%S %p', level = logging.INFO)

    logging.info('Tallying average number of lines for each problem...')  
    avgLines = {}
    for (h,p) in allprobs:
        logging.debug('\tOn homework ' + str(h) + ', part ' + str(p))
        hpcount = tallyLines(h,p)
        avgLines[(h,p)] = hpcount
    s = 'Average number of lines per submission for each problem\n'
    s += HPtableStr(allprobs,avgLines)
    with open(os.path.join(resultsDir,'avgNumLinesHP.txt'),'wt') as fid:
        fid.write(s)



run()







