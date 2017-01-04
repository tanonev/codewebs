#if !defined (treenode_distance_h)
#define treenode_distance_h 1

#include "JSONValue.h"

class treenode_distance {
public:
  treenode_distance(JSONValue* rootA, JSONValue* rootB) : rootA(rootA), rootB(rootB) {preprocess();}
  virtual int match_cost(JSONValue* nodeA, JSONValue* nodeB) {return 0;}
  virtual int delete_cost(JSONValue* node) {return 0;}
private:
  virtual void preprocess() {}
  JSONValue* rootA;
  JSONValue* rootB;
};

#endif
