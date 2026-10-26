import java.net.URL;
import java.time.Duration;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class loginTest {
    private static final By USERNAME = By.id("username");
    private static final By PASSWORD = By.id("password");
    private static final By LOGIN_BUTTON = By.id("loginButton");
    private static final By MESSAGE = By.id("message");

    private static int passedTests = 0;
    private static int totalTests = 0;

    public static void main(String[] args) throws Exception {
        URL loginPage = loginTest.class.getResource("/login.html");
        if (loginPage == null) {
            throw new IllegalStateException(
                "login.html was not found. Make sure it is inside src/main/resources."
            );
        }

        ChromeOptions options = new ChromeOptions();
        String headlessEnv = System.getenv("SELENIUM_HEADLESS");
        if (Boolean.getBoolean("selenium.headless")
                || "true".equalsIgnoreCase(headlessEnv)) {
            options.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage");
        }

        WebDriver driver = new ChromeDriver(options);
        try {
            String loginPageUrl = loginPage.toExternalForm();
            System.out.println("Login page: " + loginPageUrl);

            // Test 1: correct username and correct password
            runLoginScenario(
                driver, loginPageUrl,
                "admin", "admin123",
                "Login successful. Welcome, admin!",
                "TEST 1 - VALID USERNAME AND PASSWORD"
            );

            // Test 2: correct username, wrong password
            runLoginScenario(
                driver, loginPageUrl,
                "admin", "wrongpass",
                "Login failed. Invalid username or password.",
                "TEST 2 - INVALID PASSWORD"
            );

            // Test 3: wrong username, correct password
            runLoginScenario(
                driver, loginPageUrl,
                "wronguser", "admin123",
                "Login failed. Invalid username or password.",
                "TEST 3 - INVALID USERNAME"
            );

            // Test 4: both fields empty; HTML required validation should block submission
            runEmptyFieldsScenario(driver, loginPageUrl);
        } finally {
            driver.quit();
        }

        System.out.println("\n========== TEST SUMMARY ==========");
        System.out.println("Tests passed: " + passedTests + "/" + totalTests);
        System.out.println("Tests failed: " + (totalTests - passedTests));
        if (passedTests == totalTests) {
            System.out.println("RESULT: ALL TESTS PASSED");
        } else {
            System.out.println("RESULT: SOME TESTS FAILED");
            throw new AssertionError("One or more login tests failed.");
        }
    }

    private static void runLoginScenario(
            WebDriver driver,
            String loginPageUrl,
            String username,
            String password,
            String expectedMessage,
            String scenarioName) {

        totalTests++;
        driver.get(loginPageUrl);
        driver.findElement(USERNAME).sendKeys(username);
        driver.findElement(PASSWORD).sendKeys(password);
        driver.findElement(LOGIN_BUTTON).click();

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
        String actualMessage = wait.until(currentDriver -> {
            WebElement messageElement = currentDriver.findElement(MESSAGE);
            String text = messageElement.getText().trim();
            return text.isEmpty() ? null : text;
        });

        boolean passed = expectedMessage.equals(actualMessage);
        recordResult(scenarioName, passed,
            "message: " + expectedMessage,
            "message: " + actualMessage);
    }

    private static void runEmptyFieldsScenario(WebDriver driver, String loginPageUrl) {
        totalTests++;
        driver.get(loginPageUrl);

        WebElement usernameField = driver.findElement(USERNAME);
        WebElement passwordField = driver.findElement(PASSWORD);
        driver.findElement(LOGIN_BUTTON).click();

        JavascriptExecutor js = (JavascriptExecutor) driver;
        boolean usernameIsRequiredAndEmpty = (Boolean) js.executeScript(
            "return arguments[0].required && arguments[0].validity.valueMissing;",
            usernameField
        );
        boolean passwordIsRequiredAndEmpty = (Boolean) js.executeScript(
            "return arguments[0].required && arguments[0].validity.valueMissing;",
            passwordField
        );
        String actualMessage = driver.findElement(MESSAGE).getText().trim();
        boolean loginWasNotSubmitted = actualMessage.isEmpty();
        boolean passed = usernameIsRequiredAndEmpty
                      && passwordIsRequiredAndEmpty
                      && loginWasNotSubmitted;

        recordResult(
            "TEST 4 - EMPTY USERNAME AND PASSWORD",
            passed,
            "both fields required; submission blocked; no login message",
            "username blocked=" + usernameIsRequiredAndEmpty
                + ", password blocked=" + passwordIsRequiredAndEmpty
                + ", message='" + actualMessage + "'"
        );
    }

    private static void recordResult(
            String scenarioName, boolean passed, String expected, String actual) {
        if (passed) {
            passedTests++;
        }
        System.out.println("\n[" + scenarioName + "] " + (passed ? "PASS" : "FAIL"));
        System.out.println("  Expected: " + expected);
        System.out.println("  Actual:   " + actual);
    }
}
