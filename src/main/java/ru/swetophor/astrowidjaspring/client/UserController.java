package ru.swetophor.astrowidjaspring.client;

import ru.swetophor.astrowidjaspringshell.mainframe.Main;
import ru.swetophor.astrowidjaspringshell.model.ChartList;
import ru.swetophor.astrowidjaspringshell.model.ChartObject;

public interface UserController {

    void welcome();

    void mainCycle(Main main);

    boolean mergeChartIntoList(ChartList list, ChartObject nextChart, String listName);

    boolean confirmationAnswer(String prompt);
}
