package models;

public class ASTID {
  private final int id;
  public ASTID(int id) {this.id = id;}
  
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + id;
    return result;
  }
  
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    ASTID other = (ASTID) obj;
    if (id != other.id)
      return false;
    return true;
  }
  
  @Override
  public String toString() {
    return "" + id;
  }
}
