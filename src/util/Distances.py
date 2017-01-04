import pickle

class Distances(object):
    
    def __init__(self, filePath = None):
        self.distances = {}
        if filePath:
            self.distances = pickle.load(open(filePath))
        
        
    def _getKey(self, id1, id2):
        assert id1 != id2
        maxId = max(id1, id2)
        minId = min(id1, id2)
        return (maxId, minId)
    
    def save(self, filePath):
        pickle.dump(self.distances, open(filePath, 'wb'))
        
    def add(self, id1, id2, value):
        if id1 == id2: return
        key = self._getKey(id1, id2)
        self.distances[key] = value
        
    def getDistance(self, id1, id2):
        if id1 == id2: return 0
        key = self._getKey(id1, id2)
        if not key in self.distances:
            raise Exception('no distance for ' + str(key))
        return self.distances[key]
    
    def hasDistance(self, id1, id2):
        if id1 == id2: return True
        key = self._getKey(id1, id2)
        return key in self.distances