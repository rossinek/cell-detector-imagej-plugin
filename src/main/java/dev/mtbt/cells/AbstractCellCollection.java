package dev.mtbt.cells;

import java.io.Serializable;
import java.util.List;

public abstract class AbstractCellCollection implements Serializable {
  private static final long serialVersionUID = -8752681597115932465L;
  protected AbstractCellCollection parent = null;

  public AbstractCellCollection getParentCollection() {
    return this.parent;
  }

  public void setParentCollection(AbstractCellCollection parent) {
    this.parent = parent;
  }

  abstract public boolean isEmpty();

  abstract public int getF0();

  abstract public void addToCollection(AbstractCellCollection cellCollection);

  abstract public void removeFromCollection(AbstractCellCollection cellCollection);

  /**
   * removes all frames in all cells from `index` (inclusive) for `index > f0`
   *
   * @throws IllegalArgumentException for index <= f0
   */
  abstract public void clearFuture(int index) throws IllegalArgumentException;

  /**
   * Returns descendants alive at frame `index`
   */
  abstract public List<Cell> getCells(int index);

  /**
   * Recursively cleanup collection and unbound from parent
   */
  public void destroy() {
    AbstractCellCollection parent = this.getParentCollection();
    if (parent != null) {
      parent.removeFromCollection(this);
      this.setParentCollection(null);
    }
  }
}
