package ru.swetophor.astrowidjaspring.mainframe;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import ru.swetophor.astrowidjaspring.client.UserController;
import ru.swetophor.astrowidjaspring.config.Settings;
import ru.swetophor.astrowidjaspring.model.astro.AstraEntity;
import ru.swetophor.astrowidjaspring.model.astro.AstroSet;
import ru.swetophor.astrowidjaspring.model.chart.ChartList;
import ru.swetophor.astrowidjaspring.model.chart.ChartObject;
import ru.swetophor.astrowidjaspring.service.HarmonicService;
import ru.swetophor.astrowidjaspring.service.LibraryService;

import static ru.swetophor.astrowidjaspring.config.Settings.*;
import static ru.swetophor.astrowidjaspring.mainframe.ActiveScreen.MAIN;
import static ru.swetophor.astrowidjaspring.utils.Decorator.print;

/**
 * Сценарий исполнения АстроВидьи.
 */
@Component
@Getter
@Setter
@RequiredArgsConstructor
public class Main {

    /**
     * Компонент, отвечающий за взаимодействие с пользователем.
     */
    private final UserController userController;
    /**
     * Компонент, отвечающий за управление картами.
     */
    private final LibraryService libraryService;
    /**
     * Компонент, отвечающий за гармонические расчёты.
     */
    private final HarmonicService harmonicService;
    /**
     * Стандартный набор астр.
     */
    public static final AstroSet DEFAULT_ASTRO_SET = new AstroSet(AstraEntity.values());

    /**
     * Рабочий Стол, то есть загруженный в память список карт,
     * с которого можно брать карты в анализ или использовать
     * для получения новых карт (или многокарт).
     */
    public final ChartList DESK = new ChartList("Стол Астровидьи");
    /**
     * Какой из пользовательских экранов активен в данный момент.
     */
    private ActiveScreen activeScreen = MAIN;
    /**
     * Какая карта (или многокарта) заряжена для анализа.
     */
    public ChartObject activeChart = null;


    /**
     * Сценарий исполнения сеанса программы.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void runAstrowidja() {
        if (isAutoloadEnabled())
            print(libraryService.loadAlbum(getAutoloadFile(), DESK));

        userController.mainCycle(this);

        if (isAutosave())
            print(libraryService.autosave(DESK));

        Settings.saveSettings();
    }

}
