package com.akif;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import com.akif.testsupport.TestcontainersConfiguration;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
public class CarRentalApplicationTests {

    @Test
    void contextLoads() {
    }

}
