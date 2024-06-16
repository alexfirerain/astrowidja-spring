package ru.swetophor.astrowidjaspring.mainframe;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import ru.swetophor.astrowidjaspringshell.client.UserController;
import ru.swetophor.astrowidjaspringshell.model.*;
import ru.swetophor.astrowidjaspringshell.service.HarmonicService;
import ru.swetophor.astrowidjaspringshell.service.LibraryService;

import static ru.swetophor.astrowidjaspringshell.config.Settings.*;
import static ru.swetophor.astrowidjaspringshell.config.Settings.getAutoloadFile;
import static ru.swetophor.astrowidjaspringshell.config.Settings.isAutoloadEnabled;
import static ru.swetophor.astrowidjaspringshell.config.Settings.isAutosave;
import static ru.swetophor.astrowidjaspringshell.utils.Decorator.*;
import static ru.swetophor.astrowidjaspringshell.utils.Decorator.print;

@Component
@Getter
@RequiredArgsConstructor
public class Main {
    public final ChartList DESK = new ChartList("Стол Астровидьи");

    private final UserController userController;
    private final LibraryService libraryService;
    private final HarmonicService harmonicService;

    public static final AstroSet DEFAULT_ASTRO_SET = new AstroSet(AstraEntity.values());


    @EventListener(ApplicationReadyEvent.class)
    public void runAstrowidja() {
        userController.welcome();

        if (isAutoloadEnabled())
            print(libraryService.loadAlbum(getAutoloadFile(), DESK));

        userController.mainCycle(this);

        if (isAutosave())
            print(libraryService.autosave(DESK));
    }




}
