package ru.swetophor.astrowidjaspring.client;

import org.springframework.stereotype.Component;
import ru.swetophor.astrowidjaspring.exception.ChartNotFoundException;
import ru.swetophor.astrowidjaspring.exception.EmptyRequestException;
import ru.swetophor.astrowidjaspring.mainframe.Main;
import ru.swetophor.astrowidjaspring.model.astro.Astra;
import ru.swetophor.astrowidjaspring.model.astro.AstraEntity;
import ru.swetophor.astrowidjaspring.model.chart.Chart;
import ru.swetophor.astrowidjaspring.model.chart.ChartList;
import ru.swetophor.astrowidjaspring.model.chart.ChartObject;
import ru.swetophor.astrowidjaspring.model.chart.MultiChart;
import ru.swetophor.astrowidjaspring.service.HarmonicService;
import ru.swetophor.astrowidjaspring.service.LibraryService;
import ru.swetophor.astrowidjaspring.utils.Mechanics;

import java.util.Scanner;
import java.util.Set;

import static ru.swetophor.astrowidjaspring.config.Settings.*;
import static ru.swetophor.astrowidjaspring.config.Settings.saveSettings;
import static ru.swetophor.astrowidjaspring.mainframe.ActiveScreen.*;
import static ru.swetophor.astrowidjaspring.mainframe.ActiveScreen.MAIN;
import static ru.swetophor.astrowidjaspring.mainframe.Main.DEFAULT_ASTRO_SET;
import static ru.swetophor.astrowidjaspring.utils.Decorator.*;

@Component
public class CommandLineController implements UserController {

    public static final Scanner KEYBOARD = new Scanner(System.in);
    static final Set<String> yesValues = Set.of("да", "+", "yes", "true", "д", "y", "t", "1");
    static final Set<String> noValues = Set.of("нет", "-", "no", "false", "н", "n", "f", "0");

    public static boolean negativeAnswer(String value) {
        return noValues.contains(value.toLowerCase());
    }

    public static boolean positiveAnswer(String value) {
        return yesValues.contains(value.toLowerCase());
    }

    /*
        Циклы меню
     */
    @Override
    public void mainCycle(Main application) {
        welcome();
        ChartList desk = application.DESK;
        displayDesk(desk);
        boolean exit = false;
        while (!exit) {
            showMainMenu();
            switch (getUserInput()) {
                case "1", "стол" -> displayDesk(desk);
                case "2", "настройки" -> editSettings(application);
                case "3", "карты" -> libraryCycle(application);
                case "4", "анализ" -> workCycle(application);
                case "5", "добавить" -> addChartFromUserInput(desk);
                case "0", "выход" -> exit = true;
            }
        }
        print("Спасибо за ведание резонансов!");
    }

    /**
     * Экран работы с настройками.
     * Отображает действующие настройки и позволяет изменять их.
     */
    private void editSettings(Main application) {
        application.setActiveScreen(SETTINGS);
        showSettingsMenu();

        while (true) {
            String command = getUserInput();
            if (command.isBlank()) {
                application.setActiveScreen(MAIN);
                break;
            }
            int delimiter = command.indexOf("=");
            if (delimiter == -1) {
                print("Команда должна содержать оператор '='");
                continue;
            }
            String parameter = command.substring(0, delimiter).trim();
            String value = command.substring(delimiter + 1).trim();
            try {
                switch (parameter) {
                    case "1", "h", "harmonic" -> setEdgeHarmonic(Integer.parseInt(value));
                    case "2", "d", "divisor" -> setOrbDivider(Integer.parseInt(value));
                    case "3", "r", "reduction" -> {
                        if (positiveAnswer(value)) enableHalfOrbForDoubles();
                        if (negativeAnswer(value)) disableHalfOrbForDoubles();
                    }
                    case "4", "s", "autosave" -> {
                        if (positiveAnswer(value)) setAutosave(true);
                        if (negativeAnswer(value)) setAutosave(false);
                    }
                    case "5", "l", "autoload" -> setAutoloadFile(value);
                    default -> print("Введи номер или псевдоним существующего параметра, а не вот это вот '%s'"
                            .formatted(parameter));
                }
            } catch (NumberFormatException e) {
                print("Не удалось прочитать значение.");
            }
        }
        saveSettings();
    }


    private void libraryCycle(Main application) {
        application.setActiveScreen(LIBRARY);
        LibraryService libraryService = application.getLibraryService();
        ChartList DESK = application.DESK;
        showLibraryMenu();
        while (true) {
            String input = getUserInput();

            // выход из цикла
            if (input == null || input.isBlank()) {
                application.setActiveScreen(MAIN);
                return;
            }

            // вывод списка групп (файлов)
            if (input.equals("=")) {
                printInAsterisk(libraryService.listAlbums());

                // вывод двухуровневого списка групп (файлов) и карт в них
            } else if (input.equals("==")) {
                printInAsterisk(libraryService.listAlbumsContents());

            } else if (input.equals("+=")) {
                libraryService.updateLibrary();

            // удаление файла (группы)
            } else if (input.toLowerCase().startsWith("xxx") || input.toLowerCase().startsWith("ххх")) {
                print(libraryService.deleteAlbum(extractOrder(input, 3)));

                // очистка стола и загрузка в него карт из группы (из файла)
            } else if (input.endsWith(">>")) {
                try {
                    DESK.substitute(libraryService.findList(extractOrder(input, -2)));
                    displayDesk(DESK);
                } catch (IllegalArgumentException e) {
                    print(e.getLocalizedMessage());
                }

                // добавление къ столу карт из группы (из файла)
            } else if (input.endsWith("->")) {
                try {
                    DESK.addAll(libraryService.findList(extractOrder(input, -2)));
                    displayDesk(DESK);
                } catch (IllegalArgumentException e) {
                    print(e.getLocalizedMessage());
                }

                // сохранение стола в новый файл (группу)
            } else if (input.startsWith(">>")) {
                libraryService.saveChartsAsAlbum(DESK, extractOrder(input, 2));

                // добавление стола к существующему файлу (группе)
            } else if (input.startsWith("->")) {
                print(libraryService.addChartListToAlbum(DESK, extractOrder(input, 2)));
            }
        }

    }

    /**
     * Цикл работы с картой.
     * Предоставляет действия, которые можно выполнить с картой: просмотр статистики,
     * сохранение в список (файл), построение средней и синастрической карт.
     * Пустой ввод означает выход из цикла и метода.
     */
    public void workCycle(Main application) {
        application.setActiveScreen(WORK);
        ChartObject activeChart = application.getActiveChart();
        LibraryService libraryService = application.getLibraryService();
        HarmonicService harmonicService = application.getHarmonicService();
        ChartList DESK = application.DESK;

        if (activeChart == null)
            application.setActiveChart(activeChart = selectChartOnDesk(DESK));

        if (activeChart == null) {
            application.setActiveScreen(MAIN);
            return;
        }

        print(activeChart.getCaption());
        showChartActionsMenu();
        String input;
        while (true) {
            input = getUserInput();
            if (input == null || input.isBlank()) {
                application.setActiveScreen(MAIN);
                return;
            }

            if (input.startsWith("->")) {
                libraryService.addChartsToAlbum(extractOrder(input, 2), activeChart);

            } else if (input.startsWith("+")) {
                ChartObject counterpart;
                String order = extractOrder(input, 1);
                try {
                    counterpart = findChart(DESK, order, "на столе");
                    print(addChart(new MultiChart(activeChart, counterpart), DESK));
                } catch (ChartNotFoundException e) {
                    print("Карта '%s' не найдена: %s".formatted(order, e.getLocalizedMessage()));
                }

            } else if (input.startsWith("*")) {
                if (activeChart instanceof Chart) {
                    ChartObject counterpart;
                    String order = extractOrder(input, 1);
                    try {
                        counterpart = findChart(DESK, order, "на столе");
                        print(counterpart instanceof Chart ?
                                addChart(Mechanics.composite((Chart) activeChart, (Chart) counterpart), DESK) :
                                "Композит строится для двух одинарных карт.");
                    } catch (ChartNotFoundException e) {
                        print("Карта '%s' не найдена: %s".formatted(order, e.getLocalizedMessage()));
                    }
                } else {
                    print("Композит строится для двух одинарных карт.");
                }

            }
            else switch (input) {
                    case "=" -> {
                        application.setActiveChart(activeChart = selectChartOnDesk(DESK));
                        if (activeChart == null) {
                            application.setActiveScreen(MAIN);
                            return;
                        }
                    }
                    case "1" -> print(activeChart.getAstrasList());
                    case "2" -> print(harmonicService.calculateAspectTable(activeChart).getAspectReport());
                    case "3" -> print(harmonicService.calculatePatternTable(activeChart).getPatternReport(false));
                    case "4" -> print(harmonicService.calculatePatternTable(activeChart).getPatternReport(true));
                }
        }
    }


    /*
        Получатели ввода пользователя.
     */
    /**
     * Получает ввод пользователя.
     * @return строку, введённую юзером с клавиатуры.
     */
    public String getUserInput() {
        return KEYBOARD.nextLine().trim();
    }
    /**
     * Получает ввод пользователя вслед приглашению.
     * @param prompt    приглашение.
     * @return  то, что введёт пользователь.
     */
    public String getUserInput(String prompt) {
        console(prompt + ": ");
        return getUserInput();
    }
    /**
     * Интерактивный получатель данных карты от пользователя.
     * Предлагает ввести название, затем координаты в виде "градусы минуты секунды"
     * (или "знак градусы минуты секунды", где знак это число от 1 до 12)
     * для каждой стандартной {@link AstraEntity АстроСущности}. Затем предлагает вводить
     * дополнительные {@link Astra астры} в виде "название градусы минуты секунды".
     * Пустой ввод означает пропуск астры или отказ от дополнительного ввода.

     * @return  многострочный пользовательский ввод,
     * соответствующий текстовому представлению данных карты,
     * аналогично как для сохранения.
     */
    public String getUserChartInput() {
        StringBuilder userOrder = new StringBuilder();
        userOrder.append(getUserInput("Название новой карты")).append("\n");
        for (AstraEntity a : DEFAULT_ASTRO_SET) {
            String input = getUserInput(a.name);
            if (!input.isBlank())
                userOrder.append("%s %s%n".formatted(a.name, input));
        }
        console("Ввод дополнительных астр в формате\n'название градусы минуты секунды':\n");
        String input = getUserInput();
        while (!input.isBlank()) {
            userOrder.append(input).append("\n");
            input = getUserInput();
        }
        return userOrder.toString();
    }
    /*
        Процедуры интерфейса.
     */

    @Override
    public void welcome() {
        print("%sСчитаем резонансы с приближением в %.0f° (1/%d часть круга) до числа %d%n%n"
                .formatted(asteriskFrame("Начато исполнение АстроВидьи!"),
                        getPrimalOrb(),
                        getOrbDivisor(),
                        getEdgeHarmonic()));
    }
    private void showMainMenu() {
        String MENU = """
                1. карты на столе
                2. настройки
                3. списки карт
                4. работа с картой
                5. добавить карту с клавиатуры
                0. выход
                """;
        printInDoubleFrame(MENU);
    }
    private void showSettingsMenu() {
        String MENU = """
                                * НАСТРОЙКИ *
                
                1: крайняя гармоника: %d
                2: делитель для первичного орбиса: %d
                             (первичный орбис = %s)
                3: для двойных карт орбис уменьшен вдвое: %s
                4: автосохранение стола при выходе: %s
                5: файл загрузки при старте: %s
                
                    _   _   _   _   _   _   _   _   _
                < введи новое как "номер_параметра = значение"
                        или пустой ввод для выхода >
                
                """;
        printInFrame(MENU.formatted(
                getEdgeHarmonic(),
                getOrbDivisor(),
                Mechanics.secondFormat(getPrimalOrb(), false),
                isHalfOrbsForDoubles() ? "да" : "нет",
                isAutosave() ? "да" : "нет",
                getAutoloadFile()
        ));
    }
    private void showLibraryMenu() {
        String LIST_MENU = """
                ("список" — список по номеру или имени,
                 "карты" — карты по номеру или имени через пробел)
                    =               = список файлов в базе
                    ==              = полный список файлов и карт
                    ххх список      = удалить файл
                
                    список >>       = заменить стол на список
                    список ->       = добавить список ко столу
                    >> список       = заменить файл столом
                    -> список       = добавить стол к списку
                
                    карты -> список         = добавить карты со стола к списку
                    список:карты -> список  = переместить карты из списка в список
                    список:карты +> список  = копировать карты из списка в список
                
                    [пусто]        = выход в главное меню
                """;
        printInSemiDouble(LIST_MENU);
    }
    private void showChartActionsMenu() {
        String CHART_MENU = """         
                    действия с картой:
                "-> имя_файла"  = сохранить в файл
                "+карта"        = построить синастрию
                "*карта"        = построить композит
                
                "1" = о положениях астр
                "2" = о резонансах
                "3" = о паттернах кратко
                "4" = о паттернах со статистикой
                
                "="            = выбор карты со стола
                [пусто]        = выход в главное меню
                """;
        printInFrame(CHART_MENU);
    }

    /**
     * Выводит на экран список карт, лежащих на {@link Main#DESK столе}, то есть загруженных в программу.
     */
    private void displayDesk(ChartList desk) {
        printInFrame(desk.isEmpty() ?
                "На столе нет ни одной карты." :
                desk.toString()
        );
    }

    /**
     * Запрашивает, какую карту из списка (со {@link Main#DESK стола}) взять в работу,
     * т.е. запустить в {@link #workCycle(Main) цикле процедур для карты}.
     * Если карта не опознана по номеру на столе или имени, сообщает об этом.
     *
     * @return  найденную по номеру или имени карту со стола, или {@code ПУСТО}, если не найдено.
     */
    private ChartObject selectChartOnDesk(ChartList desk) {
        String order = getUserInput("Укажите карту по имени или номеру на столе");
        try {
            return findChart(desk, order, "на столе");
        } catch (ChartNotFoundException e) {
            if (!(e instanceof EmptyRequestException))
                print("Карты '%s' не найдено: %s"
                    .formatted(order, e.getLocalizedMessage()));
            return null;
        }
    }

    /**
     * Находит в этом списке карту, заданную по имени или номеру в списке (начинающемуся с 1).
     * Если запрос состоит только из цифр, рассматривает его как запрос по номеру,
     * иначе как запрос по имени.
     * @param order запрос, какую карту ищем в списке: по имени или номеру (с 1).
     * @param inList    строка, описывающая этот список в местном падеже.
     * @return  найденный в списке объект, соответствующий запросу.
     * @throws ChartNotFoundException   если в списке не найдено соответствующих запросу объектов.
     */
    public ChartObject findChart(ChartList list, String order, String inList) throws ChartNotFoundException {
        if (order == null || order.isBlank())
            throw new EmptyRequestException("Пустой запрос.");
        if (!inList.startsWith("на "))
            inList = "в " + inList;

        if (order.matches("^\\d+"))
            try {
                int i = Integer.parseInt(order) - 1;
                if (i >= 0 && i < list.size())
                    return list.get(i);
                else
                    throw new ChartNotFoundException("Всего %d карт %s%n"
                            .formatted(list.size(), inList));
            } catch (NumberFormatException e) {
                throw new ChartNotFoundException("Число не распознано.");
            }
        else if (list.contains(order)) {
            return list.get(order);
        } else {
            for (String name : list.getNames())
                if (name.startsWith(order))
                    return list.get(name);

            throw new ChartNotFoundException("Карты '%s' нет %s%n"
                    .formatted(order, inList));
        }
    }

    /**
     * Добавляет карту на {@link Main#DESK стол}.
     * Если карта с таким именем уже
     * присутствует, запрашивает решение у юзера.
     *
     * @param chart добавляемая карта.
     * @return  строку, сообщающую итог операции.
     */
    private String addChart(ChartObject chart, ChartList desk) {
        if (mergeChartIntoList(desk, chart, "на столе"))
            return "Карта загружена на стол: " + chart.getName();
        else
            return "Карта не загружена.";
    }

    /**
     * Добавляет карту к списку. Если имя добавляемой карты в нём уже содержится,
     * запрашивает решение у астролога, требуя выбора одного из трёх вариантов:
     * <li>переименовать – запрашивает новое имя для добавляемой карты и добавляет обновлённую;</li>
     * <li>обновить – ставит новую карту на место старой карты с этим именем;</li>
     * <li>заменить – удаляет из списка карту с конфликтным именем, добавляет новую;</li>
     * <li>отмена – карта не добавляется.</li>
     *
     * @param list  список, в который вливается карта.
     * @param nextChart добавляемая карта.
     * @param listName      название файла или иного списка, в который добавляется карта, в предложном падеже.
     * @return {@code ДА}, если добавление карты (с переименованием либо с заменой) состоялось,
     *          или {@code НЕТ}, если была выбрана отмена.
     */
    @Override
    public boolean mergeChartIntoList(ChartList list, ChartObject nextChart, String listName) {
        if (!list.contains(nextChart.getName())) {
            return list.addItem(nextChart);
        }
        while (true) {
            print("""
                            
                            Карта с именем '%s' уже есть %s:
                            1. добавить под новым именем
                            2. заменить присутствующую в списке
                            3. удалить старую, добавить новую в конец списка
                            0. отмена
                            """.formatted(nextChart.getName(),
                    listName.startsWith("на ") ?
                            listName : "в " + listName));
            switch (getUserInput()) {
                case "1" -> {
                    String rename;
                    do {
                        print("Новое имя: ");
                        rename = getUserInput();         // TODO: допустимое имя
                        print("\n");
                    } while (list.contains(rename));
                    nextChart.setName(rename);
                    return list.addItem(nextChart);
                }
                case "2" -> {
                    list.setItem(list.indexOf(nextChart.getName()), nextChart);
                    return true;
                }
                case "3" -> {
                    list.remove(nextChart.getName());
                    return list.addItem(nextChart);
                }
                case "0" -> {
                    print("Отмена добавления карты: " + nextChart.getName());
                    return false;
                }
            }
        }
    }

    /**
     * Добавляет карту к картосписку. В случае возникновения коллизии имени
     * не запрашивает у человека, а поступает согласно параметру:
     * если {@code заменять = ДА}, обновляет данные карты,
     * если {@code заменять = НЕТ}, игнорирует карту с повторным именем.
     * @param list  список, в который добавляем.
     * @param chart добавляемая карта.
     * @param replace   заменять ли карту с совпавшим именем.
     * @return  изменился ли список в результате вызова.
     */
    public boolean mergeChartToListSilently(ChartList list, ChartObject chart, boolean replace) {
        if (!list.contains(chart.getName()))
            return list.addItem(chart);
        if (replace) {
            list.setItem(list.indexOf(chart), chart);
            return true;
        }
        return false;
    }

    /**
     * Добавляет к указанному списку (Столу) карту на основе юзерского ввода.
     *
     */
    public void addChartFromUserInput(ChartList list) {
        print(addChart(Chart.readFromString(getUserChartInput()), list));
    }

    /**
     * Создаёт карту на основе юзерского ввода.
     * @return {@link Chart одиночную карту}, созданную на основе ввода.
     */
    public Chart getChartFromUserInput(String userInput) {
        // TODO: вообще-то это должен быть построитель карты на основе заданного момента времени
        return Chart.readFromString(userInput);
    }

    /**
     * Задаёт пользователю вопрос и возвращает булево, соответствующее его ответу.
     * @param prompt    вопрос, который спрашивает программа.
     * @return  {@code ДА} или {@code НЕТ} сообразно вводу пользователя.
     */
    @Override
    public boolean confirmationAnswer(String prompt) {
        printInFrame(prompt);
        while (true) {
            String answer = getUserInput();
            if (positiveAnswer(answer)) return true;
            if (negativeAnswer(answer)) return false;
            print("Введи да или нет, надо определить, третьего не дано.");
        }
    }

    /**
     * Извлекает из строки аргумент, удаляя из её начала или конца оператор в заданной позиции.
     * @param input входная строка.
     * @param offset    какая часть строки откусывается.
     *                  Если положительное число, то от начала, если отрицательное, то от конца.
     * @return  входную строку с удалённым из неё оператором в указанной позиции.
     */
    private String extractOrder(String input, int offset) {
        return offset >= 0 ?
                input.trim().substring(offset).trim() :
                input.trim().substring(0, input.length() + offset).trim();
    }
}
