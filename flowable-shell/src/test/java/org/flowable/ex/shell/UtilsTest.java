package org.flowable.ex.shell;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.shell.Shell;
import org.springframework.shell.jline.InteractiveShellApplicationRunner;

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipFile;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {InteractiveShellApplicationRunner.SPRING_SHELL_INTERACTIVE_ENABLED + "=" + false})
public class UtilsTest {

    @Autowired
    Shell shell;

    @Test
    void setFullConfiguration() {
        assertThat((String) shell.evaluate(() -> "configure login password restUrl")).
                isEqualTo("login@restUrl/");
        assertThat((String) shell.evaluate(() -> "configure --password password --rest-url restUrlUpdate")).
                isEqualTo("login@restUrlUpdate/");
        assertThat((String) shell.evaluate(() -> "configure admin test http://localhost:8080/flowable-ui/app-api/")).
                as("restore configuration").isEqualTo("admin@http://localhost:8080/flowable-ui/app-api/");
    }

    @Test
    void zipUnZip() {
        shell.evaluate(() -> "unzip src/test/resources/app.zip target/test/app");
        shell.evaluate(() -> "zip target/test/app target/test-app.zip");

        File testZipFile = new File("target/test-app.zip");
        File sourceZipFile = new File("src/test/resources/app.zip");
        assertThat(testZipFile).hasSize(sourceZipFile.length());

        testZipFile.delete();
        (new File("target/test/app")).delete();
    }

}
