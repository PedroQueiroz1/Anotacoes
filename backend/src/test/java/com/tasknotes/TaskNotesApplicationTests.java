package com.tasknotes;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@Disabled("Requires a running MySQL database — not available in local unit test environment")
@SpringBootTest
class TaskNotesApplicationTests {

    @Test
    void contextLoads() {
    }
}
