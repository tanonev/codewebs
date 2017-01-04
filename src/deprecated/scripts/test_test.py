#! /usr/bin/env python

from PathImporter import PathImporter
PathImporter.importPythonPaths()
from UnitTester import UnitTester

tester = UnitTester(1,1)
#print(tester.unitTestFile)

codeStr = open('unittestexample.m').read()
tester.loadCode(codeStr)
output,correct = tester.run()


print('Out: ')
print(output)
print('Correct: ')
print(correct)
