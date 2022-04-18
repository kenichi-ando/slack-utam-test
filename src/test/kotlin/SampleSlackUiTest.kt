import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import utam.core.driver.DriverType
import utam.core.framework.consumer.UtamLoaderConfigImpl
import utam.core.framework.consumer.UtamLoaderImpl
import utam.core.selenium.factory.WebDriverFactory
import utam.slack.pageobjects.Client
import utam.slack.pageobjects.Login
import utam.slack.pageobjects.Redirect
import utam.slack.pageobjects.Search
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SampleSlackUiTest {
    private val driver = WebDriverFactory.getWebDriver(DriverType.chrome)
    private val loader = UtamLoaderImpl(UtamLoaderConfigImpl(), driver)
    private val props = javaClass.classLoader.getResourceAsStream("env.properties").use {
        Properties().apply { load(it) }
    }

    @BeforeAll
    fun initialize() {
        driver.get(props.getProperty("slack.workspace.url"))
        loader.load(Login::class.java).login(props.getProperty("slack.user.email"), props.getProperty("slack.user.password"))
        loader.load(Redirect::class.java).waitAndClick()
    }

    @AfterAll
    fun terminate() {
        driver.quit()
    }

    @Test
    fun postMessage() {
        val client = loader.load(Client::class.java)
        for (message in arrayOf("Hello", "こんにちは")) {
            client.messageInput.clearAndType(message)
            client.textSendButton.click()
            assertEquals(message, client.messageTexts.last().text)
            Thread.sleep(200)
        }
        Thread.sleep(2000)
    }

    @Test
    fun searchOnTopNavigation() {
        val client = loader.load(Client::class.java)
        for (message in arrayOf("Hello", "こんにちは")) {
            client.topNavSearch.click()
            loader.load(Search::class.java).search(message)
            for (result in client.waitForSearchResults()) {
                println(result.text)
                assertTrue(result.text.uppercase().contains(message.uppercase()))
            }
            Thread.sleep(2000)
        }
        client.searchPageCloseButton.click()
    }
}
