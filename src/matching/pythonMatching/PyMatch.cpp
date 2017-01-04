#include <string>
#include <Python.h>
#include <iostream>
#include "Match.h"
using namespace std;

static PyObject * helloWorld(PyObject *self, PyObject *args) {
    cout << "hello world" << endl;
	PyObject* rv = Py_BuildValue("i", 0);
	return rv;
}

static PyObject * getEditDistance(PyObject *self, PyObject *args){
    char * astOneArg;
    char * astTwoArg;
    char * keywordsArg;
    
	if (!PyArg_ParseTuple(args, "sss", &astOneArg, &astTwoArg, &keywordsArg))
		return NULL;

    string astOne = string(astOneArg);
    string astTwo = string(astTwoArg);
    string keywords = string(keywordsArg);

    int distance = getEditDistance(astOne, astTwo, keywords);
    PyObject* rv = Py_BuildValue("i", distance);
	return rv;
}

static PyMethodDef PyMatchMethods[] = {
	{"helloWorld", helloWorld , METH_VARARGS,
		"Print hello world"},
    {"getEditDistance", getEditDistance , METH_VARARGS,
		"Get the AST edit distance."},
	{NULL, NULL, 0, NULL} /* Sentinel */
};

PyMODINIT_FUNC
initPyMatch(void) {
	(void) Py_InitModule("PyMatch", PyMatchMethods);
}
