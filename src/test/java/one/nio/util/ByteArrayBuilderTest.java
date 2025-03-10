/*
 * Copyright 2025 VK
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package one.nio.util;

import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

public class ByteArrayBuilderTest {

    @Test
    public void appendInt() {
        check(0);
        check(1);
        check(-1);
        check(64);
        check(100);
        check(999999);
        check(-1000000);
        check(Integer.MAX_VALUE);
        check(Integer.MIN_VALUE);
    }

    private void check(int n) {
        byte[] expected = Integer.toString(n).getBytes(StandardCharsets.UTF_8);
        byte[] actual = new ByteArrayBuilder().append(n).toBytes();
        Assert.assertArrayEquals(expected, actual);
    }
}
