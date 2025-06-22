package org.lebastudios.theroundtableplugins.rm;

import com.github.anastaciocintra.escpos.EscPos;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lombok.Setter;
import net.sf.jasperreports.engine.JasperPrint;
import org.lebastudios.theroundtable.MainStageController;
import org.lebastudios.theroundtable.controllers.PaneController;
import org.lebastudios.theroundtable.database.Database;
import org.lebastudios.theroundtable.locale.Translator;
import org.lebastudios.theroundtable.maths.BigDecimalOperations;
import org.lebastudios.theroundtable.reports.ReportPaneController;
import org.lebastudios.theroundtableplugins.cr.PluginCashRegisterEvents;
import org.lebastudios.theroundtableplugins.cr.entities.Product;
import org.lebastudios.theroundtableplugins.cr.entities.Product_Receipt;
import org.lebastudios.theroundtableplugins.cr.entities.Receipt;
import org.lebastudios.theroundtableplugins.cr.printers.CashRegisterPrinters;
import org.lebastudios.theroundtableplugins.cr.reports.ReceiptReportGenerator;
import org.lebastudios.theroundtableplugins.rm.editor.ReceiptEditorStageController;
import org.lebastudios.theroundtableplugins.rm.entities.SimpleReceipt;
import org.lebastudios.theroundtable.printers.PrinterManager;
import org.lebastudios.theroundtable.components.IconTextButton;
import org.lebastudios.theroundtable.components.LoadingPaneController;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.TreeMap;

public class ReceiptViewerController extends PaneController<ReceiptViewerController>
{
    private final SimpleReceipt simpleReceipt;
    
    @FXML public IconTextButton editButton;
    @FXML public StackPane receiptReportContainer;

    @Setter private Runnable onClose = () -> {};
    
    public ReceiptViewerController(SimpleReceipt simpleReceipt)
    {
        this.simpleReceipt = simpleReceipt;
    }

    @FXML
    @Override
    protected void initialize()
    {
        JasperPrint print = new ReceiptReportGenerator().generate(simpleReceipt.getId());
        
        receiptReportContainer.getChildren().add(new ReportPaneController(print).getRoot());
    }

    @FXML
    public void edit(ActionEvent actionEvent)
    {
        var receipt = Database.getInstance().connectQuery(session ->
        {
            return session.get(Receipt.class, simpleReceipt.getId());
        });

        var editor = new ReceiptEditorStageController(receipt);
        editor.setOnReceiptSaved(_ -> ReceiptManagerPaneController.getInstance().updateReceiptsList());

        MainStageController.getInstance().setCentralNode(editor);
    }

    @FXML
    public void print(ActionEvent actionEvent)
    {
        Database.getInstance().connectQuery(session ->
        {
            Receipt receipt = session.get(Receipt.class, simpleReceipt.getId());

            try (var escpos = CashRegisterPrinters.getInstance().printReceipt(receipt, PrinterManager.getInstance().getDefaultPrintService()))
            {
                escpos.feed(5).cut(EscPos.CutMode.PART);
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        });
    }

    @FXML
    public void close(ActionEvent actionEvent)
    {
        ((Pane) root.getParent()).getChildren().remove(root);
        onClose.run();
    }
}
