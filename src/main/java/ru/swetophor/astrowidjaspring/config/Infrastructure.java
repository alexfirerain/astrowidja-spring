package ru.swetophor.astrowidjaspring.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Infrastructure {
    @Value("${astrowidja.working-dir}")
    private String workDir;
    @Value("${astrowidja.settings}")
    private String settings;
    @Value("${astrowidja.base-dir}")
    private String baseDir;
    @Value("${astrowidja.reports-dir}")
    private String reportsDir;


    @Bean
    @Qualifier("working-dir")
    public String getWorkingDirFromSettings() {
        return workDir;
    }

    @Bean
    @Qualifier("settings")
    public String getSettingsFileFromSettings() {
        return settings;
    }

    @Bean
    @Qualifier("base-dir")
    public String getBaseDirFromSettings() {
        return baseDir;
    }

    @Bean
    @Qualifier("reports-dir")
    public String getReportsDirFromSettings() {
        return reportsDir;
    }


}
