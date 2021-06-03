package com.ms.silverking.cloud.dht.client.example;

import java.io.IOException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.Test;

import com.ms.silverking.cloud.dht.client.ClientException;
import com.ms.silverking.cloud.dht.gridconfig.SKGridConfiguration;
import com.ms.silverking.testing.Util;

import com.ms.silverking.testing.annotations.SkLarge;

@SkLarge
public class MultiDHTExampleTest {

    private static MultiDHTExample mDht;

    @BeforeAll
    public static void setUpBeforeClass() throws IOException {
        if (!Util.isSetSkipMultiMachineTests())
            setup();
    }
    
    private static void setup() throws IOException {
        SKGridConfiguration gc = Util.getTestGridConfig();
        SKGridConfiguration[] gridConfigurations = {gc,                gc};
        String[] preferredServers                = {Util.getServer1(), Util.getServer2()};
        mDht = new MultiDHTExample(gridConfigurations, preferredServers);
    }

    @Test(timeout=10_000)
    public void testOneGcOneServer_OneIteration() throws IOException, ClientException {
        startTest(1);
    }

    @Test(timeout=10_000)
    public void testOneGcOneServer_TenIterations() throws IOException, ClientException {
        startTest(10);
    }
    
    private void startTest(int iterations) throws ClientException, IOException {
        if (!Util.isSetSkipMultiMachineTests())
            mDht.start(iterations);
    }
    
    public static void main(String[] args) {
        Util.runTests(MultiDHTExampleTest.class);
    }

}
