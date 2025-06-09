package org.lebastudios.theroundtableplugins.rm;

import com.sun.javafx.collections.ObservableListWrapper;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import lombok.Getter;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.lebastudios.theroundtable.components.DateRangePicker;
import org.lebastudios.theroundtable.controllers.PaneController;
import org.lebastudios.theroundtable.database.Database;
import org.lebastudios.theroundtable.events.Event1;
import org.lebastudios.theroundtable.events.IEventMethod1;
import org.lebastudios.theroundtable.locale.Translator;
import org.lebastudios.theroundtableplugins.rm.analyzers.HoursOfActivityDataAnalyzer;
import org.lebastudios.theroundtableplugins.rm.analyzers.IDataAnalyzer;
import org.lebastudios.theroundtableplugins.rm.analyzers.IncomeDataAnalyzer;
import org.lebastudios.theroundtableplugins.rm.analyzers.ProductsSoldAnalyzer;
import org.lebastudios.theroundtableplugins.rm.entities.SimpleReceipt;
import org.lebastudios.theroundtable.components.LoadingPaneController;
import org.lebastudios.theroundtable.components.PaginableListView;
import org.lebastudios.theroundtable.components.SearchBox;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public class ReceiptManagerPaneController extends PaneController<ReceiptManagerPaneController>
{
    @Getter private static ReceiptManagerPaneController instance;

    @FXML public PaginableListView<SimpleReceipt> receiptList;
    @FXML public BorderPane rightView;
    @FXML public DateRangePicker dateRangePicker;
    @FXML public TabPane statsTabPane;
    @FXML public SearchBox searchBox;

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
        addAnalyzerTab(new HoursOfActivityDataAnalyzer(), Translator.getInstance().t("rm:word.activity"));
        addAnalyzerTab(new IncomeDataAnalyzer(), Translator.getInstance().t("rm:word.income"));
        addAnalyzerTab(new ProductsSoldAnalyzer(), Translator.getInstance().t("rm:word.productssold"));

        receiptList.setReciclablePaneFactory(ReceiptLabelController::new);
        
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

        dateRangePicker.setOnDateChange((from, to) ->
        {
            LocalDate start = from != null ? from : LocalDate.now();
            LocalDate end = to != null ? to : LocalDate.now();

            refreshReceiptsListView(
                    searchBox.getText(),
                    start.atStartOfDay(),
                    end.atTime(LocalTime.MAX)
            );
        });
        
        searchBox.setOnSearch(searchText -> refreshReceiptsListView(
                searchText,
                dateRangePicker.getStartDate().getValue().atStartOfDay(),
                dateRangePicker.getEndDate().getValue().atTime(LocalTime.MAX)
        ));

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

    private record ListItemsGenerator(String textFilter, LocalDateTime startDate, LocalDateTime endDate)
            implements PaginableListView.ItemsGenerator<SimpleReceipt>
    {
        private static final String COMMON_QUERY_SQL = "from Receipt r " +
                "where r.id = :id or (r.transaction.date >= :startDate " +
                "and r.transaction.date <= :endDate " +
                "and (r.clientName like :searchTextPartial " +
                "or r.transaction.account.name like :searchTextPartial " +
                "or r.tableName like :searchTextPartial)) " +
                "order by r.id desc";

        @Override
        public List<SimpleReceipt> generateItems(int from, int to)
        {
            return Database.getInstance().connectQuery(session ->
            {
                return generateQuery(session).setFirstResult(from)
                        .setMaxResults(to).list();
            });
        }

        @Override
        public long count()
        {
            return Database.getInstance().connectQuery(session ->
            {
                Query<Long> countQuery = session.createQuery("select count(*) " + COMMON_QUERY_SQL, Long.class);

                assignQueryParams(countQuery);

                return countQuery.getSingleResult();
            });
        }

        public List<SimpleReceipt> queryAll()
        {
            return Database.getInstance().connectQuery(session ->
            {
                return generateQuery(session).list();
            });
        }

        private Query<SimpleReceipt> generateQuery(Session session)
        {
            Query<SimpleReceipt> contentQuery = session.createQuery(
                    "select new SimpleReceipt(r.id, r.transaction.date, r.status) " + COMMON_QUERY_SQL,
                    SimpleReceipt.class
            );

            assignQueryParams(contentQuery);

            return contentQuery;
        }

        private void assignQueryParams(Query<?> query)
        {
            final var parseInt = textFilter.matches("\\d+") ? Integer.parseInt(textFilter) : -1;

            query.setParameter("id", parseInt)
                    .setParameter("searchTextPartial", "%" + textFilter + "%")
                    .setParameter("startDate", startDate)
                    .setParameter("endDate", endDate);
        }
    }
}
