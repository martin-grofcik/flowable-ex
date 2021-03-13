package org.flowable.ex.shell;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.shell.Shell;
import org.springframework.shell.jline.InteractiveShellApplicationRunner;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = { InteractiveShellApplicationRunner.SPRING_SHELL_INTERACTIVE_ENABLED + "=" + false })
public class ExampleIT {
    @Autowired
    private Shell shell;

    @Test
    void deployModel() throws IOException {
        //import app model to be able to export and deploy it
        shell.evaluate(() -> "import --input-file-name src/test/resources/app.zip");

        //export model, unzip/zip bar, deploy
        shell.evaluate(() -> "export-bar --name app --output-file-name target/test/app.bar");
        shell.evaluate(() -> "unzip target/test/app.bar target/test/app");
        shell.evaluate(() -> "zip target/test/app target/test/app-out.bar");
        shell.evaluate(() -> "deploy target/test/app-out.bar");

        //clean up
        shell.evaluate(() -> "rmd app-out");

        assertThat(shell.evaluate(() -> "ls app").toString()).
                contains("\"size\":1");
        shell.evaluate(() -> "rm app");
        assertThat(shell.evaluate(() -> "ls app").toString()).
                contains("\"size\":0");

        assertThat(shell.evaluate(() -> "ls oneTaskProcess bpmn").toString()).
                contains("\"size\":1");
        shell.evaluate(() -> "rm oneTaskProcess bpmn");
        assertThat(shell.evaluate(() -> "ls oneTaskProcess bpmn").toString()).
                contains("\"size\":0");
        FileUtils.deleteDirectory(new File("target/test"));
    }

}
