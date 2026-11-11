package org.microg;

import com.squareup.wire.ProtoReader;
import java.io.IOException;
import java.util.Arrays;
import okio.Buffer;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

/** Regression checks for malformed protobuf group handling in the shipped Wire runtime. */
public class ProtoReaderSecurityTest {
    private static ProtoReader reader(int... bytes) throws IOException {
        Buffer input = new Buffer();
        for (int value : bytes) input.writeByte(value);
        ProtoReader reader = new ProtoReader(input);
        reader.beginMessage();
        return reader;
    }

    @Test public void validUnknownGroupPreservesFollowingField() throws IOException {
        ProtoReader normal = reader(0x0b, 0x12, 0x01, 0x2a, 0x0c, 0x18, 0x01);
        assertEquals(3, normal.nextTag());
        assertEquals(1, normal.readVarint32());
    }

    @Test public void rejectsNegativeLengthInsideGroup() {
        assertThrows(IOException.class, () -> reader(0x0b, 0x12, 0xff, 0xff, 0xff, 0xff, 0x0f, 0x0c).nextTag());
    }

    @Test public void rejectsTruncatedOversizedGroupField() {
        assertThrows(IOException.class, () -> reader(0x0b, 0x12, 0xff, 0xff, 0xff, 0xff, 0x07, 0x0c).nextTag());
    }

    @Test public void rejectsExcessiveGroupNesting() {
        int[] nested = new int[256];
        Arrays.fill(nested, 0, 128, 0x0b);
        Arrays.fill(nested, 128, 256, 0x0c);
        assertThrows(IOException.class, () -> reader(nested).nextTag());
    }
}
