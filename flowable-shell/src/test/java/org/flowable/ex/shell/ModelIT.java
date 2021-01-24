package org.flowable.ex.shell;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.shell.Shell;
import org.springframework.shell.jline.InteractiveShellApplicationRunner;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = { InteractiveShellApplicationRunner.SPRING_SHELL_INTERACTIVE_ENABLED + "=" + false })
public class ModelIT {
    @Autowired
    Shell shell;

    @Test
    void importExportApp() {
        File outFile = new File("target/outputFile.zip");
        try {
            shell.evaluate(() -> "import --input-file-name src/test/resources/app.zip");

            assertThat(shell.evaluate(() -> "list app").toString()).contains("\"name\":\"app\"");

            shell.evaluate(() -> "export --name app --output-file-name target/outputFile.zip");

            assertThat(outFile).exists();
        } finally {
            if (outFile.exists()) {
                if (!outFile.delete()) {
                    System.err.println("Unable to delete file");
                }
            }
            shell.evaluate(() -> "rm app");
            assertThat(shell.evaluate(() -> "ls app").toString()).
                    contains("\"size\":0");

        }
    }
}
