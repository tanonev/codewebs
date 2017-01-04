#! /usr/bin/env python

#from PathImporter import PathImporter
#PathImporter.importPythonPaths()
import os,sys
sys.path.append(os.path.abspath('../../'))

import src.util.DBSetup
from src.util.MLClass import MLClass
from src.util.FileSystem import FileSystem
from cwsite.models.models import *
import operator
import logging

resultsDir = os.path.join(FileSystem.getResultsDir(),'generalStatistics/')
logDir = os.path.join(FileSystem.getLogDir(),'generalStats/')
allprobs = MLClass.allProblems()

def tallyOutputs(hw_id,p_id):
    submissions = Original_submissions.objects.filter(homework_id = hw_id, part_id = p_id)
    D = {}
    for s in submissions:
        try:
            D[s.output] += 1
        except KeyError:
            D[s.output] = 1
    sortedOutputs = sorted(D.iteritems(), key = operator.itemgetter(1),reverse = True)
    return sortedOutputs

def tallyLines(hw_id,p_id):
    asts = Octave.obj
    ects.filter(homework_id = hw_id,part_id = p_id)
    totalLines = 0
    totalSubmissions = 0
    for ast,astIdx in zip(asts,range(len(asts))):
        if astIdx % 100 == 0:
            logging.debug('\ttallyLines(hw ' +str(hw_id)+ ',part '+str(p_id)+ '): on AST ' \
                                + str(astIdx) + ' of ' + str(len(asts)))
        numLines = len(ast.code.split('\n'))
        numSubmissions = len(ast.coursera_submission_ids[1:-1].split(','))
        totalSubmissions += numSubmissions
        totalLines += numSubmissions*numLines
    return float(totalLines) / totalSubmissions

def tallyUsers(hw_id,p_id):
    submissions = Original_submissions.objects.filter(homework_id = hw_id, part_id = p_id)
    users = set([s.proj_id for s in submissions])
    return len(users)

def allUsers():
    users = [x['proj_id'] for x in Original_submissions.objects.all().values('proj_id')]
    return len(set(users))

def allFinishers():
    S = {}
    users = set([x['proj_id'] for x in \
            Original_submissions.objects.all().values('proj_id')])
    for (h,p) in allprobs:
        S = set([x['proj_id'] for x in \
                            Original_submissions.objects.filter(homework_id = h, \
                            part_id = p).values('proj_id')])
        users = users.intersection(S)
    return len(users)

# make a dictionary with key=output and value is the set of people 
# who submitted something which gave that output at some point
# remember total number of unique users who submitted something for a particular homework
def tallyUsersPerOutput(hw_id,p_id,kmax,numusers = 0,sortedOutputs = []):
    if len(sortedOutputs) == 0:
        sortedOutputs = tallyOutputs(hw_id,p_id)
    if numusers == 0:
        numusers = tallyUsers(hw_id,p_id)
    submissions = Original_submissions.objects.filter(homework_id = hw_id, part_id = p_id)
    D = {}
    for s in submissions:
        try:
            D[s.output].add(s.proj_id)
        except KeyError:
            D[s.output] = set([s.proj_id])
    numcovered = []
    for k in range(1,kmax+1):
        #union all the people outside of 1...k and take the size of this set
        # subtract from numusers ---- this is the number of people covered by top k
        nottopk = [D[x] for (x,y) in sortedOutputs[k:]]
        numcovered.append(numusers - len(set.union(*nottopk)))
    return numcovered

def tallyCorrectOutputs(hw_id,p_id):
    octave = Original_submissions.objects.filter(homework_id = hw_id, part_id = p_id)
    correctSet = set([])
    incorrectSet = set([])
    for s,idx in zip(octave,range(len(octave))):
        #print(str(idx) + ' of ' + str(len(octave)))
        try:
            outputstr = s.output.rstrip()
            if len(outputstr) > 0:
                outvec = str([float(x) for x in outputstr.split(' ')])
            else:
                outvec = str([])
            if s.raw_score > 0:
                correctSet.add(outvec)
            else:
                incorrectSet.add(outvec)
        except ValueError:
            print('ERROR parsing output!')
            print('|' + outputstr + '|')
            #print([float(x) for x in outputstr.split(' ')])
    return correctSet,incorrectSet

def HPtableStr(allprobs,T):
    s = ''
    for (h,p) in allprobs:
        s += str(h) + ' & ' + str(p) + ' & ' + str(T[(h,p)]) + ' \\\\\n' 
    return s

def runHighLevel():
    logging.debug('Computing high level statistics...')
    s = 'Number of homeworks: 8\n'
    s += 'Number of problems: ' + str(len(allprobs)) + '\n'
    numOriginalSubmissions = Original_submissions.objects.count()
    s += 'Number of original submissions: ' + str(numOriginalSubmissions) + '\n'
    numAllUsers = allUsers()
    s += 'Total number of users submitting something: ' + str(numAllUsers) + '\n'  
    numAllFinishers = allFinishers()
    s += 'Total number of users submitting every homework: ' + str(numAllFinishers) + '\n'  

    numASTs = Octave.objects.count()
    s += 'Number of distinct ASTs: ' + str(numASTs) + '\n'
    with open(os.path.join(resultsDir,'general.txt'),'wt') as fid:
        fid.write(s)

def runNumSubmissions(numUsers):
    logging.debug('Tallying number of submissions to each problem...')
    numSubmissions = {}
    numSubmissionsPerUser = {}
    for (h,p) in allprobs:
        hpcount = Original_submissions.objects.filter(homework_id = h, part_id = p).count()
        numSubmissions[(h,p)] = hpcount
        numSubmissionsPerUser[(h,p)] = hpcount / float(numUsers[(h,p)])
    s = 'Number of original submissions for each problem:\n'
    s += HPtableStr(allprobs,numSubmissions)
    with open(os.path.join(resultsDir,'numOriginalSubmissionsHP.txt'),'wt') as fid:
        fid.write(s)
    s = 'Number of original submissions per user for each problem:\n'
    s += HPtableStr(allprobs,numSubmissionsPerUser)
    with open(os.path.join(resultsDir,'numSubmissionsPerUserHP.txt'),'wt') as fid:
        fid.write(s)
    return numSubmissions
 
def runNumASTs():
    logging.debug('Tallying number of distinct ASTs for each problem...')
    T = {}
    for (h,p) in allprobs:
        hpcount = Octave.objects.filter(homework_id = h, part_id = p).count()
        T[(h,p)] = hpcount    
    s = 'Number of distinct ASTs for each problem:\n'
    s += HPtableStr(allprobs,T)
    with open(os.path.join(resultsDir,'numDistinctASTsHP.txt'),'wt') as fid:
        fid.write(s)
    
def runDistinctCorrects():
    logging.debug('Tally number of distinct correct possible outputs for each problem...')
    T = {}
    correctOutputsDir = 'correctOutputs'
    if not os.path.exists(os.path.join(resultsDir,correctOutputsDir)):
        os.makedirs(os.path.join(resultsDir,correctOutputsDir))
    for (h,p) in allprobs:
        correctSet, incorrectSet = tallyCorrectOutputs(h,p)
        T[(h,p)] = len(correctSet)
        s = 'Correct outputs for HW ' + str(h) + ', Part ' + str(p) + ':\n'
        for output in correctSet:
            s += output + '\n'
        with open(os.path.join(resultsDir,correctOutputsDir,'correctOutputs_' \
                        + str(h) + '_' + str(p) + '.txt'),'wt') as fid:
            fid.write(s)
    s = 'Number of distinct correct possible outputs for each problem:\n'
    s += HPtableStr(allprobs,T)
    with open(os.path.join(resultsDir,'correctOutputsHP.txt'),'wt') as fid:
        fid.write(s)

def runNumUsers():
    logging.debug('Tallying number of users who submitted to each problem...')
    numUsers = {}
    for (h,p) in allprobs:
        logging.debug('\tOn homework ' + str(h) + ', part ' + str(p))
        hpcount = tallyUsers(h,p)
        numUsers[(h,p)] = hpcount
    s = 'Number of users who submitted at least one thing for each problem:\n'
    s += HPtableStr(allprobs,numUsers)   
    with open(os.path.join(resultsDir,'numSubmittingUsersHP.txt'),'wt') as fid:
        fid.write(s)
    return numUsers

def runDistinctOutputs():
    logging.debug('Tallying the distinct outputs for each problem...')
    T = {}
    outputs = {}
    outputHistDir = 'outputHists'
    if not os.path.exists(os.path.join(resultsDir,outputHistDir)):
        os.makedirs(os.path.join(resultsDir,outputHistDir))
    for (h,p) in allprobs:
        logging.debug('\tOn homework ' + str(h) + ', part ' + str(p))
        outputs[(h,p)] = tallyOutputs(h,p)
        T[(h,p)]  = len(outputs[(h,p)])
        flippedTally = flipTally(outputs[(h,p)])

        shist = 'Output histogram for HW ' + str(h) + ', Part ' + str(p) + ':\n'
        shist_flipped = 'Flipped tally for HW ' + str(h) + ', Part ' + str(p) + ':\n'
        shist_flipped += '1, 20 means that 20 outputs had 1 user hit them\n'
        for outputtally,i in zip(outputs[(h,p)],range(len(outputs[(h,p)]))):
            shist += str(i) + ',' + str(outputtally[1]) + '\n'
        with open(os.path.join(resultsDir,outputHistDir,'outputHist_' \
                    + str(h) + '_' + str(p) + '.txt'),'wt') as fid:
            fid.write(shist)
        for numUsers,numOutputs in flippedTally:
            shist_flipped += str(numUsers) + ',' + str(numOutputs) + '\n'
        with open(os.path.join(resultsDir,outputHistDir,'outputFlipped_' \
                    + str(h) + '_' + str(p) + '.txt'),'wt') as fid:
            fid.write(shist_flipped)
    s = 'Number of distinct outputs for each part:\n'
    s += HPtableStr(allprobs,T)
    with open(os.path.join(resultsDir,'numDistinctOutputsHP.txt'),'wt') as fid:
        fid.write(s)
    return outputs
       
## find percentage of outputs with 1 user, percentage of outputs with 2 users...
def flipTally(output):
    usercounts = [y for (x,y) in output]
    D = {}
    for c in usercounts:
        try:
            D[c] += 1.0/len(usercounts)
        except KeyError:
            D[c] = 1.0/len(usercounts)
    flippedTally = sorted(D.iteritems(), key = operator.itemgetter(0))
    return flippedTally

def runNumCovered(numUsers,outputs):
    logging.debug('Tallying number of users covered by the top-k outputs of each problem...')
    kmax = 50    
    numCovered = {}
    s = 'Number of users covered by the top-k outputs of each problem:\n'
    for (h,p) in allprobs:
        logging.debug('\tOn homework ' + str(h) + ', part ' + str(p))
        numCovered[(h,p)] = tallyUsersPerOutput(h,p,kmax,numUsers[(h,p)], outputs[(h,p)])       
        for nc,i in zip(numCovered[(h,p)],range(kmax)):
            s += str(h) + ' & ' + str(p) + ' & ' + str(i+1) + ' & ' + \
                    str(nc) + ' & ' + str(numUsers[(h,p)]) + ' \\\\\n'
    with open(os.path.join(resultsDir,'outputCoverage.txt'),'wt') as fid:
        fid.write(s)
    return numCovered

def runCoverageStats(numCovered):
    logging.debug('Summarizing top-k output coverage statistics...')
    fracCovered = {}
    for (h,p) in allprobs:
        fracCovered[(h,p)] = numCovered[(h,p)][-1] / float(numUsers[(h,p)])
    s = ''
    s += HPtableStr(allprobs,fracCovered)
    with open(os.path.join(resultsDir,'fracCoverage.txt'),'wt') as fid:
        fid.write(s)
    logging.debug('Done.')

def run():
    logFileName = os.path.join(logDir,'log')
    logging.basicConfig(filename = logFileName, format = '%(asctime)s %(message)s', \
                    datefmt = '%m/%d/%Y %I:%M:%S %p', level = logging.DEBUG)

    runHighLevel()
    #numUsers = runNumUsers()
    #numSubmissions = runNumSubmissions(numUsers)
    #runNumASTs()
    #runDistinctCorrects()
    #outputs = runDistinctOutputs()
    #numCovered = runNumCovered(numUsers,outputs)
    #runCoverageStats(numCovered)


if __name__ == "__main__":
    run()







