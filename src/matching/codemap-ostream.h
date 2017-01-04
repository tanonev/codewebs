#if !defined (octave_codemap_ostream_h)
#define octave_codemap_ostream_h 1

#include <ostream>

#include "codemap-streambuf.h"

using namespace std;

class codemap_ostream : public ostream {
public:
  codemap_ostream(ostream &os) : ostream(&cbuf), cbuf(os.rdbuf()) {}
  
  void set_id(int id) {this->cbuf.set_id(id);}
  
  void print_codemap(ostream &os) {this->cbuf.print_codemap(os);}
private:
  codemap_streambuf cbuf;
};

#endif
