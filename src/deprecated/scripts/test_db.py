#! /usr/bin/env python

from PathImporter import PathImporter
PathImporter.importPythonPaths()

import DBSetup
from models.models import *
import sys

print('database test')

L = Logs.objects.all()

# print remote_addr for every visitor to site
for l in L:
    print(l.remote_addr)

# get all submissions for a particular hw problem
hwId = 1
partId = 1
Submissions = Octave.objects.filter(homework_id = hwId, part_id = partId)
for s in Submissions:
    print(s.code)

# get all comments associated with a particular ast_id
astId = 1
CommentList = Comments.objects.filter(ast_id = astId)
for c in CommentList:
    print('--------------------------------------------')
    print(c.codesnippet)
    print(c.text)

# make changes to db: 
commentId = 26
CommentToChange = Comments.objects.get(id = commentId)
print(CommentToChange.text)
CommentToChange.text = 'test'
#CommentToChange.save()


