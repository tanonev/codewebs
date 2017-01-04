#include <vector>
#include <list>
#include <set>
#include <iostream>
#include <fstream>
#include <string>
#include <sstream>
#include <cstring>
#include <cmath>
#include <ctime>

#include <mysql.h>

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
typedef vector<bool> VB;
typedef vector<nodeT> VN;
typedef vector<VN> VVN;

VVR dp, dp2;
VN post;
VVN postArr;

set<wstring> important;

int getSize(JSONValue* root) {
  while (root->AsObject().at(L"children")->AsArray().size() > 0)
    root = root->AsObject().at(L"children")->AsArray().back();
  return static_cast<int>(root->AsObject().at(L"id")->AsNumber() + 1);
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

int postorder(JSONValue* node, VN& post, bool isKey) {
  const JSONArray& children = node->AsObject().at(L"children")->AsArray();
  int ans = -1;
  int size = 1;
  for (int i = 0; i < children.size(); i++) {
    int tmp = postorder(children[i], post, i > 0);
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
  if (argc < 4) {
    cout << "Usage: db-matching homework part exemplar1 ..." << endl;
    return 1;
  }
  
  stringstream sIn;
  string tmp;
  while (getline(cin, tmp)) {
    sIn << tmp << endl;
  }
  
  JSONValue* jIn = JSON::Parse(sIn.str().c_str());
  JSONValue* rootIn = jIn->AsObject().at(L"root");
  
  
  int nIn = getSize(rootIn);
  postorder(rootIn, post, true);
  
  int exemplarCount = argc - 3;
  VI nEx(exemplarCount);
  postArr = VVN(exemplarCount);

  MYSQL *conn;
  MYSQL_RES *res;
  MYSQL_ROW row;
  
  char *server = "evariste.stanford.edu";
  char *user = "codewebdb";
  char *password = "n3gr0n1"; 
  char *database = "codewebdb";

  // Connecting
  conn = mysql_init(NULL);
  if(!mysql_real_connect(conn, server, user, 
              password, database, 0, NULL, 0)){
      cerr <<mysql_error(conn) <<endl;
      return 1;
  }
  
  stringstream querystr2;
  querystr2 << "SELECT keywords FROM assn_keywords WHERE homework_id = " << argv[1];
  querystr2 << " AND part_id = " << argv[2];
  if (mysql_query(conn, querystr2.str().c_str())) {
      cerr <<mysql_error(conn) <<endl;
      return 1;
  }
  
  res = mysql_store_result(conn);
  if ((row = mysql_fetch_row(res)) != NULL) {
    string str(row[0]);
    wstring all;
    all.assign(str.begin(), str.end());
    wstringstream keywords(all);
    wstring keyword;
    while (keywords >> keyword) important.insert(keyword);
  } else {
    cerr << "no keywords" << endl;
    return 1;
  }
  
  mysql_free_result(res);
  
  // Issuing query
  stringstream querystr;
  querystr << "SELECT ast_id, json FROM octave WHERE homework_id = " << argv[1];
  querystr << " AND part_id = " << argv[2] << " AND ast_id IN (";
  for (int i = 0; i < exemplarCount; i++) {
    if (i > 0) querystr << ",";
    querystr << argv[i+3];
  }
  querystr << ")";
  if (mysql_query(conn, querystr.str().c_str())){
      cerr <<mysql_error(conn) <<endl;
      return 1;
  }

  // Getting result and printing
  res = mysql_store_result(conn);
  clock_t start = clock();
  int idx = 0;
  int maxN = 0;
  while ((row = mysql_fetch_row(res)) != NULL) {
    JSONValue* jEx = JSON::Parse(row[1]);
    JSONValue* rootEx = jEx->AsObject().at(L"root");
    nEx[idx] = getSize(rootEx);
    if (maxN < nEx[idx]) maxN = nEx[idx];
    postorder(rootEx, postArr[idx], true);
    idx++;
  }
      
  // Close connection
  mysql_free_result(res);
  mysql_close(conn);
  
  
  dp = VVR(nIn, VR(maxN));
  dp2 = VVR(nIn+1, VR(maxN+1));
  
  int best = -1;
  int cost = maxN + 1;
  map<int, int> matching;
  for (int exemplar = 0; exemplar < exemplarCount; exemplar++) {
    if (abs(nIn - nEx[exemplar]) >= cost) continue;
  
    for (int i = 0; i < nIn; i++) {
      if (!post[i].key) continue;
      for (int j = 0; j < nEx[exemplar]; j++) {
        if (!postArr[exemplar][j].key) continue;
        matchTree(post, postArr[exemplar], i, j);
      }
    }
    
    int ans = dp[nIn-1][nEx[exemplar]-1].cost;
    if (ans < cost) {
      cost = ans;
      best = exemplar;
      matching.clear();
      populateMatching(matching, dp[nIn-1][nEx[exemplar]-1]);
    }
  }
  
  clock_t ends = clock();
  
  cout << "Time elapsed " << (double) (ends - start) / CLOCKS_PER_SEC << endl;
  
  cout << argv[best+3] << " " << cost << " " << matching.size() << endl;

  for (map<int, int>::iterator it = matching.begin(); it != matching.end(); ++it) {
    cout << it->first << " " << it->second << endl;
  }
  return 0;
}

