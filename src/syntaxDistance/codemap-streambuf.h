#if !defined (octave_codemap_streambuf_h)
#define octave_codemap_streambuf_h 1

#include <ostream>
#include <streambuf>
#include <vector>
#include <utility>
#include <cstdio>

using namespace std;

class codemap_streambuf : public streambuf {
public:
  codemap_streambuf(streambuf *sb) : sb(sb), codemap(), id(0) {codemap.push_back(vector<int>());}
  
  void set_id(int id) {this->id = id;}
  
  void print_codemap(ostream &os);
private:
  virtual int overflow(int c);
  
  virtual int sync() {
    int const r = sb->pubsync();
  }
  
  streambuf *sb;
  vector<vector<int> > codemap;
  int id;
};

#endif
