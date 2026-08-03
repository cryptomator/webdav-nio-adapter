package org.cryptomator.frontend.webdav.mount;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class WindowsMounterTest {

    @DisplayName("Drive letter in net use output is matched")
    @ParameterizedTest
    @CsvSource( value = {
        "täxt Z: text, Z:",
        "X: ㅉext, X:",
        "tex𐤀 M:, M:",
        "L:, L:",
     })
    void testParseSystemChosenMountpoint(String input, String expected) {
        var result = Assertions.assertDoesNotThrow(() -> WindowsMounter.parseDriveLetter(input));
        Assertions.assertEquals(expected, result);
    }
}
