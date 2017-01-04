#include <vector>
#include <list>
#include <set>
#include <iostream>
#include <fstream>
#include <string>
#include <sstream>
#include <cstring>

#include "JSON.h"

using namespace std;

// if ref is false then we're mapping node a to node b
// otherwise we're referring to the mapping from subtree a to subtree b
struct mapT {
  int a;
  int b;
  bool ref;
};

enum matchT {DELA, DELB, MATCH};
struct resultT {
  resultT(matchT match = MATCH, int cost = 1000000) : match(match), cost(cost), map() {}
  matchT match;
  int cost;
  vector<mapT> map;
};

enum typeT {ANON_FCN_HANDLE, ARGUMENT_LIST, BINARY_EXP, BREAK, COLON, CONTINUE, GLOBAL, STATIC, DECL_ELT, DECL_INIT_LIST, SIMPLE_FOR, COMPLEX_FOR, USER_SCRIPT, USER_FCN, FCN_DEF, IDENT, IF_CLAUSE, IF_COMMAND, IF_COMMAND_LIST, INDEX_EXP, MATRIX, CELL, MULTI_ASSIGN, NO_OP, CONST, FCN_HANDLE, PARAM_LIST, POSTFIX, PREFIX, RETURN, RETURN_LIST, ASSIGN, STATEMENT, STATEMENT_LIST, SWITCH, SWITCH_CASE, SWITCH_CASE_LIST, TRY_CATCH, UNWIND_PROTECT, WHILE, DO_UNTIL, FCN_HANDLE_BODY, VARARGOUT, VARARGIN};

struct nodeT {
  int id;
  typeT type;
  wstring name;
  int leaf;
  bool key;
  int size;
};

typedef vector<resultT> VR;
typedef vector<VR> VVR;
typedef vector<int> VI;
typedef vector<VI> VVI;
typedef vector<JSONValue*> VJ;
typedef vector<nodeT> VN;
typedef vector<VN> VVN;

VVR dp, dp2;
VVN post;
VVN pre;

set<wstring> important;

JSONValue* readJSON(const char* filename) {
  ifstream in(filename);
  in.seekg(0, ios::end);
  int length = in.tellg();
  in.seekg(0, ios::beg);
  char* buffer = new char[length+1];
  in.read(buffer, length);
  in.close();
  buffer[length] = '\0';
  JSONValue* ans = JSON::Parse(buffer);
  delete[] buffer;
  return ans;
}

int getSize(JSONValue* root) {
  while (root->AsObject().at(L"children")->AsArray().size() > 0)
    root = root->AsObject().at(L"children")->AsArray().back();
  return static_cast<int>(root->AsObject().at(L"id")->AsNumber() + 1);
}

int precomputeSize(JSONValue* node, VI& size) {
  int ans = 1;
  const JSONArray& children = node->AsObject().at(L"children")->AsArray();
  for (int i = 0; i < children.size(); i++) ans += precomputeSize(children[i], size);
  size.push_back(ans);
  return ans;
}

nodeT makeNode(JSONValue* node, int leaf, bool key, int size) {
  nodeT n;
  wstring type = node->AsObject().at(L"type")->AsString();
  
  if (type == L"ANON_FCN_HANDLE") n.type = ANON_FCN_HANDLE;
  if (type == L"ARGUMENT_LIST") n.type = ARGUMENT_LIST;
  if (type == L"BINARY_EXP") n.type = BINARY_EXP;
  if (type == L"BREAK") n.type = BREAK;
  if (type == L"COLON") n.type = COLON;
  if (type == L"CONTINUE") n.type = CONTINUE;
  if (type == L"GLOBAL") n.type = GLOBAL;
  if (type == L"STATIC") n.type = STATIC;
  if (type == L"DECL_ELT") n.type = DECL_ELT;
  if (type == L"DECL_INIT_LIST") n.type = DECL_INIT_LIST;
  if (type == L"SIMPLE_FOR") n.type = SIMPLE_FOR;
  if (type == L"COMPLEX_FOR") n.type = COMPLEX_FOR;
  if (type == L"USER_SCRIPT") n.type = USER_SCRIPT;
  if (type == L"USER_FCN") n.type = USER_FCN;
  if (type == L"FCN_DEF") n.type = FCN_DEF;
  if (type == L"IDENT") n.type = IDENT;
  if (type == L"IF_CLAUSE") n.type = IF_CLAUSE;
  if (type == L"IF_COMMAND") n.type = IF_COMMAND;
  if (type == L"IF_COMMAND_LIST") n.type = IF_COMMAND_LIST;
  if (type == L"INDEX_EXP") n.type = INDEX_EXP;
  if (type == L"MATRIX") n.type = MATRIX;
  if (type == L"CELL") n.type = CELL;
  if (type == L"MULTI_ASSIGN") n.type = MULTI_ASSIGN;
  if (type == L"NO_OP") n.type = NO_OP;
  if (type == L"CONST") n.type = CONST;
  if (type == L"FCN_HANDLE") n.type = FCN_HANDLE;
  if (type == L"PARAM_LIST") n.type = PARAM_LIST;
  if (type == L"POSTFIX") n.type = POSTFIX;
  if (type == L"PREFIX") n.type = PREFIX;
  if (type == L"RETURN") n.type = RETURN;
  if (type == L"RETURN_LIST") n.type = RETURN_LIST;
  if (type == L"ASSIGN") n.type = ASSIGN;
  if (type == L"STATEMENT") n.type = STATEMENT;
  if (type == L"STATEMENT_LIST") n.type = STATEMENT_LIST;
  if (type == L"SWITCH") n.type = SWITCH;
  if (type == L"SWITCH_CASE") n.type = SWITCH_CASE;
  if (type == L"SWITCH_CASE_LIST") n.type = SWITCH_CASE_LIST;
  if (type == L"TRY_CATCH") n.type = TRY_CATCH;
  if (type == L"UNWIND_PROTECT") n.type = UNWIND_PROTECT;
  if (type == L"WHILE") n.type = WHILE;
  if (type == L"DO_UNTIL") n.type = DO_UNTIL;
  if (type == L"FCN_HANDLE_BODY") n.type = FCN_HANDLE_BODY;
  if (type == L"VARARGOUT") n.type = VARARGOUT;
  if (type == L"VARARGIN") n.type = VARARGIN;
  
  if (node->AsObject().count(L"name") == 0) {
    n.name = L"";
  } else {
    n.name = node->AsObject().at(L"name")->AsString();
  }
  
  if (n.type == IDENT && important.find(n.name) == important.end()) n.name = L"";
  
  n.id = static_cast<int>(node->AsObject().at(L"id")->AsNumber());
  
  n.leaf = leaf;
  n.key = key;
  n.size = size;
  
  return n;
}

int postorder(JSONValue* node, VN& pre, VN& post, bool isKey) {
  pre.push_back(makeNode(node, 0, false, 0));
  const JSONArray& children = node->AsObject().at(L"children")->AsArray();
  int ans = -1;
  int size = 1;
  for (int i = 0; i < children.size(); i++) {
    int tmp = postorder(children[i], pre, post, i > 0);
    size += post[post.size() - 1].size;
    if (ans == -1) ans = tmp;
  }
  if (ans == -1) ans = post.size();
  post.push_back(makeNode(node, ans, isKey, size));
  return ans;
}

inline int match(const nodeT& a, const nodeT& b) {
  if (a.type != b.type) return 1000000;
  if (a.name != b.name) return 1;
  return 0;
}

void matchString(const VN& A, const VN& B) {
  int a = A.size(), b = B.size();
  dp2[0][0].cost = 0;
  for (int i = 1; i <= a; i++) {
    dp2[i][0].cost = dp2[i-1][0].cost + 1;
    dp2[i][0].match = DELA;
  }
  for (int j = 1; j <= b; j++) {
    dp2[0][j].cost = dp2[j-1][0].cost + 1;
    dp2[0][j].match = DELB;
  }
  
  for (int i = 1; i <= a; i++) {
    for (int j = 1; j <= b; j++) {
      dp2[i][j].cost = dp2[i-1][j].cost + 1;
      dp2[i][j].match = DELA;
      if (dp2[i][j].cost > dp2[i][j-1].cost + 1) {
        dp2[i][j].cost = dp2[i][j-1].cost + 1;
        dp2[i][j].match = DELB;
      }
      int m = match(A[i-1], B[j-1]);
      if (dp2[i][j].cost > dp2[i-1][j-1].cost + m) {
        dp2[i][j].cost = dp2[i-1][j-1].cost + m;
        dp2[i][j].match = MATCH;
      }
    }
  }
}

void matchTree(const VN& A, const VN& B, int a, int b) {
  dp2[0][0].cost = 0;
  int leafA = A[a].leaf, leafB = B[b].leaf;
  for (int i = leafA; i <= a; i++) {
    dp2[i-leafA+1][0].cost = dp2[i-leafA][0].cost + 1;
    dp2[i-leafA+1][0].match = DELA;
  }
  for (int j = leafB; j <= b; j++) {
    dp2[0][j-leafB+1].cost = dp2[0][j-leafB].cost + 1;
    dp2[0][j-leafB+1].match = DELB;
  }
  
  for (int i = leafA; i <= a; i++) {
    for (int j = leafB; j <= b; j++) {
      int ai = i - leafA + 1, bj = j - leafB + 1;
      int leafI = A[i].leaf, leafJ = B[j].leaf;
      if (leafI == leafA && leafJ == leafB) {
        dp2[ai][bj].cost = dp2[ai-1][bj].cost + 1;
        dp2[ai][bj].match = DELA;
        if (dp2[ai][bj].cost > dp2[ai][bj-1].cost + 1) {
          dp2[ai][bj].cost = dp2[ai][bj-1].cost + 1;
          dp2[ai][bj].match = DELB;
        }
        int m = match(A[i], B[j]);
        if (dp2[ai][bj].cost > dp2[ai-1][bj-1].cost + m) {
          dp2[ai][bj].cost = dp2[ai-1][bj-1].cost + m;
          dp2[ai][bj].match = MATCH;
        }
        dp[i][j] = dp2[ai][bj];
      } else {
        dp2[ai][bj].cost = dp2[ai-1][bj].cost + 1;
        dp2[ai][bj].match = DELA;
        if (dp2[ai][bj].cost > dp2[ai][bj-1].cost + 1) {
          dp2[ai][bj].cost = dp2[ai][bj-1].cost + 1;
          dp2[ai][bj].match = DELB;
        }
        if (dp2[ai][bj].cost > dp2[leafI - leafA][leafJ - leafB].cost + dp[i][j].cost) {
          dp2[ai][bj].cost = dp2[leafI - leafA][leafJ - leafB].cost + dp[i][j].cost;
          dp2[ai][bj].match = MATCH;
        }
      }
    }
  }
  
  for (int ci = leafA; ci <= a; ci++) {
    for (int cj = leafB; cj <= b; cj++) {
      if (A[ci].leaf != leafA || B[cj].leaf != leafB) continue;
      int i = ci, j = cj;
      while (i >= leafA || j >= leafB) {
        int ai = i - leafA + 1, bj = j - leafB + 1;
        switch (dp2[ai][bj].match) {
          case DELA: i--; break;
          case DELB: j--; break;
          case MATCH:
            mapT mapEntry;
            if (A[i].leaf == leafA && B[j].leaf == leafB && i == ci && j == cj) {
              mapEntry.a = A[i].id;
              mapEntry.b = B[j].id;
              mapEntry.ref = false;
              i--;
              j--;
            } else {
              mapEntry.a = i;
              mapEntry.b = j;
              mapEntry.ref = true;
              i -= A[i].size;
              j -= B[j].size;
            }
            dp[ci][cj].map.push_back(mapEntry);
        }
      }
    }
  }
}

void populateMatching(map<int, int> &matching, resultT &res) {
  for (unsigned int i = 0; i < res.map.size(); i++) {
    if (res.map[i].ref) {
      populateMatching(matching, dp[res.map[i].a][res.map[i].b]);
    } else {
      matching[res.map[i].a] = res.map[i].b;
    }
  }
}

int main(int argc, char** argv) {
  int ind[4] = {0, 3, 10, 14};
  
  if (false) {
    cout << "Usage: batch-matching matchtype base keywords blocksize rowblock colblock" << endl;
    return 1;
  }
  
  string matchtype(argv[1]);
  
  wifstream keywords(argv[3]);
  wstring keyword;
  while (keywords >> keyword) important.insert(keyword);
  keywords.close();
  
  VJ json(5), root(5);
  VI n(5);
  post = VVN(5);
  pre = VVN(5);
  
  for (int i = 0; i < 4; i++) {
    stringstream str;
    str << argv[2] << ind[i] << ".json";
    json[i] = readJSON(str.str().c_str());
    root[i] = json[i]->AsObject().at(L"root");
    n[i] = getSize(root[i]);
    postorder(root[i], pre[i], post[i], true);
  }
  
  dp = VVR(1000, VR(1000));
  dp2 = VVR(1001, VR(1001));
  
  for (int A = 0; A < 4; A++) {
    for (int B = 0; B < 25407; B++) {
      if (B % 1000 == 0) {
        cout << A << " " << B << endl;
      }
      stringstream str;
      str << argv[2] << B << ".json";
      json[4] = readJSON(str.str().c_str());
      if (json[4] == NULL) {
        cerr << "skipping file " << B << endl;
        continue;
      }
      root[4] = json[4]->AsObject().at(L"root");
      n[4] = getSize(root[4]);
      if (n[4] > 1000) cerr << "PROBLEM" << endl;
      
      post[4].clear();
      pre[4].clear();
      postorder(root[4], pre[4], post[4], true);
      
      for (int i = 0; i < n[A]; i++) {
        if (!post[A][i].key) continue;
        for (int j = 0; j < n[4]; j++) {
          if (!post[4][j].key) continue;
          matchTree(post[A], post[4], i, j);
        }
      }
      int ans = dp[n[A]-1][n[4]-1].cost;
      
      map<int, int> matching;
      populateMatching(matching, dp[n[A]-1][n[4]-1]);
      stringstream ostr;
      ostr << "/home/andy/ast_1_3/" << ind[A] << "/" << B << ".matching";
      ofstream fout(ostr.str().c_str());
      fout << ans << " " << matching.size() << endl;

      for (map<int, int>::iterator it = matching.begin(); it != matching.end(); ++it) {
        fout << it->first << " " << it->second << endl;
      }
      
      fout.close();
      
      delete json[4];
    }
  }
  
  return 0;
}

