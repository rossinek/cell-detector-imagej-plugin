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
    if (Cell.class.isAssignableFrom(c.getClass())) {
      ((Cell) c).setFamily(this.getNextFamily());
    }
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
    if (index < this.getF0()) {
      throw new IllegalArgumentException("fromIndex has to be in the future");
    }
    // destroy and remove from collections all cells that starts at or after `index`
    this.subCollections.stream().filter(c -> c.getF0() >= index).collect(Collectors.toList())
        .forEach(c -> c.destroy());
    // rest of sub collections starts before `index`
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
    this.clear();
  }

  public void clear() {
    new ArrayList<>(this.subCollections).forEach(c -> c.destroy());
  }

  public List<Cell> getAllRootCells() {
    return this.subCollections.stream().flatMap(cell -> cell.getCells(cell.getF0()).stream())
        .collect(Collectors.toList());
  }

  private String getNextFamily() {
    List<String> families =
        getAllRootCells().stream().map(c -> c.getFamily()).collect(Collectors.toList());
    int i = 0;
    while (true) {
      String family = "" + (char) ('A' + i);
      if (!families.contains(family)) {
        return family;
      }
      i++;
    }
  }
}
