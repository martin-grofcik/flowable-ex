package org.flowable.ex.shell;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.shell.Shell;
import org.springframework.shell.jline.InteractiveShellApplicationRunner;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {InteractiveShellApplicationRunner.SPRING_SHELL_INTERACTIVE_ENABLED + "=" + false})
public class ConfigurationTest {

    @Autowired
    Shell shell;

    @Test
    void setFullConfiguration() {
        assertThat((String) shell.evaluate(() -> "configure login password restUrl")).
                isEqualTo("login@restUrl");
        assertThat((String) shell.evaluate(() -> "configure --password password --rest-url restUrlUpdate")).
                isEqualTo("login@restUrlUpdate/");
        assertThat((String) shell.evaluate(() -> "configure admin test http://localhost:8080/flowable-ui/app-api/")).
                as("restore configuration").isEqualTo("admin@http://localhost:8080/flowable-ui/app-api/");
    }
}
