

class Lca(object):

    @staticmethod
    def getLca(jsonTree, startNode, endNode):
        root = jsonTree['root']
        parentMap = Lca._getParentMap(root)

        startPath = Lca._getPathToRoot(startNode, parentMap)
        endPath = Lca._getPathToRoot(endNode, parentMap)

       
        lca = None
        while startPath and endPath:
            startId = startPath.pop()
            endId = endPath.pop()
            if startId != endId:
                break
            lca = startId
            
        assert lca != None

        return lca

    @staticmethod
    def _getPathToRoot(node, parentMap):
        path = []
        while node != None:
            path.append(node)
            node = parentMap[node]
        return path

    @staticmethod
    def _getParentMap(root):
        parentMap = {}
        Lca._populateParentMap(root, None, parentMap)
        return parentMap

    @staticmethod
    def _populateParentMap(node, parentId, parentMap):
        curId = int(node['id'])
        parentMap[curId] = parentId;
        for child in node['children']:
            Lca._populateParentMap(child, curId, parentMap)
    
'''
#include <vector>
#include <list>
#include <iostream>
#include <fstream>
#include <string>

#include "JSON.h"

using namespace std;

typedef vector<int> VI;

JSONValue* readJSON(const char* filename) {
  ifstream in(filename);
  in.seekg(0, ios::end);
  int length = in.tellg();
  in.seekg(0, ios::beg);
  char* buffer = new char[length];
  in.read(buffer, length);
  in.close();
  JSONValue* ans = JSON::Parse(buffer);
  delete[] buffer;
  return ans;
}

int size(JSONValue* root) {
  while (root->AsObject().at(L"children")->AsArray().size() > 0)
    root = root->AsObject().at(L"children")->AsArray().back();
  return static_cast<int>(root->AsObject().at(L"id")->AsNumber() + 1);
}

int id(JSONValue* node) {
  return static_cast<int>(node->AsObject().at(L"id")->AsNumber());
}

void precomputeParent(JSONValue* node, VI& parent, int pid) {
  int curid = id(node);
  parent[curid] = pid;
  const JSONArray& children = node->AsObject().at(L"children")->AsArray();
  for (int i = 0; i < children.size(); i++) precomputeParent(children[i], parent, curid);
}

int main(int argc, char** argv) {
  JSONValue* json = readJSON(argv[1]);
  JSONValue* root = json->AsObject().at(L"root");
  
  int start = atoi(argv[2]);
  int end = atoi(argv[3]);
  
  int n = size(root);
  VI parent = VI(n);
  
  precomputeParent(root, parent, -1);
  
  VI startpath, endpath;
  while (start != -1) {
    startpath.push_back(start);
    start = parent[start];
  }
  while (end != -1) {
    endpath.push_back(end);
    end = parent[end];
  }
  
  int ans = -1;
  while (!startpath.empty() && !endpath.empty() && startpath.back() == endpath.back()) {
    ans = startpath.back();
    startpath.pop_back();
    endpath.pop_back();
  }
  
  cout << ans << endl;
  
  return 0;
}'''
