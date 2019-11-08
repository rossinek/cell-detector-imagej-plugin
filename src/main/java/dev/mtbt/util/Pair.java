package dev.mtbt.util;

import java.io.Serializable;

public class Pair<K, V> extends Object implements Serializable {
  K key;
  V value;

  public Pair(K key, V value) {
    this.key = key;
    this.value = value;
  }

  public K getKey() {
    return this.key;
  }

  public V getValue() {
    return this.value;
  }

  @Override
  public String toString() {
    return key + "=" + value;
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash = 31 * hash + (key != null ? key.hashCode() : 0);
    hash = 31 * hash + (value != null ? value.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o instanceof Pair) {
      Pair pair = (Pair) o;
      if (key != null ? !key.equals(pair.key) : pair.key != null)
        return false;
      if (value != null ? !value.equals(pair.value) : pair.value != null)
        return false;
      return true;
    }
    return false;
  }

}
