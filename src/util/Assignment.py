# this class will hold and process hw part information
class Assignment(object):

    def __init__(self, hwNum, partNum):
        self.homework = hwNum
        self.part = partNum
        
    def getPart(self):
        return self.part
    
    def getHomework(self):
        return self.homework
    
    def __str__(self):
        return str(self.homework) + '_' + str(self.part)

    def __eq__(self, other):
        if self.homework != other.homework:
            return False
        if self.part != other.part:
            return False
        return True

    def __ne__(self, other):
        return not self == other

    def __hash__(self):
        return hash(str(self))
    
    def getTuple(self):
        return (self.homework, self.part)

    @staticmethod
    def allAssignments():
        assns = []
        assnTuples = [(1,i) for i in range(1,8)] + [(2,i) for i in range(1,7)] + [(3,i) for i in range(1,5)] + [(4,i) for i in range(1,6)] + [(5,i) for i in range(1,6)] + [(6,i) for i in range(1,5)] + [(7,i) for i in range(1,6)] + [(8,i) for i in range(1,7)]
        for assnTuple in assnTuples:
            homeworkId = assnTuple[0]
            partId = assnTuple[1]
            assns.append(Assignment(homeworkId, partId))
        return assns
