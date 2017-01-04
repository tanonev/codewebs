#include "codemap-streambuf.h"

int codemap_streambuf::overflow(int c) {
  if (c == EOF) return !EOF;
  if (c == '\n') {
    codemap.push_back(vector<int>());
  } else {
    codemap.back().push_back(id);
  }
  int const r = sb->sputc(c);
  return r == EOF ? EOF : c;
}

void codemap_streambuf::print_codemap(ostream &os) {
  os << codemap.size() << endl;
  for (unsigned int i = 0; i < codemap.size(); i++) {
    os << codemap[i].size();
    for (unsigned int j = 0; j < codemap[i].size(); j++) {
      os << " " << codemap[i][j];
    }
    os << endl;
  }
}

