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

import java.util.PrimitiveIterator;

/**
 * Iterator over pointers. A pointer is a unidimensional index into the storage. Each NArray stores its data into
 * a contiguous block of memory.
 * <p>
 * Pointers are used to access the tensor data in a more direct way.
 * <p>
 * The iterator is backed by a {@link PrimitiveIterator.OfInt}
 */
public interface PointerIterator extends PrimitiveIterator.OfInt {

    /**
     * Returns position for the corresponding pointer.
     *
     * @return current position
     */
    int position();

    /**
     * @return the total number of elements in the iterator
     */
    int size();
}

