package dev.mtbt.cells;

import java.util.List;
import java.util.concurrent.Future;

import org.scijava.command.Command;

public interface ICellDetector<Frame extends ICellFrame> extends Command {
  public Future<List<Cell<ICellFrame>>> output();
}
