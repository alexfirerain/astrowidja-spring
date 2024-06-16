package ru.swetophor.astrowidjaspring.client;

import ru.swetophor.astrowidjaspring.mainframe.Main;
import ru.swetophor.astrowidjaspring.model.chart.ChartList;
import ru.swetophor.astrowidjaspring.model.chart.ChartObject;

public interface UserController {

    void welcome();

    void mainCycle(Main main);

    boolean mergeChartIntoList(ChartList list, ChartObject nextChart, String listName);

    boolean confirmationAnswer(String prompt);
}
