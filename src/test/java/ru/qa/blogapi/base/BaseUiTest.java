package ru.qa.blogapi.base;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public abstract class BaseUiTest {

    protected WebDriver driver;
    protected String uiBaseUrl;
    protected String apiBaseUrl;

    @BeforeEach
    void setUpUi() {
        Properties properties = new Properties();

        try (InputStream inputStream = BaseUiTest.class
                .getClassLoader()
                .getResourceAsStream("application.properties")) {

            if (inputStream == null) {
                throw new IllegalStateException("application.properties not found");
            }

            properties.load(inputStream);
            uiBaseUrl = properties.getProperty("ui.base.url");
            apiBaseUrl = properties.getProperty("api.base.url");

        } catch (IOException e) {
            throw new RuntimeException("Failed to load application.properties", e);
        }

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--start-maximized");

        driver = new ChromeDriver(options);
    }

    @AfterEach
    void tearDownUi() {
        if (driver != null) {
            driver.quit();
        }
    }
}