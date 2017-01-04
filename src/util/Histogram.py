'''
Created on Nov 23, 2012

@author: Chris
'''

import math

class Histogram(object):
    
    def __init__(self, minPossible, maxPossible, bucketRange):
        self.minPossible = minPossible
        self.maxPossible = maxPossible
        self.bucketRange = bucketRange
        scoreRange = float(maxPossible - minPossible)
        self.numBuckets = int(math.ceil(scoreRange / bucketRange))
        self.buckets = [0 for i in range(self.numBuckets)]
        
    def addValue(self, value):
        bucket = int((value - self.minPossible) / self.bucketRange)
        if bucket < 0:
            raise Exception('bucket is < 0 for value: ' + str(value))
        if bucket >= self.numBuckets:
            raise Exception('bucket is >= ' + str(self.numBuckets) + ' for value: ' + str(value))
        self.buckets[bucket] += 1
        
    def toString(self, name):
        result = ''    
        result += name + ' histogram:' + '\n'
        result += '-------'+ '\n'
        for i in range(len(self.buckets)):
            bucket = self.buckets[i]
            bucketMin = self.minPossible + i * self.bucketRange
            result += str(bucketMin) + ', ' + str(bucket)+ '\n'
        result += '-------'+ '\n'
        return result
    
    def getNumBuckets(self):
        return self.numBuckets
        
    def getBucketCount(self, bucketIndex):
        return self.buckets[bucketIndex]
    
    def getBucketMin(self, bucketIndex):
        return self.minPossible + bucketIndex * self.bucketRange
    
    def getBucketMax(self, bucketIndex):
        return self.getBucketMin(bucketIndex) + self.bucketRange
    
    @staticmethod
    def printHistLazy(diffArray, bucketRange):
        minPossible = min(diffArray) - bucketRange
        maxPossible = max(diffArray) + bucketRange
        Histogram.printSpecificHist(diffArray, minPossible, maxPossible, bucketRange)
    
    @staticmethod
    def getHistAsString(name, values, minPossible, maxPossible, bucketRange):
        maxPossible = max(max(values) + bucketRange, maxPossible)
        scoreRange = float(maxPossible - minPossible)
        numBuckets = int(math.ceil(scoreRange / bucketRange))
        
        buckets = [0 for i in range(numBuckets)]
        for value in values:
            bucket = int((value - minPossible) / bucketRange)
            if bucket < 0:
                raise Exception('bucket is < 0 for value: ' + str(value))
            if bucket >= numBuckets:
                raise Exception('bucket is >= ' + str(numBuckets) + ' for value: ' + str(value))
            buckets[bucket] += 1
        
        result = ''    
        result += name + ' histogram:' + '\n'
        result += '-------'+ '\n'
        for i in range(len(buckets)):
            bucket = buckets[i]
            bucketMin = minPossible + i * bucketRange
            result += str(bucketMin) + '\t' + str(bucket)+ '\n'
        result += '-------'+ '\n'
        return result
    
    @staticmethod
    def printHist(name, values, minPossible, maxPossible, bucketRange):
        string = Histogram.getHistAsString(name, values, minPossible, maxPossible, bucketRange)
        print string
            
            