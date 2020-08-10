package crawler;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.openqa.selenium.By.xpath;
import static org.openqa.selenium.support.ui.ExpectedConditions.visibilityOfElementLocated;

public final class BankCrawler {

    private final char[] password;
    private final String clientId;
    private final WebDriver driver;
    private final Map<String, String> data = new HashMap<>();

    private BankCrawler(char[] password, String clientId) {
        this.clientId = clientId;
        this.password = password;
        driver = new ChromeDriver();
        driverAdditionalConfig();
    }

    private void driverAdditionalConfig() {
        driver.manage().window().maximize();
    }

    public final static BankCrawler getInstance(char[] password, String clientId) {
        return new BankCrawler(password, clientId);
    }

    //main facade
    public final void crawl() {
        login();
        checkAndCloseModals();
        getDataFromAccount();
        //takeScreenshot();
    }

    //taking screenshot of main banking account view
    private final void takeScreenshot() {
        File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
        try {
            Files.copy(screenshot.toPath(),
                    Paths.get(System.getProperty("user.home"),
                            "Documents", "selenium_tutorial", "screens", "test"
                                    + System.currentTimeMillis()/10000 + ".png"));
        } catch (IOException e) {
            System.out.println("Unable to capture screenshot, with reason: " + e.getMessage());
        }
    }

    private final void login() {

        //going to login page
        driver.get("https://www.pekao24.pl/logowanie");

        //customer id menu
        driver.findElement(xpath("//*[@id=\"customer\"]")).sendKeys(clientId);
        driver.findElement(xpath("//pekao-login-customer//pekao-button/button[text()='Dalej']")).click();

        waitForCondition(visibilityOfElementLocated(xpath("//button[text()='Zaloguj']")));

        //password fields
        List<WebElement> elements = driver.findElements(xpath("//*[@id=\"password\"]/div"));

        //populating password active fields
        for (int i = 0; i < elements.size(); i++) {
            if (elements.get(i).getAttribute("class").contains("wrapper-active")) {
                elements.get(i).findElement(By.tagName("input")).sendKeys(Character.toString(password[i]));
            }
        }

        //login
        driver.findElement(xpath("//button[text()='Zaloguj']")).click();
    }

    //closing pop-up modals
    private final void checkAndCloseModals() {
        waitForCondition(visibilityOfElementLocated(By.tagName("pekao-main-product-transactions")));
        List<WebElement> modalClose = driver
                .findElements(xpath("//pekao-modal-close-button//button[@aria-label='Zamknij']"));

        //closing optional modal
        if (!modalClose.isEmpty()) {
            modalClose.forEach(m -> m.click());
        }
    }

    private final void getDataFromAccount() {
        List<WebElement> section = driver.findElements(xpath("//main/section"));

        if (section.isEmpty()) {
            System.out.println("Error during data acquisition");
            return;
        }

        //main banking product
        getNameAndValueFromSection(section.get(0));

        //side products, filtering out loan section that will be processed in the next operation
        driver.findElements(xpath("//main/pekao-dashboard-section-box")).stream()
                .filter(e -> e.findElements(xpath("./pekao-loan-product-section-box")).isEmpty())
                .forEach(e -> getNameAndValueFromSection(e));

        //loans
        driver.findElements(xpath("//main//pekao-loan-product-section-box/section"))
                .forEach(s -> getNameAndValueFromSection(s));
    }

    private void getNameAndValueFromSection(WebElement section) {
        List<WebElement> name = section.findElements(xpath(".//span[@class='ellipsis']"));
        List<WebElement> value = section.findElements(xpath(".//p[@class='balance']"));

        //sometimes section is used as an advert, this check should filter unnecessary sections.
        if (!name.isEmpty() && !value.isEmpty()) {
            data.put(name.get(0).getText(), value.get(0).getText());
        }
    }

    private final void waitForCondition(ExpectedCondition<WebElement> condition) {
        WebDriverWait wait = new WebDriverWait(driver, 7);
        wait.pollingEvery(Duration.ofNanos(500L));
        wait.until(condition);
    }

    public final Map<String, String> getData() {
        return new HashMap<>(data);
    }
}
