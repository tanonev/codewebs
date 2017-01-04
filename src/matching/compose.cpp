#include <map>
#include <iostream>
#include <fstream>
#include <string>
#include <sstream>
#include <cstring>

using namespace std;

typedef map<int, int> MII;

int main(int argc, char** argv) {
  if (argc != 4) {
    cout << "Usage: compose inAtoB inBtoC outAtoC" << endl;
    return 1;
  }
  
  ifstream in1(argv[1]);
  ifstream in2(argv[2]);
  
  int score1, count1, score2, count2;
  in1 >> score1 >> count1;
  in2 >> score2 >> count2;
  
  MII map1, map2;
  for (int i = 0; i < count1; i++) {
    int from, to;
    in1 >> from >> to;
    map1[from] = to;
  }
  for (int i = 0; i < count2; i++) {
    int from, to;
    in2 >> from >> to;
    map2[from] = to;
  }
  
  in1.close();
  in2.close();
  
  MII mapout;
  for (MII::iterator it = map1.begin(); it != map1.end(); it++) {
    if (map2.count(it->second) != 0) mapout[it->first] = map2[it->second];
  }
  
  ofstream out(argv[3]);
  out << score1 + score2 << " " << mapout.size() << endl;
  for (MII::iterator it = mapout.begin(); it != mapout.end(); it++) {
    out << it->first << " " << it->second << endl;
  }
  
  out.close();
  
  return 0;
}

