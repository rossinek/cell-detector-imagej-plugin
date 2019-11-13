package dev.mtbt.cells;

import java.util.List;
import java.util.concurrent.Future;

import org.scijava.command.Command;

public interface CellLifeTracker extends Command {
  public void init(List<Cell> cells);

  public Future<Void> output();
}
