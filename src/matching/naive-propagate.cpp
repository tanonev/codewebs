#include <map>
#include <iostream>
#include <fstream>
#include <string>
#include <sstream>
#include <cstring>

#include "JSON.h"

using namespace std;

typedef map<int, int> MII;

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

int main(int argc, char** argv) {
  int ind[4] = {0, 3, 10, 14};
  
  JSONArray annotations[4];
  for (int i = 0; i < 4; i++) {
    stringstream str;
    str << "/home/cw_user/projects/codeweb/data/annotationJson/hw1_3/" << ind[i] << ".json";
   annotations[i] = readJSON(str.str().c_str())->AsObject().at(L"comments")->AsArray();
  }
  
  for (int i = 0; i < 25398; i++) {
    if (i == 0 || i == 3 || i == 10 || i == 14) continue;
    
    JSONObject root;
    JSONArray arr;
    //root[L"comments"] = new JSONValue(arr);
    
    for (int j = 0; j < 4; j++) {
      stringstream str;
      str << "/home/cw_user/projects/codeweb/data/matching/ast_1_3/" << ind[j] << "/" << i << ".matching";
      ifstream in(str.str().c_str());
    
      int score, count;
      in >> score >> count;
      
      MII map;
      for (int k = 0; k < count; k++) {
        int from, to;
        in >> from >> to;
        map[from] = to;
      }
      in.close();
      
      for (int k = 0; k < annotations[j].size(); k++) {
        const JSONObject& obj = annotations[j][k]->AsObject();
        int ast_id = (int) obj.at(L"ast_id")->AsNumber();
        if (map.find(ast_id) != map.end()) {
          JSONObject annotation;
          annotation[L"ast_id"] = new JSONValue((double) map[ast_id]);
          annotation[L"comment_id"] = new JSONValue(obj.at(L"comment_id")->AsNumber());
          annotation[L"confidence"] = new JSONValue(0.5);
          annotation[L"type"] = new JSONValue(obj.at(L"type")->AsString());
          arr.push_back(new JSONValue(annotation));
        }
      }
    }
    root[L"comments"] = new JSONValue(arr);
    
    stringstream str;
    str << "/home/cw_user/projects/codeweb/data/annotationJson/propagated/hw1_3/" << i << ".json";
    wofstream out(str.str().c_str());
    JSONValue *value = new JSONValue(root);
    out << value->Stringify() << endl;
    out.close();
  }
  
  return 0;
}

