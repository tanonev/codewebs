#! /usr/bin/env python

from PathImporter import PathImporter
PathImporter.importPythonPaths()

from RunExternal import RunExternal
import MySQLdb as mdb
import os
import sys
import json
import logging

ASTCMD = '../bin/astgen'
TARCMD = 'tar -czf'
tmpdir = '../log/octtojson/'
datadir = '../data/ast/'

astfilePrefix = 'ast'
octaveSuffix = '.m'
JSONSuffix = '.json'
codeSuffix = '.code'
mapSuffix = '.map'
tarSuffix = '.tar.gz'

MAXTIME = 10 # in seconds

# Needs to be configured to run properly
REMOTEDB = False
if REMOTEDB:
    dbServer = 'galois.stanford.edu'
    dbUser = 'mldebug_remote'
    dbPwd = 'direwolf37*'
    dbName = 'mldebug'
    dbTableName = 'code_submissions'
else:
    #dbUser = 'mldebuguser'
    #dbPwd = 'direwolf37*'
    #dbName = 'mldebug'
    #dbTableName = 'code_submissions'
    dbServer = 'evariste'
    dbUser = 'codewebdb'
    dbPwd = 'n3gr0n1'
    dbName = 'codewebdb'
    dbTableName = 'original_submissions'
    dbServer = 'localhost'
    dbUser = 'jhuang11'
    dbPwd = 'happiness83'
    dbName = 'mlclass'
    dbTableName = 'homework_submissions'

def opendb():
    print('Opening db.')
    con = mdb.connect(dbServer, \
                      dbUser, \
                      dbPwd, \
                      dbName)
    cur = con.cursor()
    connection = (con,cur)
    return connection

def closedb(connection):
    print('Closing db.')
    (con,cur) = connection
    if con:
        con.close()
        cur.close()

def getAllSubmissionIDs(connection,hw_id,part_id):
    (con,cur) = connection
    cur.execute("SELECT id FROM " + dbTableName + " WHERE homework_id = %s AND part_id = %s",\
                    (hw_id,part_id,))
    rows = cur.fetchall()
    submissionids = [int(r[0]) for r in rows]
    return submissionids

def getSource(connection,submissionid):
    (con,cur) = connection
    cur.execute("SELECT source FROM " + dbTableName + " WHERE id = %s", \
                    (submissionid,))
    rows = cur.fetchall()
    sourceStr = sourceStrip(rows[0][0])
    return sourceStr

def sourceStrip(sourceStr):
    idx = sourceStr.find('||||||||')
    return sourceStr[:idx]

def loadJSON(fname):
    fid = open(fname)
    jsonobj = json.load(fid)
    fid.close()
    return jsonobj
  
def writeJSONtoDB(connection,fname_json,submissionid):
    fid = open(fname_json)
    jsonstr = fid.read()
    fid.close()
    (con,cur) = connection
    cur.execute("UPDATE " + dbTableName + " SET json = %s WHERE id = %s",(jsonstr,submissionid,))
    con.commit()

def writeTextFile(s,fname):
    fid = open(fname,'wt')
    fid.write(s)
    fid.close()

def identstr(hw_id,part_id,submission_id):
    return '_' + str(hw_id) + '_' + str(part_id) + '_' + str(submission_id)

# to follow, tail -n 10 -f tmp/log_1_1.log
# to follow multiple logs, multitail -s 2 --closeidle 60 *.log
def report(hw_id,part_id,it,submissions,successcnt,errorcnt):
    rpt  =  '\n+------------------------------\n' \
          + '| Homework: ' + str(hw_id) + ', Part: ' + str(part_id) + '\n' \
          + '| Iteration: ' + str(it) + ' of ' + str(len(submissions)) + '\n' \
          + '| success: ' + str(successcnt) + \
                    ', errors: ' + str(errorcnt) + '\n' \
          + '+------------------------------\n'
    return rpt

def run():  
    if len(sys.argv)>2:
        hw_id = int(sys.argv[1])
        part_id = int(sys.argv[2])
    logfilename = tmpdir + 'log_' + str(hw_id) + '_' + str(part_id) + '.log'
    logging.basicConfig(filename=logfilename, format='%(asctime)s %(message)s', \
                            datefmt='%m/%d/%Y %I:%M:%S %p', level=logging.DEBUG)
    
    # setup
    errorcnt = 0
    successcnt = 0
    connection = opendb()
    submissions = getAllSubmissionIDs(connection,hw_id,part_id) 
    ASTdict = {}
    CodeExamples = {}

    for submission_id,it in zip(submissions,range(len(submissions))):
        filePrefix = tmpdir + astfilePrefix + identstr(hw_id,part_id,submission_id)
        
        # download submission from db and write to temporary file
        src = getSource(connection,submission_id)
        fname_tmp = filePrefix + octaveSuffix
        fid = open(fname_tmp,'wt')
        fid.write(src)
        fid.close()

        # run astgen on temp file and write out more temp files
        fname_json = filePrefix + JSONSuffix
        fname_code = filePrefix + codeSuffix
        fname_map =  filePrefix + mapSuffix
        #outputcode = os.system(ASTCMD + ' ' + fname_tmp + ' ' + fname_json \
        #                + ' ' + fname_code + ' ' + fname_map)
        astgenRunner = RunExternal([ASTCMD, \
                            fname_tmp,fname_json,fname_code,fname_map],MAXTIME)
        astgenRunner.go()
        outputcode = astgenRunner.outputcode        

        if outputcode < 1 and not astgenRunner.killed:
            # make sure json file is less than 1Mb
            # read in json temp file and put it as key in dictionary,
            #   appending submissionid as well as string length of submission to the value
            largeFile = os.path.getsize(fname_json) > 2**20
            if not largeFile:
                astkey = open(fname_json).read()
                src = open(fname_code).read()
                srcLen = len(src)
                try:
                    ASTdict[astkey].append(submission_id)
                    if srcLen < CodeExamples[astkey][0]:
                        charmap = open(fname_map).read()
                        CodeExamples[astkey] = (srcLen,src,charmap)
                except KeyError:
                    ASTdict[astkey] = [submission_id]
                    charmap = open(fname_map).read()
                    CodeExamples[astkey] = (srcLen,src,charmap)
                successcnt += 1
            else:
                errorcnt += 1
            os.system('rm ' + fname_tmp + ' ' + fname_json + ' ' + fname_code + ' ' + fname_map)
        else:
            os.system('rm ' + fname_tmp)
            errorcnt += 1
        if it % 50 == 0:
            logging.debug(report(hw_id,part_id,it,submissions,successcnt,errorcnt))
    closedb(connection)
    logging.debug('Finished.')

    # sort ASTdict by number of submissions matching to each AST
    sortedASTs = [x[0] for x in sorted(ASTdict.items(), key = lambda e: len(e[1]),reverse = True)]

    # For each AST in the dictionary find shortest submission in length
    # write out the AST, submission, and character map
    logging.debug('\n\nWriting ASTs to file.')
    jsondirname = datadir + astfilePrefix + '_' + str(hw_id) + '_' + str(part_id) + '/'
    os.system('mkdir ' + jsondirname) 
    cnt = 0
    for ast in sortedASTs:
        filePrefix = jsondirname + astfilePrefix + '_' + str(cnt)
        output_json = filePrefix + JSONSuffix
        output_code = filePrefix + codeSuffix
        output_map = filePrefix + mapSuffix
        (srcLen,src,charmap) = CodeExamples[ast]
        writeTextFile(ast,output_json)
        writeTextFile(src,output_code)
        writeTextFile(charmap,output_map)
        cnt += 1
    # write out number of distinct asts
    # write out all submissionids and the number of submissions for each ast
    numAST = cnt
    output_ids = jsondirname + 'submissionids.dat'
    fid = open(output_ids,'wt')
    fid.write('Number of ASTs:' + str(numAST) + '\n')
    fid.write('astindex:Number of submissions:submission ids(comma separated)\n')
    cnt = 0
    for ast in sortedASTs:
        idlist = ASTdict[ast]
        fid.write(str(cnt) + ':' + str(len(idlist)) + ':')
        for submission in idlist:
            fid.write(str(submission)+',')
        fid.write('\n')
        cnt += 1
    fid.close()

    logging.debug('Compressing results.')
    astdirname = astfilePrefix + '_' + str(hw_id) + '_' + str(part_id)
    tarfile = astdirname + tarSuffix
    os.system('cd ' + datadir +'; ' + TARCMD + ' ' + tarfile + ' ' + astdirname)
    os.system('rm -rf ' + datadir + astdirname)
    logging.debug('Done.')

    # compress result and delete the things we don't need
    print('\n\nReport (' + str(hw_id) + ', ' + str(part_id) + '):')
    print('success: ' + str(successcnt))
    print('errors: ' + str(errorcnt))

run()



