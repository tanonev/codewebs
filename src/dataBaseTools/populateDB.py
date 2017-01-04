#! /usr/bin/env python

import os
import sys
sys.path.append(os.path.abspath('../../'))
from src.util.FileSystem import FileSystem
from src.util.MLClass import MLClass
import MySQLdb as mdb
import logging
from operator import itemgetter
import warnings
import cPickle as pk

# The assumption is that the table that is being written to is cleared!

tardir = os.path.join(FileSystem.getDataDir(), 'ast')
logdir = os.path.join(FileSystem.getLogDir(), 'populatedb')

USESKIPLIST = True
#problemList = [(1,1),(1,2),(1,3),(2,6),(4,4)]
 
maxEntries = 300
MAXQUEUESIZE = 100

dbread = {}
dbread['Server'] = 'evariste'
dbread['User'] = 'codewebdb'
dbread['Pwd'] = 'n3gr0n1'
dbread['Name'] = 'codewebdb'
dbread['TableName'] = 'original_submissions'

dbwrite = {}
dbwrite['Server'] = 'evariste'
dbwrite['User'] = 'codewebdb'
dbwrite['Pwd'] = 'n3gr0n1'
dbwrite['Name'] = 'codewebdb'
dbwrite['TableName'] = 'octave'

class MultiInserter(object):
    
    def __init__(self,db,maxQueueSize):
        self.queue = []
        print('Opening database: ' + db['Name'] + '.')
        self.con = mdb.connect(db['Server'],db['User'],db['Pwd'],db['Name'])
        self.cur = self.con.cursor()
        self.db = db
        self.maxQueueSize = maxQueueSize

    def __del__(self):
        print('Flushing...')
        self.flush()
        print('Closing database: ' + self.db['Name'] + '.')
        if self.con:
            self.con.close()
            self.cur.close()

    def add(self, dbentry):
        dbentryTuple = (dbentry['hw_id'],dbentry['part_id'],dbentry['ast_id'],dbentry['codestr'],str(dbentry['idlist']),dbentry['jsonstr'],dbentry['mapstr'],dbentry['output'],dbentry['correct'])
        self.queue.append(dbentryTuple)
        if len(self.queue) == self.maxQueueSize:
            self.flush()

    def flush(self):
        with warnings.catch_warnings():
            warnings.simplefilter('error', mdb.Warning)
            try:
                self.cur.executemany("""INSERT INTO """ + self.db['TableName'] + """ (homework_id,part_id,ast_id,code,coursera_submission_ids,json,map,output,correct) VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s)""",self.queue)
                self.con.commit()
            except mdb.Error, e:
                raise e 
        self.queue = []

def opendb(db):
    print('Opening database: ' + db['Name'] + '.')
    con = mdb.connect(db['Server'],db['User'],db['Pwd'],db['Name'])
    cur = con.cursor()
    db['connection'] = (con,cur)

def closedb(db):
    print('Closing database: ' + db['Name'] + '.')
    (con,cur) = db['connection']
    if con:
        con.close()
        cur.close()

def grabOutput(db, submissionids):
    (con,cur) = db['connection']
    corrects = [0, 0]
    outputs = {}
    for subid in submissionids:
        cur.execute("SELECT output, raw_score FROM " + db['TableName'] + " WHERE id = %s", (subid,))
        r = cur.fetchone()
        try:
            outputs[r[0]] += 1
        except KeyError:
            outputs[r[0]] = 1
        corrects[int(int(r[1])>0)] += 1
    correct = int(corrects[0] < corrects[1])
    output = max(outputs.iteritems(), key = itemgetter(1))[0]
    count = correct
    return output, correct, count

def printEntry(dbentry):
    print('Homework id: ' + str(dbentry['hw_id']))
    print('Part id: ' + str(dbentry['part_id']))
    print('Correct: ' + str(dbentry['correct']))
    print('Number of submissions: ' + str(len(dbentry['idlist'])))

def loadSubmissionsFile(fname):
    submissionids = []
    fid = open(fname)
    rows = fid.readlines()
    for r in rows[2:]:
        tmp = r.split(':')
        astindex = int(tmp[0])
        numsubmissions = int(tmp[1])
        idlist = [int(x) for x in tmp[2].split(',')[:-1]]
        submissionids.append(idlist)
    fid.close()
    return submissionids

def loadTextFile(fname):
    return open(fname).read()

def loadSkipList(fname):
    try:
        fid = open(fname,'r')
    except IOError:
        fid = open(fname,'wt')
        fid.write('')
        fid.close()
        return []
    rows = fid.readlines()
    fid.close()
    pList = []
    for r in rows:
        pList.append(tuple([int(x) for x in r.rstrip(' \n').split()]))
    return pList

def logHwPart(hw_id,part_id,fname):
    fid = open(fname,'a')
    fid.write(str(hw_id) + ' ' + str(part_id) + '\n')
    fid.close()

def report(dbentry,ast_id,numUniqueAST):
    rpt =   '\n+------------------------------------------------\n' \
            + 'Homework: ' + str(dbentry['hw_id']) + ', Part: ' + str(dbentry['part_id']) + '\n' \
            + 'On AST #' + str(ast_id) + ' of ' + str(numUniqueAST) + '\n' \
            + 'Number of matching submissions: ' + str(len(dbentry['idlist'])) + '\n' 
    return rpt

def run(writeToTestDB):
    logfilename = os.path.join(logdir,'log')
    logging.basicConfig(filename = logfilename, format='%(asctime)s %(message)s',\
                    datefmt='%m/%d/%Y %I:%M:%S %p', level=logging.DEBUG)
    
    coarselogfilename = os.path.join(logdir,'coarselog')
    skipList = loadSkipList(coarselogfilename)

    #set up problems and allfiles
    allfiles = []
    for (h,p) in MLClass.allProblems():
        tarfilepath = 'ast_' + str(h) + '_' + str(p) + '.tar.gz'
        allfiles.append(os.path.join(tardir, tarfilepath))

    # open database connections        
    Inserter = MultiInserter(dbwrite,MAXQUEUESIZE)
    opendb(dbread)

    # iterate through files
    for tarfile, prob in zip(allfiles,MLClass.allProblems()):
        # filter out problems that we don't want to expand         
        (hw_id,part_id) = [int(x) for x in prob]
        if USESKIPLIST and (hw_id,part_id) in skipList: 
            continue

        print('Untarring Homework: ' + str(hw_id) + ', Problem: ' + str(part_id))    
        dirname = tarfile[:(-7)]
        
        if not os.path.isdir(os.path.join(tardir,dirname)):
            os.system('tar -xzf ' + tarfile + ' -C ' + tardir)

        submissionsfile = os.path.join(dirname, 'submissionids.dat')
        submissionIDs = loadSubmissionsFile(submissionsfile)
        
        # iterate through each ast id
        for idlist,ast_id in zip(submissionIDs,range(len(submissionIDs))):
            if ast_id % 100 == 0:
                print(str(ast_id) + ' of ' + str(len(submissionIDs)))
            if writeToTestDB == True and ast_id >= maxEntries: 
                break

            # load json, map and code files
            fname_prefix = os.path.join(dirname,'ast_' + str(ast_id))
            fname_json = fname_prefix + '.json'
            fname_map = fname_prefix + '.map'
            fname_code = fname_prefix + '.code'

            # output and correct (grab this from other database)
            dbentry = {}
            dbentry['output'],dbentry['correct'],count = grabOutput(dbread, idlist)
            dbentry['jsonstr'] = loadTextFile(fname_json)
            dbentry['mapstr'] = loadTextFile(fname_map)
            dbentry['codestr'] = loadTextFile(fname_code)
            dbentry['hw_id'] = hw_id
            dbentry['part_id'] = part_id
            dbentry['idlist'] = idlist
            dbentry['ast_id'] = ast_id

            # write to db and log entry
            Inserter.add(dbentry)
            if ast_id % 20 == 0:
                logging.debug(report(dbentry,ast_id,len(submissionIDs)))

        # delete the folder
        # os.system('rm -rf ' + dirname)
        logHwPart(hw_id,part_id,coarselogfilename)

    # close database connections
    closedb(dbread)
    
if len(sys.argv) == 2:
    argstr = sys.argv[1]
if len(sys.argv) != 2 or (argstr != '-test' and argstr != '-full'):
    print('Usage: python populateDB.py [-test, -full]')
    sys.exit(1)
print(argstr)
testoption = False
if argstr == '-test':
    testoption = True
    dbwrite['Name'] = 'codewebdb_test'
run(testoption)

