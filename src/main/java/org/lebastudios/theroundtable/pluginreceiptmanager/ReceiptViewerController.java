package org.lebastudios.theroundtable.pluginreceiptmanager;

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
import javafx.scene.layout.VBox;
import lombok.Setter;
import org.lebastudios.theroundtable.MainStageController;
import org.lebastudios.theroundtable.controllers.PaneController;
import org.lebastudios.theroundtable.database.Database;
import org.lebastudios.theroundtable.locale.Translator;
import org.lebastudios.theroundtable.maths.BigDecimalOperations;
import org.lebastudios.theroundtable.plugincashregister.PluginCashRegisterEvents;
import org.lebastudios.theroundtable.plugincashregister.entities.Product;
import org.lebastudios.theroundtable.plugincashregister.entities.Product_Receipt;
import org.lebastudios.theroundtable.plugincashregister.entities.Receipt;
import org.lebastudios.theroundtable.plugincashregister.printers.CashRegisterPrinters;
import org.lebastudios.theroundtable.pluginreceiptmanager.editor.ReceiptEditorStageController;
import org.lebastudios.theroundtable.pluginreceiptmanager.entities.SimpleReceipt;
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

    @FXML public Label receiptIDLabel;
    @FXML public Label receiptDateLabel;
    @FXML public Label receiptTimeLabel;
    @FXML public Label customerNameLabel;
    @FXML public Label attendantNameLabel;
    @FXML public Label tableNameLabel;
    @FXML public TableView<TableProduct> productListContainer;
    @FXML public Label totalLabel;
    @FXML public Label paymentAmountLabel;
    @FXML public Label paymentMethodLabel;
    @FXML public Label changeLabel;
    @FXML public VBox taxesDesgloseContainer;
    @FXML public IconTextButton editButton;

    @Setter private Runnable onClose = () -> {};
    
    public ReceiptViewerController(SimpleReceipt simpleReceipt)
    {
        this.simpleReceipt = simpleReceipt;
    }

    @FXML
    @Override
    protected void initialize()
    {
        productListContainer.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        ((TableColumn<TableProduct, String>) productListContainer.getColumns().getFirst()).setCellValueFactory(
                param -> param.getValue().qty());
        ((TableColumn<TableProduct, String>) productListContainer.getColumns().get(1)).setCellValueFactory(
                param -> param.getValue().name());
        ((TableColumn<TableProduct, String>) productListContainer.getColumns().get(2)).setCellValueFactory(
                param -> param.getValue().price());
        ((TableColumn<TableProduct, String>) productListContainer.getColumns().getLast()).setCellValueFactory(
                param -> param.getValue().total());
        
        editButton.setVisible(simpleReceipt.getStatus() == Receipt.Status.DEFAULT);
        
        Database.getInstance().connectQuery(session ->
        {
            Receipt receipt = session.get(Receipt.class, this.simpleReceipt.getId());

            StringBuffer receiptBillNumber = new StringBuffer();
            PluginCashRegisterEvents.onRequestReceiptBillNumber.invoke(simpleReceipt.getId(), receiptBillNumber);
            
            String receipId = receiptBillNumber.isEmpty() ? receipt.getId() + "" : receiptBillNumber.toString();
            
            receiptIDLabel.setText(receipId);
            receiptDateLabel.setText(receipt.getTransaction().getDate().toLocalDate().toString());
            receiptTimeLabel.setText(
                    receipt.getTransaction().getDate().toLocalTime().truncatedTo(ChronoUnit.SECONDS).toString());

            tableNameLabel.setText(receipt.getTableName());
            customerNameLabel.setText(receipt.getClientString());
            attendantNameLabel.setText(receipt.getTransaction().getAccount().getName());

            List<Product_Receipt> products = session
                    .createQuery("select products from Receipt r where r.id = :id", Product_Receipt.class)
                    .setParameter("id", receipt.getId())
                    .list();

            TreeMap<BigDecimal, BigDecimal> taxes = new TreeMap<>();

            products.forEach(productReceipt ->
            {
                var product = productReceipt.getProduct();
                var qty = productReceipt.getQuantity();

                taxes.put(product.getTaxes(),
                        taxes.getOrDefault(product.getTaxes(), BigDecimal.ZERO).add(qty.multiply(product.getPrice()))
                );

                productListContainer.getItems().add(new TableProduct(product, qty));
            });

            taxes.forEach((key, value) ->
                    taxesDesgloseContainer.getChildren().add(createTaxesLabel(key, value))
            );
            paymentAmountLabel.setText(BigDecimalOperations.toString(receipt.getPaymentAmount()));
            paymentMethodLabel.setText(receipt.getTransaction().getMethod().translate());

            var receiptTotal = receipt.getTransaction().getAmount();
            changeLabel.setText(BigDecimalOperations.toString(receipt.getPaymentAmount().subtract(receiptTotal)));

            totalLabel.setText(BigDecimalOperations.toString(receiptTotal));
        });
    }

    public static Node createTaxesLabel(BigDecimal taxesPercentage, BigDecimal total)
    {
        BigDecimal percentageOver100 = taxesPercentage.multiply(BigDecimal.valueOf(100));
        var base = BigDecimalOperations.divide(total, taxesPercentage.add(BigDecimal.ONE));
        var taxes = total.subtract(base);

        String text = BigDecimalOperations.toString(percentageOver100) + "  %  "
                + Translator.getInstance().t("word.iva")
                + "  " + Translator.getInstance().t("word.over") + "  "
                + BigDecimalOperations.toString(base) + " € " + "  "
                + BigDecimalOperations.toString(taxes) + " € ";

        return new Label(text);
    }

    @FXML
    public void edit(ActionEvent actionEvent)
    {
        var receipt = Database.getInstance().connectQuery(session ->
        {
            return session.get(Receipt.class, simpleReceipt.getId());
        });

        var editor = new ReceiptEditorStageController();
        editor.setOnReceiptSaved(_ -> ReceiptManagerPaneController.getInstance().updateReceiptsList());

        MainStageController.getInstance().setCentralNode(new LoadingPaneController().getRoot());
        
        new Thread(() ->
        {
            Node root = editor.getRoot();
            Platform.runLater(() ->
            {
                MainStageController.getInstance().setCentralNode(root);
                editor.showReceipt(receipt);
            });
        }).start();
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

    public record TableProduct(SimpleStringProperty qty, SimpleStringProperty name, SimpleStringProperty price,
                                SimpleStringProperty total)
    {
        private TableProduct(Product product, BigDecimal qty)
        {
            this(
                    new SimpleStringProperty(qty.toString()),
                    new SimpleStringProperty(product.getName()),
                    new SimpleStringProperty(BigDecimalOperations.toString(product.getPrice())),
                    new SimpleStringProperty(BigDecimalOperations.toString(product.getPrice().multiply(qty)))
            );
        }

        private TableProduct(BigDecimal qty, String name, BigDecimal price)
        {
            this(
                    new SimpleStringProperty(qty.toString()),
                    new SimpleStringProperty(name),
                    new SimpleStringProperty(BigDecimalOperations.toString(price)),
                    new SimpleStringProperty(BigDecimalOperations.toString(price.multiply(qty)))
            );
        }
    }
}
