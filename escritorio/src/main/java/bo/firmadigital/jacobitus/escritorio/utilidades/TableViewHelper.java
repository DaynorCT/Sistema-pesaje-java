package bo.firmadigital.jacobitus.escritorio.utilidades;

import javafx.scene.control.OverrunStyle;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.text.Text;

public class TableViewHelper {
    private static final double PADDING_COLUMNA = 25;
    private static final double ANCHO_MAXIMO_COLUMNA = 320;
    private static final String SIN_SCROLL_HORIZONTAL_STYLE_CLASS = "tabla-sin-scroll-horizontal";

    private TableViewHelper() {}

    public static <S> void ajustarColumnas(TableView<S> table) {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        for (TableColumn<S, ?> column : table.getColumns()) {
            double maxAncho = maxAnchoTexto(column.getText()) + PADDING_COLUMNA;
            for (S fila : table.getItems()) {
                if (column.getCellData(fila) != null) {
                    String valorCelda = column.getCellData(fila).toString();
                    double anchoTexto = maxAnchoTexto(valorCelda) + PADDING_COLUMNA;
                    if (anchoTexto > maxAncho) {
                        maxAncho = anchoTexto;
                    }
                }
            }
            column.setPrefWidth(Math.min(maxAncho, ANCHO_MAXIMO_COLUMNA));
            truncarTextoConTooltip(column);
        }
    }

    public static <S, T> void truncarTextoConTooltip(TableColumn<S, T> column) {
        column.setCellFactory(col -> new javafx.scene.control.TableCell<S, T>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setTooltip(null);
                    return;
                }
                String texto = item.toString();
                setText(texto);
                setTextOverrun(OverrunStyle.ELLIPSIS);
                setTooltip(new Tooltip(texto));
            }
        });
    }

    public static <S, T> void ajustarColumnaUnica(TableView<S> table, TableColumn<S, T> column) {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        column.setMinWidth(0);
        column.setMaxWidth(Double.MAX_VALUE);
        if (!table.getStyleClass().contains(SIN_SCROLL_HORIZONTAL_STYLE_CLASS)) {
            table.getStyleClass().add(SIN_SCROLL_HORIZONTAL_STYLE_CLASS);
        }
        truncarTextoConTooltip(column);
    }

    private static double maxAnchoTexto(String texto) {
        Text t = new Text(texto);
        return t.getLayoutBounds().getWidth();
    }
}
