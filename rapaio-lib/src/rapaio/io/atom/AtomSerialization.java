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

package rapaio.io.atom;

import java.lang.reflect.ParameterizedType;

public abstract class AtomSerialization<T> {

    private final Class<T> persistentClass;

    @SuppressWarnings("unchecked")
    public AtomSerialization() {
        this.persistentClass = (Class<T>) ((ParameterizedType) getClass()
                .getGenericSuperclass()).getActualTypeArguments()[0];
    }

    public final Class<T> getClassType() {
        return persistentClass;
    }

    public abstract LoadAtomHandler<? extends T> loadAtomHandler();

    public abstract SaveAtomHandler<? extends T> saveAtomHandler();
}