package ru.swetophor.astrowidjaspring.model.chart;


import lombok.Getter;
import ru.swetophor.astrowidjaspring.exception.ChartNotFoundException;
import ru.swetophor.astrowidjaspring.utils.Mechanics;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Список карт-объектов, воспроизводящий многие функции обычного списка.
 * Список поддерживает уникальность имён содержащихся карт и обращение
 * к картам по имени карты.
 */
public class ChartList {
    /**
     * Карты, хранимые в списке.
     * -- GETTER --
     *  Отдаёт список всех присутствующих карт в историческом порядке.

     */
    @Getter
    private final List<ChartObject> charts = new ArrayList<>();
    /**
     * Имена хранимых в списке карт.
     * -- GETTER --
     *  Отдаёт список всех имён присутствующих карт.
     *
     * @return список имён карт в историческом порядке.

     */
    @Getter
    private final List<String> names = new ArrayList<>();
    private String listName = "список карт";
    protected transient int modCount = 0;

    public ChartList() {
    }

    public ChartList(String listName) {
        if (listName != null)
            this.listName = listName;
    }

    public ChartList(List<ChartObject> charts) {
        this();
        addAll(charts);
    }

    public ChartList(String listName, List<ChartObject> charts) {
        this(listName);
        addAll(charts);
    }

    @Override
    public String toString() {
        return IntStream.range(0, size())
                .mapToObj(i -> "%d. %s%n"
                        .formatted(i + 1, get(i).getName()))
                .collect(Collectors.joining());
    }

    /**
     * Содержит ли указанный список указанное имя карты.
     *
     * @param content   список, в котором проверяем.
     * @param chartName проверяемое имя.
     * @return есть ли карта с таким именем в этом списке.
     */
    public static boolean containsName(List<? extends ChartObject> content, String chartName) {
        return content.stream()
                .anyMatch(c -> c.getName()
                        .equals(chartName));
    }

    /**
     * Содержит ли указанный список карт указанное имя карты.
     *
     * @param content   список карт, в котором проверяем.
     * @param chartName проверяемое имя.
     * @return есть ли карта с таким именем в этом списке.
     */
    public static boolean containsName(ChartList content, String chartName) {
        return content.getNames().contains(chartName);
    }

    /**
     * Процедура добавления карты к списку. Если название карты входит в
     * коллизию с именем уже присутствующей карты, запускается интерактивная процедура
     * в соответствии с методом {@link Mechanics#resolveCollision(ChartList, ChartObject, String)}.
     *
     * @param chart      добавляемая карта.
     * @param toListName название пополняемого списка (в предложном падеже,
     *                   аналогично вышеуказанному методу).
     * @return {@code ДА}, если список был обновлён в результате операции, или {@code НЕТ},
     * если была выбрана отмена.
     */
    public boolean addResolving(ChartObject chart, String toListName) {
        return contains(chart.getName()) ?
                Mechanics.resolveCollision(this, chart, toListName) :
                addItem(chart);
    }

    /**
     * Выдаёт текстовое представление всех карт списка
     * в том виде, как они хранятся в daw-файле.
     *
     * @return многостроку, содержащую строковые представления всех
     * карт и описания многокарт, содержащихся в этом списке.
     */
    public String getString() {
        // отделим карты
        List<Chart> extraCharts = charts.stream()
                // которые в присутствующие многокарты
                .filter(c -> c instanceof MultiChart)
                // входят в качестве компонентов
                .flatMap(chartObject -> Arrays.stream(chartObject.getData()))
                // и при этом которых пока нет в этом картосписке по отдельности
                .filter(c -> !contains(c))
                .toList();
        // так добавим же эти карты в конец картосписка
        charts.addAll(extraCharts);
        // и выдадим конкатенацию строкового представления всех карт и многокарт
        return charts.stream()
                .map(ChartObject::toStoringString)
                .collect(Collectors.joining());
    }

    /**
     * @return количество карт-объектов в этом списке.
     */
    public int size() {
        return charts.size();
    }

    /**
     * @return является ли этот список пустым.
     */
    public boolean isEmpty() {
        return charts.isEmpty();
    }


    /**
     * Применяет по конвейеру указанное действие последовательно ко всем картам,
     * контролируя, что список карт не изменился в его ходе.
     *
     * @param action действие, которое будет последовательно применено ко всем картам.
     */
    public void forEach(Consumer<? super ChartObject> action) {
        Objects.requireNonNull(action);
        int expectedModCount = this.modCount;
        int size = size();

        for (int i = 0; this.modCount == expectedModCount && i < size; ++i)
            action.accept(charts.get(i));

        if (this.modCount != expectedModCount)
            throw new ConcurrentModificationException();
    }

    /**
     * Добавляет в список указанную карту.
     * Если карта с таким именем уже в наличии, запускает интерактивную процедуру разрешения
     * конфликта {@link Mechanics#resolveCollision(ChartList, ChartObject, String)} (с названием списка "этот список").
     *
     * @param chart добавляемая.
     * @return {@code true}, если список изменился в результате операции.
     */
    public boolean add(ChartObject chart) {
        if (chart == null) {
            charts.add(null);
            names.add(null);
            ++this.modCount;
            return true;
        }
        int mod = this.modCount;
        if (contains(chart.getName()))
            Mechanics.resolveCollision(this, chart, "этом списке");
        else
            addItem(chart);
        return this.modCount != mod;
    }

    /**
     * Добавляет к этому картосписку все карты из коллекции.
     * @param collection любая коллекция карт.
     * @return  {@code true}, если картосписок изменился
     * в результате вызова.
     */
    public boolean addAll(Collection<ChartObject> collection) {
        if (collection == null) return false;
        int mod = this.modCount;
        collection.forEach(this::add);
        return this.modCount != mod;
    }

    /**
     * Добавляет все карты коллекции к списку в определённую позицию.
     * @param i          в какую позицию вставлять новые карты.
     * @param collection    коллекция, из которой добавляем.
     * @return  {@code ДА}, если картосписок изменился в результате.
     */
    public boolean addAll(int i, Collection<ChartObject> collection) {
        ++this.modCount;
        names.addAll(i, collection.stream().map(ChartObject::getName).toList());
        return charts.addAll(i, collection);
    }

    public boolean substitute(ChartList collection) {
        clear();
        return addAll(collection);
    }

    /**
     * Сортирует карты сообразно полученному компаратору,
     * затем перезаполняет список имён в соответствие с обновлённым
     * списком карт.
     *
     * @param c компаратор для внесения порядка в список карт.
     */
    public void sort(Comparator<? super ChartObject> c) {
        charts.sort(c);
        names.clear();
        names.addAll(chartsToNames(charts));
        ++this.modCount;
    }

    /**
     * Выдаёт
     * @return массив с содержащимися картами.
     */
    public ChartObject[] toArray() {
        return charts.toArray(ChartObject[]::new);
    }

    /**
     * Опустошает список карт (и имён).
     */
    public void clear() {
        if (!isEmpty()) {
            charts.clear();
            names.clear();
            ++this.modCount;
        }
    }

    /**
     * Заменяет элемент с указанным номером в списке на указанный объект.
     *
     * @param i           в какую позицию ставим элемент.
     * @param chartObject что за элемент.
     * @return объект, находившийся по этому адресу прежде.
     * @throws IndexOutOfBoundsException если указана неадекватная позиция i.
     */
    public ChartObject setItem(int i, ChartObject chartObject) {
        names.set(i, chartObject.getName());
        ++this.modCount;
        return charts.set(i, chartObject);
    }

    /**
     * @param i
     * @param chartObject
     */
    public void insertItem(int i, ChartObject chartObject) {
        charts.add(i, chartObject);
        names.add(i, chartObject.getName());
    }

    /**
     * @param i
     * @return
     */
    public ChartObject remove(int i) {
        names.remove(i);
        ++this.modCount;
        return charts.remove(i);
    }

    /**
     * Сообщает первый найденный индекс элемента в этом списке,
     * соответствующего карте с таким именем.
     * @param name  имя искомой карты.
     * @return  индекс искомой карты в этом списке,
     * или -1, если не найдено.
     */
    public int indexOf(String name) {
        return names.indexOf(name);
    }
    /**
     * @param o
     * @return
     */
    public int indexOf(ChartObject o) {
        return charts.indexOf(o);
    }

    /**
     * @param name
     * @return
     */
    public int lastIndexOf(String name) {
        return names.lastIndexOf(name);
    }

    /**
     * @param o
     * @return
     */
    public int lastIndexOf(ChartObject o) {
        return charts.lastIndexOf(o);
    }

    /**
     * Возвращает новый список карт, содержащий карты этого списка
     * с номера i включительно до номера i1 исключительно.
     *
     * @param i  первый номер карты в списке, который
     *           попадёт в новый список.
     * @param i1 первый номер карты в списке, который не
     *           попадёт в новый список.
     * @return новый список карт, включающий карты этого списка с i до i1.
     */
    public ChartList subList(int i, int i1) {
        return new ChartList(charts.subList(i, i1));
    }

    /**
     * @return поток из карт-объектов.
     */
    public Stream<ChartObject> stream() {
        return charts.stream();
    }

    /**
     * @return параллельный поток из карт-объектов, как он определён в {@link ArrayList}.
     */
    public Stream<ChartObject> parallelStream() {
        return charts.parallelStream();
    }

    /**
     * Удаляет из списка карт все элементы, которые не присутствуют в указанном собрании.
     *
     * @param collection любое собрание карт-элементов.
     * @return {@code истинно}, если этот список изменился в результате вызова.
     */
    public boolean retainAll(Collection<ChartObject> collection) {
        names.retainAll(chartsToNames(collection));
        boolean changed = charts.retainAll(collection);
        if (changed) ++this.modCount;
        return changed;
    }

    private static List<String> chartsToNames(Collection<ChartObject> collection) {
        return collection.stream().map(ChartObject::getName).toList();
    }

    /**
     * Удаляет из списка карт все элементы, которые присутствуют в указанном собрании.
     *
     * @param collection любое собрание карт-элементов.
     * @return {@code истинно}, если этот список изменился в результате вызова.
     */
    public boolean removeAll(Collection<ChartObject> collection) {
        names.removeAll(chartsToNames(collection));
        boolean changed = charts.removeAll(collection);
        if (changed)
            ++this.modCount;
        return changed;
    }

    /**
     * Присутствуют ли в этом списке все указанные карты.
     *
     * @param collection собрание карт.
     * @return да, если все суть, нет, если хотя бы какой-то нет.
     */
    public boolean containsAll(Collection<ChartObject> collection) {
        return new HashSet<>(charts).containsAll(collection);
    }

    /**
     * Добавляет карту и имя карты в соответствующие списки
     * без дополнительных проверок уникальности имени.
     *
     * @param chart добавляемая карта.
     */
    public boolean addItem(ChartObject chart) {
        boolean add = charts.add(chart);
        if (add) {
            ++this.modCount;
            names.add(chart.getName());
        }
        return add;
    }

    /**
     * Даёт карту по её индексу в списке.
     *
     * @param i индекс карты в списке.
     * @return карту из списка с соответствующим индексом,
     * или {@code НУЛЬ}, если указан индекс за пределами списка.
     */
    public ChartObject get(int i) {
        return i >= 0 && i < charts.size() ?
                charts.get(i) :
                null;
    }

    /**
     * Даёт карту по её имени.
     *
     * @param name имя карты, которую хотим получить.
     * @return карту с соответствующим именем, или {@code НУЛЬ},
     * если карты с таким именем здесь нет.
     */
    public ChartObject get(String name) {
        return get(indexOf(name));
    }

    /**
     * Содержит ли этот список карту с таким именем.
     *
     * @param name имя, которое есть ли.
     * @return да, если есть, нет, если нет.
     */
    public boolean contains(String name) {
        return names.contains(name);
    }

    /**
     * Содержит ли этот список указанную карту.
     *
     * @param chart карта, которая есть ли.
     * @return да, если есть, нет, если нет.
     */
    public boolean contains(ChartObject chart) {
        return charts.contains(chart);
    }

    /**
     * Удаляет из списка карту с указанным именем.
     *
     * @param name имя карты, которую нужно удалить.
     * @throws IndexOutOfBoundsException если карта
     *                                   с указанным именем отсутствует.
     */
    public void remove(String name) {
        int index = names.indexOf(name);
        charts.remove(index);
        names.remove(index);
    }

    /**
     * Удаляет из списка указанную карту.
     *
     * @param item карта, которую нужно удалить.
     * @throws IndexOutOfBoundsException если указанная карта
     *                                   отсутствует.
     */
    public void remove(ChartObject item) {
        int index = charts.indexOf(item);
        names.remove(index);
        charts.remove(index);
    }

    public boolean addAll(ChartList adding) {
        return addAll(adding.getCharts());
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
    public ChartObject findChart(String order, String inList) throws ChartNotFoundException {
        if (order == null || order.isBlank())
            throw new ChartNotFoundException("Пустой запрос.");
        if (!inList.startsWith("на "))
            inList = "в " + inList;

        if (order.matches("^\\d+"))
            try {
                int i = Integer.parseInt(order) - 1;
                if (i >= 0 && i < size())
                    return get(i);
                else
                    throw new ChartNotFoundException("Всего %d карт %s%n"
                            .formatted(size(), inList));
            } catch (NumberFormatException e) {
                throw new ChartNotFoundException("Число не распознано.");
            }
        else if (contains(order))
            return get(order);
        else
            throw new ChartNotFoundException("Карты '%s' нет %s%n"
                    .formatted(order, inList));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChartList chartList = (ChartList) o;
        return charts.equals(chartList.charts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(charts);
    }

}
