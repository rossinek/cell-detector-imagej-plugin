package dev.mtbt.cells;

import java.util.concurrent.Future;

import org.scijava.command.Command;

public interface ICellLifeTracker extends Command {
  public void init(CellCollection cells);

  public Future<Void> output();
}
