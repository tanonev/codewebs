'''
Created on Feb 20, 2013

@author: chrispiech
'''

#import termcolor

class WarningManager(object):
    
    printedWarnings = {}
    
    @staticmethod
    def printWarning(warning):
        if not warning in WarningManager.printedWarnings:
            toPrint = '\n'
            toPrint += '####################################\n'
            toPrint += '# Warning: ' + warning +'\n'
            toPrint += '####################################'
            toPrint += '\n'
            
            #print termcolor.colored(toPrint, 'magenta')
            print toPrint
            WarningManager.printedWarnings[warning] = True
        