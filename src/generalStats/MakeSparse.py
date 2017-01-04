import sys
import os.path
sys.path.append(os.path.abspath('../../'))

import numpy
import scipy
import mmap

from src.util.FileSystem import FileSystem
from src.util.Distances import Distances

MAX_VALUE = 50

class MakeSparse(object):
    
    def test(self):
        databaseDir = FileSystem.getDistanceMatrixDir()
        newFileName = 'dist_1_1.sparse50.pickle'
        newPath = os.path.join(databaseDir, newFileName)
        d = Distances(newPath)
        
        for i in range(5):
            values = []
            for j in range(5):
                value = 0
                if d.hasDistance(i, j):
                    value = d.getDistance(i, j)
                values.append(value)
            print values
    
    def makeSparse(self, hwPart):
        databaseDir = FileSystem.getDistanceMatrixDir()
        hwString = str(hwPart[0]) + '_' + str(hwPart[1])
        fileName = 'dist_' + hwString + '.txt'
        newFileName = 'dist_' + hwString + '.sparse' + str(MAX_VALUE) + '.pickle'
        matrixFile = open(os.path.join(databaseDir, fileName))
        matrixMap = mmap.mmap(matrixFile.fileno(), 0, access=mmap.ACCESS_READ)
        newPath = os.path.join(databaseDir, newFileName)
        
        d = Distances()
        
        row = 0
        while True:
            line = matrixMap.readline()
            if not line: break
            rowValues = map(int, line.strip().split())
            for col in range(row + 1, len(rowValues)):
                value = rowValues[col]
                if value != -1 and value <= MAX_VALUE:
                    d.add(row, col, value)
            row += 1
        d.save(newPath)
    
    def run(self):
        self.makeSparse((1,3))
            
if __name__ == "__main__":
    MakeSparse().run()
    MakeSparse().test()