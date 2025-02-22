package org.lebastudios.theroundtable.pluginreceiptmanager.editor;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.AllArgsConstructor;
import lombok.Setter;
import org.lebastudios.theroundtable.MainStageController;
import org.lebastudios.theroundtable.apparience.UIEffects;
import org.lebastudios.theroundtable.controllers.PaneController;
import org.lebastudios.theroundtable.database.Database;
import org.lebastudios.theroundtable.dialogs.ConfirmationTextDialogController;
import org.lebastudios.theroundtable.dialogs.InformationTextDialogController;
import org.lebastudios.theroundtable.locale.LangFileLoader;
import org.lebastudios.theroundtable.maths.BigDecimalOperations;
import org.lebastudios.theroundtable.plugincashregister.PluginCashRegisterEvents;
import org.lebastudios.theroundtable.plugincashregister.cash.PaymentMethod;
import org.lebastudios.theroundtable.plugincashregister.entities.*;
import org.lebastudios.theroundtable.plugincashregister.products.ProductPaneController;
import org.lebastudios.theroundtable.plugincashregister.products.ProductsUIController;
import org.lebastudios.theroundtable.pluginreceiptmanager.PluginReceiptManager;
import org.lebastudios.theroundtable.pluginreceiptmanager.ReceiptViewerController;
import org.lebastudios.theroundtable.plugincashregister.entities.ReceiptModification;
import org.lebastudios.theroundtable.ui.BigDecimalField;
import org.lebastudios.theroundtable.ui.LabeledTextField;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

public class ReceiptEditorStageController extends PaneController<ReceiptEditorStageController>
{
    @FXML private Label receiptIDLabel;
    @FXML private Label receiptTimeLabel;
    @FXML private Label receiptDateLabel;
    @FXML private LabeledTextField customerNameField;
    @FXML private LabeledTextField customerIdField;
    @FXML private LabeledTextField attendantNameField;
    @FXML private LabeledTextField tableNameField;
    @FXML private TableView<ProductTableItem> productsTableView;
    @FXML private Label totalLabel;
    @FXML private BigDecimalField paymentAmountField;
    @FXML private ChoiceBox<PaymentMethod> paymentMethodChoiceBox;
    @FXML private Label changeLabel;
    @FXML private HBox centerContent;
    @FXML private VBox taxesDesgloseContainer;
    @FXML private TextArea modificationReasonTextArea;

    @Setter private Consumer<Receipt> onReceiptSaved;
    private final Node lastAppCentralPane;
    private Receipt modifiedReceipt;

    public ReceiptEditorStageController()
    {
        lastAppCentralPane = MainStageController.getInstance().getCentralNode();
    }

    @Override
    protected void initialize()
    {
        ProductPaneController.onAction = this::addProduct;

        centerContent.getChildren().addFirst(new ProductsUIController(false).getRoot());

        paymentMethodChoiceBox.getItems().addAll(PaymentMethod.values());
        paymentMethodChoiceBox.setConverter(PaymentMethod.converter);

        paymentMethodChoiceBox.getSelectionModel().selectedItemProperty().addListener((_, oldValue, newValue) ->
        {
            if (newValue == null || oldValue == newValue) return;
            
            if (newValue == PaymentMethod.CARD) 
            {
                paymentAmountField.setValue(new BigDecimal(totalLabel.getText()));
                paymentAmountField.setDisable(true);
            }
            else
            {
                paymentAmountField.setDisable(false);
            }
        });
        
        paymentAmountField.getOnValueChangeEvent().addListener(_ -> updateCalculatedValues());

        initializeTableView();
    }

    private void initializeTableView()
    {
        productsTableView.setEditable(true);
        productsTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<ProductTableItem, String> col1 =
                ((TableColumn<ProductTableItem, String>) productsTableView.getColumns().getFirst());
        col1.setCellValueFactory(cellData -> cellData.getValue().qty);
        col1.setEditable(true);
        col1.setCellFactory(TextFieldTableCell.forTableColumn());
        col1.setOnEditCommit(event ->
        {
            ProductTableItem item = event.getRowValue();

            if (!event.getNewValue().matches("\\d+(\\.\\d+)?"))
            {
                UIEffects.shakeNode(event.getTableView());
                item.qty.setValue(event.getOldValue());
                return;
            }

            if (event.getNewValue().matches("0(\\.0+)?"))
            {
                productsTableView.getItems().remove(item);
            }
            else
            {
                item.qty.setValue(event.getNewValue());
                item.updateTotal();
            }

            updateCalculatedValues();
        });

        TableColumn<ProductTableItem, String> col2 =
                ((TableColumn<ProductTableItem, String>) productsTableView.getColumns().get(1));
        col2.setCellValueFactory(cellData -> cellData.getValue().productName);
        col2.setEditable(true);
        col2.setCellFactory(TextFieldTableCell.forTableColumn());
        col2.setOnEditCommit(event ->
        {
            ProductTableItem item = event.getRowValue();
            item.productName.setValue(event.getNewValue());
        });

        TableColumn<ProductTableItem, String> col3 =
                ((TableColumn<ProductTableItem, String>) productsTableView.getColumns().get(2));
        col3.setCellValueFactory(cellData -> cellData.getValue().price);
        col3.setEditable(true);
        col3.setCellFactory(TextFieldTableCell.forTableColumn());
        col3.setOnEditCommit(event ->
        {
            ProductTableItem item = event.getRowValue();

            if (!event.getNewValue().matches("\\d+(\\.\\d+)?"))
            {
                UIEffects.shakeNode(event.getTableView());
                item.price.setValue(event.getOldValue());
                return;
            }

            item.price.setValue(event.getNewValue());
            item.updateTotal();

            updateCalculatedValues();
        });

        TableColumn<ProductTableItem, String> col4 =
                ((TableColumn<ProductTableItem, String>) productsTableView.getColumns().get(3));
        col4.setCellValueFactory(cellData -> cellData.getValue().total);
    }

    private void updateCalculatedValues()
    {
        BigDecimal total = productsTableView.getItems().stream()
                .map(item -> new BigDecimal(item.total.getValue()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        totalLabel.setText(BigDecimalOperations.toString(total));
        changeLabel.setText(BigDecimalOperations.toString(paymentAmountField.getValue().subtract(total)));

        if (paymentMethodChoiceBox.getValue() == PaymentMethod.CARD) 
        {
            paymentAmountField.setValue(total);
        }
        
        taxesDesgloseContainer.getChildren().clear();

        calculateTotalPerTax().forEach((key, value) ->
                taxesDesgloseContainer.getChildren().add(ReceiptViewerController.createTaxesLabel(key, value))
        );
    }

    private TreeMap<BigDecimal, BigDecimal> calculateTotalPerTax()
    {
        TreeMap<BigDecimal, BigDecimal> taxes = new TreeMap<>();

        productsTableView.getItems().forEach(item ->
        {
            BigDecimal tax = item.taxes;
            BigDecimal total = new BigDecimal(item.total.getValue());

            taxes.put(tax, taxes.getOrDefault(tax, BigDecimal.ZERO).add(total));
        });

        return taxes;
    }

    public void showReceipt(Receipt receipt)
    {
        Database.getInstance().connectQuery(session ->
        {
            Receipt r = session.get(Receipt.class, receipt.getId());
            Transaction transaction = r.getTransaction();

            StringBuffer receiptBillNumber = new StringBuffer();
            PluginCashRegisterEvents.onRequestReceiptBillNumber.invoke(r.getId(), receiptBillNumber);

            receiptIDLabel.setText(receiptBillNumber.isEmpty() ? r.getId() + "" : receiptBillNumber.toString());
            receiptDateLabel.setText(transaction.getDate().toLocalDate().toString());
            receiptTimeLabel.setText(transaction.getDate().toLocalTime().truncatedTo(ChronoUnit.SECONDS).toString());

            if (r.getClientName() == null)
            {
                customerNameField.setText("");
                customerIdField.setText("");
            }
            else
            {
                customerNameField.setText(r.getClientName());
                customerIdField.setText(r.getClientIdentifier());
            }

            attendantNameField.setText(r.getAttendantName());
            tableNameField.setText(r.getTableName());

            r.getProducts().forEach(
                    pr -> productsTableView.getItems().add(new ProductTableItem(pr.getProduct(), pr.getQuantity()))
            );

            totalLabel.setText(BigDecimalOperations.toString(transaction.getAmount()));
            paymentAmountField.setValue(r.getPaymentAmount());
            paymentMethodChoiceBox.setValue(PaymentMethod.valueOf(r.getPaymentMethod()));
            changeLabel.setText(
                    BigDecimalOperations.toString(r.getPaymentAmount().subtract(transaction.getAmount()))
            );

            modifiedReceipt = receipt;
        });
    }

    private void addProduct(Product product)
    {
        productsTableView.getItems().stream()
                .filter(item -> item.id == product.getId()
                        || (item.productName.getValue().equals(product.getName())
                        && new BigDecimal(item.price.getValue()).compareTo(product.getPrice()) == 0))
                .findFirst()
                .ifPresentOrElse(
                        item ->
                        {
                            item.qty.setValue(
                                    BigDecimalOperations.toString(
                                            new BigDecimal(item.qty.getValue()).add(BigDecimal.ONE))
                            );

                            item.updateTotal();
                        },
                        () -> productsTableView.getItems().add(new ProductTableItem(product, BigDecimal.ONE))
                )
        ;

        updateCalculatedValues();
    }

    @FXML
    private void save()
    {
        if (!validateForm()) return;

        new ConfirmationTextDialogController(LangFileLoader.getTranslation("confirmdialog.editreceipt"), response ->
        {
            if (!response) return;

            Receipt receipt = createReceipt();

            boolean result = Database.getInstance().connectTransactionWithBool(session ->
            {
                ReceiptModification receiptModification = new ReceiptModification(
                        modifiedReceipt, receipt, modificationReasonTextArea.getText()
                );

                session.persist(receiptModification);
            });

            if (!result)
            {
                new InformationTextDialogController(
                        LangFileLoader.getTranslation("infodialog.errorsavingmodifiedreceipt")
                ).instantiate();

                return;
            }
            
            StringBuffer billNumber = new StringBuffer();
            PluginCashRegisterEvents.onRequestNewRectificationBillNumber.invoke(receipt.getId(), billNumber);

            if (!billNumber.isEmpty())
            {
                PluginCashRegisterEvents.onModifiedReceiptBilled.invoke(receipt, billNumber.toString());
            }

            onReceiptSaved.accept(receipt);
            
            exit();
        }).instantiate();
    }

    private boolean validateForm()
    {
        if (new BigDecimal(changeLabel.getText()).compareTo(BigDecimal.ZERO) < 0)
        {
            UIEffects.shakeNode(changeLabel);
            return false;
        }
        
        if (modificationReasonTextArea.getText().isBlank()) 
        {
            UIEffects.shakeNode(modificationReasonTextArea);
            return false;
        }

        final String customerName = customerNameField.getText();
        final String customerId = customerIdField.getText();
        
        if (!(customerName.isBlank() && customerId.isBlank()) && (customerName.isBlank() || customerId.isBlank())) 
        {
            UIEffects.shakeNode(customerNameField.getParent());
            return false;
        }
        
        return true;
    }

    @FXML
    private void exit()
    {
        MainStageController.getInstance().setCentralNode(lastAppCentralPane);
    }

    @FXML
    private void printPreview() {}

    private Receipt createReceipt()
    {
        Receipt receipt = new Receipt();
        
        if (!customerNameField.getText().isBlank() && !customerIdField.getText().isBlank()) 
        {
            receipt.setClient(customerNameField.getText().trim(), customerIdField.getText().trim());
        }
        
        receipt.setEmployeeName(attendantNameField.getText());
        receipt.setTableName(tableNameField.getText());
        receipt.setPaymentAmount(paymentAmountField.getValue());
        receipt.setPaymentMethod(paymentMethodChoiceBox.getValue().name());

        BigDecimal totalTaxes = calculateTotalPerTax()
                .entrySet()
                .stream()
                .map(entry ->
                {
                    BigDecimal taxesPercentage = entry.getKey();
                    BigDecimal total = entry.getValue();
                    
                    var base = BigDecimalOperations.dividePrecise(total, taxesPercentage.add(BigDecimal.ONE));
                    return total.subtract(base);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        receipt.setTaxesAmount(totalTaxes);

        HashSet<Product_Receipt> products = new HashSet<>();

        for (ProductTableItem p : productsTableView.getItems())
        {
            Product_Receipt productReceipt = new Product_Receipt(p.getProduct(), new BigDecimal(p.qty.getValue()));
            productReceipt.setReceipt(receipt);

            products.add(productReceipt);
        }

        receipt.setProducts(products);

        Transaction transaction = new Transaction();
        transaction.setAmount(new BigDecimal(totalLabel.getText()));
        transaction.setDate(LocalDateTime.now());
        transaction.setReceipt(receipt);
        receipt.setTransaction(transaction);

        transaction.setDescription(LangFileLoader.getTranslation("phrase.rectbillof") + " " + receiptIDLabel.getText());

        return receipt;
    }

    @Override
    public Class<?> getBundleClass()
    {
        return PluginReceiptManager.class;
    }

    @AllArgsConstructor
    private static class ProductTableItem
    {
        private int id;
        private StringProperty qty;
        private StringProperty productName;
        private StringProperty price;
        private StringProperty total;
        private BigDecimal taxes;
        private boolean taxesIncluded;

        public ProductTableItem(Product product, BigDecimal qty)
        {
            this(
                    product.getId(),
                    new SimpleStringProperty(BigDecimalOperations.toString(qty)),
                    new SimpleStringProperty(product.getName()),
                    new SimpleStringProperty(BigDecimalOperations.toString(product.getPrice())),
                    new SimpleStringProperty(BigDecimalOperations.toString(product.getPrice().multiply(qty))),
                    product.getTaxes(),
                    product.getTaxesIncluded()
            );
        }

        public Product getProduct()
        {
            Product product = new Product();
            product.setName(productName.getValue());
            product.setTaxType(new TaxType("", taxes, ""));
            product.setTaxesIncluded(taxesIncluded);
            product.setTaxedPrice(new BigDecimal(price.getValue()));
            return product;
        }

        public void updateTotal()
        {
            total.setValue(BigDecimalOperations.toString(
                    new BigDecimal(price.getValue()).multiply(new BigDecimal(qty.getValue())))
            );
        }
    }
}
