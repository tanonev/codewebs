#include dbsetup
from PathImporter import PathImporter
PathImporter.importPythonPaths()
import DBSetup
from models.models import *
import sys 
import json
from subprocess import call
from Lca import Lca
import os

HOMEWORK_ID = '1'
PART_ID = '3'
AST_IDS = [0, 3, 10, 14]

class Runner(object):

    def createNewTask(self):
        fullList = Octave.objects.filter(homework_id='1',part_id='3')
        
        task = AnnotationTask.objects.create()
        print 'loaded assignment part'
        for astId in AST_IDS:
            print astId
            octave = Octave.objects.get( \
                homework_id = HOMEWORK_ID, \
                part_id = PART_ID, \
                ast_id = astId \
            )
            task.todo.add(octave)
        task.save()
        return task.id

    def run(self):
        print '-----------------------------------'
        print 'running...'
        taskId = self.createNewTask()
        print('Created task ' + str(taskId))

Runner().run()
