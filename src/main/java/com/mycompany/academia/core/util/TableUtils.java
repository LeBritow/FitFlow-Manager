package com.mycompany.academia.core.util;

import javafx.scene.control.TableView;

public class TableUtils {

  public static void autoFitColumns(TableView<?> tableView) {
    tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    tableView.getColumns().forEach(col -> {
      double maxWidth = col.getText().length() * 8 + 20;
      int limit = Math.min(tableView.getItems().size(), 50);
      for (int i = 0; i < limit; i++) {
        Object data = col.getCellData(i);
        if (data != null) {
          maxWidth = Math.max(maxWidth, data.toString().length() * 8 + 30);
        }
      }
      col.setPrefWidth(Math.min(maxWidth, 600));
      col.setMinWidth(50);
    });
  }
}
