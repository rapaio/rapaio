/*
 * Apache License
 * Version 2.0, January 2004
 * http://www.apache.org/licenses/
 *
 *    Copyright 2013 - 2025 Aurelian Tutuianu
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

package rapaio.darray.iterators;

import java.util.NoSuchElementException;

import rapaio.darray.Order;
import rapaio.darray.layout.StrideLayout;

public final class StridePointerIterator implements PointerIterator {

    private final StrideLayout c;
    private final int[] index;
    private int position = 0;
    private int ptr;

    public StridePointerIterator(StrideLayout layout, Order askOrder) {
        this(layout, askOrder, true);
    }

    public StridePointerIterator(StrideLayout layout, Order askOrder, boolean compact) {
        c = layout.computeFortranLayout(askOrder, compact);

        this.index = new int[c.rank()];
        this.ptr = c.offset();
    }

    @Override
    public boolean hasNext() {
        return position < c.size();
    }

    @Override
    public int nextInt() {
        if (position >= c.size()) {
            throw new NoSuchElementException();
        }
        int currentPtr = ptr;
        position++;
        int i = 0;
        if (c.rank() > 0) {
            index[i]++;
            ptr += c.stride(i);
        }
        while (i < c.strides().length) {
            if (index[i] == c.dim(i)) {
                index[i] = 0;
                ptr -= c.dim(i) * c.stride(i);
                if (i < c.rank() - 1) {
                    index[i + 1]++;
                    ptr += c.stride(i + 1);
                }
                i++;
                continue;
            }
            break;
        }
        return currentPtr;
    }

    @Override
    public int position() {
        return position - 1;
    }

    @Override
    public int size() {
        return c.size();
    }
}
