#include "basic-treenode-distance.h"

int basic_treenode_distance::match_cost(JSONValue* nodeA, JSONValue* nodeB) {
  if (nodeA->AsObject().at(L"type")->AsString() != nodeB->AsObject().at(L"type")->AsString()) return 1000000;
  if (nodeA->AsObject().count(L"name") == 0) return 0;
  if (nodeA->AsObject().at(L"name")->AsString() != nodeB->AsObject().at(L"name")->AsString()) return 1000000;
  return 0;
}

