#if !defined (basic_treenode_distance_h)
#define basic_treenode_distance_h 1

#include "treenode-distance.h"

class basic_treenode_distance : treenode_distance {
public:
  basic_treenode_distance(JSONValue* rootA, JSONValue* rootB) : treenode_distance(rootA, rootB) {}
  virtual int match_cost(JSONValue* nodeA, JSONValue* nodeB);
  virtual int delete_cost(JSONValue* node) {return 1;}
private:
  JSONValue* rootA;
  JSONValue* rootB;
};

#endif
