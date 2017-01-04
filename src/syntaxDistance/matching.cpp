#include <vector>
#include <list>
#include <iostream>
#include <fstream>
#include <string>

#include "JSON.h"
#include "basic-treenode-distance.h"

enum matchT {DELA, DELB, MATCH};
struct resultT {
  resultT(matchT match = MATCH, int cost = 1000000) : match(match), cost(cost) {}
  matchT match;
  int cost;
};

using namespace std;

typedef vector<resultT> VR;
typedef vector<VR> VVR;
typedef vector<int> VI;
typedef vector<JSONValue*> VJ;
typedef vector<bool> VB;
typedef map<int, int> MII;
typedef vector<MII> VMII;
typedef vector<VMII> VVMII;

VVR dp, dp2;
VVMII matching;
VI sizeA, sizeB;
VJ postA, postB;
VI leafA, leafB;
VB keyA, keyB;

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

int precomputeSize(JSONValue* node, VI& size) {
  int ans = 1;
  const JSONArray& children = node->AsObject().at(L"children")->AsArray();
  for (int i = 0; i < children.size(); i++) ans += precomputeSize(children[i], size);
  size.push_back(ans);
  return ans;
}

int postorder(JSONValue* node, VJ& post, VI& leaf, VB& key, bool isKey) {
  const JSONArray& children = node->AsObject().at(L"children")->AsArray();
  int ans = -1;
  for (int i = 0; i < children.size(); i++) {
    int tmp = postorder(children[i], post, leaf, key, i > 0);
    if (ans == -1) ans = tmp;
  }
  if (ans == -1) ans = post.size();
  post.push_back(node);
  leaf.push_back(ans);
  key.push_back(isKey);
  return ans;
}

void matchTree(int a, int b, basic_treenode_distance& dist) {
  dp2[0][0].cost = 0;
  for (int i = leafA[a]; i <= a; i++) {
    dp2[i-leafA[a]+1][0].cost = dp2[i-leafA[a]][0].cost + dist.delete_cost(postA[i]);
    dp2[i-leafA[a]+1][0].match = DELA;
  }
  for (int j = leafB[b]; j <= b; j++) {
    dp2[0][j-leafB[b]+1].cost = dp2[0][j-leafB[b]].cost + dist.delete_cost(postB[j]);
    dp2[0][j-leafB[b]+1].match = DELB;
  }
  
  for (int i = leafA[a]; i <= a; i++) {
    for (int j = leafB[b]; j <= b; j++) {
      int ai = i - leafA[a] + 1, bj = j - leafB[b] + 1;
      if (leafA[i] == leafA[a] && leafB[j] == leafB[b]) {
        dp2[ai][bj].cost = dp2[ai-1][bj].cost + dist.delete_cost(postA[i]);
        dp2[ai][bj].match = DELA;
        if (dp2[ai][bj].cost > dp2[ai][bj-1].cost + dist.delete_cost(postB[j])) {
          dp2[ai][bj].cost = dp2[ai][bj-1].cost + dist.delete_cost(postB[j]);
          dp2[ai][bj].match = DELB;
        }
        if (dp2[ai][bj].cost > dp2[ai-1][bj-1].cost + dist.match_cost(postA[i], postB[j])) {
          dp2[ai][bj].cost = dp2[ai-1][bj-1].cost + dist.match_cost(postA[i], postB[j]);
          dp2[ai][bj].match = MATCH;
        }
        dp[i][j] = dp2[ai][bj];
      } else {
        dp2[ai][bj].cost = dp2[ai-1][bj].cost + dist.delete_cost(postA[i]);
        dp2[ai][bj].match = DELA;
        if (dp2[ai][bj].cost > dp2[ai][bj-1].cost + dist.delete_cost(postB[j])) {
          dp2[ai][bj].cost = dp2[ai][bj-1].cost + dist.delete_cost(postB[j]);
          dp2[ai][bj].match = DELB;
        }
        if (dp2[ai][bj].cost > dp2[leafA[i] - leafA[a]][leafB[j] - leafB[b]].cost + dp[i][j].cost) {
          dp2[ai][bj].cost = dp2[leafA[i] - leafA[a]][leafB[j] - leafB[b]].cost + dp[i][j].cost;
          dp2[ai][bj].match = MATCH;
        }
      }
    }
  }
  
  for (int ci = leafA[a]; ci <= a; ci++) {
    for (int cj = leafB[b]; cj <= b; cj++) {
      if (leafA[ci] != leafA[a] || leafB[cj] != leafB[b]) continue;
      int i = ci, j = cj;
      while (i >= leafA[a] || j >= leafB[b]) {
        int ai = i - leafA[a] + 1, bj = j - leafB[b] + 1;
        switch (dp2[ai][bj].match) {
          case DELA: i--; break;
          case DELB: j--; break;
          case MATCH:
            if (leafA[i] == leafA[a] && leafB[j] == leafB[b] && i == ci && j == cj) {
              matching[ci][cj][id(postA[i])] = id(postB[j]);
              i--;
              j--;
            } else {
              for (MII::iterator it = matching[i][j].begin(); it != matching[i][j].end(); it++) {
                matching[ci][cj][it->first] = it->second;
              }
              i -= sizeA[i];
              j -= sizeB[j];
            }
        }
      }
    }
  }
}

int main(int argc, char** argv) {
  JSONValue* A = readJSON(argv[1]);
  JSONValue* rootA = A->AsObject().at(L"root");
  JSONValue* B = readJSON(argv[2]);
  JSONValue* rootB = B->AsObject().at(L"root");
  
  basic_treenode_distance dist(rootA, rootB);
  
  int na = size(rootA), nb = size(rootB);
  
  dp = VVR(na, VR(nb));
  dp2 = VVR(na+1, VR(nb+1));
  matching = VVMII(na, VMII(nb));
  precomputeSize(rootA, sizeA);
  precomputeSize(rootB, sizeB);
  
  postorder(rootA, postA, leafA, keyA, true);
  postorder(rootB, postB, leafB, keyB, true);
  
  for (int i = 0; i < na; i++) {
    if (!keyA[i]) continue;
    for (int j = 0; j < nb; j++) {
      if (!keyB[j]) continue;
      matchTree(i, j, dist);
    }
  }
  
  for (MII::iterator it = matching[na-1][nb-1].begin(); it != matching[na-1][nb-1].end(); it++) {
    cout << it->first << " -> " << it->second << endl;
  }
  return 0;
}

