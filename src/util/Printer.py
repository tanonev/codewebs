import os.path
import sys
sys.path.append(os.path.abspath('../../'))

from FileSystem import FileSystem
from src.util.OctaveAST import OctaveAST
from termcolor import colored

class Printer(object):

    @staticmethod
    def highlightNode(octave, nodeId):
        ast = OctaveAST(octave)
		(startLine, startCol, endLine, endCol) = ast.getCodeBounds(nodeId)
        presnip,snip,postsnip = ast.snippetSplit( \
                                startLine,startCol,endLine,endCol)
        cpre = colored(presnip,'blue')
        csnip = colored(snip,'green',attrs=['bold'])
        cpost = colored(postsnip,'blue')
        print(cpre+csnip+cpost)

    @staticmethod
    def mask(octave, maskMap):
        fgColor = 'green'
        bgColor = 'blue'
        s = ''
        codeLines = octave.code.split('\n')
        for line, maskline in zip(codeLines,maskMap):
            for char,mchar in zip(line,maskline):
                if mchar == 1:
                    s += colored(char, fgColor, attrs = ['bold'])
                else:
                    s += colored(char, bgColor)
            s += '\n'
        print(s)
        

