package dev.mtbt.cells;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CellCollection extends AbstractCellCollection {
  List<AbstractCellCollection> subCollections;

  public CellCollection() {
    this.subCollections = new ArrayList<>();
  }

  public CellCollection(List<AbstractCellCollection> subCollections) {
    this.subCollections = subCollections;
    this.subCollections.forEach(cell -> cell.setParentCollection(this));
  }

  @Override
  public void addToCollection(AbstractCellCollection c) {
    c.setParentCollection(this);
    this.subCollections.add(c);
  }

  @Override
  public void removeFromCollection(AbstractCellCollection c) {
    this.subCollections.remove(c);
    c.setParentCollection(null);
  }

  @Override
  public boolean isEmpty() {
    return this.subCollections.isEmpty();
  }

  @Override
  public int getF0() {
    return this.subCollections.stream().mapToInt(cell -> cell.getF0()).min().getAsInt();
  }

  @Override
  public void clearFuture(int index) {
    if (index <= this.getF0()) {
      throw new IllegalArgumentException("fromIndex has to be in the future");
    }
    this.subCollections.stream().filter(c -> c.getF0() >= index).forEach(c -> c.destroy());
    this.subCollections.forEach(cell -> cell.clearFuture(index));
  }

  @Override
  public List<Cell> getCells(int index) {
    return this.subCollections.stream().flatMap(cell -> cell.getCells(index).stream())
        .collect(Collectors.toList());
  }

  @Override
  public void destroy() {
    super.destroy();
    for (AbstractCellCollection collection : this.subCollections) {
      collection.destroy();
    }
  }
}
