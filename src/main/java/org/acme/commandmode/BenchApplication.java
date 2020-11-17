package org.acme.commandmode;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain
public class BenchApplication implements QuarkusApplication {

    @Override
    public int run(String... args) throws Exception {
        System.err.println("Wait for exit");
        Quarkus.waitForExit();
        System.err.println("Wait for exit DONE");
        return 0;
    }
}
