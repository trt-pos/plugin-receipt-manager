package org.lebastudios.theroundtable.pluginreceiptmanager;

import com.sun.javafx.collections.ObservableListWrapper;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import lombok.Getter;
import lombok.NonNull;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.lebastudios.theroundtable.controllers.PaneController;
import org.lebastudios.theroundtable.database.Database;
import org.lebastudios.theroundtable.events.Event1;
import org.lebastudios.theroundtable.events.IEventMethod1;
import org.lebastudios.theroundtable.locale.LangFileLoader;
import org.lebastudios.theroundtable.pluginreceiptmanager.analyzers.HoursOfActivityDataAnalyzer;
import org.lebastudios.theroundtable.pluginreceiptmanager.analyzers.IDataAnalyzer;
import org.lebastudios.theroundtable.pluginreceiptmanager.analyzers.IncomeDataAnalyzer;
import org.lebastudios.theroundtable.pluginreceiptmanager.analyzers.ProductsSoldAnalyzer;
import org.lebastudios.theroundtable.pluginreceiptmanager.entities.SimpleReceipt;
import org.lebastudios.theroundtable.ui.IconView;
import org.lebastudios.theroundtable.ui.LoadingPaneController;
import org.lebastudios.theroundtable.ui.MultipleItemsListView;
import org.lebastudios.theroundtable.ui.SearchBox;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class ReceiptManagerPaneController extends PaneController<ReceiptManagerPaneController>
{
    @Getter private static ReceiptManagerPaneController instance;

    @FXML private MultipleItemsListView<SimpleReceipt> receiptList;
    @FXML private BorderPane rightView;
    @FXML private DatePicker startDate;
    @FXML private DatePicker endDate;
    @FXML private TabPane statsTabPane;
    @FXML private SearchBox searchBox;

    private final Event1<List<SimpleReceipt>> onFoundReceipt = new Event1<>();
    private ListItemsGenerator contentGenerator;

    public ReceiptManagerPaneController()
    {
        instance = this;
    }

    @FXML
    @Override
    protected void initialize()
    {
        startDate.setValue(LocalDate.now());
        endDate.setValue(LocalDate.now());

        addAnalyzerTab(new HoursOfActivityDataAnalyzer(), LangFileLoader.getTranslation("word.activity"));
        addAnalyzerTab(new IncomeDataAnalyzer(), LangFileLoader.getTranslation("word.income"));
        addAnalyzerTab(new ProductsSoldAnalyzer(), LangFileLoader.getTranslation("word.productssold"));

        receiptList.setOnItemSelected(simpleReceipt ->
        {
            var receiptViewerController = new ReceiptViewerController(simpleReceipt);
            receiptViewerController.setOnClose(() ->
            {
                rightView.setCenter(statsTabPane);
                receiptList.getListView().getSelectionModel().clearSelection();
            });

            rightView.setCenter(new LoadingPaneController().getRoot());

            new Thread(() ->
            {
                var node = receiptViewerController.getRoot();
                Platform.runLater(() -> rightView.setCenter(node));
            }).start();
        });

        receiptList.setCellReciclerGenerator(_ -> new MultipleItemsListView.ICellRecicler<>()
        {
            @Getter private final IconView graphic = new IconView();
            @Getter private String text = "";

            {
                graphic.setFitHeight(25);
                graphic.setFitWidth(25);
            }

            @Override
            public void update(@NonNull SimpleReceipt item)
            {
                graphic.setIconName(item.getStatus().getIconName());
                text = item.toString();
            }
        });

        searchBox.setOnSearch(searchText -> refreshReceiptsListView(
                searchText,
                startDate.getValue().atStartOfDay(),
                endDate.getValue().atTime(23, 59, 59)
        ));
        
        search();
    }

    @FXML
    private void search()
    {
        searchBox.clear();
    }

    private void addAnalyzerTab(IDataAnalyzer analyzer, String tabName)
    {
        Tab tab = new Tab(tabName);

        tab.setOnSelectionChanged(new EventHandler<>()
        {
            final IEventMethod1<List<SimpleReceipt>> onFoundReceiptListener = foundReceipts ->
            {
                tab.setContent(new LoadingPaneController().getRoot());

                new Thread(() ->
                {
                    var content = analyzer.getDataChart(new ObservableListWrapper<>(foundReceipts));

                    Platform.runLater(() -> tab.setContent(content));
                }).start();
            };

            @Override
            public void handle(Event event)
            {
                if (tab.isSelected())
                {
                    onFoundReceipt.addListener(this.onFoundReceiptListener);
                    tab.setContent(new LoadingPaneController().getRoot());

                    if (contentGenerator != null)
                    {
                        new Thread(() ->
                        {
                            var content =
                                    analyzer.getDataChart(new ObservableListWrapper<>(contentGenerator.queryAll()));

                            Platform.runLater(() -> tab.setContent(content));
                        }).start();
                    }
                }
                else
                {
                    onFoundReceipt.removeListener(onFoundReceiptListener);
                    tab.setContent(null);
                }
            }
        });

        statsTabPane.getTabs().add(tab);
    }

    private boolean occupied;

    private void refreshReceiptsListView(String textFilter, LocalDateTime startDate, LocalDateTime endDate)
    {
        if (occupied) return;

        occupied = true;

        contentGenerator = new ListItemsGenerator(textFilter, startDate, endDate);

        receiptList.setItemsGenerator(contentGenerator);
        receiptList.refresh();

        new Thread(() ->
        {
            final var simpleReceipts = contentGenerator.queryAll();
            Platform.runLater(() ->
            {
                onFoundReceipt.invoke(simpleReceipts);
                occupied = false;
            });
        }).start();
    }

    public void updateReceiptsList()
    {
        receiptList.refresh();
    }

    public void showStats()
    {
        rightView.setCenter(statsTabPane);
    }

    @Override
    public Class<?> getBundleClass()
    {
        return PluginReceiptManager.class;
    }

    private record ListItemsGenerator(String textFilter, LocalDateTime startDate, LocalDateTime endDate)
            implements MultipleItemsListView.ItemsGenerator<SimpleReceipt>
    {

        @Override
        public List<SimpleReceipt> generateItems(int from, int to)
        {
            return Database.getInstance().connectQuery(session ->
            {
                return generateQuery(session).setFirstResult(from)
                        .setMaxResults(to)
                        .stream()
                        .toList();
            });
        }

        @Override
        public long count()
        {
            return Database.getInstance().connectQuery(session ->
            {
                return generateQuery(session).getResultCount();
            });
        }

        private Query<SimpleReceipt> generateQuery(Session session)
        {
            Query<SimpleReceipt> query;

            final var parseInt = textFilter.matches("\\d+") ? Integer.parseInt(textFilter) : -1;

            query = session.createQuery("select new SimpleReceipt(r.id, r.transaction.date, r.status) " +
                            "from Receipt r " +
                            "where r.id = :id or (r.transaction.date >= :startDate " +
                            "and r.transaction.date <= :endDate " +
                            "and (r.clientName like :searchTextPartial " +
                            "or r.employeeName like :searchTextPartial " +
                            "or r.tableName like :searchTextPartial)) " +
                            "order by r.id desc ", SimpleReceipt.class)
                    .setParameter("id", parseInt)
                    .setParameter("searchTextPartial", "%" + textFilter + "%")
                    .setParameter("startDate", startDate)
                    .setParameter("endDate", endDate);

            return query;
        }

        public List<SimpleReceipt> queryAll()
        {
            return Database.getInstance().connectQuery(session ->
            {
                return generateQuery(session).list();
            });
        }
    }
}
