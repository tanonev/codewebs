#! /usr/bin/env python

from PathImporter import PathImporter
PathImporter.importPythonPaths()

import DBSetup
from cwsite.models.models import *
from cwsite.ast.OctaveAST import OctaveAST
import os,sys
from termcolor import colored
import json

hwId = 1
partId = 3
numast = 50

dataDir = '../data/'
annotatedJSONDir = dataDir + 'annotationJson/hw' 
propagatedDir = dataDir + 'annotationJson/propagated/hw'
outputDir = dataDir + 'annotationJson/evaluation'

def listHandAnnotated():
    dirname = annotatedJSONDir + str(hwId) + '_' + str(partId)
    astlist = []
    ls = os.listdir(dirname)
    for fname in ls:
        parts = fname.split('.')
        if len(parts) == 2 and parts[1] == 'json':
            astlist.append(int(parts[0]))
    return astlist

def loadComments(fname):
    try:
        commentjson = json.loads(open(fname).read())
    except:
        return None
    return commentjson['comments']

def findComment(commentId,comments):
    for c in comments:
        if c['comment_id'] == commentId:
            return c['ast_id']
    return None

def codeHighlight(ast,nodeId):
    (startLine,startCol,endLine,endCol) = ast.getCodeBounds(nodeId)
    presnip,snip,postsnip = ast.snippetSplit( \
                                startLine,startCol,endLine,endCol)
    cpre = colored(presnip,'blue')
    csnip = colored(snip,'green',attrs=['bold'])
    cpost = colored(postsnip,'blue')
    print(cpre+csnip+cpost)
                   
def report(annAST,propAST,annNode,propNode,success):
    return str(annAST) + ',' + str(annNode) + ',' + str(propAST) + ',' \
                    + str(propNode) + ',' + success + '\n'

def getUserInput(octaveId,nodeId,octaveast):
    print('\n\nPlease rate as good (g), bad (b), or pass (p)')
    print('\tKeep in mind if the highlighted region does not reasonably correspond to a')
    print('\tregion in the second implementation, then there might not be a highlighted area')
    print('\tin the second region.  This should count as \"good\"')
    kbin = raw_input('? ')
    while kbin != 'g' and kbin != 'b' and kbin != 'p':
        print('Invalid input!')
        kbin = raw_input('? ')
    if kbin == 'p':
        return None
    if kbin == 'g':
        if not nodeId:
            return ('success',-1)
        else:
            return ('success',nodeId)
    print('Please propose a better match.')
    print('\t Go to http://evariste.stanford.edu/mldebug/getNodeId?octaveId='+str(octaveId)+',')
    print('\t  highlight the matching code, and report the resulting node id.')
    print('\t If there is no match, write -1')
    legal = octaveast.allNodes() + [-1]
    kbin = raw_input('? ')
    while (not (kbin.isdigit() or kbin == '-1')) or (int(kbin) not in legal):
        print('Invalid Input!')
        kbin = raw_input('? ')
    return ('failure',int(kbin))

def loadEvalResults(outfile):
    results = {'annAST' : [], 'propAST' : [], 'annNode' : [], 'propNode' : [], 'success': []}
    fid = open(outfile,'r')
    rows = fid.readlines()
    fid.close()
    completedWork = {}
    for row in rows[1:]:
        r = row.split(',')
        results['annAST'].append(int(r[0]))
        results['annNode'].append(int(r[1]))
        results['propAST'].append(int(r[2]))
        results['propNode'].append(int(r[3]))
        results['success'].append(r[4])
        completedWork[(results['annAST'][-1], \
                       results['propAST'][-1], \
                       results['annNode'][-1] )] = results['propNode']
    return results,completedWork

def run():
    annotatedASTs = listHandAnnotated()
    propDir = propagatedDir + str(hwId) + '_' + str(partId)
    annDir = annotatedJSONDir + str(hwId) + '_' + str(partId)
    outfile = os.path.join(outputDir,'eval_' + \
                    str(hwId) + '_' + str(partId) + '.dat')

    if os.path.isfile(outfile):
        evalResults,completedWork = loadEvalResults(outfile)    
        fid = open(outfile,'a')
    else:
        completedWork = {}
        fid = open(outfile,'a')
        fid.write('Annotated AST, Node index in Annotated AST, Propagated AST,Node index in Propagated AST, success/failure\n')
    
    os.system('clear')
    #print(completedWork)
    # loop over propagated
    for j in range(numast):
        fname_prop = os.path.join(propDir,str(j) + '.json')
        propComments = loadComments(fname_prop)
        octave_j = Octave.objects.filter(homework_id = hwId,part_id = partId, \
                                ast_id = j)[0]
        ast_j = OctaveAST(octave_j)
        if propComments == None:
            continue
        # loop over annotated JSON
        for i,iidx in zip(annotatedASTs,range(len(annotatedASTs))):
            fname_ann = os.path.join(annDir,str(i) + '.json')
            annComments = loadComments(fname_ann)
            octave_i = Octave.objects.filter(homework_id = hwId, \
                                part_id = partId, ast_id = i)[0]
            ast_i = OctaveAST(octave_i)
            for comment,commentidx in zip(annComments,range(len(annComments))):
                nodeId_i = comment['ast_id']
                commentId = comment['comment_id']
            
                if (i, j, nodeId_i) in completedWork:
                    continue

                #find the corresponding ast node ids in each ast
                nodeId_j = findComment(commentId,propComments)
                print('On annotated AST #'+ str(iidx+1) + ' of ' + \
                        str(len(annotatedASTs)) + ', (octaveid: ' + str(octave_i.id) + ')')
                print('On propagated AST #'+ str(j+1) + ' of ' + str(numast) + ', (octaveid: '\
                            + str(octave_j.id) + ')')
                print('     + comment ' + str(commentidx) + ' of ' + str(len(annComments)))
                print('')
                print('------------------------------------------------------')
                print('Hand Annotated: (octaveid: ' + str(octave_i.id) + ')')
                codeHighlight(ast_i,nodeId_i)
                print('------------------------------------------------------')
                print('Propagated: (octaveid: ' + str(octave_j.id) + ')')
                codeHighlight(ast_j,nodeId_j)
                result = getUserInput(octave_j.id,nodeId_j,ast_j)
                if result:
                    print('Saving...')
                    fid.write(report(i,j,nodeId_i,result[1],result[0]))
                    fid.flush()
                os.system('clear')
    fid.close()
run()
