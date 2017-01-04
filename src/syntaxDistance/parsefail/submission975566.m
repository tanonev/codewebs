#!/usr/bin/python
# coding=utf-8
from __future__ import print_function, division
import numpy as np
from Utils import *

partNames = ['Warm up exercise ',
             'Computing Cost (for one variable)',
             'Gradient Descent (for one variable)',
             'Feature Normalization',
             'Computing Cost (for multiple variables)',
             'Gradient Descent (for multiple variables)',
             'Normal Equations']


def warmUpExercise():
    return np.eye(5)

def computeCost(X, y, theta):
    m = len(y)
    J = 1/(2*m) * np.sum((X.dot(theta) - y)**2)
    return J
    
def gradientDescent(X, y, theta, alpha, num_iters):
    m = len(y)
    J_history = np.zeros((num_iters, 1))
    theta_History = []
    theta = theta.reshape(-1,1)
    for i in range(num_iters):
        theta_History.append(theta.flatten())
        theta -= alpha / m * X.T.dot(X.dot(theta) - y)
        J_history[i] = computeCost(X, y, theta)
        
    return theta, theta_History
    
def featureNormalize(X):
    mean = np.mean(X, 0)
    sigma = np.std(X, 0, ddof=1)
    return (X-mean)/sigma
 
def computeCostMulti(X, y, theta):
    m = len(y)
    J = 1/(2*m) * np.sum((X.dot(theta) - y)**2)
    return J
    
def gradientDescentMulti(X, y, theta, alpha, num_iters):
    m = len(y)
    J_history = np.zeros((num_iters, 1))
    for i in range(num_iters):
        theta -= alpha / m * X.T.dot(X.dot(theta) - y)
        J_history[i] = computeCost(X, y, theta)
    return theta
  
def normalEqn(X, y):
    return np.linalg.pinv(X.T.dot(X)).dot(X.T).dot(y)

def output(partId):
    # Random Test Cases
    X1 = np.array([[1, np.exp(1) + np.exp(2)*i/10] for i in range(1,21)])
    Y1 = (X1[:,1] + np.sin(X1[:,0]) + np.cos(X1[:,1])).reshape(20,1)
    X2 = np.hstack((X1, X1[:,np.newaxis,1]**0.5, X1[:,np.newaxis,1]**0.25))
    Y2 = Y1**0.5 + Y1
    theta1 = np.array([[0.5],[-0.5]])
    theta2 = np.array([[0.1],[0.2],[0.3],[0.4]])
    result = None
    if not partId:
        result = warmUpExercise()
    elif partId == 1:
        result = computeCost(X1, Y1, theta1)
    elif partId == 2:
        result = gradientDescent(X1, Y1, theta1, 0.01, 10)[0]
    elif partId == 3:
        result = featureNormalize(X2[:,1:4])
    elif partId == 4:
        result = computeCostMulti(X2, Y2, theta2)
    elif partId == 5:
        result = gradientDescentMulti(X2, Y2, -theta2, 0.01, 10)
    elif partId == 6:
        result = normalEqn(X2, Y2)

    return printLotsOfFloats(result)