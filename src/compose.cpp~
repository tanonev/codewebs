#include <vector>
#include <list>
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
  resultT(matchT match = MATCH, int cost = 1000000) : match(match), cost(cost)/*, map()*/ {}
  matchT match;
  int cost;
//  vector<mapT> map;
};

enum typeT {ANON_FCN_HANDLE, ARGUMENT_LIST, BINARY_EXP, BREAK, COLON, CONTINUE, GLOBAL, STATIC, DECL_ELT, DECL_INIT_LIST, SIMPLE_FOR, COMPLEX_FOR, USER_SCRIPT, USER_FCN, FCN_DEF, IDENT, IF_CLAUSE, IF_COMMAND, IF_COMMAND_LIST, INDEX_EXP, MATRIX, CELL, MULTI_ASSIGN, NO_OP, CONST, FCN_HANDLE, PARAM_LIST, POSTFIX, PREFIX, RETURN, RETURN_LIST, ASSIGN, STATEMENT, STATEMENT_LIST, SWITCH, SWITCH_CASE, SWITCH_CASE_LIST, TRY_CATCH, UNWIND_PROTECT, WHILE, DO_UNTIL, FCN_HANDLE_BODY, VARARGOUT, VARARGIN};

struct nodeT {
  int id;
  typeT type;
  wstring name;
  int leaf;
  bool key;
};

typedef vector<resultT> VR;
typedef vector<VR> VVR;
typedef vector<int> VI;
typedef vector<VI> VVI;
typedef vector<JSONValue*> VJ;
typedef vector<nodeT> VN;
typedef vector<VN> VVN;

VVR dp, dp2;
VVI size;
VVN post;
VVN pre;

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

nodeT makeNode(JSONValue* node, int leaf, bool key) {
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
  
  n.id = static_cast<int>(node->AsObject().at(L"id")->AsNumber());
  
  n.leaf = leaf;
  n.key = key;
  
  return n;
}

int postorder(JSONValue* node, VN& pre, VN& post, bool isKey) {
  pre.push_back(makeNode(node, 0, false));
  const JSONArray& children = node->AsObject().at(L"children")->AsArray();
  int ans = -1;
  for (int i = 0; i < children.size(); i++) {
    int tmp = postorder(children[i], pre, post, i > 0);
    if (ans == -1) ans = tmp;
  }
  if (ans == -1) ans = post.size();
  post.push_back(makeNode(node, ans, isKey));
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
  
/*  for (int ci = leafA; ci <= a; ci++) {
    for (int cj = leafB; cj <= b; cj++) {
      if (post[A][ci].leaf != leafA || post[B][cj].leaf != leafB) continue;
      int i = ci, j = cj;
      while (i >= leafA || j >= leafB) {
        int ai = i - leafA + 1, bj = j - leafB + 1;
        switch (dp2[ai][bj].match) {
          case DELA: i--; break;
          case DELB: j--; break;
          case MATCH:
            mapT mapEntry;
            if (post[A][i].leaf == leafA && post[B][j].leaf == leafB && i == ci && j == cj) {
              mapEntry.a = post[A][i].id;
              mapEntry.b = post[B][j].id;
              mapEntry.ref = false;
              i--;
              j--;
            } else {
              mapEntry.a = i;
              mapEntry.b = j;
              mapEntry.ref = true;
              i -= size[A][i];
              j -= size[B][j];
            }
            dp[ci][cj].map.push_back(mapEntry);
        }
      }
    }
  }*/
}
/*
void populateMatching(map<int, int> &matching, resultT &res) {
  for (unsigned int i = 0; i < res.map.size(); i++) {
    if (res.map[i].ref) {
      populateMatching(matching, dp[res.map[i].a][res.map[i].b]);
    } else {
      matching[res.map[i].a] = res.map[i].b;
    }
  }
}
*/
int main(int argc, char** argv) {
  if (argc != 6) {
    cout << "Usage: batch-matching matchtype base blocksize rowblock colblock" << endl;
    return 1;
  }
  
  string matchtype(argv[1]);
  
  int blocksize = atoi(argv[3]);
  int rowblock = atoi(argv[4]);
  int colblock = atoi(argv[5]);
  
  VJ json(2 * blocksize), root(2 * blocksize);
  VI n(2 * blocksize);
  int maxn = 0;
  size = VVI(2 * blocksize);
  post = VVN(2 * blocksize);
  pre = VVN(2 * blocksize);
  
  VVI ans(blocksize, VI(blocksize));
  
  int maxA = blocksize, maxB = blocksize;
  for (int i = 0; i < blocksize; i++) {
    stringstream str;
    str << argv[2] << blocksize * rowblock + i << ".json";
    json[i] = readJSON(str.str().c_str());
    if (json[i] == NULL && !ifstream(str.str().c_str())) {
      maxA = i;
      break;
    } else if (json[i] == NULL) {
      cerr << "skipping file " << blocksize * rowblock + i << endl;
      continue;
    }
    root[i] = json[i]->AsObject().at(L"root");
    n[i] = getSize(root[i]);
    maxn = max(maxn, n[i]);
    precomputeSize(root[i], size[i]);
    postorder(root[i], pre[i], post[i], true);
  }
  
  for (int i = 0; i < blocksize; i++) {
    stringstream str;
    str << argv[2] << blocksize * colblock + i << ".json";
    json[blocksize + i] = readJSON(str.str().c_str());
    if (json[blocksize + i] == NULL && !ifstream(str.str().c_str())) {
      maxB = i;
      break;
    } else if (json[blocksize + i] == NULL) {
      cerr << "skipping file " << blocksize * colblock + i << endl;
      continue;
    }
    root[blocksize + i] = json[blocksize + i]->AsObject().at(L"root");
    n[blocksize + i] = getSize(root[blocksize + i]);
    maxn = max(maxn, n[blocksize + i]);
    precomputeSize(root[blocksize + i], size[blocksize + i]);
    postorder(root[blocksize + i], pre[blocksize + i], post[blocksize + i], true);
  }
  
  cerr << "blocksize " << blocksize << " row " << rowblock << " col " << colblock << " rowsize " << maxA << " colsize " << maxB << endl;
  
  dp = VVR(maxn, VR(maxn));
  dp2 = VVR(maxn+1, VR(maxn+1));
  
  for (int A = 0; A < maxA; A++) {
    for (int B = 0; B < maxB; B++) {
      if (blocksize * rowblock + A >= blocksize * colblock + B) continue;
      if (json[A] == NULL || json[blocksize + B] == NULL) {
        ans[A][B] = -1;
        continue;
      }
      
      if (matchtype == "pre") {
        matchString(pre[A], pre[blocksize + B]);
        ans[A][B] = dp2[n[A]][n[blocksize + B]].cost;
      } else if (matchtype == "post") {
        matchString(post[A], post[blocksize + B]);
        ans[A][B] = dp2[n[A]][n[blocksize + B]].cost;
      } else if (matchtype == "tree") {
        for (int i = 0; i < n[A]; i++) {
          if (!post[A][i].key) continue;
          for (int j = 0; j < n[blocksize + B]; j++) {
            if (!post[blocksize + B][j].key) continue;
            matchTree(post[A], post[blocksize + B], i, j);
          }
        }
        ans[A][B] = dp[n[A]-1][n[blocksize + B]-1].cost;
      }
      
/*      map<int, int> matching;
      populateMatching(matching, dp[n[A]-1][n[B]-1]);
      stringstream ostr;
      ostr << "/home/andy/ast_1_1/" << A << "/" << B << ".map";
      ofstream fout(ostr.str().c_str());
      fout << ans[A][B] << " " << matching.size() << endl;

      for (map<int, int>::iterator it = matching.begin(); it != matching.end(); ++it) {
        fout << it->first << " " << it->second << endl;
      }
      
      fout.close();*/
      
//      cout << A << " " << B << " " << ans[A][B] << endl;
    }
  }
  
  for (int A = 0; A < maxA; A++) {
    for (int B = 0; B < maxB; B++) {
      cout << ans[A][B] << " ";
    }
    cout << endl;
  }
  
  return 0;
}

